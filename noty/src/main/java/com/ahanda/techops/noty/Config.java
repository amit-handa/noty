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

	public int getHttpPort()
	{
		JSONObject obj = config.optJSONObject("http");
		if (obj != null)
		{
			return obj.optInt("port", 8080);
		}
		return 8080;
	}
	
	public String getHttpHost()
	{
		JSONObject obj = config.optJSONObject("http");
		if (obj != null)
		{
			return obj.optString("host", "127.0.0.1");
		}
		return "127.0.0.1";
	}
	
	public int getHttpObjectAggregatorSize()
	{
		JSONObject obj = config.optJSONObject("http");
		if (obj != null)
		{
			return obj.optInt("objectAggregatorSize", 1048576);
		}
		return 1048576;
	}
	
	public String getMongoDbHost()
	{
		JSONObject obj = config.optJSONObject("mongodb");
		if (obj != null)
		{
			return obj.optString("host", "127.0.0.1");
		}
		return "127.0.0.1";
	}
	
	public int getMongoDbPort()
	{
			JSONObject obj = config.optJSONObject("mongodb");
			if (obj != null)
			{
				return obj.optInt("port", 27017);
			}
			return 27017;
	}
	
	public Set getMongoDbs()
	{
		JSONObject dbs = config.optJSONObject("dbs");
		if(dbs != null)
			return dbs.keySet();
		return null; 
	}
	
	public JSONObject getCollectionsForDb(String dbName)
	{
		JSONObject dbs = config.optJSONObject("dbs");
		if(dbs != null)
		{
			JSONObject colls = dbs.optJSONObject(dbName);
			if(colls == null)
				return null;
			return colls;
		}
		return null; 
	}
	
}
