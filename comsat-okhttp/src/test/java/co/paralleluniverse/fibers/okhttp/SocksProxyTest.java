/*
 * COMSAT
 * Copyright (c) 2013-2015, Parallel Universe Software Co. All rights reserved.
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
/*
 * Based on the corresponding class in okhttp-tests.
 * Copyright 2015 Square, Inc.
 * Licensed under the Apache License, Version 2.0 (the "License").
 */
package co.paralleluniverse.fibers.okhttp;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.MockWebServer;
import java.io.IOException;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public final class SocksProxyTest {
  private final SocksProxy socksProxy = new SocksProxy();
  private final MockWebServer server = new MockWebServer();

  @Before public void setUp() throws Exception {
    server.start();
    socksProxy.play();
  }

  @After public void tearDown() throws Exception {
    server.shutdown();
    socksProxy.shutdown();
  }

  @Test public void proxy() throws Exception {
    server.enqueue(new MockResponse().setBody("abc"));
    server.enqueue(new MockResponse().setBody("def"));

    OkHttpClient client = new FiberOkHttpClient()
        .setProxy(socksProxy.proxy());

    Request request1 = new Request.Builder().url(server.getUrl("/")).build();
    Response response1 = FiberOkHttpUtil.executeInFiber((FiberOkHttpClient) client, request1);
    assertEquals("abc", response1.body().string());

    Request request2 = new Request.Builder().url(server.getUrl("/")).build();
    Response response2 = FiberOkHttpUtil.executeInFiber((FiberOkHttpClient) client, request2);
    assertEquals("def", response2.body().string());

    // The HTTP calls should share a single connection.
    assertEquals(1, socksProxy.connectionCount());
  }

  @Test public void proxySelector() throws Exception {
    server.enqueue(new MockResponse().setBody("abc"));

    ProxySelector proxySelector = new ProxySelector() {
      @Override public List<Proxy> select(URI uri) {
        return Collections.singletonList(socksProxy.proxy());
      }

      @Override public void connectFailed(URI uri, SocketAddress socketAddress, IOException e) {
        throw new AssertionError();
      }
    };

    OkHttpClient client = new FiberOkHttpClient()
        .setProxySelector(proxySelector);

    Request request = new Request.Builder().url(server.getUrl("/")).build();
    Response response = FiberOkHttpUtil.executeInFiber((FiberOkHttpClient) client, request);
    assertEquals("abc", response.body().string());

    assertEquals(1, socksProxy.connectionCount());
  }
}
