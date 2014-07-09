package com.ahanda.techops.noty.msg;

import java.io.IOException;

public class PingRespMessage extends Message
{

    public PingRespMessage()
    {
        super(Type.PINGRESP);
    }

    public PingRespMessage(Header header)
            throws IOException
    {
        super(header);
    }

    @Override
    public void setDup(boolean dup)
    {
        throw new UnsupportedOperationException("PINGRESP message does not support the DUP flag");
    }

    @Override
    public void setQos(QoS qos)
    {
        throw new UnsupportedOperationException("PINGRESP message does not support the QoS flag");
    }

    @Override
    public void setRetained(boolean retain)
    {
        throw new UnsupportedOperationException("PINGRESP message does not support the RETAIN flag");
    }
}
