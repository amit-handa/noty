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

import java.io.StringWriter;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.ClientCookieEncoder;
import io.netty.handler.codec.http.DefaultCookie;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.util.CharsetUtil;

public class ClientHandler extends SimpleChannelInboundHandler<HttpObject>
{
	static final Logger l = LoggerFactory.getLogger(ClientHandler.class );

	public static void init( Channel ch ) {
        // Prepare the HTTP request.
        JSONObject e = new JSONObject().put("id", "Ping").put("type", "SecSyncer").put("source", "PROD.Topaz").put("etime", System.currentTimeMillis() / 1000L)
                        .put("status", "OK").put("message", "Up Since last 15 minutes");

        StringWriter estr = new StringWriter();
        e.write(estr);
        String contentStr = estr.toString();
        FullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST,
        		"/events", ch.alloc().buffer().writeBytes(contentStr.getBytes()));

        request.headers().set(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.CLOSE);
        request.headers().set(HttpHeaders.Names.ACCEPT_ENCODING, HttpHeaders.Values.GZIP);
        request.headers().set(HttpHeaders.Names.CONTENT_LENGTH, request.content().readableBytes());

        System.out.println(" readable bytes {}" + request.content().readableBytes());
        l.info(" readable bytes {}", request.content().readableBytes());
        // Set some example cookies.
        request.headers().set(HttpHeaders.Names.COOKIE, ClientCookieEncoder.encode(new DefaultCookie("userId", "ahanda"), new DefaultCookie("sessStart", "kal")));

        // Send the HTTP request.
        ch.writeAndFlush(request);
	}

	@Override
	public void channelRead0(ChannelHandlerContext ctx, HttpObject msg)
	{
		if (msg instanceof HttpResponse)
		{
			HttpResponse response = (HttpResponse) msg;

			System.out.println("STATUS: " + response.getStatus());
			System.out.println("VERSION: " + response.getProtocolVersion());
			System.out.println();

			if (!response.headers().isEmpty())
			{
				for (String name : response.headers().names())
				{
					for (String value : response.headers().getAll(name))
					{
						System.out.println("HEADER: " + name + " = " + value);
					}
				}
				System.out.println();
			}

			if (HttpHeaders.isTransferEncodingChunked(response))
			{
				System.out.println("CHUNKED CONTENT {");
			}
			else
			{
				System.out.println("CONTENT {");
			}
		}
		if (msg instanceof HttpContent)
		{
			HttpContent content = (HttpContent) msg;

			System.out.print(content.content().toString(CharsetUtil.UTF_8));
			System.out.flush();

			if (content instanceof LastHttpContent)
			{
				System.out.println("} END OF CONTENT");
				ctx.close();
			}
		}
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
	{
		cause.printStackTrace();
		ctx.close();
	}
}
