package com.ahanda.techops.noty;

import com.fasterxml.jackson.databind.ObjectMapper;

public class Utils
{
	// objectmapper is thread safe, only config shouldnt be altered.
	public static final ObjectMapper om = new ObjectMapper();
	
	@SuppressWarnings( {"unchecked"} )
	public static <T> T doCast( Object o ) {
		return (T)o;
	}
}
