package com.ahanda.techops.noty;

import java.util.Set;

public class NotyConstants
{
	public static final String REQUEST_TYPE = "request_type";

	public static final String PUBLISH_EVENT = "publish_event";

	public static class CONF
	{
		public static class HTTP
		{
			public static final String HOST = "host";

			public static final String PORT = "port";

			public static final String MAX_REQUEST_SIZE = "maxRequestSize";
		}

		public static class MONGODB
		{
			public static final String HOST = "host";

			public static final String PORT = "port";

            public static final String PINT_DB = "pint";

			public static class PINT
			{
                public static final String EVENTS_COLL = "events";

                public static final String USERS_COLL = "users";

                public static class EVENTS
                {
                    public static final String ESOURCE = "esource";

                    public static final String ETIME = "etime";
                }

                public static class USERS
                {
                    public static final String name = "esource";
                }

			}
		}
	}
}
