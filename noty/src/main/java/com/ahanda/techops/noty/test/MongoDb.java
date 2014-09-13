package com.ahanda.techops.noty.test;

import java.util.List;
import java.util.Map;

import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.ahanda.techops.noty.Config;
import com.ahanda.techops.noty.db.MongoDBManager;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

public class MongoDb
{
	private static MongoDBManager m;

	private static Config cf;

	public static void main(String[] args) throws JSONException, Exception
	{
		cf = Config.getInstance();
		cf.setupConfig();
		m = MongoDBManager.getInstance();
		// insertEvents(500);
		int limit = 20;
		List<Map> result = m.getPagedEvents(null, limit, null, -1);
		long t1 = (long) result.get(0).get("etime");
		long t2 = (long) result.get(result.size() - 1).get("etime");
		String ev1 = (String) result.get(0).get("esource");
		String ev2 = (String) result.get(result.size() - 1).get("esource");
		System.out.println("Latest Time : " + t1 + " , 20th element time : " + t2);
		System.out.println("Latest event : " + ev1 + " , 20th element event : " + ev2);
		ObjectId id = new ObjectId((String) result.get(result.size() - 1).get("_id"));
		result = m.getPagedEvents(id, 50, null, -1);
		t1 = (long) result.get(0).get("etime");
		t2 = (long) result.get(result.size() - 1).get("etime");
		ev1 = (String) result.get(0).get("esource");
		ev2 = (String) result.get(result.size() - 1).get("esource");
		System.out.println("Latest Time : " + t1 + " , 50th element time : " + t2);
		System.out.println("Latest event : " + ev1 + " , 50th element event : " + ev2);
		Map val = result.get(result.size() - 5);
		String oid = (String) val.get("_id");
		System.out.println("Oid : " + oid + "esource : " + (String) val.get("esource"));
		id = new ObjectId(oid);
		result = m.getPagedEvents(id, 50, null, 1);
		t1 = (long) result.get(0).get("etime");
		t2 = (long) result.get(result.size() - 1).get("etime");
		ev1 = (String) result.get(0).get("esource");
		ev2 = (String) result.get(result.size() - 1).get("esource");
		System.out.println("Latest Time : " + t1 + " , 50th element time : " + t2);
		System.out.println("Latest event : " + ev1 + " , 50th element event : " + ev2);
	}

	private static void insertEvents(int count) throws JSONException, Exception
	{
		String sample = "{'esource':'GK.%d','etime':%d,'message':'hi gautam','id':'org.apache.camel.main.MainSupport','status':'START','etype':'initialization'}";
		ObjectMapper mapper = new ObjectMapper();
		JsonFactory factory = mapper.getFactory(); // since 2.1 use mapper.getFactory() instead
		ArrayNode arr = mapper.createArrayNode();
		JSONArray jarr = new JSONArray();
		for (int i = 0; i < count; i++)
		{
			String x = String.format(sample, (i + 1), System.currentTimeMillis());
			Thread.sleep(1);
			JSONObject objj = new JSONObject(x);
			jarr.put(objj);
		}
		JsonNode rr = mapper.readTree(jarr.toString());
		m.save(rr);
	}

}
