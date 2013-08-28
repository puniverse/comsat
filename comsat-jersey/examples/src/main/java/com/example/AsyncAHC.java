package com.example;

import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.FiberAsync;
import co.paralleluniverse.fibers.SuspendExecution;
import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.Response;
import java.io.IOException;

abstract class AsyncAHC extends FiberAsync<Response, AsyncCompletionHandler<Response>, Void, IOException> {
    private final Fiber fiber;

    public AsyncAHC() {
        this.fiber = Fiber.currentFiber();
    }

    @Override
    protected AsyncCompletionHandler<Response> getCallback() {
        return new AsyncCompletionHandler<Response>() {
            @Override
            public Response onCompleted(Response response) throws Exception {
                AsyncAHC.super.completed(response, fiber);
                return response;
            }

            @Override
            public void onThrowable(Throwable t) {
                AsyncAHC.super.failed(t, fiber);
            }
        };
    }

    @Override
    public Response run() throws IOException, SuspendExecution {
        try {
            return super.run();
        } catch (InterruptedException e) {
            throw new AssertionError(e);
        }
    }
}
