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

import static com.ahanda.techops.noty.NotyConstants.*;

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

	private static MongoDBManager instance;

	JSONObject config = Config.getInstance().get().getJSONObject("mongodb");
	JSONObject defconfig = Config.getDefault().getJSONObject("mongodb");

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
		String host = config.optString("host", defconfig.getString("host"));

		int port = config.optInt("port", defconfig.getInt("port"));

		dbconn = new MongoClient( host, port );
		l.info("Events DB non-null : {}", dbconn);
		ensureIndex();
	}

	@SuppressWarnings("unchecked")
	private void ensureIndex() throws Exception
	{
		JSONObject dbconf = config.getJSONObject(CONF.MONGODB.PINT_DB);
        DB db = dbconn.getDB(CONF.MONGODB.PINT_DB);
        for (String collname : (Set<String>)dbconf.keySet() )
        {
            DBCollection dbcoll = db.getCollection(collname);
            JSONObject collconf = dbconf.getJSONObject(collname);
            if (collconf != null)
            {
                JSONArray indexes = collconf.optJSONArray("indexes");
                // index for that collection could be null
                if (indexes != null)
                {
                    BasicDBObject dbindex = new BasicDBObject();
                    for (int i = 0; i < indexes.length(); i++)
                    {
                        String idx = indexes.getString(i);
                        dbindex.append(idx, i + 1);
                    }
                    dbcoll.createIndex(dbindex);
                }
            }
        }
	}

	public JSONObject execOp(JSONObject op)
	{
		DB pintDB = dbconn.getDB(op.getString("db"));
		DBCollection dbcoll = pintDB.getCollection(op.getString("collection"));
		switch (op.getString("action"))
		{
		case "save":
			return doSave(dbcoll, op.getJSONArray("document"));
		case "find":
			return doFind(dbcoll, op);
		case "update":
			return doUpdate(dbcoll, op);
		default:
			l.error("Unsupported DB operation {}", op.getString("action"));
			break;
		}
		
		return null;
	}

	private JSONObject doUpdate(DBCollection dbcoll, JSONObject op)
	{
		return new JSONObject();
	}

	private JSONObject doFind(DBCollection dbcoll, JSONObject op)
	{
		l.info("reached exec find!");
		JSONObject retval = new JSONObject();
        DBObject dbquery = (DBObject) JSON.parse(op.getJSONObject( "matcher" ).toString() );

		DBCursor result = dbcoll.find( dbquery );
		JSONArray results = new JSONArray();
		for( DBObject o : result )
		{
			o.removeField("_id");
			results.put(new JSONObject( o.toMap() ) );
		}

		retval.put("results", results );
		retval.put("status", "ok" );
		return retval;
	}

	private JSONObject doSave(DBCollection dbcoll, JSONArray op)
	{
		JSONObject retval = new JSONObject();

		int n = 0;
		for( int i = 0; i < op.length(); i++ ) {
            DBObject dbObject = (DBObject) JSON.parse(op.get(i).toString());
            WriteResult wr = dbcoll.insert(dbObject);
            n += wr.getN();
		}
            l.info("row : {} inserted. Rows affected : {}", dbcoll, n);
        retval.put("status", "ok" ).put( "results", n );
		return retval;
	}

	public String getEvent(JSONObject query)
	{
		DB db = dbconn.getDB( query.getString("db") );
		DBCollection coll = db.getCollection( query.getString("collection") );
		DBObject dbquery = (DBObject) JSON.parse(query.getJSONObject( "matcher" ).toString() );
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
		String event = mgr.getEvent(new JSONObject().put("esource", "TOPAZ.PROD"));
		System.out.println(event);
	}

	public static void testInsert() throws JSONException, Exception
	{
		MongoDBManager mgr = getInstance();
		String j2 = "{'esource':'TOPAZ.PROD','etime':'2014-06-27 06:17:57.878','message':'Apache Camel 2.12.1 starting','id':'org.apache.camel.main.MainSupport','status':'START','etype':'initialization'}";
		// JSONObject o1 = new JSONObject(j1);
		JSONObject o2 = new JSONObject(j2);
		o2.put("action", "save");
		o2.put("db", CONF.MONGODB.PINT_DB);
		o2.put("collection", CONF.MONGODB.PINT.EVENTS_COLL);
		mgr.execOp(o2);
		List<DBObject> list = mgr.dbconn.getDB(CONF.MONGODB.PINT_DB).getCollection(CONF.MONGODB.PINT.EVENTS_COLL).getIndexInfo();

		for (DBObject o : list)
		{
			Object obj = o.get("key");
			String s = obj != null ? obj.toString() : null;
			l.info(s);
		}
	}

	public void save(JSONArray eventList)
	{
		int count = eventList.length();
		DB pintDB = dbconn.getDB(CONF.MONGODB.PINT_DB);
		DBCollection events = pintDB.getCollection(CONF.MONGODB.PINT.EVENTS_COLL);
		events.createIndex(new BasicDBObject(CONF.MONGODB.PINT.EVENTS.ESOURCE, 1).append(CONF.MONGODB.PINT.EVENTS.ETIME, 2));
		
		List<DBObject> list = new ArrayList<>(count);
		for (int i = 0; i < count; i++)
		{
			DBObject dbObject = (DBObject) JSON.parse(eventList.getJSONObject(i).toString());
			list.add(dbObject);
		}
		WriteResult wr = events.insert(list);
		l.info("Bulk insert rows affected : " + wr.getN());
	}
}
