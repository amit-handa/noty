package com.ahanda.techops.noty;

import java.util.Arrays;

public class UnitTest
{
	public static void main( String[] args ) throws Exception {
		String s = new String( "/events/get" );
		String[] paths = s.split( "/" );
		System.out.println( Arrays.toString( paths ) );
	}

}
