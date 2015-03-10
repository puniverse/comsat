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
package co.paralleluniverse.fibers.okhttp;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

/**
 * An SSLSocketFactory that delegates calls. Sockets created by the delegate are wrapped with ones
 * that will not accept the {@link #TLS_FALLBACK_SCSV} cipher, thus bypassing server-side fallback
 * checks on platforms that support it. Unfortunately this wrapping will disable any
 * reflection-based calls to SSLSocket from Platform.
 */
public class FallbackTestClientSocketFactory extends DelegatingSSLSocketFactory {
  /**
   * The cipher suite used during TLS connection fallback to indicate a fallback.
   * See https://tools.ietf.org/html/draft-ietf-tls-downgrade-scsv-00
   */
  public static final String TLS_FALLBACK_SCSV = "TLS_FALLBACK_SCSV";

  public FallbackTestClientSocketFactory(SSLSocketFactory delegate) {
    super(delegate);
  }

  @Override public SSLSocket createSocket(Socket s, String host, int port, boolean autoClose)
      throws IOException {
    SSLSocket socket = super.createSocket(s, host, port, autoClose);
    return new TlsFallbackScsvDisabledSSLSocket(socket);
  }

  @Override public SSLSocket createSocket() throws IOException {
    SSLSocket socket = super.createSocket();
    return new TlsFallbackScsvDisabledSSLSocket(socket);
  }

  @Override public SSLSocket createSocket(String host,int port) throws IOException {
    SSLSocket socket = super.createSocket(host, port);
    return new TlsFallbackScsvDisabledSSLSocket(socket);
  }

  @Override public SSLSocket createSocket(String host,int port, InetAddress localHost,
      int localPort) throws IOException {
    SSLSocket socket = super.createSocket(host, port, localHost, localPort);
    return new TlsFallbackScsvDisabledSSLSocket(socket);
  }

  @Override public SSLSocket createSocket(InetAddress host,int port) throws IOException {
    SSLSocket socket = super.createSocket(host, port);
    return new TlsFallbackScsvDisabledSSLSocket(socket);
  }

  @Override public SSLSocket createSocket(InetAddress address,int port,
      InetAddress localAddress, int localPort) throws IOException {
    SSLSocket socket = super.createSocket(address, port, localAddress, localPort);
    return new TlsFallbackScsvDisabledSSLSocket(socket);
  }

  private static class TlsFallbackScsvDisabledSSLSocket extends DelegatingSSLSocket {

    public TlsFallbackScsvDisabledSSLSocket(SSLSocket socket) {
      super(socket);
    }

    @Override public void setEnabledCipherSuites(String[] suites) {
      List<String> enabledCipherSuites = new ArrayList<String>(suites.length);
      for (String suite : suites) {
        if (!suite.equals(TLS_FALLBACK_SCSV)) {
          enabledCipherSuites.add(suite);
        }
      }
      delegate.setEnabledCipherSuites(
          enabledCipherSuites.toArray(new String[enabledCipherSuites.size()]));
    }
  }
}
