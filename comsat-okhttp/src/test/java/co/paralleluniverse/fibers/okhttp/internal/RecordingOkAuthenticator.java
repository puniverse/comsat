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
 * Copyright 2014 Square, Inc.
 * Licensed under the Apache License, Version 2.0 (the "License").
 */
package co.paralleluniverse.fibers.okhttp.internal;

import com.squareup.okhttp.Authenticator;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import java.net.Proxy;
import java.util.ArrayList;
import java.util.List;

public final class RecordingOkAuthenticator implements Authenticator {
  public final List<Response> responses = new ArrayList<>();
  public final List<Proxy> proxies = new ArrayList<>();
  public final String credential;

  public RecordingOkAuthenticator(String credential) {
    this.credential = credential;
  }

  public Response onlyResponse() {
    if (responses.size() != 1) throw new IllegalStateException();
    return responses.get(0);
  }

  public Proxy onlyProxy() {
    if (proxies.size() != 1) throw new IllegalStateException();
    return proxies.get(0);
  }

  @Override public Request authenticate(Proxy proxy, Response response) {
    responses.add(response);
    proxies.add(proxy);
    return response.request().newBuilder()
        .addHeader("Authorization", credential)
        .build();
  }

  @Override public Request authenticateProxy(Proxy proxy, Response response) {
    responses.add(response);
    proxies.add(proxy);
    return response.request().newBuilder()
        .addHeader("Proxy-Authorization", credential)
        .build();
  }
}
