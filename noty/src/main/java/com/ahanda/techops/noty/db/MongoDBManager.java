package com.ahanda.techops.noty.db;

import java.net.UnknownHostException;
import java.util.List;

import org.json.JSONObject;

import com.ahanda.techops.noty.NotyConstants;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonObjectFormatVisitor;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.WriteResult;
import com.mongodb.util.JSON;

public class MongoDBManager
{
	private static final String eventsDBName = "eventsDB";

	private static final String host = "localhost";

	private static final int port = 27017;

	private MongoClient mongoClient;

	private DB eventsDB;

	private static MongoDBManager instance;

	/*
	 * This is for unit testing
	 */
	public static MongoDBManager getInstance() throws UnknownHostException
	{
		if (instance == null)
		{
			synchronized (MongoDBManager.class)
			{
				if (instance == null)
				{
					instance = new MongoDBManager();
				}
			}
		}
		return instance;
	}

	private MongoDBManager() throws UnknownHostException
	{
		mongoClient = new MongoClient(host, port);
		eventsDB = mongoClient.getDB(eventsDBName);
		System.out.println("Events DB not null : " + eventsDB != null ? true : false);
	}

	public void insertEvent(JSONObject event)
	{
		insertEvent(event.toString());
	}

	public void insertEvent(String event)
	{
		DBCollection events = eventsDB.getCollection(NotyConstants.EVENTS_COLLECTION);
		events.createIndex(new BasicDBObject("source", 1));
		events.createIndex(new BasicDBObject("etime", 1));
		DBObject dbObject = (DBObject) JSON.parse(event);
		WriteResult wr = events.insert(dbObject);
		wr.getN();
	}

	public void getEvent(JSONObject event)
	{

	}

	public static void main(String[] args) throws UnknownHostException
	{
		MongoDBManager mgr = getInstance();
		//String j1 = "{'database' : 'mkyongDB','table' : 'hosting'," + "'detail' : {'records' : 99, 'index' : 'vps_index1', 'active' : 'true'}}}";
		String j2 = "{'source':'TOPAZ.PROD','etime':'2014-06-27 06:17:57.878','message':'Apache Camel 2.12.1 starting','id':'org.apache.camel.main.MainSupport','status':'START','etype':'initialization'}";
		//JSONObject o1 = new JSONObject(j1);
		JSONObject o2 = new JSONObject(j2);
		//mgr.insertEvent(o1);
		mgr.insertEvent(o2);
		List<DBObject> list = mgr.eventsDB.getCollection(NotyConstants.EVENTS_COLLECTION).getIndexInfo();

		for (DBObject o : list)
		{
			System.out.println(o.get("key"));
		}
	}
}
