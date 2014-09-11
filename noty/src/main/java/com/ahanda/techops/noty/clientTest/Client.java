/*
 * Copyright 2012 The Netty Project
 * 
 * The Netty Project licenses this file to you under the Apache License, version 2.0 (the "License"); you may not use
 * this file except in compliance with the License. You may obtain a copy of the License at:
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.ahanda.techops.noty.clientTest;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;

import java.io.IOException;
import java.net.URI;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ahanda.techops.noty.Config;
import com.ahanda.techops.noty.NotyConstants;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * A simple HTTP client that prints out the content of the HTTP response to {@link System#out} to test
 * {@link HttpSnoopServer}.
 */
public final class Client
{
	static final String URL = System.getProperty("url", "http://127.0.0.1:8080/events");

	static final Logger l = LoggerFactory.getLogger(Client.class);

	public static void main(String[] args) throws Exception
	{
		if (args.length == 1)
		{
			System.setProperty("PINT.conf", args[0]);
		}

		if (System.getProperty("PINT.conf") == null)
			throw new IllegalArgumentException();

		JsonNode config = null;
		Config cf = Config.getInstance();
		cf.setupConfig();

		String scheme = "http";
		String host = cf.getHttpHost();
		int port = cf.getHttpPort();

		if (port == -1)
		{
			port = 8080;
		}

		if (!"http".equalsIgnoreCase(scheme))
		{
			l.warn("Only HTTP is supported.");
			return;
		}

		// Configure SSL context if necessary.
		final boolean ssl = "https".equalsIgnoreCase(scheme);
		final SslContext sslCtx;
		if (ssl)
		{
			sslCtx = SslContext.newClientContext(InsecureTrustManagerFactory.INSTANCE);
		}
		else
		{
			sslCtx = null;
		}

		// Configure the client.
		EventLoopGroup group = new NioEventLoopGroup();
		try
		{
			Bootstrap b = new Bootstrap();
			b.group(group).channel(NioSocketChannel.class).handler(new ChannelInitializer<SocketChannel>()
			{
				@Override
				protected void initChannel(SocketChannel ch) throws Exception
				{
					ChannelPipeline p = ch.pipeline();
					p.addLast(new HttpClientCodec());
					// p.addLast( "decoder", new HttpResponseDecoder());
					// p.addLast( "encoder", new HttpRequestEncoder());

					// Remove the following line if you don't want automatic content decompression.
					// p.addLast(new HttpContentDecompressor());

					// Uncomment the following line if you don't want to handle HttpContents.
					p.addLast(new HttpObjectAggregator(10485760));

					p.addLast(new ClientHandler());
				}
			});

			// Make the connection attempt.
			Channel ch = b.connect(host, port).sync().channel();

			ClientHandler client = new ClientHandler();
			client.login(ch, ClientHandler.credential);
			// ClientHandler.pubEvent( ch, ClientHandler.event );

			// Wait for the server to close the connection.
			ch.closeFuture().sync();
			l.info("Closing Client side Connection !!!");
		}
		finally
		{
			// Shut down executor threads to exit.
			group.shutdownGracefully();
		}
	}
}
