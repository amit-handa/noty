package com.ahanda.techops.noty.http;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.Cookie;
import io.netty.handler.codec.http.CookieDecoder;
import io.netty.handler.codec.http.DefaultCookie;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.ServerCookieEncoder;
import io.netty.util.CharsetUtil;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ahanda.techops.noty.Config;
import com.ahanda.techops.noty.Utils;
import com.ahanda.techops.noty.http.message.FullEncodedResponse;
import com.ahanda.techops.noty.http.message.Request;
import com.fasterxml.jackson.core.type.TypeReference;

/**
 * 
 * This class handles the authorization for the request. For publishers, an auth key should be there in the header in
 * every request using which, request is granted. For subscribers, a cookie should be set in the header to validate the
 * request. If the validation fails, server will create the cookie while user login
 *
 */
public class AuthHandler extends SimpleChannelInboundHandler<Request>
{
	// process all the uconfs and populate user-data
	private static final Logger logger = LoggerFactory.getLogger(AuthHandler.class);

	private static String UNAUTH_ACCESS = "Unauthorized access: kindly sign-in again";

	private static String SESSION_EXPIRED = "Session Expired : %d";

	private static String SESSION_DELETED = "Session Deleted Successfully";

	private static String NOT_AUTHORIZED = "Authorization absent, kindly sign-in first";

	private static String PUBLISHER_KEY = "Fh7AANW";

	private Mac mac;

	private static SecretKeySpec sks;

	/**
	 * All the security stuff should be common for all the channels, and should be instantiated before processing
	 */
	static
	{
		try
		{
			String macAlgoName = Config.getInstance().getMacAlgoName();
			String secretKey = Config.getInstance().getSecretKey();
			sks = new SecretKeySpec(secretKey.getBytes(), macAlgoName);
		}
		catch (IllegalArgumentException | IOException exc)
		{
			logger.warn("Exception while instantiating SecretKeySpec : {} {}", exc.getMessage(), exc.getStackTrace());
		}
	}

	public void initMac()
	{
		try
		{
			/* If MAC is null then init it else reuse the same mac object */
			if (mac == null)
			{
				mac = Mac.getInstance(sks.getAlgorithm());
				mac.init(sks);
				logger.info("Mac Initiated ! {} {}", new Object[] { sks.getEncoded(), mac });
			}
		}
		catch (Exception exc)
		{
			logger.warn("Exception while init MAC : {} {}", exc.getMessage(), exc.getStackTrace());
		}
	}

	public AuthHandler()
	{
		super(false);
	}

	public Map<String, Object> checkCredential(final Map<String, Object> msg)
	{
		String userId = (String) msg.get("userId");
		String password = (String) msg.get("password");

		String sessId = null;

		if (password != null)
		{
			long sessStart = System.currentTimeMillis() / 1000L;
			sessId = getSessId(userId, sessStart);
			Map<String, Object> reply = new HashMap<String, Object>();
			reply.put("userId", userId);
			reply.put("sessStart", sessStart);
			reply.put("sessId", sessId);
			reply.put("status", "ok");

			return reply;
		}

		sessId = (String) msg.get("sessId");
		long sessStart = (long) msg.get("sessStart");
		String nsessId = getSessId(userId, sessStart);
		if (sessId.equals(nsessId))
			msg.put("status", "ok");
		else
			msg.put("status", "error");
		return msg;
	}

	public String getSessId(String userId, long sessStart)
	{
		String cval = String.format("%s&%d", userId, sessStart);
		initMac();
		return new String(Base64.encodeBase64(mac.doFinal(cval.getBytes())));
	}

	@Override
	protected void channelRead0(final ChannelHandlerContext ctx, final Request request) throws Exception
	{
		FullHttpRequest httpReq = request.getHttpRequest();
		if (reqValid(request))
		{
			ctx.writeAndFlush(request);
			return;
		}
		String path = request.getRequestPath();
		HttpMethod accessMethod = httpReq.getMethod();

		Cookie sessIdc = null, userIdc = null, sessStartc = null;
		String cookiestr = httpReq.headers().get(HttpHeaders.Names.COOKIE);
		Set<Cookie> cookies = null;

		if (cookiestr != null)
		{
			cookies = CookieDecoder.decode(cookiestr);
			logger.info("Intercepted msg : headers {} {} {}!!!", path, cookies);
			
			for (Cookie c : cookies)
			{
				switch (c.getName())
				{
				case "sessId":
					sessIdc = c;
					break;
				case "userId":
					userIdc = c;
					break;
				case "sessStart":
					sessStartc = c;
					break;
				default:
					break;
				}
			}
		}

		String sessId = null, userId = null;
		long sessStart = -1;
		if (path.matches("/login") && accessMethod == HttpMethod.POST)
		{
			if(cookies == null)
				cookies = new HashSet<Cookie>();
			
			String body = httpReq.content().toString(CharsetUtil.UTF_8);
			logger.info("Login request: {}", path);
			Map<String, String> credentials = Utils.om.readValue(body, new TypeReference<Map<String, String>>()
			{
			});
			userId = credentials.get("userId");

			if (userId == null)
			{ // authenticate userId
				logger.debug("Cannot validate User {}, Fix it, continuing as usual !");
				// return null;
			}

			sessStart = System.currentTimeMillis() / 1000L;
			sessId = getSessId(userId, sessStart);

			for (Cookie reqcookie : cookies)
			{
				reqcookie.setMaxAge(0);
			}

			sessIdc = new DefaultCookie("sessId", sessId);
			sessIdc.setHttpOnly(true);
			sessIdc.setPath("/");
			cookies.remove(sessIdc);

			sessIdc.setMaxAge(sessStart + Config.getInstance().getValidityWindow());
			cookies.add(sessIdc);

			sessStartc = new DefaultCookie("sessStart", Long.toString(sessStart));
			sessStartc.setHttpOnly(true);
			sessStartc.setPath("/");
			cookies.remove(sessStartc);

			sessIdc.setMaxAge(sessStart + Config.getInstance().getValidityWindow());
			cookies.add(sessStartc);

			userIdc = new DefaultCookie("userId", userId);
			userIdc.setHttpOnly(true);
			userIdc.setPath("/");
			cookies.remove(userIdc);

			userIdc.setMaxAge(sessStart + Config.getInstance().getValidityWindow());
			cookies.add(userIdc);

			FullHttpResponse resp = new DefaultFullHttpResponse(request.getHttpRequest().getProtocolVersion(), HttpResponseStatus.OK);
			resp.headers().set(HttpHeaders.Names.SET_COOKIE, ServerCookieEncoder.encode(cookies));
			ctx.writeAndFlush(new FullEncodedResponse(request, resp));
			return;
		}

		if (sessIdc == null || userIdc == null || sessStartc == null)
		{
			// invalid request, opensession first
			logger.error("Invalid request, session doesnt exist!");
			sendResponse(ctx, request, HttpResponseStatus.UNAUTHORIZED, NOT_AUTHORIZED);
			return;
		}

		if (sessId == null)
			sessId = sessIdc.getValue();
		if (userId == null)
			userId = userIdc.getValue();
		if (sessStart < 0)
			sessStart = Long.valueOf(sessStartc.getValue());

		String csessid = getSessId(userId, sessStart);

		if (!csessid.equals(sessId))
		{
			logger.error("Invalid credentials {} {}!!", sessId, csessid);
			sendResponse(ctx, request, HttpResponseStatus.UNAUTHORIZED, UNAUTH_ACCESS);
			return;
		}

		long elapseSecs = System.currentTimeMillis() / 1000L - sessStart;
		if (elapseSecs > Config.getInstance().getValidityWindow())
		{
			sendResponse(ctx, request, HttpResponseStatus.UNAUTHORIZED, String.format(SESSION_EXPIRED, elapseSecs));
			return;
		}

		if (path.matches("/logout") && accessMethod == HttpMethod.DELETE)
		{
			sendResponse(ctx, request, HttpResponseStatus.OK, SESSION_DELETED);
			ctx.close();
			return;
		}

		httpReq.headers().set("userId", userId);
		httpReq.headers().set("sessStart", Long.toString(sessStart));
		ctx.fireChannelRead(request);
	}

	private boolean reqValid(Request request)
	{
		String token = request.getHttpRequest().headers().get("auth-token");
		if (PUBLISHER_KEY.equals(token))
			return true;
		return false;
	}

	private void sendResponse(ChannelHandlerContext ctx, Request req, HttpResponseStatus status, String msg)
	{
		FullHttpResponse resp = new DefaultFullHttpResponse(req.getHttpRequest().getProtocolVersion(), status, ctx.alloc().buffer().writeBytes(msg.getBytes()));
		ctx.writeAndFlush(new FullEncodedResponse(req, resp));
	}
}
