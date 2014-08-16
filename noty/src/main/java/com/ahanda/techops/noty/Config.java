package com.ahanda.techops.noty;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import javax.crypto.Mac;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class Config
{
	private ObjectNode config;

	private ObjectNode defconfig;

	private static class Pair
	{
		private Object val;

		private Object def;

		Pair(Object first, Object second)
		{
			val = first;
			def = second;
		}

		public Object getValue()
		{
			return val;
		}

		public Object getDefaultValue()
		{
			return def;
		}
	}

	private HashMap<String, Pair> configMap;

	private static Config _instance;

	private Config()
	{
		defconfig = Utils.om.createObjectNode();

		defconfig.put("macAlgoName", "HmacSHA256");
		defconfig.put("sessKey", "NQtV5zDQjVqg9vofDSEmX7WA+wXhBhjaxengpeyFh7AANWoMEPe+qebTViYb7db6fAEJJK+tWP8KEh4J10PAFQ==");
		defconfig.put("auth-token", "Fh7AANW");

		defconfig.put("http", Utils.om.createObjectNode().put("host", "localhost").put("port", 8080).put("sessValidityWindow", 3600).put("maxRequestSize", 1048576));
		defconfig.put("mongodb", Utils.om.createObjectNode().put("host", "localhost").put("port", 27017));

		configMap = new HashMap<String, Pair>(defconfig.size());
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
		createConfigMap(config);
	}

	private void createConfigMap(ObjectNode node)
	{
		Iterator<Entry<String, JsonNode>> i = node.fields();
		while (i.hasNext())
		{
			Entry<String, JsonNode> e = i.next();
			String key = e.getKey();
			switch (key)
			{
			case NotyConstants.MAC_ALGO_NAME:
			case NotyConstants.SESS_KEY:
			case NotyConstants.AUTH_TOKEN:
				configMap.put(key, new Pair(e.getValue().asText(), defconfig.get(key).asText()));
				break;
			case NotyConstants.MONGO_DB:
				JsonNode mdefNode = defconfig.get(key);
				JsonNode mjnode = e.getValue();
				Iterator<Entry<String, JsonNode>> mit = mjnode.fields();
				while (mit.hasNext())
				{
					Entry<String, JsonNode> en = mit.next();
					String k = en.getKey();
					// key will be mongodb.host, mongodb.port etc
					switch (k)
					{
					case NotyConstants.HOST:
						configMap.put(key + "." + k, new Pair(en.getValue().asText(), mdefNode.get(k).asText()));
						break;
					case NotyConstants.PORT:
						configMap.put(key + "." + k, new Pair(en.getValue().asInt(), mdefNode.get(k).asInt()));
						break;
					case NotyConstants.MONGODB.PINT_DB:
						String mapKey = key + "." + k + "." + NotyConstants.MONGODB.EVENTS_COLL + "." + NotyConstants.MONGODB.INDEXES;
						ArrayNode val = (ArrayNode) en.getValue().get(NotyConstants.MONGODB.EVENTS_COLL).get(NotyConstants.MONGODB.INDEXES);
						ArrayNode defVal = (ArrayNode) mdefNode.get(k).get(NotyConstants.MONGODB.EVENTS_COLL).get(NotyConstants.MONGODB.INDEXES);
						List<String> valList = new ArrayList<String>(val.size());
						List<String> defValList = new ArrayList<String>(val.size());
						for (int j = 0; j < val.size(); j++)
						{
							valList.add(val.get(j).asText());
							defValList.add(val.get(j).asText());
						}
						configMap.put(mapKey, new Pair(valList, defValList));
						break;
					}
				}
				break;
			case NotyConstants.HTTP:
				JsonNode hdefNode = defconfig.get(key);
				JsonNode hjnode = e.getValue();
				Iterator<Entry<String, JsonNode>> hit = hjnode.fields();
				while (hit.hasNext())
				{
					Entry<String, JsonNode> en = hit.next();
					String k = en.getKey();
					// key will be http.host, http.port etc

					Pair p;

					if (NotyConstants.HOST.equals(k))
						p = new Pair(en.getValue().asText(), hdefNode.get(k).asText());
					else
						p = new Pair(en.getValue().asInt(), hdefNode.get(k).asInt());

					configMap.put(key + "." + k, p);
				}
				break;
			}
		}
	}

	public ObjectNode get()
	{
		return config;
	}

	public String getMongoHost()
	{
		return (String) configMap.get(NotyConstants.MONGO_DB + "." + NotyConstants.HOST).getValue();
	}

	public int getMongoPort()
	{
		return (int) configMap.get(NotyConstants.MONGO_DB + "." + NotyConstants.PORT).getValue();
	}

	public List<String> getEventsCollIndexes()
	{
		String mapKey = NotyConstants.MONGO_DB + "." + NotyConstants.MONGODB.PINT_DB + "." + NotyConstants.MONGODB.EVENTS_COLL + "." + NotyConstants.MONGODB.INDEXES;
		return (List<String>) configMap.get(mapKey).getValue();
	}

	public String getHttpHost()
	{
		return (String) configMap.get(NotyConstants.HTTP + "." + NotyConstants.HOST).getValue();
	}

	public int getHttpPort()
	{
		return (int) configMap.get(NotyConstants.HTTP + "." + NotyConstants.PORT).getValue();
	}

	public int getHttpMaxRequestSize()
	{
		return (int) configMap.get(NotyConstants.HTTP + "." + NotyConstants.HTTP_MAX_REQUEST_SIZE).getValue();
	}

	public int getValidityWindow()
	{
		return (int) configMap.get(NotyConstants.HTTP + "." + NotyConstants.HTTP_SESSIONS_VALIDITY).getValue();
	}

	public String getSecretKey()
	{
		return (String) configMap.get(NotyConstants.SESS_KEY).getValue();
	}

	public String getMacAlgoName()
	{
		return (String) configMap.get(NotyConstants.MAC_ALGO_NAME).getValue();
	}

	public String getAuthToken()
	{
		return (String) configMap.get(NotyConstants.AUTH_TOKEN).getValue();
	}
}
