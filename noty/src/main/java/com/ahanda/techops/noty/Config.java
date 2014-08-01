package com.ahanda.techops.noty;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Set;

import org.json.JSONObject;

public class Config
{
	private JSONObject config;
	private static JSONObject defconfig;
	
	static {
		defconfig = new JSONObject()
			.put("http", new JSONObject()
                .put("host", "localhost")
                .put("port", 8080 )
                .put("maxRequestSize", 1048576 ) )
             .put( "mongodb", new JSONObject()
             	.put( "host", "localhost" )
             	.put( "port", 27017 ) );
	}

	public static JSONObject getDefault() {
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
		config = new JSONObject(jsonStr);
	}

	public JSONObject get() {
		return config;
	}

}
