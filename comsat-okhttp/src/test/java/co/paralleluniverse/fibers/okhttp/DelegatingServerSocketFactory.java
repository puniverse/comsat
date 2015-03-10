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
import java.net.ServerSocket;
import javax.net.ServerSocketFactory;

/**
 * A {@link ServerSocketFactory} that delegates calls. Sockets can be configured after creation by
 * overriding {@link #configureServerSocket(java.net.ServerSocket)}.
 */
public class DelegatingServerSocketFactory extends ServerSocketFactory {

  private final ServerSocketFactory delegate;

  public DelegatingServerSocketFactory(ServerSocketFactory delegate) {
    this.delegate = delegate;
  }

  @Override
  public ServerSocket createServerSocket() throws IOException {
    ServerSocket serverSocket = delegate.createServerSocket();
    configureServerSocket(serverSocket);
    return serverSocket;
  }

  @Override
  public ServerSocket createServerSocket(int port) throws IOException {
    ServerSocket serverSocket = delegate.createServerSocket(port);
    configureServerSocket(serverSocket);
    return serverSocket;
  }

  @Override
  public ServerSocket createServerSocket(int port, int backlog) throws IOException {
    ServerSocket serverSocket = delegate.createServerSocket(port, backlog);
    configureServerSocket(serverSocket);
    return serverSocket;
  }

  @Override
  public ServerSocket createServerSocket(int port, int backlog, InetAddress ifAddress)
      throws IOException {
    ServerSocket serverSocket = delegate.createServerSocket(port, backlog, ifAddress);
    configureServerSocket(serverSocket);
    return serverSocket;
  }

  protected void configureServerSocket(ServerSocket serverSocket) throws IOException {
    // No-op by default.
  }
}
