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
/*
 * Based on the corresponding class in okhttp-tests.
 * Copyright 2014 Square, Inc.
 * Licensed under the Apache License, Version 2.0 (the "License").
 */
package co.paralleluniverse.fibers.okhttp;

import com.squareup.okhttp.Address;
import com.squareup.okhttp.Connection;
import com.squareup.okhttp.Dispatcher;
import com.squareup.okhttp.Interceptor;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.Protocol;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;
import com.squareup.okhttp.ResponseBody;
import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.RecordedRequest;
import com.squareup.okhttp.mockwebserver.rule.MockWebServerRule;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import okio.Buffer;
import okio.BufferedSink;
import okio.ForwardingSink;
import okio.ForwardingSource;
import okio.GzipSink;
import okio.Okio;
import okio.Sink;
import okio.Source;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;
import org.junit.Ignore;

public final class InterceptorTest {
  @Rule public MockWebServerRule server = new MockWebServerRule();

  private FiberOkHttpClient client = new FiberOkHttpClient();
  private RecordingCallback callback = new RecordingCallback();

  @Test public void applicationInterceptorsCanShortCircuitResponses() throws Exception {
    server.get().shutdown(); // Accept no connections.

    Request request = new Request.Builder()
        .url("https://localhost:1/")
        .build();

    final Response interceptorResponse = new Response.Builder()
        .request(request)
        .protocol(Protocol.HTTP_1_1)
        .code(200)
        .message("Intercepted!")
        .body(ResponseBody.create(MediaType.parse("text/plain; charset=utf-8"), "abc"))
        .build();

    client.interceptors().add(new Interceptor() {
      @Override public Response intercept(Chain chain) throws IOException {
        return interceptorResponse;
      }
    });

    Response response = FiberOkHttpUtil.executeInFiber(client, request);
    assertSame(interceptorResponse, response);
  }

  @Ignore @Test public void networkInterceptorsCannotShortCircuitResponses() throws Exception {
    server.enqueue(new MockResponse().setResponseCode(500));

    Interceptor interceptor = new Interceptor() {
      @Override public Response intercept(Chain chain) throws IOException {
        return new Response.Builder()
            .request(chain.request())
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("Intercepted!")
            .body(ResponseBody.create(MediaType.parse("text/plain; charset=utf-8"), "abc"))
            .build();
      }
    };
    client.networkInterceptors().add(interceptor);

    Request request = new Request.Builder()
        .url(server.getUrl("/"))
        .build();

    try {
      FiberOkHttpUtil.executeInFiber(client, request);
      fail();
    } catch (IllegalStateException expected) {
      assertEquals("network interceptor " + interceptor + " must call proceed() exactly once",
          expected.getMessage());
    }
  }

  @Ignore @Test public void networkInterceptorsCannotCallProceedMultipleTimes() throws Exception {
    server.enqueue(new MockResponse());
    server.enqueue(new MockResponse());

    Interceptor interceptor = new Interceptor() {
      @Override public Response intercept(Chain chain) throws IOException {
        chain.proceed(chain.request());
        return chain.proceed(chain.request());
      }
    };
    client.networkInterceptors().add(interceptor);

    Request request = new Request.Builder()
        .url(server.getUrl("/"))
        .build();

    try {
      FiberOkHttpUtil.executeInFiber(client, request);
      fail();
    } catch (IllegalStateException expected) {
      assertEquals("network interceptor " + interceptor + " must call proceed() exactly once",
          expected.getMessage());
    }
  }

  @Ignore @Test public void networkInterceptorsCannotChangeServerAddress() throws Exception {
    server.enqueue(new MockResponse().setResponseCode(500));

    Interceptor interceptor = new Interceptor() {
      @Override public Response intercept(Chain chain) throws IOException {
        Address address = chain.connection().getRoute().getAddress();
        String sameHost = address.getUriHost();
        int differentPort = address.getUriPort() + 1;
        return chain.proceed(chain.request().newBuilder()
            .url(new URL("http://" + sameHost + ":" + differentPort + "/"))
            .build());
      }
    };
    client.networkInterceptors().add(interceptor);

    Request request = new Request.Builder()
        .url(server.getUrl("/"))
        .build();

    try {
      FiberOkHttpUtil.executeInFiber(client, request);
      fail();
    } catch (IllegalStateException expected) {
      assertEquals("network interceptor " + interceptor + " must retain the same host and port",
          expected.getMessage());
    }
  }

  @Test public void networkInterceptorsHaveConnectionAccess() throws Exception {
    server.enqueue(new MockResponse());

    client.networkInterceptors().add(new Interceptor() {
      @Override public Response intercept(Chain chain) throws IOException {
        Connection connection = chain.connection();
        assertNotNull(connection);
        return chain.proceed(chain.request());
      }
    });

    Request request = new Request.Builder()
        .url(server.getUrl("/"))
        .build();
    FiberOkHttpUtil.executeInFiber(client, request);
  }

  @Test public void networkInterceptorsObserveNetworkHeaders() throws Exception {
    server.enqueue(new MockResponse()
        .setBody(gzip("abcabcabc"))
        .addHeader("Content-Encoding: gzip"));

    client.networkInterceptors().add(new Interceptor() {
      @Override public Response intercept(Chain chain) throws IOException {
        // The network request has everything: User-Agent, Host, Accept-Encoding.
        Request networkRequest = chain.request();
        assertNotNull(networkRequest.header("User-Agent"));
        assertEquals(server.get().getHostName() + ":" + server.get().getPort(),
            networkRequest.header("Host"));
        assertNotNull(networkRequest.header("Accept-Encoding"));

        // The network response also has everything, including the raw gzipped content.
        Response networkResponse = chain.proceed(networkRequest);
        assertEquals("gzip", networkResponse.header("Content-Encoding"));
        return networkResponse;
      }
    });

    Request request = new Request.Builder()
        .url(server.getUrl("/"))
        .build();

    // No extra headers in the application's request.
    assertNull(request.header("User-Agent"));
    assertNull(request.header("Host"));
    assertNull(request.header("Accept-Encoding"));

    // No extra headers in the application's response.
    Response response = FiberOkHttpUtil.executeInFiber(client, request);
    assertNull(request.header("Content-Encoding"));
    assertEquals("abcabcabc", response.body().string());
  }

  @Test public void applicationInterceptorsRewriteRequestToServer() throws Exception {
    rewriteRequestToServer(client.interceptors());
  }

  @Test public void networkInterceptorsRewriteRequestToServer() throws Exception {
    rewriteRequestToServer(client.networkInterceptors());
  }

  private void rewriteRequestToServer(List<Interceptor> interceptors) throws Exception {
    server.enqueue(new MockResponse());

    interceptors.add(new Interceptor() {
      @Override public Response intercept(Chain chain) throws IOException {
        Request originalRequest = chain.request();
        return chain.proceed(originalRequest.newBuilder()
            .method("POST", uppercase(originalRequest.body()))
            .addHeader("OkHttp-Intercepted", "yep")
            .build());
      }
    });

    Request request = new Request.Builder()
        .url(server.getUrl("/"))
        .addHeader("Original-Header", "foo")
        .method("PUT", RequestBody.create(MediaType.parse("text/plain"), "abc"))
        .build();

    FiberOkHttpUtil.executeInFiber(client, request);

    RecordedRequest recordedRequest = server.takeRequest();
    assertEquals("ABC", recordedRequest.getBody().readUtf8());
    assertEquals("foo", recordedRequest.getHeader("Original-Header"));
    assertEquals("yep", recordedRequest.getHeader("OkHttp-Intercepted"));
    assertEquals("POST", recordedRequest.getMethod());
  }

  @Test public void applicationInterceptorsRewriteResponseFromServer() throws Exception {
    rewriteResponseFromServer(client.interceptors());
  }

  @Test public void networkInterceptorsRewriteResponseFromServer() throws Exception {
    rewriteResponseFromServer(client.networkInterceptors());
  }

  private void rewriteResponseFromServer(List<Interceptor> interceptors) throws Exception {
    server.enqueue(new MockResponse()
        .addHeader("Original-Header: foo")
        .setBody("abc"));

    interceptors.add(new Interceptor() {
      @Override public Response intercept(Chain chain) throws IOException {
        Response originalResponse = chain.proceed(chain.request());
        return originalResponse.newBuilder()
            .body(uppercase(originalResponse.body()))
            .addHeader("OkHttp-Intercepted", "yep")
            .build();
      }
    });

    Request request = new Request.Builder()
        .url(server.getUrl("/"))
        .build();

    Response response = FiberOkHttpUtil.executeInFiber(client, request);
    assertEquals("ABC", response.body().string());
    assertEquals("yep", response.header("OkHttp-Intercepted"));
    assertEquals("foo", response.header("Original-Header"));
  }

  @Test public void multipleApplicationInterceptors() throws Exception {
    multipleInterceptors(client.interceptors());
  }

  @Test public void multipleNetworkInterceptors() throws Exception {
    multipleInterceptors(client.networkInterceptors());
  }

  private void multipleInterceptors(List<Interceptor> interceptors) throws Exception {
    server.enqueue(new MockResponse());

    interceptors.add(new Interceptor() {
      @Override public Response intercept(Chain chain) throws IOException {
        Request originalRequest = chain.request();
        Response originalResponse = chain.proceed(originalRequest.newBuilder()
            .addHeader("Request-Interceptor", "Android") // 1. Added first.
            .build());
        return originalResponse.newBuilder()
            .addHeader("Response-Interceptor", "Donut") // 4. Added last.
            .build();
      }
    });
    interceptors.add(new Interceptor() {
      @Override public Response intercept(Chain chain) throws IOException {
        Request originalRequest = chain.request();
        Response originalResponse = chain.proceed(originalRequest.newBuilder()
            .addHeader("Request-Interceptor", "Bob") // 2. Added second.
            .build());
        return originalResponse.newBuilder()
            .addHeader("Response-Interceptor", "Cupcake") // 3. Added third.
            .build();
      }
    });

    Request request = new Request.Builder()
        .url(server.getUrl("/"))
        .build();

    Response response = FiberOkHttpUtil.executeInFiber(client, request);
    assertEquals(Arrays.asList("Cupcake", "Donut"),
        response.headers("Response-Interceptor"));

    RecordedRequest recordedRequest = server.takeRequest();
    assertEquals(Arrays.asList("Android", "Bob"),
        recordedRequest.getHeaders().values("Request-Interceptor"));
  }

  @Test public void asyncApplicationInterceptors() throws Exception {
    asyncInterceptors(client.interceptors());
  }

  @Test public void asyncNetworkInterceptors() throws Exception {
    asyncInterceptors(client.networkInterceptors());
  }

  private void asyncInterceptors(List<Interceptor> interceptors) throws Exception {
    server.enqueue(new MockResponse());

    interceptors.add(new Interceptor() {
      @Override public Response intercept(Chain chain) throws IOException {
        Response originalResponse = chain.proceed(chain.request());
        return originalResponse.newBuilder()
            .addHeader("OkHttp-Intercepted", "yep")
            .build();
      }
    });

    Request request = new Request.Builder()
        .url(server.getUrl("/"))
        .build();
    client.newCall(request).enqueue(callback);

    callback.await(request.url())
        .assertCode(200)
        .assertHeader("OkHttp-Intercepted", "yep");
  }

  @Test public void applicationInterceptorsCanMakeMultipleRequestsToServer() throws Exception {
    server.enqueue(new MockResponse().setBody("a"));
    server.enqueue(new MockResponse().setBody("b"));

    client.interceptors().add(new Interceptor() {
      @Override public Response intercept(Chain chain) throws IOException {
        chain.proceed(chain.request());
        return chain.proceed(chain.request());
      }
    });

    Request request = new Request.Builder()
        .url(server.getUrl("/"))
        .build();

    Response response = FiberOkHttpUtil.executeInFiber(client, request);
    assertEquals(response.body().string(), "b");
  }

  /** Make sure interceptors can interact with the OkHttp client. */
  @Test public void interceptorMakesAnUnrelatedRequest() throws Exception {
    server.enqueue(new MockResponse().setBody("a")); // Fetched by interceptor.
    server.enqueue(new MockResponse().setBody("b")); // Fetched directly.

    client.interceptors().add(new Interceptor() {
      @Override public Response intercept(Chain chain) throws IOException {
        if (chain.request().url().getPath().equals("/b")) {
          Request requestA = new Request.Builder()
              .url(server.getUrl("/a"))
              .build();
          Response responseA = null;
            try {
                responseA = FiberOkHttpUtil.executeInFiber(client, requestA);
            } catch (InterruptedException ex) {
                throw new AssertionError(ex);
            }
          assertEquals("a", responseA.body().string());
        }

        return chain.proceed(chain.request());
      }
    });

    Request requestB = new Request.Builder()
        .url(server.getUrl("/b"))
        .build();
    Response responseB = FiberOkHttpUtil.executeInFiber(client, requestB);
    assertEquals("b", responseB.body().string());
  }

  /** Make sure interceptors can interact with the OkHttp client asynchronously. */
  @Test public void interceptorMakesAnUnrelatedAsyncRequest() throws Exception {
    server.enqueue(new MockResponse().setBody("a")); // Fetched by interceptor.
    server.enqueue(new MockResponse().setBody("b")); // Fetched directly.

    client.interceptors().add(new Interceptor() {
      @Override public Response intercept(Chain chain) throws IOException {
        if (chain.request().url().getPath().equals("/b")) {
          Request requestA = new Request.Builder()
              .url(server.getUrl("/a"))
              .build();

          try {
            RecordingCallback callbackA = new RecordingCallback();
            client.newCall(requestA).enqueue(callbackA);
            callbackA.await(requestA.url()).assertBody("a");
          } catch (Exception e) {
            throw new RuntimeException(e);
          }
        }

        return chain.proceed(chain.request());
      }
    });

    Request requestB = new Request.Builder()
        .url(server.getUrl("/b"))
        .build();
    RecordingCallback callbackB = new RecordingCallback();
    client.newCall(requestB).enqueue(callbackB);
    callbackB.await(requestB.url()).assertBody("b");
  }

  @Ignore @Test public void applicationkInterceptorThrowsRuntimeExceptionSynchronous() throws Exception {
    interceptorThrowsRuntimeExceptionSynchronous(client.interceptors());
  }

  @Ignore @Test public void networkInterceptorThrowsRuntimeExceptionSynchronous() throws Exception {
    interceptorThrowsRuntimeExceptionSynchronous(client.networkInterceptors());
  }

  /**
   * When an interceptor throws an unexpected exception, synchronous callers can catch it and deal
   * with it.
   *
   * TODO(jwilson): test that resources are not leaked when this happens.
   */
  private void interceptorThrowsRuntimeExceptionSynchronous(
      List<Interceptor> interceptors) throws Exception {
    interceptors.add(new Interceptor() {
      @Override public Response intercept(Chain chain) throws IOException {
        throw new RuntimeException("boom!");
      }
    });

    Request request = new Request.Builder()
        .url(server.getUrl("/"))
        .build();

    try {
      FiberOkHttpUtil.executeInFiber(client, request);
      fail();
    } catch (RuntimeException expected) {
      assertEquals("boom!", expected.getMessage());
    }
  }

  @Test public void applicationInterceptorThrowsRuntimeExceptionAsynchronous() throws Exception {
    interceptorThrowsRuntimeExceptionAsynchronous(client.interceptors());
  }

  @Test public void networkInterceptorThrowsRuntimeExceptionAsynchronous() throws Exception {
    interceptorThrowsRuntimeExceptionAsynchronous(client.networkInterceptors());
  }

  @Test public void networkInterceptorModifiedRequestIsReturned() throws IOException, InterruptedException, ExecutionException {
    server.enqueue(new MockResponse());

    Interceptor modifyHeaderInterceptor = new Interceptor() {
      @Override public Response intercept(Chain chain) throws IOException {
        return chain.proceed(chain.request().newBuilder()
          .header("User-Agent", "intercepted request")
          .build());
      }
    };

    client.networkInterceptors().add(modifyHeaderInterceptor);

    Request request = new Request.Builder()
        .url(server.getUrl("/"))
        .header("User-Agent", "user request")
        .build();

    Response response = FiberOkHttpUtil.executeInFiber(client, request);
    assertNotNull(response.request().header("User-Agent"));
    assertEquals("user request", response.request().header("User-Agent"));
    assertEquals("intercepted request", response.networkResponse().request().header("User-Agent"));
  }

  /**
   * When an interceptor throws an unexpected exception, asynchronous callers are left hanging. The
   * exception goes to the uncaught exception handler.
   *
   * TODO(jwilson): test that resources are not leaked when this happens.
   */
  private void interceptorThrowsRuntimeExceptionAsynchronous(
        List<Interceptor> interceptors) throws Exception {
    interceptors.add(new Interceptor() {
      @Override public Response intercept(Chain chain) throws IOException {
        throw new RuntimeException("boom!");
      }
    });

    ExceptionCatchingExecutor executor = new ExceptionCatchingExecutor();
    client.setDispatcher(new Dispatcher(executor));

    Request request = new Request.Builder()
        .url(server.getUrl("/"))
        .build();
    client.newCall(request).enqueue(callback);

    assertEquals("boom!", executor.takeException().getMessage());
  }

  private RequestBody uppercase(final RequestBody original) {
    return new RequestBody() {
      @Override public MediaType contentType() {
        return original.contentType();
      }

      @Override public long contentLength() throws IOException {
        return original.contentLength();
      }

      @Override public void writeTo(BufferedSink sink) throws IOException {
        Sink uppercase = uppercase(sink);
        BufferedSink bufferedSink = Okio.buffer(uppercase);
        original.writeTo(bufferedSink);
        bufferedSink.emit();
      }
    };
  }

  private Sink uppercase(final BufferedSink original) {
    return new ForwardingSink(original) {
      @Override public void write(Buffer source, long byteCount) throws IOException {
        original.writeUtf8(source.readUtf8(byteCount).toUpperCase(Locale.US));
      }
    };
  }

  static ResponseBody uppercase(ResponseBody original) throws IOException {
    return ResponseBody.create(original.contentType(), original.contentLength(),
        Okio.buffer(uppercase(original.source())));
  }

  private static Source uppercase(final Source original) {
    return new ForwardingSource(original) {
      @Override public long read(Buffer sink, long byteCount) throws IOException {
        Buffer mixedCase = new Buffer();
        long count = original.read(mixedCase, byteCount);
        sink.writeUtf8(mixedCase.readUtf8().toUpperCase(Locale.US));
        return count;
      }
    };
  }

  private Buffer gzip(String data) throws IOException {
    Buffer result = new Buffer();
    BufferedSink sink = Okio.buffer(new GzipSink(result));
    sink.writeUtf8(data);
    sink.close();
    return result;
  }

  /** Catches exceptions that are otherwise headed for the uncaught exception handler. */
  private static class ExceptionCatchingExecutor extends ThreadPoolExecutor {
    private final BlockingQueue<Exception> exceptions = new LinkedBlockingQueue<>();

    public ExceptionCatchingExecutor() {
      super(1, 1, 0, TimeUnit.SECONDS, new SynchronousQueue<Runnable>());
    }

    @Override public void execute(final Runnable runnable) {
      super.execute(new Runnable() {
        @Override public void run() {
          try {
            runnable.run();
          } catch (Exception e) {
            exceptions.add(e);
          }
        }
      });
    }

    public Exception takeException() throws InterruptedException {
      return exceptions.take();
    }
  }
}
