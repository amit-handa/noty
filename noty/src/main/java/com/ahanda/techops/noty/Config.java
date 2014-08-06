package com.ahanda.techops.noty;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Set;

import com.fasterxml.jackson.databind.node.ObjectNode;

public class Config
{
	private ObjectNode config;
	private static ObjectNode defconfig;
	
	static {
		defconfig = Utils.om.createObjectNode();

        defconfig.put("macAlgoName", "HmacSHA256" );
        defconfig.put( "sessKey", "NQtV5zDQjVqg9vofDSEmX7WA+wXhBhjaxengpeyFh7AANWoMEPe+qebTViYb7db6fAEJJK+tWP8KEh4J10PAFQ==" );

        defconfig.put("http", Utils.om.createObjectNode()
            .put("host", "localhost")
            .put("port", 8080 )
            .put("maxRequestSize", 1048576 ) );
         defconfig.put( "mongodb", Utils.om.createObjectNode()
             .put( "host", "localhost" )
             .put( "port", 27017 ) );
	}

	public static ObjectNode getDefault() {
		return defconfig;
	}

	private static Config _instance;

	private Config() throws IOException
	{
		setupConfig();
	}

	public static Config getInstance() throws IOException
	{
		if (_instance == null)
		{
			_instance = new Config();
		}
		return _instance;
	}

	private void setupConfig() throws IOException
	{
		URL configFile = Thread.currentThread().getClass().getResource("/config.json");
		if (configFile == null)
		{
			throw new FileNotFoundException("Config file not found");
		}
		File f = new File(configFile.getFile());
		byte[] encoded = Files.readAllBytes(Paths.get(f.getAbsolutePath()));
		String jsonStr = new String(encoded, Charset.defaultCharset());
		config = Utils.om.readValue(jsonStr, ObjectNode.class );
	}

	public ObjectNode get() {
		return config;
	}

}
