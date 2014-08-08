package com.ahanda.techops.noty.http.message;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.QueryStringDecoder;

public class Request
{

	private final FullHttpRequest httpRequest;

	private final long orderNumber;
	
	private final String reqPath;

	public Request(FullHttpRequest httpRequest, long orderNumber)
	{
		this.httpRequest = httpRequest;
		this.orderNumber = orderNumber;
		QueryStringDecoder queryStringDecoder = new QueryStringDecoder(httpRequest.getUri());
		reqPath =  queryStringDecoder.path();
	}

	public long getOrderNumber()
	{
		return orderNumber;
	}

	public FullHttpRequest getHttpRequest()
	{
		return httpRequest;
	}
	
	public String getRequestPath()
	{
		return reqPath;
	}
}
