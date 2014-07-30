package com.ahanda.techops.noty.http;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.util.concurrent.DefaultEventExecutorGroup;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ahanda.techops.noty.Config;
import com.ahanda.techops.noty.db.MongoDBManager;
import com.ahanda.techops.noty.http.exception.DefaultExceptionHandler;

/**
 * Discards any incoming data.
 */
public class ServerMain
{
	private Config config;

	private String host;

	private int port;

	private static final Logger l = LoggerFactory.getLogger(ServerMain.class);

	public ServerMain(int port)
	{
		this.port = port;
	}

	public ServerMain()
	{
		l.info("Instantiating server");
	}

	public Config getConfig()
	{
		return config;
	}

	public void run() throws Exception
	{
		try
		{
			config = Config.getInstance();
		}
		catch (IOException e)
		{
			l.error("Exception while reading config file, server cannot be started", e);
			return;
		}
		host = config.getHttpHost();
		port = config.getHttpPort();
		final DefaultEventExecutorGroup group = new DefaultEventExecutorGroup(100);
		EventLoopGroup bossGroup = new NioEventLoopGroup();
		EventLoopGroup workerGroup = new NioEventLoopGroup();
		try
		{
			MongoDBManager.getInstance();
			ServerBootstrap b = new ServerBootstrap(); // (2)
			b.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class) // (3)
					.childHandler(new ChannelInitializer<SocketChannel>()
					{
						@Override
						public void initChannel(SocketChannel ch) throws Exception
						{
							ChannelPipeline chp = ch.pipeline();
							chp.addLast("decoder", new HttpRequestDecoder());
							chp.addLast("encoder", new HttpResponseEncoder());
							chp.addLast("aggregator", new HttpObjectAggregator(config.getHttpObjectAggregatorSize()));
							chp.addLast("pintRequestDecoder", new RequestDecoder());
							chp.addLast("httpPayloadEncoder", new ResponseEncoder());
							chp.addLast("httpPayloadDecoder", new ServerHandler(group));
							chp.addLast("httpExceptionHandler", new DefaultExceptionHandler());
						}
					}).option(ChannelOption.SO_BACKLOG, 128) // (5)
					.childOption(ChannelOption.SO_KEEPALIVE, true); // (6)
			l.info("Created server on port {}", port);

			// Bind and start to accept incoming connections.
			ChannelFuture f = b.bind(host,port).sync(); // (7)

			// Wait until the server socket is closed.
			// In this example, this does not happen, but you can do that to
			// gracefully
			// shut down your server.
			f.channel().closeFuture().sync();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			bossGroup.shutdownGracefully();
			workerGroup.shutdownGracefully();
		}
	}

	public static void main(String[] args) throws Exception
	{
		new ServerMain().run();
	}
}
