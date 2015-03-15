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
 * Based on the corresponding class in okhttp-urlconnection.
 * Copyright 2014 Square, Inc.
 * Licensed under the Apache License, Version 2.0 (the "License").
 */
package com.squareup.okhttp.internal.huc;

import co.paralleluniverse.fibers.okhttp.AbstractResponseCache;
import co.paralleluniverse.fibers.okhttp.FiberOkHttpClient;
import co.paralleluniverse.fibers.okhttp.FiberOkHttpUtils;
import com.squareup.okhttp.OkUrlFactory;
import com.squareup.okhttp.internal.Internal;
import com.squareup.okhttp.internal.SslContextBuilder;
import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.MockWebServer;
import java.io.IOException;
import java.net.CacheRequest;
import java.net.CacheResponse;
import java.net.HttpURLConnection;
import java.net.ResponseCache;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * A white-box test for {@link CacheAdapter}. See also:
 * <ul>
 *   <li>{@link ResponseCacheTest} for black-box tests that check that {@link ResponseCache}
 *   classes are called correctly by OkHttp.</li>
 *   <li>{@link JavaApiConverterTest} for tests that check Java API classes / OkHttp conversion
 *   logic. </li>
 * </ul>
 */
public class CacheAdapterTest {
  private static final SSLContext sslContext = SslContextBuilder.localhost();
  private static final HostnameVerifier NULL_HOSTNAME_VERIFIER = new HostnameVerifier() {
    public boolean verify(String hostname, SSLSession session) {
      return true;
    }
  };

  private MockWebServer server;

  private FiberOkHttpClient client;

  private HttpURLConnection connection;

  @Before public void setUp() throws Exception {
    server = new MockWebServer();
    client = new FiberOkHttpClient();
  }

  @After public void tearDown() throws Exception {
    if (connection != null) {
      connection.disconnect();
    }
    server.shutdown();
  }

  @Test public void get_httpGet() throws Exception {
    final URL serverUrl = configureServer(new MockResponse());
    assertEquals("http", serverUrl.getProtocol());

    ResponseCache responseCache = new AbstractResponseCache() {
      @Override
      public CacheResponse get(URI uri, String method, Map<String, List<String>> headers) throws IOException {
        assertEquals(toUri(serverUrl), uri);
        assertEquals("GET", method);
        assertTrue("Arbitrary standard header not present", headers.containsKey("User-Agent"));
        assertEquals(Collections.singletonList("value1"), headers.get("key1"));
        return null;
      }
    };
    Internal.instance.setCache(client, new CacheAdapter(responseCache));

    connection = FiberOkHttpUtils.open(new OkUrlFactory(client), serverUrl);
    connection.setRequestProperty("key1", "value1");

    executeGet(connection);
  }

  @Test public void get_httpsGet() throws Exception {
    final URL serverUrl = configureHttpsServer(new MockResponse());
    assertEquals("https", serverUrl.getProtocol());

    ResponseCache responseCache = new AbstractResponseCache() {
      @Override public CacheResponse get(URI uri, String method, Map<String, List<String>> headers)
          throws IOException {
        assertEquals("https", uri.getScheme());
        assertEquals(toUri(serverUrl), uri);
        assertEquals("GET", method);
        assertTrue("Arbitrary standard header not present", headers.containsKey("User-Agent"));
        assertEquals(Collections.singletonList("value1"), headers.get("key1"));
        return null;
      }
    };
    Internal.instance.setCache(client, new CacheAdapter(responseCache));
    client.setSslSocketFactory(sslContext.getSocketFactory());
    client.setHostnameVerifier(NULL_HOSTNAME_VERIFIER);

    connection = FiberOkHttpUtils.open(new OkUrlFactory(client), serverUrl);
    connection.setRequestProperty("key1", "value1");

    executeGet(connection);
  }

  @Test public void put_httpGet() throws Exception {
    final String statusLine = "HTTP/1.1 200 Fantastic";
    final URL serverUrl = configureServer(
        new MockResponse()
            .setStatus(statusLine)
            .addHeader("A", "c"));

    ResponseCache responseCache = new AbstractResponseCache() {
      @Override public CacheRequest put(URI uri, URLConnection connection) throws IOException {
        assertTrue(connection instanceof HttpURLConnection);
        assertFalse(connection instanceof HttpsURLConnection);

        assertEquals(0, connection.getContentLength());

        HttpURLConnection httpUrlConnection = (HttpURLConnection) connection;
        assertEquals("GET", httpUrlConnection.getRequestMethod());
        assertTrue(httpUrlConnection.getDoInput());
        assertFalse(httpUrlConnection.getDoOutput());

        assertEquals("Fantastic", httpUrlConnection.getResponseMessage());
        assertEquals(toUri(serverUrl), uri);
        assertEquals(serverUrl, connection.getURL());
        assertEquals("value", connection.getRequestProperty("key"));

        // Check retrieval by string key.
        assertEquals(statusLine, httpUrlConnection.getHeaderField(null));
        assertEquals("c", httpUrlConnection.getHeaderField("A"));
        // The RI and OkHttp supports case-insensitive matching for this method.
        assertEquals("c", httpUrlConnection.getHeaderField("a"));
        return null;
      }
    };
    Internal.instance.setCache(client, new CacheAdapter(responseCache));

    connection = FiberOkHttpUtils.open(new OkUrlFactory(client), serverUrl);
    connection.setRequestProperty("key", "value");
    executeGet(connection);
  }

  @Test public void put_httpPost() throws Exception {
    final String statusLine = "HTTP/1.1 200 Fantastic";
    final URL serverUrl = configureServer(
        new MockResponse()
            .setStatus(statusLine)
            .addHeader("A", "c"));

    ResponseCache responseCache = new AbstractResponseCache() {
      @Override public CacheRequest put(URI uri, URLConnection connection) throws IOException {
        assertTrue(connection instanceof HttpURLConnection);
        assertFalse(connection instanceof HttpsURLConnection);

        assertEquals(0, connection.getContentLength());

        HttpURLConnection httpUrlConnection = (HttpURLConnection) connection;
        assertEquals("POST", httpUrlConnection.getRequestMethod());
        assertTrue(httpUrlConnection.getDoInput());
        assertTrue(httpUrlConnection.getDoOutput());

        assertEquals("Fantastic", httpUrlConnection.getResponseMessage());
        assertEquals(toUri(serverUrl), uri);
        assertEquals(serverUrl, connection.getURL());
        assertEquals("value", connection.getRequestProperty("key"));

        // Check retrieval by string key.
        assertEquals(statusLine, httpUrlConnection.getHeaderField(null));
        assertEquals("c", httpUrlConnection.getHeaderField("A"));
        // The RI and OkHttp supports case-insensitive matching for this method.
        assertEquals("c", httpUrlConnection.getHeaderField("a"));
        return null;
      }
    };
    Internal.instance.setCache(client, new CacheAdapter(responseCache));

    connection = FiberOkHttpUtils.open(new OkUrlFactory(client), serverUrl);

    executePost(connection);
  }

  @Test public void put_httpsGet() throws Exception {
    final URL serverUrl = configureHttpsServer(new MockResponse());
    assertEquals("https", serverUrl.getProtocol());

    ResponseCache responseCache = new AbstractResponseCache() {
      @Override public CacheRequest put(URI uri, URLConnection connection) throws IOException {
        assertTrue(connection instanceof HttpsURLConnection);
        assertEquals(toUri(serverUrl), uri);
        assertEquals(serverUrl, connection.getURL());

        HttpsURLConnection cacheHttpsUrlConnection = (HttpsURLConnection) connection;
        HttpsURLConnection realHttpsUrlConnection = (HttpsURLConnection) CacheAdapterTest.this.connection;
        assertEquals(realHttpsUrlConnection.getCipherSuite(),
            cacheHttpsUrlConnection.getCipherSuite());
        assertEquals(realHttpsUrlConnection.getPeerPrincipal(),
            cacheHttpsUrlConnection.getPeerPrincipal());
        assertArrayEquals(realHttpsUrlConnection.getLocalCertificates(),
            cacheHttpsUrlConnection.getLocalCertificates());
        assertArrayEquals(realHttpsUrlConnection.getServerCertificates(),
            cacheHttpsUrlConnection.getServerCertificates());
        assertEquals(realHttpsUrlConnection.getLocalPrincipal(),
            cacheHttpsUrlConnection.getLocalPrincipal());
        return null;
      }
    };
    Internal.instance.setCache(client, new CacheAdapter(responseCache));
    client.setSslSocketFactory(sslContext.getSocketFactory());
    client.setHostnameVerifier(NULL_HOSTNAME_VERIFIER);

    connection = FiberOkHttpUtils.open(new OkUrlFactory(client), serverUrl);
    executeGet(connection);
  }

  private void executeGet(HttpURLConnection connection) throws IOException {
    connection.connect();
    connection.getHeaderFields();
    connection.disconnect();
  }

  private void executePost(HttpURLConnection connection) throws IOException {
    connection.setDoOutput(true);
    connection.connect();
    connection.getOutputStream().write("Hello World".getBytes());
    connection.disconnect();
  }

  private URL configureServer(MockResponse mockResponse) throws Exception {
    server.enqueue(mockResponse);
    server.play();
    return server.getUrl("/");
  }

  private URL configureHttpsServer(MockResponse mockResponse) throws Exception {
    server.useHttps(sslContext.getSocketFactory(), false /* tunnelProxy */);
    server.enqueue(mockResponse);
    server.play();
    return server.getUrl("/");
  }
}
