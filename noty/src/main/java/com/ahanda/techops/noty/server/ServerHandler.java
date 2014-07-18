/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package com.ahanda.techops.noty.server;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.DecoderResult;
import io.netty.handler.codec.http.Cookie;
import io.netty.handler.codec.http.CookieDecoder;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpMessage;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMessage;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.ServerCookieEncoder;
import io.netty.util.CharsetUtil;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.netty.handler.codec.http.HttpHeaders.Names.*;
import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.http.HttpVersion.*;

public class ServerHandler extends SimpleChannelInboundHandler<FullHttpRequest>
{

	// HttpMessage req;
	// CompositeByteBuf content;
	// boolean last;

	/** Buffer that stores the response content */
	private final StringBuilder buf = new StringBuilder();

	static final Logger l = LoggerFactory.getLogger(ServerHandler.class);

	@Override
	public void channelReadComplete(ChannelHandlerContext ctx)
	{
		ctx.flush();

		/*
		 * ByteBuf fullcontent = content.copy( 0, content.capacity() ); content.release();
		 * 
		 * 
		 * l.info("read complete {}", fullcontent.toString( CharsetUtil.UTF_8 ) );
		 * 
		 * fullcontent.release(); reset();
		 */
	}

	/*
	 * void reset() { req = null; content = null; last = false; }
	 */

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest msg)
	{
		assert msg instanceof LastHttpContent;
		l.info("received request ! {} {}", msg, msg.content().toString(CharsetUtil.UTF_8));
		/*
		 * if( msg instanceof HttpMessage ) { if( req != null ) l.error( "request is already assigned !!!!!!");
		 * 
		 * req = (HttpMessage)msg; } if( msg instanceof HttpContent ) { if( content == null ) content = ctx.alloc().compositeBuffer(); l.info( "received content !");
		 * msg.content().retain(); content.addComponent( msg.content() ); }
		 * 
		 * if( msg instanceof LastHttpContent ) { assert content != null; l.info( "received last content !"); last = true; }
		 */
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
	{
		cause.printStackTrace();
		ctx.close();
	}
}
