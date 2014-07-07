package com.ahanda.techops.noty.server;

import com.ahanda.techops.noty.pojo.RequestObject;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class PintMessageHandler extends SimpleChannelInboundHandler<RequestObject>
{

	/**
	 *  This methid will get called when a new request is received
	 */
	@Override
	protected void channelRead0(ChannelHandlerContext arg0, RequestObject msg) throws Exception
	{
		

	}


}
