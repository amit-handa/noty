package com.ahanda.techops.noty.msg;

import java.util.List;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.io.ByteArrayInputStream;

public class MqttMessageDecoder extends ByteToMessageDecoder
{

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf buf, List< Object > out )
            throws Exception
    {
        if (buf.readableBytes() < 2)
        {
            return ;
        }
        buf.markReaderIndex();
        buf.readByte(); // read away header
        int msgLength = 0;
        int multiplier = 1;
        int digit;
        int lengthSize = 0;
        do
        {
            lengthSize++;
            digit = buf.readByte();
            msgLength += (digit & 0x7f) * multiplier;
            multiplier *= 128;
            if ((digit & 0x80) > 0 && !buf.isReadable())
            {
                buf.resetReaderIndex();
                return ;
            }
        } while ((digit & 0x80) > 0);
        if (buf.readableBytes() < msgLength)
        {
            buf.resetReaderIndex();
            return ;
        }
        byte[] data = new byte[1 + lengthSize + msgLength];
        buf.resetReaderIndex();
        buf.readBytes(data);
        MessageInputStream mis = new MessageInputStream(
                new ByteArrayInputStream(data));
        Message msg = mis.readMessage();
        return ;
    }

}
