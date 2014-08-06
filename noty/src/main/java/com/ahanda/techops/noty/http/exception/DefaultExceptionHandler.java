package com.ahanda.techops.noty.http.exception;

import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_LENGTH;
import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.util.CharsetUtil;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ahanda.techops.noty.http.message.FullEncodedResponse;
import com.ahanda.techops.noty.http.message.Request;

public class DefaultExceptionHandler extends ChannelInboundHandlerAdapter
{

	private final Logger logger = LoggerFactory.getLogger(DefaultExceptionHandler.class);

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception
	{
		logger.error("Exception caught", cause);
		HttpResponseStatus status;
		if (cause instanceof NotyException)
		{
			NotyException e = (NotyException) cause;
			status = e.getStatus();
			String content = getTraceContent(e);
			if (e.isHandledException())
				sendEncodedResponse(ctx,e.getRequest(),status,content);
			else
				sendResponse(ctx, status, content);
		}
		else
		{
			status = INTERNAL_SERVER_ERROR;
			sendResponse(ctx, status, getTraceContent(cause));
		}
	}

	private String getTraceContent(Throwable cause)
	{
		if (cause != null)
		{
			StringWriter stringWriter = new StringWriter();
			PrintWriter printWriter = new PrintWriter(stringWriter);
			cause.printStackTrace(printWriter);
			return stringWriter.toString();
		}
		return "";
	}

	private void sendResponse(ChannelHandlerContext ctx, HttpResponseStatus status, String content)
	{
		FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, status, Unpooled.copiedBuffer(content, CharsetUtil.UTF_8));
		response.headers().set(CONTENT_TYPE, "text/plain; charset=UTF-8");
		response.headers().set(CONTENT_LENGTH, response.content().readableBytes());
		ctx.writeAndFlush(response);
		ctx.close();
	}
	
	private void sendEncodedResponse(ChannelHandlerContext ctx, Request request ,HttpResponseStatus status, String content)
	{
		FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, status, Unpooled.copiedBuffer(content, CharsetUtil.UTF_8));
		response.headers().set(CONTENT_TYPE, "text/plain; charset=UTF-8");
		response.headers().set(CONTENT_LENGTH, response.content().readableBytes());
		FullEncodedResponse encodedResponse = new FullEncodedResponse(request, response);
		ctx.writeAndFlush(encodedResponse);
	}
}
