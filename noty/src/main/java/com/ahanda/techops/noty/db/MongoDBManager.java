package com.ahanda.techops.noty.db;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
	static final Logger l = LoggerFactory.getLogger( MongoDBManager.class );

	private static final String pintDBName = "pint";
	private static final String eventsColl = "events";

	private static final String host = "192.168.1.18";

	private static final int port = 27017;

	private MongoClient mongoClient;

	private DB pintDB;

	private static MongoDBManager instance;
	static JSONObject config;

	/*
	 * This is for unit testing
	 */
	public static MongoDBManager getInstance() throws UnknownHostException
	{
		if (instance == null)
		{
			synchronized (MongoDBManager.class)
			{
				setConfig( null );
				if (instance == null)
				{
					instance = new MongoDBManager( config );
				}
			}
		}
		return instance;
	}

	public static void setConfig( JSONObject config ) {
		if( config == null ) {
			config = new JSONObject().put( "host", host ) 
					.put( "port", port )
					.put( "events", eventsColl )
					.put( "pintDB", pintDBName );
		}
		MongoDBManager.config = config;
	}

	private MongoDBManager( JSONObject config ) throws UnknownHostException
	{
		mongoClient = new MongoClient( config.getString( "host"), config.getInt( "port") );
		pintDB = mongoClient.getDB( config.getString( "pintDB" ) );
		l.info("Events DB non-null : {}", pintDB != null ? true : false);
	}

	public void insertEvent(JSONObject event)
	{
		insertEvent(event.toString());
	}

	public void insertEvent(String event)
	{
		DBCollection events = pintDB.getCollection( config.getString( "events") );
		events.createIndex(
			new BasicDBObject("source", 1).append("etime", 2) );
		DBObject dbObject = (DBObject) JSON.parse(event);
		WriteResult wr = events.insert(dbObject);
		int n = wr.getN();
		l.info("Event : {} inserted. Rows efected : {}", events, n);
	}

	public String getEvent( JSONObject query )
	{
		DBCollection events = pintDB.getCollection( config.getString( "events" ) );

		DBObject dbquery = (DBObject)JSON.parse( query.toString() );
		DBObject result = events.findOne(dbquery);
		if (result == null)
		{
			return null;
		}
		result.removeField("_id");

		String event = result.toString();

		l.info("Returning : {}", event);
		return result.toString();
	}

	public static void main(String[] args) throws UnknownHostException
	{
		testGet();
	}

	private static void testGet() throws UnknownHostException
	{
		MongoDBManager mgr = getInstance();
		String event = mgr.getEvent( new JSONObject().put( "source", "TOPAZ.PROD" ) );
		System.out.println(event);
	}

	public static void testInsert() throws UnknownHostException
	{
		MongoDBManager mgr = getInstance();
		String j2 = "{'source':'TOPAZ.PROD','etime':'2014-06-27 06:17:57.878','message':'Apache Camel 2.12.1 starting','id':'org.apache.camel.main.MainSupport','status':'START','etype':'initialization'}";
		// JSONObject o1 = new JSONObject(j1);
		JSONObject o2 = new JSONObject(j2);
		// mgr.insertEvent(o1);
		mgr.insertEvent(o2);
		List<DBObject> list = mgr.pintDB.getCollection( config.getString( "events" )).getIndexInfo();

		for (DBObject o : list)
		{
			System.out.println(o.get("key"));
		}
	}

	public void insertEvent(JSONArray eventList)
	{
		int count = eventList.length();
		DBCollection events = pintDB.getCollection( config.getString( "events" ) );
		events.createIndex(new BasicDBObject("source", 1).append("etime", 2));
		List<DBObject> list = new ArrayList<>(count);
		for (int i = 0; i < count; i++)
		{
			DBObject dbObject = (DBObject) JSON.parse(eventList.getJSONObject(i).toString());
			list.add(dbObject);
		}
		WriteResult wr = events.insert(list);
		System.out.println("Bulk insert rows affected : " + wr.getN());
	}
}
