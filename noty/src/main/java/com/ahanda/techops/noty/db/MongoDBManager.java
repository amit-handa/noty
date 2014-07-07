package com.ahanda.techops.noty.db;

public class MongoDBManager
{
	private static MongoDBManager mongoDBManager;

    /*
     * This is for unit testing
     */
    public static MongoDBManager getInstance(String dbPrefix)
    {
        if (mongoDBManager == null)
        {
            synchronized (MongoDBManager.class)
            {
                if (mongoDBManager == null)
                {
                    mongoDBManager = new MongoDBManager();
                }
            }
        }
        return mongoDBManager;
    }
    
    private MongoDBManager()
    {
    	
    }
}
