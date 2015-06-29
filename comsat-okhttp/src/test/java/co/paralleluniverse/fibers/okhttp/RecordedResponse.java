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

import com.squareup.okhttp.Handshake;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import com.squareup.okhttp.ws.WebSocket;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * A received response or failure recorded by the response recorder.
 */
public final class RecordedResponse {
  public final Request request;
  public final Response response;
  public final WebSocket webSocket;
  public final String body;
  public final IOException failure;

  public RecordedResponse(Request request, Response response, WebSocket webSocket, String body,
      IOException failure) {
    this.request = request;
    this.response = response;
    this.webSocket = webSocket;
    this.body = body;
    this.failure = failure;
  }

  public RecordedResponse assertRequestUrl(URL url) {
    assertEquals(url, request.url());
    return this;
  }

  public RecordedResponse assertRequestMethod(String method) {
    assertEquals(method, request.method());
    return this;
  }

  public RecordedResponse assertRequestHeader(String name, String... values) {
    assertEquals(Arrays.asList(values), request.headers(name));
    return this;
  }

  public RecordedResponse assertCode(int expectedCode) {
    assertEquals(expectedCode, response.code());
    return this;
  }

  public RecordedResponse assertSuccessful() {
    assertTrue(response.isSuccessful());
    return this;
  }

  public RecordedResponse assertNotSuccessful() {
    assertFalse(response.isSuccessful());
    return this;
  }

  public RecordedResponse assertHeader(String name, String... values) {
    assertEquals(Arrays.asList(values), response.headers(name));
    return this;
  }

  public RecordedResponse assertBody(String expectedBody) {
    assertEquals(expectedBody, body);
    return this;
  }

  public RecordedResponse assertHandshake() {
    Handshake handshake = response.handshake();
    assertNotNull(handshake.cipherSuite());
    assertNotNull(handshake.peerPrincipal());
    assertEquals(1, handshake.peerCertificates().size());
    assertNull(handshake.localPrincipal());
    assertEquals(0, handshake.localCertificates().size());
    return this;
  }

  /**
   * Asserts that the current response was redirected and returns the prior
   * response.
   */
  public RecordedResponse priorResponse() {
    Response priorResponse = response.priorResponse();
    assertNotNull(priorResponse);
    assertNull(priorResponse.body());
    return new RecordedResponse(priorResponse.request(), priorResponse, null, null, null);
  }

  /**
   * Asserts that the current response used the network and returns the network
   * response.
   */
  public RecordedResponse networkResponse() {
    Response networkResponse = response.networkResponse();
    assertNotNull(networkResponse);
    assertNull(networkResponse.body());
    return new RecordedResponse(networkResponse.request(), networkResponse, null, null, null);
  }

  /** Asserts that the current response didn't use the network. */
  public RecordedResponse assertNoNetworkResponse() {
    assertNull(response.networkResponse());
    return this;
  }

  /** Asserts that the current response didn't use the cache. */
  public RecordedResponse assertNoCacheResponse() {
    assertNull(response.cacheResponse());
    return this;
  }

  /**
   * Asserts that the current response used the cache and returns the cache
   * response.
   */
  public RecordedResponse cacheResponse() {
    Response cacheResponse = response.cacheResponse();
    assertNotNull(cacheResponse);
    assertNull(cacheResponse.body());
    return new RecordedResponse(cacheResponse.request(), cacheResponse, null, null, null);
  }

  public void assertFailure(String... messages) {
    assertNotNull(failure);
    assertTrue(failure.getMessage(), Arrays.asList(messages).contains(failure.getMessage()));
  }
}
