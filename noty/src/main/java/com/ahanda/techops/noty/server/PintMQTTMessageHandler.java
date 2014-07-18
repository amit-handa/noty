package com.ahanda.techops.noty.server;

import org.json.JSONObject;
import com.ahanda.techops.noty.msg.*;

import com.ahanda.techops.noty.NotyConstants;
import com.ahanda.techops.noty.db.MongoDBManager;
import com.ahanda.techops.noty.pojo.RequestObject;
import com.ahanda.techops.noty.pojo.RequestType;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class PintMQTTMessageHandler extends SimpleChannelInboundHandler<Message>
{

	/**
	 * This method will get called when a new request is received
	 */
	@Override
	protected void channelRead0(ChannelHandlerContext ctx, Message msg) throws Exception
	{
		assert msg != null;
		System.out.println("received message " + msg);
		Message reply = null;
		switch (msg.getType())
		{
		case CONNECT:
			reply = handleConnect((ConnectMessage) msg);
			break;
		case DISCONNECT:
			break;
		case PUBLISH:
			// handlePublish(req);
			break;
		default:
			break;
		}

		if (reply != null)
			ctx.writeAndFlush(reply);
	}

	private Message handleConnect(ConnectMessage connect)
	{
		ConnAckMessage ack = new ConnAckMessage(ConnAckMessage.ConnectionStatus.ACCEPTED);
		return ack;
	}
	/*
	 * private void handlePublish(JSONObject req) { try { JSONObject event = req.getJSONObject(NotyConstants.PUBLISH_EVENT); if (event != null) { // insert the event in DB
	 * MongoDBManager.getInstance().insertEvent(event); // publish the event to queue } } catch (Exception e) {
	 * 
	 * } }
	 */

}
