package com.ahanda.techops.noty.msg;

import com.ahanda.techops.noty.FormatUtil;

import java.io.*;

public class PublishMessage extends RetryableMessage
{

    private String topic;
    private byte[] data;

    public PublishMessage(String topic, String msg)
    {
        this(topic, msg.getBytes(), QoS.AT_MOST_ONCE);
    }

    public PublishMessage(String topic, String msg, QoS qos)
    {
        this(topic, msg.getBytes(), qos);
    }

    public PublishMessage(String topic, byte[] data, QoS qos)
    {
        super(Type.PUBLISH);
        this.topic = topic;
        this.data = data;
        setQos(qos);
    }

    public PublishMessage(Header header)
            throws IOException
    {
        super(header);
    }


    @Override
    protected int messageLength()
    {
        int length = FormatUtil.toMQttString(topic).length;
        length += (getQos() == QoS.AT_MOST_ONCE) ? 0 : 2;
        length += data.length;
        return length;
    }

    @Override
    protected void writeMessage(OutputStream out)
            throws IOException
    {
        DataOutputStream dos = new DataOutputStream(out);
        dos.writeUTF(topic);
        dos.flush();
        if (getQos() != QoS.AT_MOST_ONCE)
        {
            super.writeMessage(out);
        }
        //System.out.println("--> Pushing data :" + getDataAsString() + " : topic : " + topic);
        dos.write(data);
        dos.flush();
    }

    @Override
    protected void readMessage(InputStream in, int msgLength)
            throws IOException
    {
        int pos = 0;
        DataInputStream dis = new DataInputStream(in);
        topic = dis.readUTF();
        pos += FormatUtil.toMQttString(topic).length;
        if (getQos() != QoS.AT_MOST_ONCE)
        {
            super.readMessage(in, msgLength);
            pos += 2;
        }
        int payloadSize = (msgLength - pos);
        data = readPayload(payloadSize, dis);

    }

    public String getTopic()
    {
        return topic;
    }

    public byte[] getData()
    {
        return data;
    }

    public String getDataAsString()
    {
        //return new String(data);
        return FormatUtil.toString(data);
    }

    @Override
    public String toString()
    {
        StringBuilder strBuff = new StringBuilder();
        strBuff.append("PublishMessage [");
        strBuff.append("topic: " + getTopic() + ",");
        strBuff.append("messageId: " + getMessageId() + ",");
        strBuff.append("qos: " + getQos() + ",");
        strBuff.append("data: " + getDataAsString() + "]");
        return strBuff.toString();
    }


    public static void main(String[] args)
    {
        try
        {
            String txt = "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";
            System.out.println("djhfssjhdlkhfklhfjsdhalkfh/s : " + txt.length());
            PublishMessage publishMessage = new PublishMessage("djhfssjhdlkhfklhfjsdhalkfh/s", txt, QoS.AT_MOST_ONCE);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            MessageOutputStream mos = new MessageOutputStream(bos);
            mos.writeMessage(publishMessage);

            byte[] blob = bos.toByteArray();
            MessageInputStream mis = new MessageInputStream(new ByteArrayInputStream(blob));
            PublishMessage ps = (PublishMessage) mis.readMessage();
            System.out.println(ps.getDataAsString());

        }
        catch (IOException e)
        {
            e.printStackTrace();
        }


    }

}
