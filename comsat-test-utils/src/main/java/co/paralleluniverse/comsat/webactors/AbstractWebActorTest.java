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
package co.paralleluniverse.comsat.webactors;

import co.paralleluniverse.strands.SettableFuture;
import org.apache.http.client.CookieStore;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.media.sse.EventInput;
import org.glassfish.jersey.media.sse.InboundEvent;
import org.glassfish.jersey.media.sse.SseFeature;
import org.junit.Test;

import javax.websocket.*;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.URI;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author circlespainter
 */
public abstract class AbstractWebActorTest {
    private static final int DEFAULT_TIMEOUT = 60_000;

    private final RequestConfig requestConfig;

    protected int timeout = DEFAULT_TIMEOUT;

    protected AbstractWebActorTest() {
        requestConfig = RequestConfig.custom()
            .setConnectTimeout(timeout)
            .setConnectionRequestTimeout(timeout)
            .setSocketTimeout(timeout)
            .build();
    }

    @Test
    public final void testHttpMsg() throws IOException, InterruptedException, ExecutionException {
        final HttpGet httpGet = new HttpGet("http://localhost:8080");
        try (final CloseableHttpClient client = HttpClients.custom().setDefaultRequestConfig(requestConfig).build()) {
            final CloseableHttpResponse res = client.execute(httpGet);
            assertEquals(200, res.getStatusLine().getStatusCode());
            assertEquals("text/html", res.getFirstHeader("Content-Type").getValue());
            assertEquals("12", res.getFirstHeader("Content-Length").getValue());
            assertEquals("httpResponse", EntityUtils.toString(res.getEntity()));
        }
    }

    @Test
    public final void testHttpNotFound() throws IOException, InterruptedException, ExecutionException {
        final HttpGet httpGet = new HttpGet("http://localhost:8080/notfound");
        try (final CloseableHttpClient client = HttpClients.custom().setDefaultRequestConfig(requestConfig).build()) {
            final CloseableHttpResponse res = client.execute(httpGet);
            assertEquals(404, res.getStatusLine().getStatusCode());
        }
    }

    @Test
    public void testDie() throws IOException, InterruptedException, ExecutionException {
        final HttpGet httpGet = new HttpGet("http://localhost:8080/die");
        try (final CloseableHttpClient client = HttpClients.custom().setDefaultRequestConfig(requestConfig).build()) {
            final CloseableHttpResponse res = client.execute(httpGet);
            assertEquals(500, res.getStatusLine().getStatusCode());
            assertTrue(EntityUtils.toString(res.getEntity()).contains("die"));
        }
    }

    @Test
    public final void testHttpRedirect() throws IOException, InterruptedException, ExecutionException {
        final HttpGet httpGet = new HttpGet("http://localhost:8080/redirect");
        try (final CloseableHttpClient client = HttpClients.custom().disableRedirectHandling().setDefaultRequestConfig(requestConfig).build()) {
            final CloseableHttpResponse res = client.execute(httpGet);
            final String s = EntityUtils.toString(res.getEntity());
            System.out.println(s);
            assertEquals(302, res.getStatusLine().getStatusCode());
            assertTrue(res.getFirstHeader("Location").getValue().endsWith("/foo"));
        }
    }

    @Test
    public final void testWebSocketMsg() throws IOException, InterruptedException, ExecutionException, DeploymentException {
        BasicCookieStore cookieStore = new BasicCookieStore();
        final HttpGet httpGet = new HttpGet("http://localhost:8080");
        HttpClients.custom().setDefaultRequestConfig(requestConfig).setDefaultCookieStore(cookieStore).build().execute(httpGet, new BasicResponseHandler());

        final SettableFuture<String> res = new SettableFuture<>();
        final WebSocketContainer wsContainer = ContainerProvider.getWebSocketContainer();
        wsContainer.setAsyncSendTimeout(timeout);
        wsContainer.setDefaultMaxSessionIdleTimeout(timeout);
        try (final Session ignored = wsContainer.connectToServer(sendAndGetTextEndPoint("test it", res), getClientEndPointConfig(cookieStore), URI.create("ws://localhost:8080/ws"))) {
            final String s = res.get();
            assertEquals("test it", s);
        }
    }

    @Test
    public final void testSSE() throws IOException, InterruptedException, DeploymentException, ExecutionException {
        Client client = null;
        try {
            client = ClientBuilder.newBuilder().register(SseFeature.class).build();
            client.property(ClientProperties.CONNECT_TIMEOUT, timeout);
            client.property(ClientProperties.READ_TIMEOUT, timeout);
            final Response resp = client.target("http://localhost:8080/ssechannel").request().get();
            final NewCookie session = resp.getCookies().get(getSessionIdCookieName());
            final EventInput eventInput = resp.readEntity(EventInput.class);
            final SettableFuture<String> res = new SettableFuture<>();
            new Thread(new Runnable() {
                @Override
                public final void run() {
                    try {
                        while (!eventInput.isClosed() && !res.isDone()) {
                            final InboundEvent inboundEvent = eventInput.read();
                            if (inboundEvent == null)
                                break;
                            res.set(inboundEvent.readData(String.class));
                        }
                    } catch (Throwable t) {
                        t.printStackTrace();
                        res.setException(t);
                    }
                }
            }).start();
            client.target("http://localhost:8080/ssepublish").request().cookie(session).post(Entity.text("test it"));
            final String s = res.get();
            assertEquals("test it", s);
        } finally {
            if (client != null)
                client.close();
        }
    }

    protected abstract String getSessionIdCookieName();

    protected ClientEndpointConfig getClientEndPointConfig(CookieStore cs) {
        return ClientEndpointConfig.Builder.create().build();
    }

    private static Endpoint sendAndGetTextEndPoint(final String sendText, final SettableFuture<String> res) {
        return new Endpoint() {
            @Override
            public void onOpen(final Session session, EndpointConfig config) {
                session.addMessageHandler(new MessageHandler.Whole<String>() {
                    @Override
                    public final void onMessage(String text) {
                        res.set(text);
                    }
                });
                try {
                    session.getBasicRemote().sendText(sendText);
                } catch (final IOException ignored) {
                }
            }
        };
    }
}
