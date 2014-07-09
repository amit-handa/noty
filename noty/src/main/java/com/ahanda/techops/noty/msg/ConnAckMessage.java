package com.ahanda.techops.noty.msg;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ConnAckMessage extends Message
{

    public enum ConnectionStatus
    {
        ACCEPTED,
        UNACCEPTABLE_PROTOCOL_VERSION,
        IDENTIFIER_REJECTED,
        SERVER_UNAVAILABLE,
        BAD_USERNAME_OR_PASSWORD,
        NOT_AUTHORIZED
    }

    private ConnectionStatus status;

    public ConnAckMessage(ConnectionStatus status)
    {
        super(Type.CONNACK);
        this.status = status;
    }

    public ConnAckMessage(Header header)
            throws IOException
    {
        super(header);
    }

    @Override
    protected void readMessage(InputStream in, int msgLength)
            throws IOException
    {
        if (msgLength != 2)
        {
            throw new IllegalStateException("Message Length must be 2 for CONNACK. Current value: " + msgLength);
        }
        // Ignore first byte
        in.read();
        int result = in.read();
        switch (result)
        {
            case 0:
                status = ConnectionStatus.ACCEPTED;
                break;
            case 1:
                status = ConnectionStatus.UNACCEPTABLE_PROTOCOL_VERSION;
                break;
            case 2:
                status = ConnectionStatus.IDENTIFIER_REJECTED;
                break;
            case 3:
                status = ConnectionStatus.SERVER_UNAVAILABLE;
                break;
            case 4:
                status = ConnectionStatus.BAD_USERNAME_OR_PASSWORD;
                break;
            case 5:
                status = ConnectionStatus.NOT_AUTHORIZED;
                break;
            default:
                throw new UnsupportedOperationException("Unsupported CONNACK code: " + result);
        }
    }

    @Override
    protected void writeMessage(OutputStream out)
            throws IOException
    {
        out.write(0);
        switch (status)
        {
            case ACCEPTED:
                out.write(0);
                break;
            case UNACCEPTABLE_PROTOCOL_VERSION:
                out.write(1);
                break;
            case IDENTIFIER_REJECTED:
                out.write(2);
                break;
            case SERVER_UNAVAILABLE:
                out.write(3);
                break;
            case BAD_USERNAME_OR_PASSWORD:
                out.write(4);
                break;
            case NOT_AUTHORIZED:
                out.write(5);
                break;
            default:
                out.write(10);
        }

    }

    public ConnectionStatus getStatus()
    {
        return status;
    }

    @Override
    protected int messageLength()
    {
        return 2;
    }

    @Override
    public void setDup(boolean dup)
    {
        throw new UnsupportedOperationException("CONNACK messages don't use the DUP flag.");
    }

    @Override
    public void setRetained(boolean retain)
    {
        throw new UnsupportedOperationException("CONNACK messages don't use the RETAIN flag.");
    }

    @Override
    public void setQos(QoS qos)
    {
        throw new UnsupportedOperationException("CONNACK messages don't use the QoS flags.");
    }


    @Override
    public String toString()
    {
        StringBuilder strBuff = new StringBuilder();
        strBuff.append("ConnAckMessage [");
        strBuff.append("status:" + getStatus()+"]");
        return strBuff.toString();
    }


}
