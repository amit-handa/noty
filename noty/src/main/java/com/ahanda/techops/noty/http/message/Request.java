package com.ahanda.techops.noty.http.message;

import java.util.HashSet;
import java.util.Set;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.Cookie;
import io.netty.handler.codec.http.CookieDecoder;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.QueryStringDecoder;

public class Request
{

	private final FullHttpRequest httpRequest;
	private FullHttpResponse httpResponse;
	private Set< Cookie > cookies;

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

	public Set< Cookie > cookies() {
		if( cookies == null ) {
            String cookiestr = httpRequest.headers().get( HttpHeaders.Names.COOKIE );
            if( cookiestr != null )
                cookies = CookieDecoder.decode( cookiestr );
		}
		return cookies;
	}

	public FullHttpResponse getResponse() {
		return httpResponse;
	}

	public FullHttpResponse setResponse( HttpResponseStatus status, ByteBuf buf )
	{
		if( httpResponse != null )
			return null;

        httpResponse = new DefaultFullHttpResponse( httpRequest.getProtocolVersion(), status, buf );
        return httpResponse;
	}
}
