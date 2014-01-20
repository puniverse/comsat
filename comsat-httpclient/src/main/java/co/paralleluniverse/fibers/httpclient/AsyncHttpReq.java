/*
 * COMSAT
 * Copyright (c) 2013-2014, Parallel Universe Software Co. All rights reserved.
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

import co.paralleluniverse.fibers.FiberAsync;
import co.paralleluniverse.fibers.SuspendExecution;
import java.io.IOException;
import org.apache.http.HttpResponse;
import org.apache.http.concurrent.FutureCallback;

abstract class AsyncHttpReq extends FiberAsync<HttpResponse, Void, IOException> implements FutureCallback<HttpResponse> {
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
        asyncCompleted(response);
    }

    @Override
    public void failed(Exception ex) {
        asyncFailed(ex);
    }

    @Override
    public void cancelled() {
        asyncFailed(new InterruptedException());
    }
}
