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
package co.paralleluniverse.fibers.httpclient;

import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.Suspendable;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.UndeclaredThrowableException;
import java.net.URI;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.reactor.DefaultConnectingIOReactor;
import org.apache.http.impl.nio.reactor.ExceptionEvent;
import org.apache.http.nio.reactor.IOReactor;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.Args;
import org.apache.http.util.EntityUtils;

public class FiberHttpClient extends CloseableHttpClient {
    private final Log log = LogFactory.getLog(getClass());
    private final CloseableHttpAsyncClient client;
    private final HttpRequestRetryHandler httpRequestRetryHandler;

    private DefaultConnectingIOReactor ioreactor;

    public FiberHttpClient(CloseableHttpAsyncClient client) {
        this(client, null, null);
    }

    public FiberHttpClient(CloseableHttpAsyncClient client, IOReactor ioreactor) {
        this(client, null, ioreactor);
    }

    public FiberHttpClient(CloseableHttpAsyncClient client, HttpRequestRetryHandler httpRequestRetryHandler) {
        this(client, httpRequestRetryHandler, null);
    }

    public FiberHttpClient(CloseableHttpAsyncClient client, HttpRequestRetryHandler httpRequestRetryHandler, IOReactor ioreactor) {
        this.client = client;
        this.httpRequestRetryHandler = httpRequestRetryHandler;
        if (ioreactor != null && ioreactor instanceof DefaultConnectingIOReactor)
            this.ioreactor = (DefaultConnectingIOReactor) ioreactor;
        if (!client.isRunning())
            client.start();
    }

    @Override
    public HttpParams getParams() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ClientConnectionManager getConnectionManager() {
        throw new UnsupportedOperationException();
    }

    @Override
    @Suspendable
    protected final CloseableHttpResponse doExecute(final HttpHost target, final HttpRequest request, final HttpContext context) throws IOException, ClientProtocolException {
        try {
            for (int executionCount = 0;; executionCount++) {
                try {
                    final HttpResponse response = new AsyncHttpReq() {
                        @Override
                        protected void requestAsync() {
                            client.execute(target, request, context, this);
                        }
                    }.run();
                    return new CloseableHttpResponseWrapper(response);
                } catch (IOException ex) {
                    if (httpRequestRetryHandler != null && httpRequestRetryHandler.retryRequest(ex, executionCount, context)) {
                        if (this.log.isInfoEnabled()) {
                            this.log.info("I/O exception (" + ex.getClass().getName()
                                    + ") caught when processing request: "
                                    + ex.getMessage());
                        }
                        if (this.log.isDebugEnabled()) {
                            this.log.debug(ex.getMessage(), ex);
                        }
                        this.log.info("Retrying request");
                    } else
                        throw ex;
                }
            }
        } catch (SuspendExecution e) {
            throw new AssertionError();
        } catch (IllegalStateException ise) {
            if (ioreactor != null) {
                final List<ExceptionEvent> events = ioreactor.getAuditLog();
                if (events != null) {
                    for (ExceptionEvent event : events) {
                        final StringBuilder msg = new StringBuilder();
                        msg.append("Apache Async HTTP Client I/O Reactor exception timestamp: ");
                        msg.append(event.getTimestamp());
                        if (event.getCause() != null) {
                            msg.append(", cause stacktrace:\n");
                            final StringWriter sw = new StringWriter();
                            final PrintWriter pw = new PrintWriter(sw);
                            ise.getCause().printStackTrace(pw);
                            msg.append(sw.toString());
                        }
                        this.log.fatal(msg.toString());
                    }
                }
            }
            throw ise;
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
        return doExecute(determineTarget(request), request, context);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Suspendable
    public <T> T execute(final HttpUriRequest request, final ResponseHandler<? extends T> responseHandler, final HttpContext context) throws IOException, ClientProtocolException {
        throw new UnsupportedOperationException();
//        return execute(determineTarget(request), request, responseHandler, context);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Suspendable
    public CloseableHttpResponse execute(HttpHost target, HttpRequest request, HttpContext context) throws IOException, ClientProtocolException {
        Args.notNull(request, "HTTP request");
        return doExecute(target, request, context);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Suspendable
    public CloseableHttpResponse execute(HttpUriRequest request) throws IOException, ClientProtocolException {
        return execute(request, (HttpContext) null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Suspendable
    public CloseableHttpResponse execute(HttpHost target, HttpRequest request) throws IOException, ClientProtocolException {
        return execute(target, request, (HttpContext) null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Suspendable
    public <T> T execute(HttpUriRequest request, ResponseHandler<? extends T> responseHandler) throws IOException, ClientProtocolException {
        return execute(determineTarget(request), request, responseHandler, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Suspendable
    public <T> T execute(HttpHost target, HttpRequest request, ResponseHandler<? extends T> responseHandler) throws IOException, ClientProtocolException {
        return execute(target, request, responseHandler, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Suspendable
    public <T> T execute(HttpHost target, HttpRequest request, ResponseHandler<? extends T> responseHandler, HttpContext context) throws IOException, ClientProtocolException {
        final HttpResponse response = doExecute(target, request, context);

        final T result;
        try {
            result = responseHandler.handleResponse(response);
        } catch (final Exception t) {
            final HttpEntity entity = response.getEntity();
            try {
                EntityUtils.consume(entity);
            } catch (final Exception t2) {
                // Log this exception. The original exception is more
                // important and will be thrown to the caller.
                this.log.warn("Error consuming content after an exception.", t2);
            }
            if (t instanceof RuntimeException) {
                throw (RuntimeException) t;
            }
            if (t instanceof IOException) {
                throw (IOException) t;
            }
            throw new UndeclaredThrowableException(t);
        }

        // Handling the response was successful. Ensure that the content has
        // been fully consumed.
        final HttpEntity entity = response.getEntity();
        EntityUtils.consume(entity);
        return result;
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
