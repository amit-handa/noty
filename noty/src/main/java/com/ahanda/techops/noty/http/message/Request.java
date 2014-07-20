package com.ahanda.techops.noty.http.message;

import io.netty.handler.codec.http.FullHttpRequest;

public class Request
{

	private final FullHttpRequest httpRequest;

	private final long orderNumber;

	public Request(FullHttpRequest httpRequest, long orderNumber)
	{
		this.httpRequest = httpRequest;
		this.orderNumber = orderNumber;
	}

	public long getOrderNumber()
	{
		return orderNumber;
	}

	public FullHttpRequest getHttpRequest()
	{
		return httpRequest;
	}
}
