package com.ahanda.techops.noty;

import io.netty.buffer.ByteBuf;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInboundHandlerAdapter;
import static io.netty.util.CharsetUtil.*;
import io.netty.util.*;	// referencecountutil

/**
 * Handles a server-side channel.
 */
public class ReqHandler extends ChannelInboundHandlerAdapter { // (1)

    @Override
    public void channelActive( final ChannelHandlerContext ctx ) { // (2)
	  int time = (int)(System.currentTimeMillis()/1000L );

	  final ChannelFuture f = ctx.writeAndFlush( time );
	  //f.addListener( future -> { ctx.close(); } );
	  f.addListener( ChannelFutureListener.CLOSE );
	}

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) { // (2)
	  ByteBuf in = (ByteBuf) msg;
	  System.out.println( in.toString( US_ASCII ) );
	  ctx.writeAndFlush( msg );
		//ReferenceCountUtil.release(msg); // (2)
	}

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) { // (4)
        // Close the connection when an exception is raised.
        cause.printStackTrace();
        ctx.close();
    }
}
