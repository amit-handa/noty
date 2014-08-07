package com.ahanda.techops.noty.http;

import java.util.*; // arraylist
import java.nio.file.*; //Path,paths,files;
import java.nio.charset.Charset;
import java.net.URL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.LoggerContext;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.ClientCookieEncoder;
import io.netty.handler.codec.http.Cookie;
import io.netty.handler.codec.http.CookieDecoder;
import io.netty.handler.codec.http.DefaultCookie;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.util.CharsetUtil;

import javax.crypto.*; //Mac
import javax.crypto.spec.*; //SecretKeySpec

import org.apache.commons.codec.binary.Base64;

import com.ahanda.techops.noty.Config;
import com.ahanda.techops.noty.NotyConstants;
import com.ahanda.techops.noty.Utils;
import com.ahanda.techops.noty.http.message.FullEncodedResponse;
import com.ahanda.techops.noty.http.message.Request;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class AuthMgr extends SimpleChannelInboundHandler<Request>
{
	// process all the uconfs and populate user-data
	private static final Logger logger = LoggerFactory.getLogger(AuthMgr.class);

	private Mac mac;

	private String secretKey; // $TRDATADIR
	
	private long validityWindow;

	private Map<String, UserInfo> userInfos = new HashMap<String, UserInfo>();

	private Set<String> invalidSessions = new HashSet<String>();
	private static Set< Cookie > nocookies = new HashSet< Cookie >();

	public AuthMgr()
	{
		// Do not autorelease HttpObject since
		// it is passed through
		super(false);

		doInit();
	}

	public void doInit()
	{
		try
		{
			ObjectNode config = Config.getInstance().get();
			ObjectNode defconfig = Config.getDefault();
			String macAlgoName = defconfig.get( NotyConstants.MAC_ALGO_NAME ).asText();
			
			JsonNode tmp = config.get( NotyConstants.MAC_ALGO_NAME );
			if( tmp != null )
				macAlgoName = tmp.asText();

			validityWindow = defconfig.get("http").get( NotyConstants.HTTP_SESSIONS_VALIDITY ).asLong();
			tmp = config.get( "http" ).get( NotyConstants.HTTP_SESSIONS_VALIDITY );
			if( tmp != null )
				validityWindow = tmp.asLong();

			mac = Mac.getInstance( macAlgoName );

			secretKey = config.get( NotyConstants.SESS_KEY ).asText();

			SecretKeySpec sks = new SecretKeySpec(secretKey.getBytes(), macAlgoName );
			mac.init(sks);
		}
		catch (Exception exc)
		{
			logger.warn("Exception found: {} {}", exc.getMessage(), exc.getStackTrace());
		}

		logger.info("Inited ! {} {}", new Object[] { secretKey, mac });
	}

	private static class UserInfo
	{
		public String role;

		public String password;

		public String fname; // properties file

		public void set(String field, String fvalue)
		{
			switch (field)
			{
			case "role":
				role = fvalue;
				break;
			case "password":
				password = fvalue;
				break;
			default:
				break;
			}
		}
	}

    public static Map< String, Object > checkCredential( final Mac mac, final Map< String, Object > msg ) {
		String userId = (String)msg.get( "userId" );
		String password = (String)msg.get( "password" );

		String sessId = null;

		if( password != null ) {
			long sessStart = System.currentTimeMillis() / 1000L;
			sessId = getSessId( mac, userId, sessStart );
			Map< String, Object > reply = new HashMap< String, Object >();
			reply.put( "userId", userId );
			reply.put( "sessStart", sessStart );
			reply.put( "sessId", sessId );
			reply.put( "status", "ok" );

			return reply;
		}

		sessId = (String)msg.get( "sessId" );
		long sessStart = (long)msg.get( "sessStart" );
		String nsessId = getSessId( mac, userId, sessStart );
		if( sessId.equals( nsessId ) )
			msg.put( "status", "ok" );
		else msg.put( "status", "error" );
		return msg;
	}

	public static String getSessId( final Mac mac, String userId, long sessStart ) {
		String cval = String.format("%s&%d", userId, sessStart );
		return new String(Base64.encodeBase64(mac.doFinal(cval.getBytes())));
	}

	@Override
	protected void channelRead0(final ChannelHandlerContext ctx, final Request request ) throws Exception
	{
		FullHttpRequest httpReq = request.getHttpRequest();
		String path = request.getRequestPath();
		HttpMethod accessMethod = httpReq.getMethod();

        String cookiestr = httpReq.headers().get( HttpHeaders.Names.COOKIE );
        Set< Cookie > cookies = nocookies;
        if( cookiestr != null )
            cookies = CookieDecoder.decode( cookiestr );
		
		logger.info("Intercepted msg : headers {} {} {}!!!", path, cookies);

		Cookie sessIdc = null, userIdc = null, sessStartc = null;
		for( Cookie c : cookies ) {
			switch( c.getName()) {
			case "sessId" :
				sessIdc = c;
				break;
			case "userId" :
				userIdc = c;
				break;
			case "sessStart" :
				sessStartc = c;
				break;
            default:
                break;
			}
		}

		String sessId = null, userId = null;
		long sessStart = -1;
        if (path.matches("/login") && accessMethod == HttpMethod.POST ) {
            String body = httpReq.content().toString( CharsetUtil.UTF_8 );

			logger.info("Login request: {}", path);
			Map< String, String > credentials = Utils.om.readValue( body, new TypeReference< Map< String, String > >() {} );
            userId = credentials.get("userId");

            if (userId == null) { // authenticate userId
                logger.debug("Cannot validate User {}, Fix it, continuing as usual !" );
                // return null;
            }
    
            sessStart = System.currentTimeMillis() / 1000L;
            sessId = getSessId( mac, userId, sessStart );

            FullHttpResponse resp = request.setResponse( HttpResponseStatus.OK, Unpooled.buffer(0) );
            for( Cookie reqcookie : cookies ) {
            	reqcookie.setMaxAge( 0 );
            }

            sessIdc = new DefaultCookie( "sessId", sessId );
            sessIdc.setHttpOnly( true );
            sessIdc.setPath("/");
            cookies.remove( sessIdc );

            sessIdc.setMaxAge( sessStart + validityWindow );
            cookies.add( sessIdc );

            sessStartc = new DefaultCookie( "sessStart", Long.toString(sessStart) );
            sessStartc.setHttpOnly( true );
            sessStartc.setPath("/");
            cookies.remove( sessStartc );

            sessIdc.setMaxAge( sessStart + validityWindow );
            cookies.add( sessStartc );

            userIdc = new DefaultCookie( "userId", userId );
            userIdc.setHttpOnly( true );
            userIdc.setPath("/");
            cookies.remove( userIdc );

            userIdc.setMaxAge( sessStart + validityWindow );
            cookies.add( userIdc );

            resp.headers().set( HttpHeaders.Names.SET_COOKIE, ClientCookieEncoder.encode( cookies ) );
            ctx.writeAndFlush( new FullEncodedResponse( request, resp ));
            return;
        }
        
        if (sessIdc == null && userIdc == null && sessStartc == null ) { // invalid request, opensession first
			logger.error("Invalid request, session doesnt exist!");
            FullHttpResponse resp = request.setResponse(HttpResponseStatus.UNAUTHORIZED, ctx.alloc().buffer().writeBytes("Authorization absent, kindly sign-in first".getBytes() ) );
            ctx.writeAndFlush(new FullEncodedResponse( request, resp ));
			return;
		}

        if( sessId == null )
            sessId = sessIdc.getValue();
		if( userId == null )
            userId = userIdc.getValue();
		if( sessStart < 0 )
            sessStart = Long.valueOf( sessStartc.getValue() );
		
		String csessid = getSessId( mac, userId, sessStart );

		if (!csessid.equals(sessId))
		{
			logger.error("Invalid credentials {} {}!!", sessId, csessid);
			FullHttpResponse resp = request.setResponse(HttpResponseStatus.UNAUTHORIZED, ctx.alloc().buffer().writeBytes( "Unauthorized access: kindly sign-in again".getBytes() ) );
            ctx.writeAndFlush(new FullEncodedResponse( request, resp ));
			return;
		}

		long elapseSecs = System.currentTimeMillis() / 1000L - sessStart;
		if (invalidSessions.contains(sessId) || elapseSecs > validityWindow )
		{
			FullHttpResponse resp = request.setResponse(HttpResponseStatus.UNAUTHORIZED, ctx.alloc().buffer().writeBytes( String.format("Session Expired : %d", elapseSecs).getBytes() ) );
            ctx.writeAndFlush(new FullEncodedResponse( request, resp ));
			return;
		}

		if (path.matches("/logout") && accessMethod == HttpMethod.DELETE ) {
            invalidSessions.add(sessId);
            FullHttpResponse resp = request.setResponse(HttpResponseStatus.OK, ctx.alloc().buffer().writeBytes( "Session Deleted Successfully".getBytes() ) );
            ctx.writeAndFlush( new FullEncodedResponse( request, resp ) );
            return;
		}

		httpReq.headers().set("userId", userId );
		httpReq.headers().set("sessStart", Long.toString( sessStart ) );
		ctx.fireChannelRead( request );
	}
}
