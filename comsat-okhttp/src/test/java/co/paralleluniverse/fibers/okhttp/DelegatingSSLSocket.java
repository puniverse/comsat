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
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.channels.SocketChannel;
import javax.net.ssl.HandshakeCompletedListener;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;

/**
 * An {@link javax.net.ssl.SSLSocket} that delegates all calls.
 */
public abstract class DelegatingSSLSocket extends SSLSocket {
  protected final SSLSocket delegate;

  public DelegatingSSLSocket(SSLSocket delegate) {
    this.delegate = delegate;
  }

  @Override public void shutdownInput() throws IOException {
    delegate.shutdownInput();
  }

  @Override public void shutdownOutput() throws IOException {
    delegate.shutdownOutput();
  }

  @Override public String[] getSupportedCipherSuites() {
    return delegate.getSupportedCipherSuites();
  }

  @Override public String[] getEnabledCipherSuites() {
    return delegate.getEnabledCipherSuites();
  }

  @Override public void setEnabledCipherSuites(String[] suites) {
    delegate.setEnabledCipherSuites(suites);
  }

  @Override public String[] getSupportedProtocols() {
    return delegate.getSupportedProtocols();
  }

  @Override public String[] getEnabledProtocols() {
    return delegate.getEnabledProtocols();
  }

  @Override public void setEnabledProtocols(String[] protocols) {
    delegate.setEnabledProtocols(protocols);
  }

  @Override public SSLSession getSession() {
    return delegate.getSession();
  }

  @Override public void addHandshakeCompletedListener(HandshakeCompletedListener listener) {
    delegate.addHandshakeCompletedListener(listener);
  }

  @Override public void removeHandshakeCompletedListener(HandshakeCompletedListener listener) {
    delegate.removeHandshakeCompletedListener(listener);
  }

  @Override public void startHandshake() throws IOException {
    delegate.startHandshake();
  }

  @Override public void setUseClientMode(boolean mode) {
    delegate.setUseClientMode(mode);
  }

  @Override public boolean getUseClientMode() {
    return delegate.getUseClientMode();
  }

  @Override public void setNeedClientAuth(boolean need) {
    delegate.setNeedClientAuth(need);
  }

  @Override public void setWantClientAuth(boolean want) {
    delegate.setWantClientAuth(want);
  }

  @Override public boolean getNeedClientAuth() {
    return delegate.getNeedClientAuth();
  }

  @Override public boolean getWantClientAuth() {
    return delegate.getWantClientAuth();
  }

  @Override public void setEnableSessionCreation(boolean flag) {
    delegate.setEnableSessionCreation(flag);
  }

  @Override public boolean getEnableSessionCreation() {
    return delegate.getEnableSessionCreation();
  }

  @Override public SSLParameters getSSLParameters() {
    return delegate.getSSLParameters();
  }

  @Override public void setSSLParameters(SSLParameters p) {
    delegate.setSSLParameters(p);
  }

  @Override public void close() throws IOException {
    delegate.close();
  }

  @Override public InetAddress getInetAddress() {
    return delegate.getInetAddress();
  }

  @Override public InputStream getInputStream() throws IOException {
    return delegate.getInputStream();
  }

  @Override public boolean getKeepAlive() throws SocketException {
    return delegate.getKeepAlive();
  }

  @Override public InetAddress getLocalAddress() {
    return delegate.getLocalAddress();
  }

  @Override public int getLocalPort() {
    return delegate.getLocalPort();
  }

  @Override public OutputStream getOutputStream() throws IOException {
    return delegate.getOutputStream();
  }

  @Override public int getPort() {
    return delegate.getPort();
  }

  @Override public int getSoLinger() throws SocketException {
    return delegate.getSoLinger();
  }

  @Override public int getReceiveBufferSize() throws SocketException {
    return delegate.getReceiveBufferSize();
  }

  @Override public int getSendBufferSize() throws SocketException {
    return delegate.getSendBufferSize();
  }

  @Override public int getSoTimeout() throws SocketException {
    return delegate.getSoTimeout();
  }

  @Override public boolean getTcpNoDelay() throws SocketException {
    return delegate.getTcpNoDelay();
  }

  @Override public void setKeepAlive(boolean keepAlive) throws SocketException {
    delegate.setKeepAlive(keepAlive);
  }

  @Override public void setSendBufferSize(int size) throws SocketException {
    delegate.setSendBufferSize(size);
  }

  @Override public void setReceiveBufferSize(int size) throws SocketException {
    delegate.setReceiveBufferSize(size);
  }

  @Override public void setSoLinger(boolean on, int timeout) throws SocketException {
    delegate.setSoLinger(on, timeout);
  }

  @Override public void setSoTimeout(int timeout) throws SocketException {
    delegate.setSoTimeout(timeout);
  }

  @Override public void setTcpNoDelay(boolean on) throws SocketException {
    delegate.setTcpNoDelay(on);
  }

  @Override public String toString() {
    return delegate.toString();
  }

  @Override public SocketAddress getLocalSocketAddress() {
    return delegate.getLocalSocketAddress();
  }

  @Override public SocketAddress getRemoteSocketAddress() {
    return delegate.getRemoteSocketAddress();
  }

  @Override public boolean isBound() {
    return delegate.isBound();
  }

  @Override public boolean isConnected() {
    return delegate.isConnected();
  }

  @Override public boolean isClosed() {
    return delegate.isClosed();
  }

  @Override public void bind(SocketAddress localAddr) throws IOException {
    delegate.bind(localAddr);
  }

  @Override public void connect(SocketAddress remoteAddr) throws IOException {
    delegate.connect(remoteAddr);
  }

  @Override public void connect(SocketAddress remoteAddr, int timeout) throws IOException {
    delegate.connect(remoteAddr, timeout);
  }

  @Override public boolean isInputShutdown() {
    return delegate.isInputShutdown();
  }

  @Override public boolean isOutputShutdown() {
    return delegate.isOutputShutdown();
  }

  @Override public void setReuseAddress(boolean reuse) throws SocketException {
    delegate.setReuseAddress(reuse);
  }

  @Override public boolean getReuseAddress() throws SocketException {
    return delegate.getReuseAddress();
  }

  @Override public void setOOBInline(boolean oobinline) throws SocketException {
    delegate.setOOBInline(oobinline);
  }

  @Override public boolean getOOBInline() throws SocketException {
    return delegate.getOOBInline();
  }

  @Override public void setTrafficClass(int value) throws SocketException {
    delegate.setTrafficClass(value);
  }

  @Override public int getTrafficClass() throws SocketException {
    return delegate.getTrafficClass();
  }

  @Override public void sendUrgentData(int value) throws IOException {
    delegate.sendUrgentData(value);
  }

  @Override public SocketChannel getChannel() {
    return delegate.getChannel();
  }

  @Override public void setPerformancePreferences(int connectionTime, int latency, int bandwidth) {
    delegate.setPerformancePreferences(connectionTime, latency, bandwidth);
  }
}
