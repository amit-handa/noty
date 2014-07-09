package com.ahanda.techops.noty.msg;

import java.io.IOException;

public class DisconnectMessage extends Message
{

    public DisconnectMessage()
    {
        super(Type.DISCONNECT);
    }

    public DisconnectMessage(Header header)
            throws IOException
    {
        super(header);
    }

    @Override
    public void setDup(boolean dup)
    {
        throw new UnsupportedOperationException("DISCONNECT message does not support the DUP flag");
    }

    @Override
    public void setQos(QoS qos)
    {
        throw new UnsupportedOperationException("DISCONNECT message does not support the QoS flag");
    }

    @Override
    public void setRetained(boolean retain)
    {
        throw new UnsupportedOperationException("DISCONNECT message does not support the RETAIN flag");
    }

}
