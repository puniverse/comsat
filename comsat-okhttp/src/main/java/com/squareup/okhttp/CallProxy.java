/*
 * COMSAT
 * Copyright (c) 2015, Parallel Universe Software Co. All rights reserved.
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
package com.squareup.okhttp;

/**
 * Proxy to open up the {@link Call} protected (since version 2.4.0) constructor.
 *
 * @author circlespainter
 */
public class CallProxy extends Call {
    public CallProxy(final OkHttpClient client, final Request originalRequest) {
        super(client, originalRequest);
    }
}
