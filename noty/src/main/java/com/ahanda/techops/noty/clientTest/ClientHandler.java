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
package com.ahanda.techops.noty.clientTest;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.util.CharsetUtil;

public class ClientHandler extends SimpleChannelInboundHandler<HttpObject>
{

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
