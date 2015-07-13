/*
 * COMSAT
 * Copyright (C) 2014-2015, Parallel Universe Software Co. All rights reserved.
 *
 * This program and the accompanying materials are dual-licensed under
 * either the terms of the Eclipse Public License v1.0 as published by
 * the Eclipse Foundation
 *
 *   or (per the licensee's choosing)
 *
 * under the terms of the GNU Lesser General Public License version 3.0
 * as published by the Free Software Foundation.
 */
package co.paralleluniverse.comsat.webactors.netty;

import co.paralleluniverse.actors.Actor;
import co.paralleluniverse.actors.ActorImpl;
import co.paralleluniverse.comsat.webactors.MyWebActor;
import co.paralleluniverse.comsat.webactors.WebMessage;
import co.paralleluniverse.strands.SettableFuture;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.HttpClients;
import org.glassfish.jersey.media.sse.EventInput;
import org.glassfish.jersey.media.sse.InboundEvent;
import org.glassfish.jersey.media.sse.SseFeature;
import org.junit.*;

import static org.junit.Assert.*;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import javax.websocket.*;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;

@RunWith(Parameterized.class)
public class WebActorTest {
	private static final int INET_PORT = 8080;

	private static final String HTTP_RESPONSE_ENCODER_KEY = "httpResponseEncoder";

	private static final Actor actor = new MyWebActor();

	static {
		actor.spawn();
	}

	private static final WebActorHandler.DefaultActorContextImpl context = new WebActorHandler.DefaultActorContextImpl() {
		@Override
		public ActorImpl<? extends WebMessage> getActor() {
			return actor;
		}
	};

	private static final Callable<WebActorHandler> basicWebActorHandlerCreator = new Callable<WebActorHandler>() {
		@Override
		public WebActorHandler call() throws Exception {
			return new WebActorHandler(new WebActorHandler.ActorContextProvider() {
				@Override
				public WebActorHandler.ActorContext get(ChannelHandlerContext ctx, FullHttpRequest req) {
					return context;
				}
			});
		}
	};

	private static final Callable<WebActorHandler> autoWebActorHandlerCreator = new Callable<WebActorHandler>() {
		@Override
		public WebActorHandler call() throws Exception {
			return new AutoWebActorHandler();
		}
	};

	@Parameterized.Parameters(name = "{0}")
	public static Collection<Object[]> data() {
		return Arrays.asList(new Object[][]{
			{basicWebActorHandlerCreator},
			{autoWebActorHandlerCreator}
		});
	}

	private static ChannelFuture ch;
	private static NioEventLoopGroup bossGroup;
	private static NioEventLoopGroup workerGroup;
	private static Callable<WebActorHandler> webActorHandlerCreatorInEffect;

	public WebActorTest(Callable<WebActorHandler> webActorHandlerCreator) {
		webActorHandlerCreatorInEffect = webActorHandlerCreator;
	}

	@BeforeClass
	public static void setUp() throws Exception {
		bossGroup = new NioEventLoopGroup(1);
		workerGroup = new NioEventLoopGroup();
		final ServerBootstrap b = new ServerBootstrap();
		b.group(bossGroup, workerGroup)
			.channel(NioServerSocketChannel.class)
			.handler(new LoggingHandler(LogLevel.INFO))
			.childHandler(new ChannelInitializer<SocketChannel>() {
				@Override
				public void initChannel(SocketChannel ch) throws Exception {
					ChannelPipeline pipeline = ch.pipeline();
					pipeline.addLast(new LoggingHandler(LogLevel.INFO));
					pipeline.addLast(new HttpRequestDecoder());
					pipeline.addLast(new LoggingHandler(LogLevel.INFO));
					pipeline.addLast(HTTP_RESPONSE_ENCODER_KEY, new HttpResponseEncoder());
					pipeline.addLast(new LoggingHandler(LogLevel.INFO));
					pipeline.addLast(new HttpObjectAggregator(65536));
					pipeline.addLast(new LoggingHandler(LogLevel.INFO));
					pipeline.addLast(webActorHandlerCreatorInEffect.call());
				}
			});

		ch = b.bind(INET_PORT).sync();

		System.out.println("Server is up");
	}

	@AfterClass
	public static void tearDown() throws Exception {
		ch.channel().close();
		bossGroup.shutdownGracefully().sync();
		workerGroup.shutdownGracefully().sync();

		System.out.println("Server is down");
	}

	@Test
	public void testHttpMsg() throws IOException, InterruptedException, ExecutionException {
		final HttpGet httpGet = new HttpGet("http://localhost:8080");
		final String res = HttpClients.createDefault().execute(httpGet, new BasicResponseHandler());
		assertEquals("httpResponse", res);
	}

	@Test
	public void testWebSocketMsg() throws IOException, InterruptedException, ExecutionException, DeploymentException {
		BasicCookieStore cookieStore = new BasicCookieStore();
		final HttpGet httpGet = new HttpGet("http://localhost:8080");
		HttpClients.custom().setDefaultCookieStore(cookieStore).build().execute(httpGet, new BasicResponseHandler());

		final SettableFuture<String> res = new SettableFuture<>();
		try (Session ignored = ContainerProvider.getWebSocketContainer().connectToServer(sendAndGetTextEndPoint("test it", res), ClientEndpointConfig.Builder.create().build(), URI.create("ws://localhost:8080/ws"))) {
			assertEquals("test it", res.get());
		}
	}

	@Test
	public void testSSE() throws IOException, InterruptedException, DeploymentException, ExecutionException {
		final Client client = ClientBuilder.newBuilder().register(SseFeature.class).build();
		Response resp = client.target("http://localhost:8080/ssechannel").request().get();
		NewCookie session = resp.getCookies().get(WebActorHandler.SESSION_COOKIE_KEY);
		final EventInput eventInput = resp.readEntity(EventInput.class);
		final SettableFuture<String> res = new SettableFuture<>();
		new Thread(new Runnable() {
			@Override
			public void run() {
				while (!eventInput.isClosed() && !res.isDone()) {
					final InboundEvent inboundEvent = eventInput.read();
					if (inboundEvent == null)
						break;
					res.set(inboundEvent.readData(String.class));
				}
			}
		}).start();
		client.target("http://localhost:8080/ssepublish").request().cookie(session).post(Entity.text("test it"));
		assertEquals("test it", res.get());
	}

	private static Endpoint sendAndGetTextEndPoint(final String sendText, final SettableFuture<String> res) {
		return new Endpoint() {
			@Override
			public void onOpen(final Session session, EndpointConfig config) {
				session.addMessageHandler(new MessageHandler.Whole<String>() {
					@Override
					public void onMessage(String text) {
						res.set(text);
					}
				});
				try {
					session.getBasicRemote().sendText(sendText);
				} catch (IOException ignored) {
				}
			}
		};
	}
}
