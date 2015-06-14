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

import co.paralleluniverse.fibers.FiberUtil;
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
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpRequestBase;

/**
 *
 * @author circlespainter
 */
public class FiberOkHttpUtil {
  public static Response executeInFiber(final FiberOkHttpClient client, final Request request) throws InterruptedException, IOException {
    return FiberOkHttpUtil.executeInFiber(client.newCall(request));
  }

    public static Response executeInFiber(final Call call) throws InterruptedException, IOException {
        return FiberUtil.runInFiberChecked (
            new SuspendableCallable<Response>() {
                @Override
                public Response run() throws SuspendExecution, InterruptedException {
                    try {
                        return call.execute();
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            }, IOException.class
        );
    }

    public static HttpURLConnection openInFiber(final OkUrlFactory factory, final URL url) throws InterruptedException {
        return FiberUtil.runInFiberRuntime (
            new SuspendableCallable<HttpURLConnection>() {
                @Override
                public HttpURLConnection run() throws SuspendExecution, InterruptedException {
                    return factory.open(url);
                }
            }
        );
    }

    public static HttpResponse executeInFiber(final OkApacheClient client, final HttpRequestBase req) throws InterruptedException, IOException {
        return FiberUtil.runInFiberChecked (
            new SuspendableCallable<HttpResponse>() {
                @Override
                public HttpResponse run() throws SuspendExecution, InterruptedException {
                    try {
                        return client.execute(req);
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                }
           }, IOException.class
        );
    }

    private FiberOkHttpUtil() {
    }
}
