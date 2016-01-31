package co.paralleluniverse.fibers.okhttp.test.utils;

import co.paralleluniverse.fibers.okhttp.FiberCall;
import co.paralleluniverse.fibers.okhttp.FiberOkHttpClient;
import co.paralleluniverse.fibers.okhttp.FiberOkHttpUtil;
import com.squareup.okhttp.Call;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.IOException;

public final class FiberCallTestWrapper extends FiberCall {
    private final Call underlying;

    public FiberCallTestWrapper(FiberOkHttpClient client, Call call, Request request) {
        super(client, request);
        this.underlying = call;
    }

    @Override
    public Response execute() throws IOException {
        try {
            return FiberOkHttpUtil.executeInFiber(underlying);
        } catch (final InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void enqueue(Callback responseCallback) {
        underlying.enqueue(responseCallback);
    }

    @Override
    public void cancel() {
        underlying.cancel();
    }

    @Override
    public boolean isCanceled() {
        return underlying.isCanceled();
    }
}
