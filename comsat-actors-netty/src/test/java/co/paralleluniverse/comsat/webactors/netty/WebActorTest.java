/*
 * COMSAT
 * Copyright (C) 2015-2016, Parallel Universe Software Co. All rights reserved.
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
import co.paralleluniverse.actors.ActorRef;
import co.paralleluniverse.comsat.webactors.AbstractWebActorTest;
import co.paralleluniverse.comsat.webactors.WebMessage;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.Callable;

import co.paralleluniverse.embedded.containers.AbstractEmbeddedServer;
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
import org.junit.*;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * @author circlespainter
 */
@RunWith(Parameterized.class)
public class WebActorTest extends AbstractWebActorTest {
    private static final int INET_PORT = 8080;

    private static final String HTTP_RESPONSE_ENCODER_KEY = "httpResponseEncoder";

    private static final Actor actor = new NettyWebActor();
    @SuppressWarnings("unchecked")
    private static final ActorRef<? extends WebMessage> actorRef = actor.spawn();

    private static final WebActorHandler.DefaultContextImpl context = new WebActorHandler.DefaultContextImpl() {
        @SuppressWarnings("unchecked")
        @Override
        public ActorRef<? extends WebMessage> getRef() {
            return actorRef;
        }

        @Override
        public final boolean handlesWithWebSocket(String uri) {
            return uri.startsWith("/ws");
        }

        @Override
        public final boolean handlesWithHttp(String uri) {
            return !handlesWithWebSocket(uri);
        }
    };

    private static final Callable<WebActorHandler> basicWebActorHandlerCreator = new Callable<WebActorHandler>() {
        @Override
        public WebActorHandler call() throws Exception {
            return new WebActorHandler(new WebActorHandler.WebActorContextProvider() {
                @Override
                public WebActorHandler.Context get(ChannelHandlerContext ctx, FullHttpRequest req) {
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

    @Parameterized.Parameters(name = "{index}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
            {basicWebActorHandlerCreator},
            {autoWebActorHandlerCreator}
        });
    }

    private ChannelFuture ch;
    private NioEventLoopGroup group;
    private Callable<WebActorHandler> webActorHandlerCreatorInEffect;

    public WebActorTest(Callable<WebActorHandler> webActorHandlerCreator) {
        webActorHandlerCreatorInEffect = webActorHandlerCreator;
    }

    @Before
    public void setUp() throws InterruptedException, IOException {
        System.out.println("Clearing sessions");
        WebActorHandler.sessions.clear();

        group = new NioEventLoopGroup();
        final ServerBootstrap b = new ServerBootstrap();
        b.group(group)
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

        AbstractEmbeddedServer.waitUrlAvailable("http://localhost:" + INET_PORT);

        System.err.println("Server is up");
    }

    @After
    public void tearDown() throws Exception {
        ch.channel().close();
        group.shutdownGracefully().sync();

        System.out.println("Server is down");
    }

    @Override
    protected String getSessionIdCookieName() {
        return WebActorHandler.SESSION_COOKIE_KEY;
    }
}
