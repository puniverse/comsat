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
package co.paralleluniverse.comsat.webactors.servlet;

import co.paralleluniverse.embedded.containers.AbstractEmbeddedServer;
import co.paralleluniverse.embedded.containers.EmbeddedServer;
import co.paralleluniverse.embedded.containers.JettyServer;
import co.paralleluniverse.embedded.containers.TomcatServer;
// import co.paralleluniverse.embedded.containers.UndertowServer;
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
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import org.apache.http.client.CookieStore;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.HttpClients;
import org.glassfish.jersey.media.sse.EventInput;
import org.glassfish.jersey.media.sse.InboundEvent;
import org.glassfish.jersey.media.sse.SseFeature;
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
            {TomcatServer.class},
         // {UndertowServer.class},
        });
    }
    private final Class<? extends EmbeddedServer> cls;
    private EmbeddedServer embeddedServer;

    public WebActorServletTest(Class<? extends EmbeddedServer> cls) {
        this.cls = cls;
    }

//    public WebActorServletTest() {
//        this.cls = JettyServer.class;
//    }

    @Before
    public void setUp() throws Exception {
        this.embeddedServer = cls.newInstance();
        // snippet WebActorInitializer
        WebActorInitializer.setUserClassLoader(ClassLoader.getSystemClassLoader());
        embeddedServer.addServletContextListener(WebActorInitializer.class);
        // end of snippet
        embeddedServer.enableWebsockets();
        embeddedServer.start();
        AbstractEmbeddedServer.waitUrlAvailable("http://localhost:8080");
        System.out.println("Server is up");
    }

    @After
    public void tearDown() throws Exception {       
        embeddedServer.stop();
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
            assertEquals("test it", res.get());
        }
    }

    @Test
    public void testSSE() throws IOException, InterruptedException, DeploymentException, ExecutionException {
        final Client client = ClientBuilder.newBuilder().register(SseFeature.class).build();
        Response resp = client.target("http://localhost:8080/ssechannel").request().get();
        NewCookie session = resp.getCookies().get(JSESSIONID);
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
                } catch (IOException ex) {
                }
            }
        };
    }

    private static ClientEndpointConfig EmbedSessionConfig(final CookieStore cookieStore) {
        return ClientEndpointConfig.Builder.create().configurator(new ClientEndpointConfig.Configurator() {
            @Override
            public void beforeRequest(Map<String, List<String>> headers) {
                headers.put("Cookie", Lists.newArrayList(JSESSIONID + '=' + getByName(cookieStore, JSESSIONID)));
            }
        }).build();
    }

    public static String getByName(final CookieStore cookieStore, String name) {
        for (org.apache.http.cookie.Cookie cookie : cookieStore.getCookies())
            if (name.equals(cookie.getName()))
                return cookie.getValue();
        return null;
    }

    private static String after(String str, String substr) {
        return str.substring(str.indexOf(substr) + substr.length());
    }

    private static final String JSESSIONID = "JSESSIONID";
}
