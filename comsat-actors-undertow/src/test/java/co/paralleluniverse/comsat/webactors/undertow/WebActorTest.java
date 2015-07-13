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
package co.paralleluniverse.comsat.webactors.undertow;

import co.paralleluniverse.actors.Actor;
import co.paralleluniverse.actors.ActorImpl;
import co.paralleluniverse.comsat.webactors.MyWebActor;
import co.paralleluniverse.comsat.webactors.WebMessage;
import java.io.IOException;
import java.net.URI;
import java.util.concurrent.ExecutionException;

import co.paralleluniverse.strands.SettableFuture;
import io.undertow.Undertow;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.RequestDumpingHandler;
import io.undertow.server.session.SessionCookieConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.HttpClients;
import org.glassfish.jersey.media.sse.EventInput;
import org.glassfish.jersey.media.sse.InboundEvent;
import org.glassfish.jersey.media.sse.SseFeature;
import org.junit.*;

import static org.junit.Assert.*;

import javax.websocket.*;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;

public class WebActorTest {
  private static final int INET_PORT = 8080;

  private Undertow server;

  @Before
  public void setUp() throws Exception {
    final Actor actor = new MyWebActor();
    actor.spawn();

    server = Undertow.builder()
            .addHttpListener(INET_PORT, "localhost")
            .setHandler(new RequestDumpingHandler(new WebActorHandler(new WebActorHandler.ActorContextProvider() {
              @Override
              public WebActorHandler.ActorContext get(HttpServerExchange xch) {
                return new WebActorHandler.DefaultActorContextImpl() {
                  @Override
                  public ActorImpl<? extends WebMessage> getActor() {
                    return actor;
                  }
                };
              }
            }))).build();
    server.start();

    System.out.println("Server is up");
  }

  @After
  public void tearDown() throws Exception {
    server.stop();

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
    NewCookie session = resp.getCookies().get(SessionCookieConfig.DEFAULT_SESSION_ID);
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
