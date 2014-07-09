package com.ahanda.techops.noty.msg;

import java.io.IOException;

public class PingReqMessage extends Message
{

    public PingReqMessage()
    {
        super(Type.PINGREQ);
    }

    public PingReqMessage(Header header)
            throws IOException
    {
        super(header);
    }

    @Override
    public void setDup(boolean dup)
    {
        throw new UnsupportedOperationException("PINGREQ message does not support the DUP flag");
    }

    @Override
    public void setQos(QoS qos)
    {
        throw new UnsupportedOperationException("PINGREQ message does not support the QoS flag");
    }

    @Override
    public void setRetained(boolean retain)
    {
        throw new UnsupportedOperationException("PINGREQ message does not support the RETAIN flag");
    }


}
