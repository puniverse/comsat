/*
 * COMSAT
 * Copyright (C) 2015, Parallel Universe Software Co. All rights reserved.
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
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.glassfish.jersey.media.sse.EventInput;
import org.glassfish.jersey.media.sse.InboundEvent;
import org.glassfish.jersey.media.sse.SseFeature;
import org.junit.Assume;
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
	@Test
	public void testHttpMsg() throws IOException, InterruptedException, ExecutionException {
		Assume.assumeTrue(isHttpMsgEnabled());

		final HttpGet httpGet = new HttpGet("http://localhost:8080");
		final CloseableHttpResponse res = HttpClients.createDefault().execute(httpGet);
		assertEquals(200, res.getStatusLine().getStatusCode());
		assertEquals("text/html", res.getFirstHeader("Content-Type").getValue());
		assertEquals("12", res.getFirstHeader("Content-Length").getValue());
		assertEquals("httpResponse", EntityUtils.toString(res.getEntity()));
	}

	@Test
	public void testHttpRedirect() throws IOException, InterruptedException, ExecutionException {
		Assume.assumeTrue(isHttpRedirectEnabled());

		final HttpGet httpGet = new HttpGet("http://localhost:8080/redirect");
		final CloseableHttpResponse res = HttpClients.custom().disableRedirectHandling().build().execute(httpGet);
		final String s = EntityUtils.toString(res.getEntity());
		System.out.println(s);
		assertEquals(302, res.getStatusLine().getStatusCode());
		assertTrue(res.getFirstHeader("Location").getValue().endsWith("/foo"));
	}

	@Test
	public void testWebSocketMsg() throws IOException, InterruptedException, ExecutionException, DeploymentException {
		Assume.assumeTrue(isWebSocketMsgEnabled());

		BasicCookieStore cookieStore = new BasicCookieStore();
		final HttpGet httpGet = new HttpGet("http://localhost:8080");
		HttpClients.custom().setDefaultCookieStore(cookieStore).build().execute(httpGet, new BasicResponseHandler());

		final SettableFuture<String> res = new SettableFuture<>();
		try (Session ignored = ContainerProvider.getWebSocketContainer().connectToServer(sendAndGetTextEndPoint("test it", res), getClientEndPointConfig(cookieStore), URI.create("ws://localhost:8080/ws"))) {
			assertEquals("test it", res.get());
		}
	}

	@Test
	public void testSSE() throws IOException, InterruptedException, DeploymentException, ExecutionException {
		Assume.assumeTrue(isSSEEnabled());

		final Client client = ClientBuilder.newBuilder().register(SseFeature.class).build();
		Response resp = client.target("http://localhost:8080/ssechannel").request().get();
		NewCookie session = resp.getCookies().get(getSessionIdCookieName());
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

	protected abstract String getSessionIdCookieName();

	protected ClientEndpointConfig getClientEndPointConfig(CookieStore cs) {
		return ClientEndpointConfig.Builder.create().build();
	}

	protected boolean isHttpMsgEnabled() {
		return true;
	}

	protected boolean isHttpRedirectEnabled() {
		return true;
	}

	protected boolean isSSEEnabled() {
		return true;
	}

	protected boolean isWebSocketMsgEnabled() {
		return true;
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
