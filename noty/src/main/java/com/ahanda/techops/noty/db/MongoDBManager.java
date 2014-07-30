package com.ahanda.techops.noty.db;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ahanda.techops.noty.NotyConstants;
import com.ahanda.techops.noty.Utils;
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

	private MongoClient dbconn;

	private static MongoDBManager instance;
	static JSONObject config;

	static {
		config = new JSONObject()
			.put( "host", "192.168.1.18" ) 
            .put( "port", 27017 )
            .put( "dbs", new JSONObject()
            	.put( "pint", new JSONObject()
                    .put( "events", new JSONObject()
                    	.put( "indexes", new JSONArray().put( new JSONArray()
                    		.put( "esource" )
                    		.put( "etime" ) ) )
                    		) ) );
	}
	/*
	 * This is for unit testing
	 */
	public static MongoDBManager getInstance() throws JSONException, Exception
	{
		if (instance == null)
		{
			synchronized (MongoDBManager.class)
			{
				if (instance == null)
				{
					instance = new MongoDBManager( config );
				}
			}
		}
		return instance;

	}

	public static void setConfig( JSONObject config ) {
		MongoDBManager.config = config;
	}

	private MongoDBManager( JSONObject config ) throws JSONException, Exception
	{
		assert config != null;
		
		dbconn = new MongoClient( config.getString( "host"), config.getInt( "port") );
		l.info("Events DB non-null : {}", dbconn );
		ensureIndex( config.getJSONObject("dbs") );
	}

	@SuppressWarnings("unchecked")
	private void ensureIndex(JSONObject dbs) throws Exception
	{
		for( String dbname : (Set<String>)dbs.keySet() ) {
			DB db = dbconn.getDB(dbname);
			JSONObject dbconf = dbs.getJSONObject( dbname );
			for( String collname : (Set<String>)dbconf.keySet() ) {
				DBCollection dbcoll = db.getCollection( collname );
                JSONObject collconf = dbconf.getJSONObject( collname );
                JSONArray indexes = collconf.getJSONArray("indexes");
                for( int i = 0; i < indexes.length(); i++ ) {
                	JSONArray index = indexes.getJSONArray(i);
                	BasicDBObject dbindex = new BasicDBObject();
                	for( int j = 0; j < index.length(); j++ ) {
                		dbindex.append( index.getString(j), j+1);
                	}
                    dbcoll.createIndex( dbindex );
                }
			}
		}
	}

	public void execOp( JSONObject op )
	{
		DB pintDB = dbconn.getDB( op.getString( "db" ) );
		DBCollection dbcoll = pintDB.getCollection( op.getString( "collection") );
		switch( op.getString("action" ) ) {
		case "save":
			doSave( dbcoll, op );
			break;
		case "find":
			doFind( dbcoll, op );
			break;
		case "update":
			doUpdate( dbcoll, op );
			break;
        default:
        	l.error( "Unsupport DB operation {}", op.getString("action"));
            break;
		}
	}
	private void doUpdate(DBCollection dbcoll, JSONObject op)
	{
	}

	private void doFind(DBCollection dbcoll, JSONObject op)
	{
	}

	private void doSave(DBCollection dbcoll, JSONObject op)
	{
		DBObject dbObject = (DBObject) JSON.parse(op.toString());
		WriteResult wr = dbcoll.insert(dbObject);
		int n = wr.getN();
		l.info("row : {} inserted. Rows affected : {}", dbcoll, n);
	}


	public String getEvent( JSONObject query )
	{
		DB pintDB = dbconn.getDB( "pint" );
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

	public static void main(String[] args) throws JSONException, Exception
	{
		testGet();
	}

	private static void testGet() throws JSONException, Exception
	{
		MongoDBManager mgr = getInstance();
		String event = mgr.getEvent( new JSONObject().put( "source", "TOPAZ.PROD" ) );
		System.out.println(event);
	}

	public static void testInsert() throws JSONException, Exception
	{
		MongoDBManager mgr = getInstance();
		String j2 = "{'source':'TOPAZ.PROD','etime':'2014-06-27 06:17:57.878','message':'Apache Camel 2.12.1 starting','id':'org.apache.camel.main.MainSupport','status':'START','etype':'initialization'}";
		// JSONObject o1 = new JSONObject(j1);
		JSONObject o2 = new JSONObject(j2);
		o2.put( "action", "save" );
		o2.put( "db", "pint" );
		o2.put( "collection", "events" );
		// mgr.insertEvent(o1);
		mgr.execOp(o2);
		List<DBObject> list = mgr.dbconn.getDB( "pint" ).getCollection( config.getString( "events" )).getIndexInfo();

		for (DBObject o : list)
		{
			System.out.println(o.get("key"));
		}
	}

	public void save(JSONArray eventList)
	{
		int count = eventList.length();
		DB pintDB = dbconn.getDB( "pint" );
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
