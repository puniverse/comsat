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

import com.squareup.okhttp.internal.Network;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * A network that resolves only one IP address per host. Use this when testing
 * route selection fallbacks to prevent the host machine's various IP addresses
 * from interfering.
 */
public class SingleInetAddressNetwork implements Network {
  @Override public InetAddress[] resolveInetAddresses(String host) throws UnknownHostException {
    InetAddress[] allInetAddresses = Network.DEFAULT.resolveInetAddresses(host);
    return new InetAddress[] { allInetAddresses[0] };
  }
}
