package com.ahanda.techops.noty.http;

public enum RequestType
{
	CONNECT(1), 
	DISCONNECT(2),
	PUBLISH(3);

	final private byte val;

	RequestType(int val)
	{
		this.val = (byte) val;
	}

	public static RequestType valueOf(int i)
	{
		for (RequestType t : RequestType.values())
		{
			if (t.val == i)
			{
				return t;
			}
		}
		return null;
	}
}
