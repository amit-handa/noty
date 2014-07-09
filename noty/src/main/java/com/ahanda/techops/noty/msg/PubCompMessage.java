package com.ahanda.techops.noty.msg;

import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: bhuvangupta
 * Date: 23/01/12
 * Time: 2:34 PM
 * To change this template use File | Settings | File Templates.
 */
public class PubCompMessage extends RetryableMessage
{

    public PubCompMessage(short messageId)
    {
        super(Type.PUBCOMP);
        setMessageId(messageId);
    }

    public PubCompMessage(Header header)
            throws IOException
    {
        super(header);
    }
}
