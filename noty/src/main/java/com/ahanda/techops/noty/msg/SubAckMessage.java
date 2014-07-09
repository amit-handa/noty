package com.ahanda.techops.noty.msg;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class SubAckMessage extends RetryableMessage
{

    private List<QoS> grantedQoSs;

    public SubAckMessage(Header header)
            throws IOException
    {
        super(header);
    }

    public SubAckMessage(short messageId, List<QoS> qoSs)
    {
        super(Type.SUBACK);
        this.messageId = messageId;
        this.grantedQoSs = qoSs;
    }

    public SubAckMessage()
    {
        super(Type.SUBACK);
    }

    @Override
    protected void readMessage(InputStream in, int msgLength)
            throws IOException
    {
        super.readMessage(in, msgLength);
        int pos = 2;
        while (pos < msgLength)
        {
            QoS qos = QoS.valueOf(in.read());
            addQoS(qos);
            pos++;
        }
    }

    @Override
    protected void writeMessage(OutputStream out)
            throws IOException
    {
        super.writeMessage(out);
        DataOutputStream dos = new DataOutputStream(out);

        if (grantedQoSs != null)
        {
            byte[] bts = new byte[grantedQoSs.size()];
            for (int i = 0; i < grantedQoSs.size(); i++)
            {
                bts[i] = grantedQoSs.get(i).val;
            }
            dos.write(bts);
        }
        dos.flush();
    }

    private void addQoS(QoS qos)
    {
        if (grantedQoSs == null)
        {
            grantedQoSs = new ArrayList<QoS>();
        }
        grantedQoSs.add(qos);
    }

    public List<QoS> getGrantedQoSs()
    {
        return grantedQoSs;
    }

    @Override
    protected int messageLength()
    {
        int length = 2;
        if (grantedQoSs != null)
        {
            length += grantedQoSs.size();
        }
        return length;
    }


    @Override
    public String toString()
    {
        StringBuilder strBuff = new StringBuilder();
        strBuff.append("SubAckMessage [");
        strBuff.append("messageId: " + getMessageId());
        strBuff.append("qos: " + getGrantedQoSs());
        strBuff.append("]");
        return strBuff.toString();
    }
}
