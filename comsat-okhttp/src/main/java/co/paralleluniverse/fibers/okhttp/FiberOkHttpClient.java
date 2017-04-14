/*
 * COMSAT
 * Copyright (C) 2013-2015, Parallel Universe Software Co. All rights reserved.
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
package co.paralleluniverse.fibers.okhttp;

import okhttp3.Call;
import okhttp3.FiberCall;
import okhttp3.OkHttpClient;
import okhttp3.Request;

/**
 * Fiber-blocking OkHttp's {@link OkHttpClient} implementation.
 *
 * @author circlespainter
 */
public class FiberOkHttpClient extends OkHttpClient
{

    @Override
    public Call newCall(Request request) {
        return new FiberCall(this, request, false);
    }
}
