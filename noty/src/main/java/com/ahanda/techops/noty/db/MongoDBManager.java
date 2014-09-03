package com.ahanda.techops.noty.db;

import static com.ahanda.techops.noty.NotyConstants.HOST;
import static com.ahanda.techops.noty.NotyConstants.PORT;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.text.html.HTMLDocument.HTMLReader.IsindexAction;

import org.bson.types.ObjectId;
import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ahanda.techops.noty.Config;
import com.ahanda.techops.noty.NotyConstants.MONGODB;
import com.ahanda.techops.noty.Utils;
import com.fasterxml.jackson.databind.JsonNode;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.CommandResult;
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

	private DB pintDb;

	private DBCollection events;

	private DBCollection userColl;

	private static volatile MongoDBManager instance;

	Map<String, Object> mongoConfig;

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
		mongoConfig = Config.getInstance().getMongoDBConfig();
		String host = (String) mongoConfig.get(HOST);
		int port = (int) mongoConfig.get(PORT);

		dbconn = new MongoClient(host, port);
		pintDb = dbconn.getDB(MONGODB.PINT_DB);
		events = pintDb.getCollection(MONGODB.EVENTS_COLL);
		userColl = pintDb.getCollection(MONGODB.USERS_COLL);
		l.info("Events DB non-null : {}", dbconn);
		ensureIndex();
	}

	@SuppressWarnings("unchecked")
	private void ensureIndex() throws Exception
	{
		Map<String, Object> dbconf = (Map<String, Object>) mongoConfig.get(MONGODB.PINT_DB);
		for (Entry<String, Object> e : dbconf.entrySet())
		{
			DBCollection dbcoll = pintDb.getCollection(e.getKey());
			Map<String, Object> collConf = (Map<String, Object>) e.getValue();
			List<String> indexes = (List<String>) collConf.get("indexes");

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
	}

	public Map<String, Object> execOp(Map<String, Object> op)
	{
		DB db = dbconn.getDB((String) op.get("db"));
		String action = (String) op.get("action");
		DBCollection dbcoll = null;
		switch (action)
		{
		case "save":
			dbcoll = db.getCollection((String) op.get("collection"));
			List<Object> events = Utils.doCast(op.get("document"));
			return doSave(dbcoll, events);
		case "find":
			dbcoll = db.getCollection((String) op.get("collection"));
			return doFind(dbcoll, op);
		case "update":
			dbcoll = db.getCollection((String) op.get("collection"));
			return doUpdate(dbcoll, op);
		case "command":
			return doCommand(db, op);
		default:
			l.error("Unsupported DB operation {}", action);
			break;
		}

		return null;
	}

	private Map<String, Object> doCommand(DB db, Map<String, Object> op)
	{
		Map<String, Object> cmd = Utils.doCast(op.get("command"));
		CommandResult cr = db.command(new BasicDBObject(cmd));
		return Utils.doCast(cr.toMap());
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
			// upserts if the doc exists already.
			WriteResult wr = dbcoll.save(dbObject, null);
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

	/**
	 * This function returns the paged entries from the MongoDB. Pass the minimum ObjectId and it will return all the
	 * values greater than that. Results will be in the ascending order.
	 * 
	 * @param id
	 * @param limit
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	public List<Map> getPagedEvents(ObjectId id, int limit)
	{
		BasicDBObject dbquery = new BasicDBObject();
		if (id != null)
			dbquery.append("_id", new BasicDBObject().append("$lt", id));

		DBCursor result = events.find(dbquery).limit(limit).sort(new BasicDBObject().append("_id", -1));
		if (result == null)
			return null;
		List<Map> results = new ArrayList<Map>();
		for (DBObject o : result)
		{
			results.add(o.toMap());
		}
		return results;
	}

	public boolean subscribeNotification(String userId, Map<String, Object> matcher)
	{
		List<Object> queryList = (List<Object>) matcher.get("notification");
		BasicDBObject notfQuery = buildQuery(queryList);
		BasicDBObject userObj = new BasicDBObject().append("_id", userId);
		BasicDBObject notificationObj = new BasicDBObject().append("$addToSet", new BasicDBObject("notifications", notfQuery));
		userColl.update(userObj, notificationObj, true, false);
		return true;
	}

	private BasicDBObject buildQuery(List<Object> queryList)
	{
		BasicDBList dbl = null;
		BasicDBObject subQueryObject = null;

		if (queryList.size() > 1) // this means there is an OR query
			dbl = new BasicDBList();

		for (Object q : queryList)
		{
			List<Object> subQuery = (List<Object>) q;
			subQueryObject = new BasicDBObject();
			for (Object sq : subQuery)
			{
				/* here we will get the map of key value pairs to apply AND operator */
				Map<String, Object> m = (Map<String, Object>) sq;
				String type = "eq";
				if (m.containsKey("type"))
				{
					type = (String) m.get("type");
					m.remove("type");
				}
				
				for (Entry<String, Object> e : m.entrySet()) // there will be just one key value pair in this
				{
					String key = e.getKey();
					Object val = e.getValue();
					switch (type)
					{
					case "ne":
					case "gt":
					case "gte":
					case "lt":
					case "lte":
						subQueryObject.append(key, new BasicDBObject("$" + type, val));
						break;
					case "range":
						List<Object> l = (List<Object>) val;
						long v1 = (long) l.get(0);
						long v2 = (long) l.get(1);
						subQueryObject.append(key, new BasicDBObject("$gte", v1).append("$lte", v2));
						break;
					case "eq":
						subQueryObject.append(key, val);
						break;
					}
				}
			}
			if (dbl != null)
				dbl.add(subQueryObject);
		}
		if(dbl != null) // shows there is a OR here
			return new BasicDBObject("$or", dbl);
		return subQueryObject;
	}

	public List<Map> getNotifications(String userId, Map<String, Object> matcher)
	{
		List<Object> queryList = (List<Object>) matcher.get("notification");
		//int limit = (Integer) matcher.get("limit");
		//String objId = (String)matcher.get("objectId");
		//ObjectId objectId = new ObjectId(objId);
		BasicDBObject dbquery = new BasicDBObject();
		BasicDBObject notfQuery = buildQuery(queryList);
		DBCursor cursor = events.find(notfQuery);
		List<Map> results = new ArrayList<Map>();
		while(cursor.hasNext())
		{
			DBObject dbo = cursor.next();
			results.add(dbo.toMap());
		}
		return results;
	}
}
