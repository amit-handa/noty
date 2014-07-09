package com.ahanda.techops.noty.msg;

import java.io.IOException;

public class PubRecMessage extends RetryableMessage
{

    public PubRecMessage(short messageId)
    {
        super(Type.PUBREC);
        setMessageId(messageId);
    }

    public PubRecMessage(Header header)
            throws IOException
    {
        super(header);
    }
}
