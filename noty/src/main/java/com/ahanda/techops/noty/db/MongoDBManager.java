package com.ahanda.techops.noty.db;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ahanda.techops.noty.Config;
import com.ahanda.techops.noty.NotyConstants.MONGODB;
import com.ahanda.techops.noty.Utils;
import com.fasterxml.jackson.databind.JsonNode;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.WriteResult;
import com.mongodb.util.JSON;

public class MongoDBManager
{
	static final Logger l = LoggerFactory.getLogger(MongoDBManager.class);

	private MongoClient dbconn;

	private Config cf;

	private static MongoDBManager instance;

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
		cf = Config.getInstance();
		String host = cf.getMongoHost();
		int port = cf.getMongoPort();
		dbconn = new MongoClient(host, port);
		l.info("Events DB non-null : {}", dbconn);
		ensureIndex();
	}

	private void ensureIndex() throws Exception
	{
		DB db = dbconn.getDB(MONGODB.PINT_DB);
		DBCollection dbcoll = db.getCollection(MONGODB.EVENTS_COLL);
		List<String> indexes = cf.getEventsCollIndexes();
		// index for that collection could be null
		if (indexes != null)
		{
			BasicDBObject dbindex = new BasicDBObject();
			for (int i = 0; i < indexes.size(); i++)
			{
				String idx = indexes.get(i);
				dbindex.append(idx, i + 1);
			}
			dbcoll.createIndex(dbindex);
		}
	}

	public Map<String, Object> execOp(Map<String, Object> op)
	{
		DB pintDB = dbconn.getDB((String) op.get("db"));
		DBCollection dbcoll = pintDB.getCollection((String) op.get("collection"));
		String action = (String) op.get("action");

		switch (action)
		{
		case "save":
			List<Object> events = Utils.doCast(op.get("document"));
			return doSave(dbcoll, events);
		case "find":
			return doFind(dbcoll, op);
		case "update":
			return doUpdate(dbcoll, op);
		default:
			l.error("Unsupported DB operation {}", action);
			break;
		}

		return null;
	}

	private Map<String, Object> doUpdate(DBCollection dbcoll, Map<String, Object> op)
	{
		return new LinkedHashMap<String, Object>();
	}

	private Map<String, Object> doFind(DBCollection dbcoll, Map<String, Object> op)
	{
		l.info("reached exec find!");
		Map<String, Object> retval = new LinkedHashMap<String, Object>();
		Map<String, Object> matcher = Utils.doCast(op.get("matcher"));
		DBObject dbquery = new BasicDBObject(matcher);

		DBCursor result = dbcoll.find(dbquery);
		List<Object> results = new ArrayList<Object>();
		for (DBObject o : result)
		{
			o.removeField("_id");
			results.add(o.toMap());
		}

		retval.put("results", results);
		retval.put("status", "ok");
		return retval;
	}

	private Map<String, Object> doSave(DBCollection dbcoll, List<Object> op)
	{
		Map<String, Object> retval = new HashMap<String, Object>();

		int n = 0;
		for (int i = 0; i < op.size(); i++)
		{
			Map<String, Object> m = Utils.doCast(op.get(i));
			DBObject dbObject = new BasicDBObject(m);
			WriteResult wr = dbcoll.insert(dbObject);
			n += wr.getN();
		}
		l.info("row : {} inserted. Rows affected : {}", dbcoll, n);

		retval.put("status", "ok");
		retval.put("results", n);
		return retval;
	}

	public String getEvent(Map<String, Object> query)
	{
		DB db = dbconn.getDB((String) query.get("db"));
		DBCollection coll = db.getCollection((String) query.get("collection"));
		Map<String, Object> matcher = Utils.doCast(query.get("matcher"));
		DBObject dbquery = new BasicDBObject(matcher);
		DBObject result = coll.findOne(dbquery);
		if (result == null)
		{
			return null;
		}
		result.removeField("_id");

		String event = result.toString();

		l.info("find result : {}", event);
		return event;
	}

	public static void main(String[] args) throws JSONException, Exception
	{
		testGet();
	}

	private static void testGet() throws JSONException, Exception
	{
		MongoDBManager mgr = getInstance();
		Map<String, Object> matcher = new LinkedHashMap<String, Object>();
		matcher.put("esource", "TOPAZ.PROD");
		String event = mgr.getEvent(matcher);
		System.out.println(event);
	}

	public static void testInsert() throws JSONException, Exception
	{
		MongoDBManager mgr = getInstance();
		String j2 = "{'esource':'TOPAZ.PROD','etime':'2014-06-27 06:17:57.878','message':'Apache Camel 2.12.1 starting','id':'org.apache.camel.main.MainSupport','status':'START','etype':'initialization'}";
		// JsonNode o1 = new JsonNode(j1);
		Map<String, Object> o2 = new LinkedHashMap<String, Object>();
		o2.put("action", "save");
		o2.put("db", MONGODB.PINT_DB);
		o2.put("collection", MONGODB.EVENTS_COLL);
		mgr.execOp(o2);

		List<DBObject> list = mgr.dbconn.getDB(MONGODB.PINT_DB).getCollection(MONGODB.EVENTS_COLL).getIndexInfo();

		for (DBObject o : list)
		{
			Object obj = o.get("key");
			String s = obj != null ? obj.toString() : null;
			l.info(s);
		}
	}

	public void save(JsonNode eventList)
	{
		int count = eventList.size();
		DB pintDB = dbconn.getDB(MONGODB.PINT_DB);
		DBCollection events = pintDB.getCollection(MONGODB.EVENTS_COLL);
		events.createIndex(new BasicDBObject(MONGODB.ESOURCE_COL, 1).append(MONGODB.ETIME_COL, 2));

		List<DBObject> list = new ArrayList<>(count);
		for (int i = 0; i < count; i++)
		{
			DBObject dbObject = (DBObject) JSON.parse(eventList.get(i).toString());
			list.add(dbObject);
		}
		WriteResult wr = events.insert(list);
		l.info("Bulk insert rows affected : " + wr.getN());
	}
}
