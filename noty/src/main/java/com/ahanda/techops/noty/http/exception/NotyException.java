package com.ahanda.techops.noty.http.exception;

import io.netty.handler.codec.http.HttpResponseStatus;

import com.ahanda.techops.noty.http.message.Request;

public class NotyException extends Exception
{
	HttpResponseStatus status = HttpResponseStatus.BAD_REQUEST;

	Request request;

	public NotyException(String msg)
	{
		super(msg);
	}
	public NotyException(Throwable cause)
	{
		super(cause);
	}

	public NotyException(String msg, Throwable cause)
	{
		super(msg, cause);
	}

	public NotyException(HttpResponseStatus st, Throwable cause)
	{
		this(null,st,cause);
	}

	public NotyException(HttpResponseStatus st, String msg)
	{
		super(msg);
		this.status = st;
	}

	public NotyException(Request req, HttpResponseStatus st, Throwable cause)
	{
		super(cause);
		status = st;
		request = req;
	}

	public void setRequest(Request r)
	{
		this.request = r;
	}

	public Request getRequest()
	{
		return request;
	}
	
	/**
	 * Handled exceptions are those where in a connection multiple requests are sent and one of them got failed. There that request will be marked failed but rest will get
	 * processed.
	 * 
	 * @return true : if handled else false
	 * 
	 */
	public boolean isHandledException()
	{
		if (request == null)
			return false;
		return true;
	}

	public HttpResponseStatus getStatus()
	{
		return status;
	}
}
