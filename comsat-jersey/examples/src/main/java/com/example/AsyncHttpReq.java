package com.example;

import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.FiberAsync;
import co.paralleluniverse.fibers.SuspendExecution;
import org.apache.http.HttpResponse;
import org.apache.http.concurrent.FutureCallback;

abstract class AsyncHttpReq extends FiberAsync<HttpResponse, FutureCallback<HttpResponse>, Void, RuntimeException> implements FutureCallback<HttpResponse> {
    private final Fiber fiber;
    
    public AsyncHttpReq() {
        this.fiber = Fiber.currentFiber();
    }

    @Override
    public HttpResponse run() throws SuspendExecution {
        try {
            final HttpResponse run = super.run();
            return run;
        } catch (InterruptedException e) {
            throw new AssertionError(e);
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
