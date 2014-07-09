package com.ahanda.techops.noty.msg;

import com.ahanda.techops.noty.msg.Message.Header;
import com.ahanda.techops.noty.msg.Message.Type;

import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

public class MessageInputStream implements Closeable
{

    private InputStream in;

    public MessageInputStream(InputStream in)
    {
        this.in = in;
    }

    public Message readMessage()
            throws IOException
    {
        byte flags = (byte) in.read();

        //The value byte is returned as an int in the range 0 to 255.
        //If no byte is available because the end of the stream has been reached, the value -1 is returned.
        if (flags == -1)
        {
            throw new EOFException("No data available. Socket closed.");
        }

        Header header = new Header(flags);
        Message msg = null;
        Type type = header.getType();
        if (type == null)
        {
            throw new UnsupportedOperationException("No support for deserializing messages with flags=" + flags);
        }
        switch (type)
        {
            case CONNACK:
                msg = new ConnAckMessage(header);
                break;
            case PUBLISH:
                msg = new PublishMessage(header);
                break;
            case PUBACK:
                msg = new PubAckMessage(header);
                break;
            case PUBREC:
                msg = new PubRecMessage(header);
                break;
            case PUBREL:
                msg = new PubRelMessage(header);
                break;
            case SUBACK:
                msg = new SubAckMessage(header);
                break;
            case SUBSCRIBE:
                msg = new SubscribeMessage(header);
                break;
            case UNSUBSCRIBE:
                msg = new UnsubscribeMessage(header);
                break;
            case UNSUBACK:
                msg = new UnsubAckMessage(header);
                break;
            case PINGRESP:
                msg = new PingRespMessage(header);
                break;
            case PINGREQ:
                msg = new PingReqMessage(header);
                break;
            case DISCONNECT:
                msg = new DisconnectMessage(header);
                break;
            case CONNECT:
                msg = new ConnectMessage(header);
                break;
            default:
                throw new UnsupportedOperationException(
                        "No support for deserializing " + header.getType()
                                + " messages");
        }
        msg.read(in);
        return msg;
    }

    public void close()
            throws IOException
    {
        in.close();
    }
}
