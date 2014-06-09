/*
 * COMSAT
 * Copyright (C) 2014, Parallel Universe Software Co. All rights reserved.
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
package co.paralleluniverse.comsat.webactors.servlet;

import co.paralleluniverse.embedded.containers.AbstractEmbeddedServer;
import co.paralleluniverse.embedded.containers.EmbeddedServer;
import co.paralleluniverse.embedded.containers.JettyServer;
import co.paralleluniverse.strands.SettableFuture;
import com.google.common.collect.Lists;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import javax.websocket.ClientEndpointConfig;
import javax.websocket.ContainerProvider;
import javax.websocket.DeploymentException;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import javax.websocket.Session;
import org.apache.http.client.CookieStore;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.HttpClients;
import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class WebActorServletTest {

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
        {JettyServer.class},
//            {TomcatServer.class},
//            {UndertowServer.class},
        });
    }
    private final Class<? extends EmbeddedServer> cls;
    private EmbeddedServer server;

    public WebActorServletTest(Class<? extends EmbeddedServer> cls) {
        this.cls = cls;
    }

    @Before
    public void setUp() throws Exception {
        this.server = cls.newInstance();
        server.addServletContextListener(new WebActorInitializer(ClassLoader.getSystemClassLoader()));
        server.start();
        AbstractEmbeddedServer.waitUrlAvailable("http://localhost:8080");
    }

    @After
    public void tearDown() throws Exception {
        server.stop();
    }

    @Test
    public void testHttpMsg() throws IOException, InterruptedException, DeploymentException, ExecutionException {
        assertEquals("httpResponse", HttpClients.createDefault().
                execute(new HttpGet("http://localhost:8080"), new BasicResponseHandler()));
    }

    @Test
    public void testWebSocketMsg() throws IOException, InterruptedException, DeploymentException, ExecutionException {
        BasicCookieStore cookieStore = new BasicCookieStore();
        HttpClients.custom().setDefaultCookieStore(cookieStore).build().
                execute(new HttpGet("http://localhost:8080"), new BasicResponseHandler());

        final SettableFuture<String> res = new SettableFuture<>();
        try (Session s = ContainerProvider.getWebSocketContainer().connectToServer(
                sendAndGetTextEndPoint("test it", res), EmbedSessionConfig(cookieStore), URI.create("ws://localhost:8080/ws"))) {
            assertEquals("TEST IT", after(res.get(), "data:"));
        }
    }

    private static Endpoint sendAndGetTextEndPoint(final String sendText, final SettableFuture<String> res) {
        return new Endpoint() {
            @Override
            public void onOpen(final Session session, EndpointConfig config) {
                session.addMessageHandler(new MessageHandler.Whole<String>() {
                    @Override
                    public void onMessage(String text) {
                        if (text.contains("data:"))
                            res.set(text);
                    }
                });
                try {
                    session.getBasicRemote().sendText(sendText);
                } catch (IOException ex) {
                }
            }
        };
    }

    private static ClientEndpointConfig EmbedSessionConfig(final CookieStore cookieStore) {
        return ClientEndpointConfig.Builder.create().configurator(new ClientEndpointConfig.Configurator() {
            @Override
            public void beforeRequest(Map<String, List<String>> headers) {
                for (Cookie cookie : cookieStore.getCookies())
                    if (JSESSIONID.equals(cookie.getName()))
                        headers.put("Cookie", Lists.newArrayList(JSESSIONID + '=' + cookie.getValue()));

            }
        }).build();
    }

    private static String after(String str, String substr) {
        return str.substring(str.indexOf(substr) + substr.length());
    }
    private static final String JSESSIONID = "JSESSIONID";

    private static final BasicResponseHandler BASIC_RESPONSE_HANDLER = new BasicResponseHandler();
}
