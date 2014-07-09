package com.ahanda.techops.noty.msg;

import java.util.List;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;

public class MqttMessageEncoder extends MessageToMessageEncoder<Message>
{

    @Override
    protected void encode(ChannelHandlerContext ctx, Message msg, List< Object > out )
            throws Exception
    {
        byte[] data = ((Message) msg).toBytes();
        ByteBuf buf = ctx.alloc().buffer( data.length );
        buf.writeBytes(data); // data
        out.add( buf );
    }

}
