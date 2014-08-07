package com.ahanda.techops.noty;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Set;

import javax.crypto.Mac;

import com.fasterxml.jackson.databind.node.ObjectNode;

public class Config
{
	private ObjectNode config;

	private static ObjectNode defconfig;

	static
	{
		defconfig = Utils.om.createObjectNode();

		defconfig.put("macAlgoName", "HmacSHA256");
		defconfig.put("sessKey", "NQtV5zDQjVqg9vofDSEmX7WA+wXhBhjaxengpeyFh7AANWoMEPe+qebTViYb7db6fAEJJK+tWP8KEh4J10PAFQ==");
		defconfig.put("auth-token", "Fh7AANW");

		defconfig.put("http", Utils.om.createObjectNode().put("host", "localhost").put("port", 8080).put("sessValidityWindow", 3600).put("maxRequestSize", 1048576));
		defconfig.put("mongodb", Utils.om.createObjectNode().put("host", "localhost").put("port", 27017));
	}

	public static ObjectNode getDefault()
	{
		return defconfig;
	}

	private static Config _instance;

	private Config()
	{
		
	}

	public static Config getInstance()
	{
		if (_instance == null)
		{
			_instance = new Config();
		}
		return _instance;
	}

	public void setupConfig() throws IOException
	{
		URL configFile = Thread.currentThread().getClass().getResource("/config.json");
		if (configFile == null)
		{
			throw new FileNotFoundException("Config file not found");
		}
		File f = new File(configFile.getFile());
		byte[] encoded = Files.readAllBytes(Paths.get(f.getAbsolutePath()));
		String jsonStr = new String(encoded, Charset.defaultCharset());
		config = Utils.om.readValue(jsonStr, ObjectNode.class);
	}

	public ObjectNode get()
	{
		return config;
	}

	public int getValidityWindow()
	{
		return config.get("http").get(NotyConstants.HTTP_SESSIONS_VALIDITY).asInt();
	}

	public String getSecretKey()
	{
		return config.get("sessKey").asText();
	}

	public String getMacAlgoName()
	{
		return config.get("macAlgoName").asText();
	}
	
	public String getAuthToken()
	{
		return config.get("auth-token").asText();
	}
}
