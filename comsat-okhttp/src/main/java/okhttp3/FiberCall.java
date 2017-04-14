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
package okhttp3;

import co.paralleluniverse.fibers.FiberAsync;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.Suspendable;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

/**
 * Fiber-blocking OkHttp's {@link Call} implementation.
 *
 * @author circlespainter
 */
public class FiberCall implements Call {

    private RealCall realCall;

    public FiberCall(OkHttpClient client, Request originalRequest, boolean forWebSocket) {
        this.realCall = new RealCall(client, originalRequest, forWebSocket);
    }

    private FiberCall(RealCall realCall) {
        this.realCall = realCall.clone();
    }

    @Override
    public Request request()
    {
        return realCall.request();
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

    @Override
    public void enqueue(Callback responseCallback)
    {
        realCall.enqueue(responseCallback);
    }

    @Override
    public void cancel()
    {
        realCall.cancel();
    }

    @Override
    public boolean isExecuted()
    {
        return realCall.isExecuted();
    }

    @Override
    public boolean isCanceled()
    {
        return realCall.isCanceled();
    }

    @Override
    @SuppressWarnings("CloneDoesntCallSuperClone")
    public Call clone()
    {
        return new FiberCall(realCall);
    }

    private class FiberAsyncCallback extends FiberAsync<Response, IOException> implements Callback
    {
        @Override
        protected void requestAsync() {
            enqueue(this);
        }

        @Override
        protected Response requestSync() throws IOException, InterruptedException, ExecutionException {
            return realCall.execute();
        }

        @Override
        public void onFailure(Call call, IOException e)
        {
            asyncFailed(e);
        }

        @Override
        public void onResponse(Call call, Response response) throws IOException
        {
            asyncCompleted(response);
        }
    }
}
