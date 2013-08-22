/*
 * COMSAT
 * Copyright (C) 2013, Parallel Universe Software Co. All rights reserved.
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

import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.Suspendable;
import java.io.IOException;
import java.net.URI;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.Args;
import org.apache.http.util.EntityUtils;

/**
 *
 * @author pron
 */
public class FiberHttpClient extends CloseableHttpClient {
    private final CloseableHttpAsyncClient client;

    public FiberHttpClient(CloseableHttpAsyncClient client) {
        this.client = client;
    }

    @Override
    public HttpParams getParams() {
        return client.getParams();
    }

    @Override
    public ClientConnectionManager getConnectionManager() {
        throw new UnsupportedOperationException();
    }

    @Override
    @Suspendable
    protected final CloseableHttpResponse doExecute(final HttpHost target, final HttpRequest request, final HttpContext context) throws IOException, ClientProtocolException {
        try {
            final HttpResponse response = new AsyncHttpReq() {
                @Override
                protected Void requestAsync(Fiber current, FutureCallback<HttpResponse> callback) {
                    client.execute(target, request, context, callback);
                    return null;
                }
            }.run();
            return new CloseableHttpResponseWrapper(response);
        } catch (SuspendExecution e) {
            throw new AssertionError();
        }
    }

    private static class CloseableHttpResponseWrapper extends DelegatingHttpResponse implements CloseableHttpResponse {
        public CloseableHttpResponseWrapper(HttpResponse response) {
            super(response);
        }

        @Override
        public void close() throws IOException {
            final HttpEntity entity = this.response.getEntity();
            EntityUtils.consume(entity);
        }
    }

    @Override
    public void close() throws IOException {
        client.close();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Suspendable
    public CloseableHttpResponse execute(final HttpUriRequest request, final HttpContext context) throws IOException, ClientProtocolException {
        Args.notNull(request, "HTTP request");
        final HttpHost target = determineTarget(request);
        return doExecute(target, request, context);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Suspendable
    public <T> T execute(final HttpUriRequest request, final ResponseHandler<? extends T> responseHandler, final HttpContext context) throws IOException, ClientProtocolException {
        final HttpHost target = determineTarget(request);
        return execute(target, request, responseHandler, context);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Suspendable
    public CloseableHttpResponse execute(HttpHost target, HttpRequest request, HttpContext context) throws IOException, ClientProtocolException {
        return super.execute(target, request, context);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Suspendable
    public CloseableHttpResponse execute(HttpUriRequest request) throws IOException, ClientProtocolException {
        return super.execute(request);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Suspendable
    public CloseableHttpResponse execute(HttpHost target, HttpRequest request) throws IOException, ClientProtocolException {
        return super.execute(target, request);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Suspendable
    public <T> T execute(HttpUriRequest request, ResponseHandler<? extends T> responseHandler) throws IOException, ClientProtocolException {
        return super.execute(request, responseHandler);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Suspendable
    public <T> T execute(HttpHost target, HttpRequest request, ResponseHandler<? extends T> responseHandler) throws IOException, ClientProtocolException {
        return super.execute(target, request, responseHandler);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Suspendable
    public <T> T execute(HttpHost target, HttpRequest request, ResponseHandler<? extends T> responseHandler, HttpContext context) throws IOException, ClientProtocolException {
        return super.execute(target, request, responseHandler, context);
    }

    private static HttpHost determineTarget(final HttpUriRequest request) throws ClientProtocolException {
        // A null target may be acceptable if there is a default target.
        // Otherwise, the null target is detected in the director.
        HttpHost target = null;

        final URI requestURI = request.getURI();
        if (requestURI.isAbsolute()) {
            target = URIUtils.extractHost(requestURI);
            if (target == null)
                throw new ClientProtocolException("URI does not specify a valid host name: " + requestURI);
        }
        return target;
    }
}
