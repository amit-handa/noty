package com.ahanda.techops.noty.db;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ahanda.techops.noty.Config;
import com.ahanda.techops.noty.NotyConstants;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.WriteResult;
import com.mongodb.util.JSON;

public class MongoDBManager
{
	static final Logger l = LoggerFactory.getLogger(MongoDBManager.class);

	private MongoClient dbconn;

	private static MongoDBManager instance;

	Config config = Config.getInstance();

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
					instance = new MongoDBManager();
				}
			}
		}
		return instance;

	}

	private MongoDBManager() throws JSONException, Exception
	{
		dbconn = new MongoClient(config.getMongoDbHost(), config.getMongoDbPort());
		l.info("Events DB non-null : {}", dbconn);
		ensureIndex();
	}

	@SuppressWarnings("unchecked")
	private void ensureIndex() throws Exception
	{
		Set<String> dbs = config.getMongoDbs();
		if (dbs == null)
			return;
		for (String dbname : dbs)
		{
			DB db = dbconn.getDB(dbname);
			JSONObject collections = config.getCollectionsForDb(dbname);
			for (String collname : (Set<String>) collections.keySet())
			{
				DBCollection dbcoll = db.getCollection(collname);
				JSONObject collconf = collections.getJSONObject(collname);
				JSONArray indexes = collconf.getJSONArray("indexes");
				
				// index for that collection could be null
				if (indexes != null)
				{
					for (int i = 0; i < indexes.length(); i++)
					{
						JSONArray index = indexes.optJSONArray(i);
						BasicDBObject dbindex = new BasicDBObject();
						for (int j = 0; j < index.length(); j++)
						{
							dbindex.append(index.getString(j), j + 1);
						}
						dbcoll.createIndex(dbindex);
					}
				}
			}
		}
	}

	public void execOp(JSONObject op)
	{
		DB pintDB = dbconn.getDB(op.getString("db"));
		DBCollection dbcoll = pintDB.getCollection(op.getString("collection"));
		switch (op.getString("action"))
		{
		case "save":
			doSave(dbcoll, op);
			break;
		case "find":
			doFind(dbcoll, op);
			break;
		case "update":
			doUpdate(dbcoll, op);
			break;
		default:
			l.error("Unsupport DB operation {}", op.getString("action"));
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

	public String getEvent(JSONObject query)
	{
		DB pintDB = dbconn.getDB(NotyConstants.Db.PINT_DB);
		DBCollection events = pintDB.getCollection(NotyConstants.Db.EVENTS_COLLECTION);
		DBObject dbquery = (DBObject) JSON.parse(query.toString());
		DBObject result = events.findOne(dbquery);
		if (result == null)
		{
			return null;
		}
		result.removeField("_id");

		String event = result.toString();

		l.info("Returning : {}", event);
		return event;
	}

	public static void main(String[] args) throws JSONException, Exception
	{
		testGet();
	}

	private static void testGet() throws JSONException, Exception
	{
		MongoDBManager mgr = getInstance();
		String event = mgr.getEvent(new JSONObject().put("source", "TOPAZ.PROD"));
		System.out.println(event);
	}

	public static void testInsert() throws JSONException, Exception
	{
		MongoDBManager mgr = getInstance();
		String j2 = "{'esource':'TOPAZ.PROD','etime':'2014-06-27 06:17:57.878','message':'Apache Camel 2.12.1 starting','id':'org.apache.camel.main.MainSupport','status':'START','etype':'initialization'}";
		// JSONObject o1 = new JSONObject(j1);
		JSONObject o2 = new JSONObject(j2);
		o2.put("action", "save");
		o2.put("db", NotyConstants.Db.PINT_DB);
		o2.put("collection", NotyConstants.Db.EVENTS_COLLECTION);
		// mgr.insertEvent(o1);
		mgr.execOp(o2);
		List<DBObject> list = mgr.dbconn.getDB(NotyConstants.Db.PINT_DB).getCollection(NotyConstants.Db.EVENTS_COLLECTION).getIndexInfo();

		for (DBObject o : list)
		{
			System.out.println(o.get("key"));
		}
	}

	public void save(JSONArray eventList)
	{
		int count = eventList.length();
		DB pintDB = dbconn.getDB(NotyConstants.Db.PINT_DB);
		DBCollection events = pintDB.getCollection(NotyConstants.Db.EVENTS_COLLECTION);
		events.createIndex(new BasicDBObject(NotyConstants.Db.ESOURCE, 1).append(NotyConstants.Db.ETIME, 2));
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
