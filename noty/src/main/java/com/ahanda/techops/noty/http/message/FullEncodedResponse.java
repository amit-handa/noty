package com.ahanda.techops.noty.http.message;

import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponse;

public class FullEncodedResponse
{

	private final Request request;

	private final FullHttpResponse httpResponse;

	public FullEncodedResponse(Request request, FullHttpResponse httpResponse)
	{
		this.request = request;
		this.httpResponse = httpResponse;
	}

	public Request getRequest()
	{
		return request;
	}

	public FullHttpResponse getHttpResponse()
	{
		return httpResponse;
	}
}
