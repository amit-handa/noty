package com.ahanda.techops.noty;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;

public class Config
{
	private static final Logger l = LoggerFactory.getLogger(Config.class);

	private static final String CONFIG_FILE = "PINT.conf";

	private static final String CONFIG = "config";

	Map<String, Object> configMap;

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
		String conff = System.getProperty(CONFIG_FILE);
		Properties prop = new Properties();
		InputStream confStream = null;
		try
		{
			confStream = new FileInputStream(conff);
			prop.load(confStream);
		}
		catch (Exception e)
		{
			l.error("error in loading properties {} {}", e.getMessage(), e.getStackTrace());
		}
		finally
		{
			if (confStream != null)
				confStream.close();
		}

		String jsonConf = prop.getProperty(CONFIG);
		l.debug("The config file path is {}", jsonConf);

		URL configFile = Paths.get(jsonConf).toUri().toURL();
		if (configFile == null)
		{
			throw new FileNotFoundException("Config file not found");
		}
		File f = new File(configFile.getFile());
		byte[] encoded = Files.readAllBytes(Paths.get(f.getAbsolutePath()));
		String jsonStr = new String(encoded, Charset.defaultCharset());
		configMap = Utils.om.readValue(jsonStr, new TypeReference<HashMap<String, Object>>()
		{
		});
	}

	public Map<String,Object> getMongoDBConfig()
	{
		return (Map<String, Object>) configMap.get(NotyConstants.MONGO_DB);
	}

	@SuppressWarnings("unchecked")
	public long getHttpValidityWindow()
	{
		return (long)((Map<String, Object>)configMap.get(NotyConstants.HTTP)).get( NotyConstants.HTTP_SESSIONS_VALIDITY );
	}
	
	@SuppressWarnings("unchecked")
	public String getHttpHost()
	{
		return (String)((Map<String, Object>)configMap.get(NotyConstants.HTTP)).get( NotyConstants.HOST);
	}
	
	@SuppressWarnings("unchecked")
	public int getHttpPort()
	{
		return (int)((Map<String, Object>)configMap.get(NotyConstants.HTTP)).get( NotyConstants.PORT);
	}
	
	@SuppressWarnings("unchecked")
	public int getHttpMaxRequestSize()
	{
		return (int)((Map<String, Object>)configMap.get(NotyConstants.HTTP)).get( NotyConstants.HTTP_MAX_REQUEST_SIZE);
	}
	
	public Object getHttpConfig()
	{
		return configMap.get(NotyConstants.HTTP);
	}

	public String getSecretKey()
	{
		return (String) configMap.get(NotyConstants.SESS_KEY);
	}

	public String getMacAlgoName()
	{
		return (String) configMap.get(NotyConstants.MAC_ALGO_NAME);
	}

	public String getAuthToken()
	{
		return (String) configMap.get(NotyConstants.AUTH_TOKEN);
	}
}
