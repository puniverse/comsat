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
package co.paralleluniverse.fibers.okhttp;

import co.paralleluniverse.fibers.FiberAsync;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.Suspendable;
import com.squareup.okhttp.Call;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

/**
 * Fiber-blocking OkHttp's {@link Call} implementation.
 *
 * @author circlespainter
 */
public class FiberCall extends Call {

    public FiberCall(final OkHttpClient client, final Request originalRequest) {
        super(client, originalRequest);
    }

    @Override
    @Suspendable
    public Response execute() throws IOException {
        try {
            return new FiberAsyncCallback().run();
        } catch (final SuspendExecution | InterruptedException ex) {
            throw new AssertionError(ex);
        }
    }
    
    private class FiberAsyncCallback extends FiberAsync<Response, IOException> implements Callback {
        @Override
        protected void requestAsync() {
            enqueue(this);
        }

        @Override
        public void onFailure(final Request rqst, final IOException ioe) {
            asyncFailed(ioe);
        }

        @Override
        public void onResponse(final Response rspns) throws IOException {
            asyncCompleted(rspns);
        }

        @Override
        protected Response requestSync() throws IOException, InterruptedException, ExecutionException {
            return FiberCall.super.execute();
        }
    }
}
