package com.ahanda.techops.noty;

import io.netty.buffer.ByteBuf;

import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;

public class ReqEncoder extends ChannelOutboundHandlerAdapter {
    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
        Integer m = (Integer) msg;
        ByteBuf encoded = ctx.alloc().buffer(4);
        encoded.writeInt(m );
        ctx.write(encoded, promise); // (1)
    }
}
