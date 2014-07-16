package com.ahanda.techops.noty.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpRequestEncoder;
import io.netty.handler.codec.http.HttpResponseEncoder;

import com.ahanda.techops.noty.msg.MqttMessageDecoder;
import com.ahanda.techops.noty.msg.MqttMessageEncoder;
import com.ahanda.techops.noty.db.MongoDBManager;

/**
 * Discards any incoming data.
 */
public class ServerMain
{
	private int port;

	public ServerMain(int port)
	{
		this.port = port;
	}

	public void run() throws Exception
	{
		EventLoopGroup workers = new NioEventLoopGroup();
		try
		{
			MongoDBManager.getInstance();
			ServerBootstrap b = new ServerBootstrap(); // (2)
			b.group(workers).channel(NioServerSocketChannel.class) // (3)
					.childHandler(new ChannelInitializer<SocketChannel>()
					{
						@Override
						public void initChannel(SocketChannel ch) throws Exception
						{
							ChannelPipeline chp = ch.pipeline();
							chp.addLast( "decoder", new HttpRequestDecoder());
							chp.addLast( "encoder", new HttpResponseEncoder());
							chp.addLast( new ServerHandler() );
						}
					}).option(ChannelOption.SO_BACKLOG, 128) // (5)
					.childOption(ChannelOption.SO_KEEPALIVE, true); // (6)

			System.out.println("Created server on port " + port );

			// Bind and start to accept incoming connections.
			ChannelFuture f = b.bind(port).sync(); // (7)

			// Wait until the server socket is closed.
			// In this example, this does not happen, but you can do that to gracefully
			// shut down your server.
			f.channel().closeFuture().sync();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			workers.shutdownGracefully();
		}
	}

	public static void main(String[] args) throws Exception
	{
		int port;
		if (args.length > 0)
		{
			port = Integer.parseInt(args[0]);
		}
		else
		{
			port = 8080;
		}

		new ServerMain(port).run();
	}
}
