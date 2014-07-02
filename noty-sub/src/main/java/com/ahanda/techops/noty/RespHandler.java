package com.ahanda.techops.noty;

import java.util.Date;

import io.netty.buffer.ByteBuf;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInboundHandlerAdapter;
import static io.netty.util.CharsetUtil.*;
import io.netty.util.*;	// referencecountutil

/**
 * Handles a client-side channel.
 */
public class RespHandler extends ChannelInboundHandlerAdapter { // (1)

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) { // (2)
	  Integer m = (Integer) msg; // (1)
	  long currentTimeMillis = m*1000L;
	  System.out.println(new Date(currentTimeMillis));
	  ctx.close();
	}

	@Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) { // (4)
	  // Close the connection when an exception is raised.
	  System.out.println( "Uffffffffff !" );
	  cause.printStackTrace();
	  ctx.close();
    }
}
