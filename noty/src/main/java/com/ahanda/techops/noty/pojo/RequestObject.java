package com.ahanda.techops.noty.pojo;

import org.json.JSONObject;

public class RequestObject
{
	private String jString;
	
	private JSONObject obj;
	
	public RequestObject()
	{
		jString = "";
	}

	public RequestObject(String jString)
	{
		try
		{
			obj = new JSONObject(jString);
		}
		catch (Exception e)
		{
			obj = new JSONObject();
		}
	}
	
	public JSONObject getJSONRequest()
	{
		return obj;
	}
}
