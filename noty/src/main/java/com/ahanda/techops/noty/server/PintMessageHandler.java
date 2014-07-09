package com.ahanda.techops.noty.server;

import org.json.JSONObject;

import com.ahanda.techops.noty.NotyConstants;
import com.ahanda.techops.noty.db.MongoDBManager;
import com.ahanda.techops.noty.pojo.RequestObject;
import com.ahanda.techops.noty.pojo.RequestType;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class PintMessageHandler extends SimpleChannelInboundHandler<RequestObject>
{

	/**
	 * This method will get called when a new request is received
	 */
	@Override
	protected void channelRead0(ChannelHandlerContext arg0, RequestObject msg) throws Exception
	{
		if (msg != null)
		{
			JSONObject req = msg.getJSONRequest();
			RequestType type = RequestType.valueOf(req.optInt(NotyConstants.REQUEST_TYPE));
			switch (type)
			{
			case CONNECT:
				break;
			case DISCONNECT:
				break;
			case PUBLISH:
				handlePublish(req);
				break;
			}
		}

	}

	private void handlePublish(JSONObject req)
	{
		try
		{
			JSONObject event = req.getJSONObject(NotyConstants.PUBLISH_EVENT);
			if (event != null)
			{
				// insert the event in DB
				MongoDBManager.getInstance().insertEvent(event);
				// publish the event to queue
			}
		}
		catch (Exception e)
		{

		}
	}

}
