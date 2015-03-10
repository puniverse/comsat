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
 * A network that always resolves two IP addresses per host. Use this when testing route selection
 * fallbacks to guarantee that a fallback address is available.
 */
public class DoubleInetAddressNetwork implements Network {
  @Override public InetAddress[] resolveInetAddresses(String host) throws UnknownHostException {
    InetAddress[] allInetAddresses = Network.DEFAULT.resolveInetAddresses(host);
    return new InetAddress[] { allInetAddresses[0], allInetAddresses[0] };
  }
}
