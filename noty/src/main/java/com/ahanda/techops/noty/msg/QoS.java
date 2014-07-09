package com.ahanda.techops.noty.msg;

public enum QoS
{

    AT_MOST_ONCE((byte) 0),
    AT_LEAST_ONCE((byte) 1),
    EXACTLY_ONCE((byte) 2);

    final public byte val;

    QoS(byte val)
    {
        this.val = val;
    }

    public static QoS valueOf(int i)
    {
        for (QoS q : QoS.values())
        {
            if (q.val == i)
            {
                return q;
            }
            else if (i > 2)
            {
                return EXACTLY_ONCE;
            }
            else if (i < 0)
            {
                return AT_MOST_ONCE;
            }
        }
        throw new IllegalArgumentException("Not a valid QoS number: " + i);
    }
}
