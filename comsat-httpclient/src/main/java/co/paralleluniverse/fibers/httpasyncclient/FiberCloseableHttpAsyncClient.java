package co.paralleluniverse.fibers.httpasyncclient;

import co.paralleluniverse.strands.SettableFuture;
import java.io.IOException;
import java.util.concurrent.Future;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.nio.protocol.HttpAsyncRequestProducer;
import org.apache.http.nio.protocol.HttpAsyncResponseConsumer;
import org.apache.http.protocol.HttpContext;

class FiberCloseableHttpAsyncClient extends CloseableHttpAsyncClient {
    private final CloseableHttpAsyncClient client;

    private FiberCloseableHttpAsyncClient(CloseableHttpAsyncClient client) {
        this.client = client;
    }
    public static CloseableHttpAsyncClient wrap(CloseableHttpAsyncClient client) {
        return new FiberCloseableHttpAsyncClient(client);
    };

    @Override
    public boolean isRunning() {
        return client.isRunning();
    }

    @Override
    public void start() {
        client.start();
    }

    @Override
    public <T> Future<T> execute(HttpAsyncRequestProducer requestProducer, HttpAsyncResponseConsumer<T> responseConsumer, final FutureCallback<T> callback) {
        final SettableFuture<T> future = new SettableFuture<>();
        client.execute(requestProducer, responseConsumer, wrapCallbackWithFuture(future, callback));
        return future;
    }

    @Override
    public Future<HttpResponse> execute(HttpHost target, HttpRequest request, HttpContext context, FutureCallback<HttpResponse> callback) {
        final SettableFuture<HttpResponse> future = new SettableFuture<>();
        client.execute(target, request, context, wrapCallbackWithFuture(future, callback));
        return future;
    }

    @Override
    public Future<HttpResponse> execute(HttpHost target, HttpRequest request, FutureCallback<HttpResponse> callback) {
        final SettableFuture<HttpResponse> future = new SettableFuture<>();
        client.execute(target, request, wrapCallbackWithFuture(future, callback));
        return future;
    }

    @Override
    public Future<HttpResponse> execute(HttpUriRequest request, FutureCallback<HttpResponse> callback) {
        final SettableFuture<HttpResponse> future = new SettableFuture<>();
        client.execute(request, wrapCallbackWithFuture(future, callback));
        return future;
    }

    @Override
    public Future<HttpResponse> execute(HttpUriRequest request, HttpContext context, FutureCallback<HttpResponse> callback) {
        final SettableFuture<HttpResponse> future = new SettableFuture<>();
        client.execute(request, context, wrapCallbackWithFuture(future, callback));
        return future;
    }

    @Override
    public void close() throws IOException {
        client.close();
    }

    @Override
    public <T> Future<T> execute(HttpAsyncRequestProducer requestProducer, HttpAsyncResponseConsumer<T> responseConsumer, HttpContext context, FutureCallback<T> callback) {
        final SettableFuture<T> future = new SettableFuture<>();
        client.execute(requestProducer, responseConsumer, context, wrapCallbackWithFuture(future, callback));
        return future;
    }

    private static <T> FutureCallback<T> wrapCallbackWithFuture(final SettableFuture<T> future, final FutureCallback<T> callback) {
        return new FutureCallback<T>() {

            @Override
            public void completed(T result) {
                future.set(result);
                callback.completed(result);
            }

            @Override
            public void failed(Exception ex) {
                future.setException(ex);
                callback.failed(ex);
            }

            @Override
            public void cancelled() {
                future.cancel(true);
                callback.cancelled();
            }
        };
    }
}
