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
import javax.net.SocketFactory;

/**
 * A {@link SocketFactory} that delegates calls. Sockets can be configured after creation by
 * overriding {@link #configureSocket(java.net.Socket)}.
 */
public class DelegatingSocketFactory extends SocketFactory {

  private final SocketFactory delegate;

  public DelegatingSocketFactory(SocketFactory delegate) {
    this.delegate = delegate;
  }

  @Override
  public Socket createSocket() throws IOException {
    Socket socket = delegate.createSocket();
    configureSocket(socket);
    return socket;
  }

  @Override
  public Socket createSocket(String host, int port) throws IOException, UnknownHostException {
    Socket socket = delegate.createSocket(host, port);
    configureSocket(socket);
    return socket;
  }

  @Override
  public Socket createSocket(String host, int port, InetAddress localAddress, int localPort)
      throws IOException, UnknownHostException {
    Socket socket = delegate.createSocket(host, port, localAddress, localPort);
    configureSocket(socket);
    return socket;
  }

  @Override
  public Socket createSocket(InetAddress host, int port) throws IOException {
    Socket socket = delegate.createSocket(host, port);
    configureSocket(socket);
    return socket;
  }

  @Override
  public Socket createSocket(InetAddress host, int port, InetAddress localAddress, int localPort)
      throws IOException {
    Socket socket = delegate.createSocket(host, port, localAddress, localPort);
    configureSocket(socket);
    return socket;
  }

  protected void configureSocket(Socket socket) throws IOException {
    // No-op by default.
  }
}
