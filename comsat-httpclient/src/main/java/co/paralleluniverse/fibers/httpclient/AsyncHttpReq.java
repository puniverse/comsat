/*
 * COMSAT
 * Copyright (C) 2013, Parallel Universe Software Co. All rights reserved.
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
package co.paralleluniverse.fibers.httpclient;

import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.FiberAsync;
import co.paralleluniverse.fibers.SuspendExecution;
import java.io.IOException;
import org.apache.http.HttpResponse;
import org.apache.http.concurrent.FutureCallback;

abstract class AsyncHttpReq extends FiberAsync<HttpResponse, FutureCallback<HttpResponse>, Void, IOException> implements FutureCallback<HttpResponse> {
    private final Fiber fiber;

    public AsyncHttpReq() {
        this.fiber = Fiber.currentFiber();
    }

    @Override
    public HttpResponse run() throws IOException, SuspendExecution {
        try {
            return super.run();
        } catch (InterruptedException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void completed(HttpResponse response) {
        super.completed(response, fiber);
    }

    @Override
    public void failed(Exception ex) {
        super.failed(ex, fiber);
    }

    @Override
    public void cancelled() {
        super.failed(new InterruptedException(), fiber);
    }
}
