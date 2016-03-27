/*
 * COMSAT
 * Copyright (C) 2016, Parallel Universe Software Co. All rights reserved.
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
package co.paralleluniverse.comsat.webactors.undertow;

import co.paralleluniverse.actors.Actor;
import co.paralleluniverse.actors.ActorRef;
import co.paralleluniverse.comsat.webactors.AbstractWebActorTest;
import co.paralleluniverse.comsat.webactors.WebMessage;
import co.paralleluniverse.embedded.containers.AbstractEmbeddedServer;
import io.undertow.Undertow;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.RequestDumpingHandler;
import io.undertow.server.session.InMemorySessionManager;
import io.undertow.server.session.SessionAttachmentHandler;
import io.undertow.server.session.SessionCookieConfig;
import io.undertow.server.session.SessionManager;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

/**
 * @author circlespainter
 */
@RunWith(Parameterized.class)
public class WebActorTest extends AbstractWebActorTest {
    private static final Actor actor = new UndertowWebActor();
    @SuppressWarnings("unchecked")
    private static final ActorRef<? extends WebMessage> actorRef = actor.spawn();

    private static final Callable<WebActorHandler> basicWebActorHandlerCreator = new Callable<WebActorHandler>() {
        @Override
        public WebActorHandler call() throws Exception {
            return new WebActorHandler(new WebActorHandler.ContextProvider() {
                @Override
                public WebActorHandler.Context get(HttpServerExchange xch) {
                    return new WebActorHandler.DefaultContextImpl() {
                        @Override
                        public String getId() {
                            return "CONSTANT";
                        }

                        @SuppressWarnings("unchecked")
                        @Override
                        public ActorRef<? extends WebMessage> getWebActor() {
                            return actorRef;
                        }

                        @Override
                        public void restart(HttpServerExchange r) {
                            // Nothing to do
                        }

                        @Override
                        public final boolean handlesWithWebSocket(String uri) {
                            return uri.startsWith("/ws");
                        }

                        @Override
                        public WatchPolicy watch() {
                            return WatchPolicy.DIE;
                        }

                        @Override
                        public final boolean handlesWithHttp(String uri) {
                            return !handlesWithWebSocket(uri);
                        }
                    };
                }
            });
        }
    };

    @Override
    @Test
    public final void testDie() throws IOException, InterruptedException, ExecutionException {
        Assume.assumeTrue(webActorHandlerCreatorInEffect == autoWebActorHandlerCreator);

        super.testDie();
    }

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

    private static final int INET_PORT = 8080;

    private final Callable<WebActorHandler> webActorHandlerCreatorInEffect;

    private Undertow server;

    public WebActorTest(Callable<WebActorHandler> webActorHandlerCreator) {
        this.webActorHandlerCreatorInEffect = webActorHandlerCreator;
    }

    @Before
    public void setUp() throws Exception {
        final SessionManager sessionManager =
            new InMemorySessionManager("SESSION_MANAGER", 1, true);
        final SessionCookieConfig sessionConfig = new SessionCookieConfig();
        sessionConfig.setMaxAge(60);
        final SessionAttachmentHandler sessionAttachmentHandler =
            new SessionAttachmentHandler(sessionManager, sessionConfig);
        server = Undertow.builder()
            .addHttpListener(INET_PORT, "localhost")
            .setHandler(new RequestDumpingHandler(sessionAttachmentHandler.setNext(webActorHandlerCreatorInEffect.call()))).build();
        server.start();
        AbstractEmbeddedServer.waitUrlAvailable("http://localhost:" + INET_PORT);

        System.err.println("Server is up");
    }

    @After
    public void tearDown() throws Exception {
        server.stop();

        System.out.println("Server is down");
    }

    @Override
    protected String getSessionIdCookieName() {
        return SessionCookieConfig.DEFAULT_SESSION_ID;
    }
}
