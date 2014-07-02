package com.ahanda.techops.noty;

import io.netty.bootstrap.Bootstrap;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

/**
 * Discards any incoming data.
 */
public class SubClient {

	private String host;
    private int port;

    public SubClient( String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void run() throws Exception {
        EventLoopGroup workers = new NioEventLoopGroup();
        try {
            Bootstrap b = new Bootstrap(); // (2)
            b.group( workers )
             .channel(NioSocketChannel.class) // (3)
			 .option( ChannelOption.SO_KEEPALIVE, true )
             .handler(new ChannelInitializer<SocketChannel>() { // (4)
                 @Override
                 public void initChannel(SocketChannel ch) throws Exception {
                     ch.pipeline().addLast( new RespDecoder(), new RespHandler());
                 }
             });

            // Bind and start to accept incoming connections.
            ChannelFuture f = b.connect( host, port).sync(); // (7)

            // Wait until the Client socket is closed.
            // In this example, this does not happen, but you can do that to gracefully
            // shut down your Client.
            f.channel().closeFuture().sync();
        } catch( Exception e ) {
		  System.out.println( e.getMessage() );
		  e.printStackTrace();
		} finally {
            workers.shutdownGracefully();
        }
    }

    public static void main(String[] args) throws Exception {
		String host = "localhost";
		if( args.length > 0 )
		  host = args[0];

        int port;
        if (args.length > 1) {
            port = Integer.parseInt(args[1]);
        } else {
            port = 8080;
        }

        new SubClient( host, port).run();
    }
}
