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
import java.net.UnknownHostException;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

/**
 * A {@link SSLSocketFactory} that delegates calls. Sockets can be configured after
 * creation by overriding {@link #configureSocket(javax.net.ssl.SSLSocket)}.
 */
public class DelegatingSSLSocketFactory extends SSLSocketFactory {

  private final SSLSocketFactory delegate;

  public DelegatingSSLSocketFactory(SSLSocketFactory delegate) {
    this.delegate = delegate;
  }

  @Override
  public SSLSocket createSocket() throws IOException {
    SSLSocket sslSocket = (SSLSocket) delegate.createSocket();
    configureSocket(sslSocket);
    return sslSocket;
  }

  @Override
  public SSLSocket createSocket(String host, int port) throws IOException, UnknownHostException {
    SSLSocket sslSocket = (SSLSocket) delegate.createSocket(host, port);
    configureSocket(sslSocket);
    return sslSocket;
  }

  @Override
  public SSLSocket createSocket(String host, int port, InetAddress localAddress, int localPort)
      throws IOException, UnknownHostException {
    SSLSocket sslSocket = (SSLSocket) delegate.createSocket(host, port, localAddress, localPort);
    configureSocket(sslSocket);
    return sslSocket;
  }

  @Override
  public SSLSocket createSocket(InetAddress host, int port) throws IOException {
    SSLSocket sslSocket = (SSLSocket) delegate.createSocket(host, port);
    configureSocket(sslSocket);
    return sslSocket;
  }

  @Override
  public SSLSocket createSocket(InetAddress host, int port, InetAddress localAddress, int localPort)
      throws IOException {
    SSLSocket sslSocket = (SSLSocket) delegate.createSocket(host, port, localAddress, localPort);
    configureSocket(sslSocket);
    return sslSocket;
  }

  @Override
  public String[] getDefaultCipherSuites() {
    return delegate.getDefaultCipherSuites();
  }

  @Override
  public String[] getSupportedCipherSuites() {
    return delegate.getSupportedCipherSuites();
  }

  @Override
  public SSLSocket createSocket(Socket socket, String host, int port, boolean autoClose)
      throws IOException {
    SSLSocket sslSocket = (SSLSocket) delegate.createSocket(socket, host, port, autoClose);
    configureSocket(sslSocket);
    return sslSocket;
  }

  protected void configureSocket(SSLSocket sslSocket) throws IOException {
    // No-op by default.
  }
}
