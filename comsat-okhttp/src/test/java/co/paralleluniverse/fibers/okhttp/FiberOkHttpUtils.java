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

import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.SuspendableCallable;
import com.squareup.okhttp.Call;
import com.squareup.okhttp.OkUrlFactory;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import com.squareup.okhttp.apache.OkApacheClient;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutionException;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpRequestBase;

/**
 *
 * @author circlespainter
 */
public class FiberOkHttpUtils {
  public static RecordedResponse fiberExecRecorded(final FiberOkHttpClient client, final Request request) throws InterruptedException, IOException, ExecutionException {
    final Response response = FiberOkHttpUtils.fiberExec(client.newCall(request));
    return new RecordedResponse(request, response, null, response.body().string(), null);
  }

  public static Response fiberExec(final FiberOkHttpClient client, final Request request) throws InterruptedException, IOException, ExecutionException {
    return FiberOkHttpUtils.fiberExec(client.newCall(request));
  }

  public static Response fiberExec(final Call call) throws InterruptedException, IOException, ExecutionException {
    return fiberOkTry (
        new SuspendableCallable<Response>() {
            @Override
            public Response run() throws SuspendExecution, InterruptedException {
                try {
                    return call.execute();
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }
        });
  }

  public static HttpURLConnection open(final OkUrlFactory factory, final URL url) throws InterruptedException, IOException, ExecutionException {
    return fiberOkTry (
        new SuspendableCallable<HttpURLConnection>() {
            @Override
            public HttpURLConnection run() throws SuspendExecution, InterruptedException {
                return factory.open(url);
            }
        });
  }

  public static HttpResponse execute(final OkApacheClient client, final HttpRequestBase req) throws InterruptedException, IOException, ExecutionException {
    return fiberOkTry (
        new SuspendableCallable<HttpResponse>() {
            @Override
            public HttpResponse run() throws SuspendExecution, InterruptedException {
                try {
                    return client.execute(req);
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }
        });
  }
  
  private static <X> X fiberOkTry(final SuspendableCallable<X> call) throws InterruptedException, IOException, ExecutionException {
    X res = null;
    try {
        res = new Fiber<>(call).start().get();
    } catch (ExecutionException ee) {
        if (ee.getCause() instanceof RuntimeException) {
            final RuntimeException re = (RuntimeException) ee.getCause();
            if (re.getCause() instanceof IOException)
                throw (IOException) re.getCause();
            else
                throw re;
        } else if (ee.getCause() instanceof IllegalStateException)
            throw ((IllegalStateException) ee.getCause());
        else
            throw ee;
    }
    // Should never happen
    return res;
  }
}
