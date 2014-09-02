package com.ahanda.techops.noty;

import io.netty.channel.ChannelHandlerContext;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SessionManager
{
	private Map<String, ChannelHandlerContext> sessionMap;

	private static SessionManager _instance;

	public static SessionManager getInstance()
	{
		if (_instance == null)
			_instance = new SessionManager();
		return _instance;
	}

	private SessionManager()
	{
		sessionMap = new ConcurrentHashMap<String, ChannelHandlerContext>();
	}
	
	public void addSession(String uid, ChannelHandlerContext ctx)
	{
		sessionMap.put(uid, ctx);
	}
	
	public ChannelHandlerContext getUserContext(String uid)
	{
		return sessionMap.get(uid);
	}

	public void removeSession(String uid)
	{
		sessionMap.remove(uid);
	}
}
