package com.ahanda.techops.noty.http;

import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpMethod.POST;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.util.CharsetUtil;
import io.netty.util.concurrent.EventExecutorGroup;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ahanda.techops.noty.Utils;
import com.ahanda.techops.noty.db.MongoDBManager;
import com.ahanda.techops.noty.http.exception.NotyException;
import com.ahanda.techops.noty.http.message.FullEncodedResponse;
import com.ahanda.techops.noty.http.message.Request;
import com.fasterxml.jackson.core.type.TypeReference;

/**
 * 
 * @author Gautam This class is used to decode the request into FullHttpRequest object. This class will internally handle the form/json or any other form of request. If payload is
 *         json (content type : json), it will be parsed using Jackson's JSON decoder, for form handling it will be handled accordingly
 */
public class ServerHandler extends SimpleChannelInboundHandler<Request>
{
	private final EventExecutorGroup executor;

	String clientId;

	String username;

	static final Logger l = LoggerFactory.getLogger(ServerHandler.class);

	public ServerHandler(EventExecutorGroup e)
	{
		super(false);
		this.executor = e;
	}

	@Override
	protected void channelRead0(final ChannelHandlerContext ctx, final Request request) throws Exception
	{
		l.debug("Received request for {}", request.getHttpRequest());

		HttpResponseStatus status = validateReq(ctx, request);
		if (status != HttpResponseStatus.ACCEPTED)
		{
			sendResponse(ctx, request, status);
			return;
		}

		handleReq(ctx, request);
	}

	/*
	 * This functions checks if proper cookie or auth key is available in the headers or not. If not present request will not be granted. This will also check for the method too
	 */
	private HttpResponseStatus validateReq(final ChannelHandlerContext ctx, final Request request)
	{
		FullHttpRequest httpRequest = request.getHttpRequest();
		if (httpRequest.getMethod() != POST)
			return HttpResponseStatus.METHOD_NOT_ALLOWED;

		/**
		 * TODO : Check for headers here to finalize the request
		 */
		return HttpResponseStatus.ACCEPTED;
	}

	private void handleReq(final ChannelHandlerContext ctx, final Request request) throws Exception
	{
		List<String> paths = new LinkedList<String>(Arrays.asList(request.getRequestPath().split("/")));

		l.debug("paths {}", paths);
		if (paths.size() > 0 && paths.get(0).isEmpty()) // leading '/'
			paths.remove(0);

		String cpath = "";
		if (paths.size() > 0)
		{
			cpath = paths.get(0);
			paths.remove(0);
		}
		switch (cpath)
		{
		case "login":
			handleLogin(paths, ctx, request);
			break;
		case "events":
			handleEvents(paths, ctx, request);
			break;
		case "users":
			handleUsers(paths, ctx, request);
			break;
		default:
			l.error("Invalid resource requested {}", cpath);
			throw new NotyException(HttpResponseStatus.NOT_FOUND, "Invalid resource requested : " + cpath);
		}

	}

	private void handleUsers(final List<String> paths, final ChannelHandlerContext ctx, final Request request) throws NotyException
	{
		if (paths.isEmpty())
		{
			pubUsers(ctx, request);
			return;
		}

		String cpath = paths.get(0);
		switch (cpath)
		{
		case "get":
			getUsers(ctx, request);
			break;
		default:
			l.error("Invalid user resource requested {}", cpath);
			throw new NotyException(HttpResponseStatus.NOT_FOUND, "Invalid event resource requested : " + cpath);
		}
	}

	private void pubUsers(final ChannelHandlerContext ctx, final Request request) throws NotyException
	{
		l.debug("received request for publishing user !!!!");
		FullHttpRequest httpRequest = request.getHttpRequest();

		String jsonString = httpRequest.content().toString(CharsetUtil.UTF_8);
		final Map<String, Object> userConf;
		try
		{
			userConf = Utils.om.readValue(jsonString, new TypeReference<HashMap<String, Object>>()
			{
			});
		}
		catch (Exception e)
		{
			l.error("Exception while json handling", e);
			throw new NotyException(request, HttpResponseStatus.UNPROCESSABLE_ENTITY, e);
		}

		Future<Boolean> future = executor.submit(new Callable<Boolean>()
		{
			@Override
			public Boolean call() throws Exception
			{
				try
				{
					MongoDBManager.getInstance().execOp(userConf);
					return true;
				}
				catch (Exception e)
				{
					l.info("Error while inserting events!");
					return false;
				}
			}
		});
		future.addListener(new GenericFutureListener<Future<Boolean>>()
		{
			@Override
			public void operationComplete(Future<Boolean> future) throws Exception
			{
				boolean insertSuccess = future.get();
				if (future.isSuccess() && insertSuccess)
				{
					sendResponse(ctx, request, OK);
				}
				else
				{
					ctx.fireExceptionCaught(new NotyException(request, HttpResponseStatus.UNPROCESSABLE_ENTITY, future.cause()));
				}
			}
		});
	}

	private void getUsers(final ChannelHandlerContext ctx, final Request request) throws NotyException
	{
		FullHttpRequest httpRequest = request.getHttpRequest();

		final Map<String, Object> query;
		try
		{
			query = Utils.om.readValue(httpRequest.content().toString(CharsetUtil.UTF_8), new TypeReference<Map<String, Object>>()
			{
			});
		}
		catch (Exception e)
		{
			l.error("Exception while json handling", e);
			throw new NotyException(request, HttpResponseStatus.UNPROCESSABLE_ENTITY, e);
		}

		Future<String> future = executor.submit(new Callable<String>()
		{
			@Override
			public String call() throws Exception
			{
				return MongoDBManager.getInstance().getEvent(query);
			}
		});
		future.addListener(new GenericFutureListener<Future<String>>()
		{
			@Override
			public void operationComplete(Future<String> future) throws Exception
			{
				String event = future.get();
				if (future.isSuccess() && event != null)
				{
					sendJsonResponse(ctx, request, OK, event);
				}
				else
				{
					ctx.fireExceptionCaught(new NotyException(request, HttpResponseStatus.UNPROCESSABLE_ENTITY, future.cause()));
				}
			}
		});
	}

	// see user-agent header, script => just authenticate
	// For desktop-ui, send user info as well
	private void handleLogin(final List<String> paths, final ChannelHandlerContext ctx, final Request request)
	{
		assert paths.isEmpty();
	}

	private void handleEvents(final List<String> paths, final ChannelHandlerContext ctx, final Request request) throws NotyException
	{
		if (paths.isEmpty())
		{
			pubEvents(ctx, request);
			return;
		}

		String cpath = paths.get(0);

		switch (cpath)
		{
		case "get":
			getEvents(ctx, request);
			break;
		default:
			l.error("Invalid event resource requested {}", cpath);
			throw new NotyException(HttpResponseStatus.NOT_FOUND, "Invalid event resource requested : " + cpath);
		}
	}

	private void getEvents(final ChannelHandlerContext ctx, final Request request) throws NotyException
	{
		FullHttpRequest httpRequest = request.getHttpRequest();

		final Map<String, Object> matcher;
		try
		{
			matcher = Utils.om.readValue(httpRequest.content().toString(CharsetUtil.UTF_8), new TypeReference<HashMap<String, Object>>()
			{
			});
		}
		catch (Exception e)
		{
			l.error("Exception while creating a map from output", e);
			throw new NotyException(request, HttpResponseStatus.UNPROCESSABLE_ENTITY, e);
		}

		Future<Map<String, Object>> future = executor.submit(new Callable<Map<String, Object>>()
		{
			@Override
			public Map<String, Object> call() throws Exception
			{
				Map<String, Object> op = new LinkedHashMap<String, Object>();
				op.put("action", "find");
				op.put("db", "pint");
				op.put("collection", "events");
				op.put("matcher", matcher);
				return MongoDBManager.getInstance().execOp(op);
			}
		});
		future.addListener(new GenericFutureListener<Future<Map<String, Object>>>()
		{
			@Override
			public void operationComplete(Future<Map<String, Object>> future) throws Exception
			{
				if (!future.isSuccess())
				{
					ctx.fireExceptionCaught(new NotyException(request, HttpResponseStatus.UNPROCESSABLE_ENTITY, future.cause()));
					return;
				}

				Map<String, Object> event = future.get();

				HttpResponseStatus resp = HttpResponseStatus.OK;
				String msg;
				if (event == null)
				{
					resp = HttpResponseStatus.INTERNAL_SERVER_ERROR;
					msg = "Internal Server Error";
					sendResponse(ctx, request, resp);
				}
				else if (!event.get("status").equals("ok"))
				{
					resp = HttpResponseStatus.INTERNAL_SERVER_ERROR;
					msg = (String) event.get("message");
					sendResponse(ctx, request, resp);
				}
				else
				{
					resp = HttpResponseStatus.OK;
					msg = event.get("results").toString();
					sendJsonResponse(ctx, request, resp, msg);
				}
			}
		});
	}

	private void pubEvents(final ChannelHandlerContext ctx, final Request request) throws NotyException
	{
		FullHttpRequest httpRequest = request.getHttpRequest();
		l.debug("received request for publishing message !!!! " );
		final List<Object> eventList;
		try
		{
			String jsonString = httpRequest.content().toString(CharsetUtil.UTF_8);
			l.debug( "eventlist body {}", jsonString );
			eventList = Utils.om.readValue(jsonString, new TypeReference<ArrayList<Object>>()
			{
			});
		}
		catch (Exception e)
		{
			l.error("Exception while json handling", e);
			throw new NotyException(request, HttpResponseStatus.UNPROCESSABLE_ENTITY, e);
		}
		Future<Void> future = executor.submit(new Callable<Void>()
		{
			@Override
			public Void call() throws JSONException, Exception
			{
				Map<String, Object> op = new LinkedHashMap<String, Object>();
				op.put("action", "save");
				op.put("db", "pint");
				op.put("collection", "events");
				op.put("document", eventList);
				MongoDBManager.getInstance().execOp(op);
				return null;
			}
		});
		future.addListener(new GenericFutureListener<Future<Void>>()
		{
			@Override
			public void operationComplete(Future<Void> future) throws Exception
			{
				if (future.isSuccess())
				{
					sendResponse(ctx, request, HttpResponseStatus.OK);
				}
				else
				{
					ctx.fireExceptionCaught(new NotyException(request, HttpResponseStatus.UNPROCESSABLE_ENTITY, future.cause()));
				}
			}
		});
	}

	private void sendResponse(ChannelHandlerContext ctx, Request request, HttpResponseStatus status)
	{
		FullHttpResponse httpResponse = new DefaultFullHttpResponse(HTTP_1_1, status);
		FullEncodedResponse encodedResponse = new FullEncodedResponse(request, httpResponse);
		ctx.writeAndFlush(encodedResponse);
	}

	private void sendJsonResponse(ChannelHandlerContext ctx, Request request, HttpResponseStatus status, String content)
	{
		FullHttpResponse httpResponse = new DefaultFullHttpResponse(HTTP_1_1, OK, Unpooled.copiedBuffer(content, CharsetUtil.UTF_8));
		httpResponse.headers().set(CONTENT_TYPE, "application/json");
		FullEncodedResponse encodedResponse = new FullEncodedResponse(request, httpResponse);
		ctx.writeAndFlush(encodedResponse);
	}
}
