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
package co.paralleluniverse.jersey.connector;

import com.google.common.base.Predicates;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.SettableFuture;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.ning.http.client.AsyncHandler;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.HttpResponseBodyPart;
import com.ning.http.client.HttpResponseHeaders;
import com.ning.http.client.HttpResponseStatus;
import com.ning.http.client.RequestBuilder;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.client.ClientRequest;
import org.glassfish.jersey.client.ClientResponse;
import org.glassfish.jersey.client.spi.AsyncConnectorCallback;
import org.glassfish.jersey.client.spi.Connector;
import org.glassfish.jersey.internal.util.PropertiesHelper;
import org.glassfish.jersey.internal.util.collection.ByteBufferInputStream;
import org.glassfish.jersey.internal.util.collection.NonBlockingInputStream;
import org.glassfish.jersey.message.internal.OutboundMessageContext;

/**
 *
 * @author pron
 */
public class AsyncHttpConnector implements Connector {
    private final AsyncHttpClient client;

    public AsyncHttpConnector(Configuration config) {
        AsyncHttpClientConfig.Builder builder = new AsyncHttpClientConfig.Builder();

        final ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat("AsyncHttpClient-Callback-%d").setDaemon(true).build();

        if (config != null) {
            final ExecutorService executorService;
            final Object threadPoolSize = config.getProperties().get(ClientProperties.ASYNC_THREADPOOL_SIZE);
            if (threadPoolSize != null && threadPoolSize instanceof Integer && (Integer) threadPoolSize > 0)
                executorService = Executors.newFixedThreadPool((Integer) threadPoolSize, threadFactory);
            else
                executorService = Executors.newCachedThreadPool(threadFactory);
            builder = builder.setExecutorService(executorService);

            builder.setConnectionTimeoutInMs(PropertiesHelper.getValue(config.getProperties(), ClientProperties.CONNECT_TIMEOUT, 0));
            builder.setRequestTimeoutInMs(PropertiesHelper.getValue(config.getProperties(), ClientProperties.READ_TIMEOUT, 0));
        } else
            builder.setExecutorService(Executors.newCachedThreadPool(threadFactory));

        AsyncHttpClientConfig asyncClientConfig = builder.setAllowPoolingConnection(true).build();
        this.client = new AsyncHttpClient(asyncClientConfig);
    }

    /**
     * Get name of current connector.
     *
     * Should contain identification of underlying specification and optionally version number.
     * Will be used in User-Agent header.
     *
     * @return name of current connector. Returning {@code null} or empty string means not including
     * this information in a generated <tt>{@value javax.ws.rs.core.HttpHeaders#USER_AGENT}</tt> header.
     */
    @Override
    public String getName() {
        return AsyncHttpClient.class.getName();
    }

    /**
     * Close connector and release all it's internally associated resources.
     */
    @Override
    public void close() {
        client.close();
    }

    /**
     * Synchronously process client request into a response.
     *
     * The method is used by Jersey client runtime to synchronously send a request
     * and receive a response.
     *
     * @param request Jersey client request to be sent.
     * @return Jersey client response received for the client request.
     * @throws javax.ws.rs.ProcessingException in case of any invocation failure.
     */
    @Override
    public ClientResponse apply(final ClientRequest request) throws ProcessingException {
        final SettableFuture<ClientResponse> responseFuture = SettableFuture.create();
        final ByteBufferInputStream entityStream = new ByteBufferInputStream();
        final AtomicBoolean futureSet = new AtomicBoolean(false);

        try {
            client.executeRequest(translateRequest(request), new AsyncHandler<Void>() {
                private volatile HttpResponseStatus status = null;

                @Override
                public STATE onStatusReceived(final HttpResponseStatus responseStatus) throws Exception {
                    status = responseStatus;
                    return STATE.CONTINUE;
                }

                @Override
                public STATE onHeadersReceived(HttpResponseHeaders headers) throws Exception {
                    if (!futureSet.compareAndSet(false, true)) {
                        return STATE.ABORT;
                    }

                    responseFuture.set(translateResponse(request, this.status, headers, entityStream));
                    return STATE.CONTINUE;
                }

                @Override
                public STATE onBodyPartReceived(HttpResponseBodyPart bodyPart) throws Exception {
                    entityStream.put(bodyPart.getBodyByteBuffer());
                    return STATE.CONTINUE;
                }

                @Override
                public Void onCompleted() throws Exception {
                    entityStream.closeQueue();
                    return null;
                }

                @Override
                public void onThrowable(Throwable t) {
                    entityStream.closeQueue(t);

                    if (futureSet.compareAndSet(false, true)) {
                        t = t instanceof IOException ? new ProcessingException(t.getMessage(), t) : t;
                        responseFuture.setException(t);
                    }
                }
            });

            return responseFuture.get();
        } catch (IOException ex) {
            throw new ProcessingException(ex.getMessage(), ex.getCause());
        } catch (ExecutionException ex) {
            Throwable e = ex.getCause() == null ? ex : ex.getCause();
            throw new ProcessingException(e.getMessage(), e);
        } catch (InterruptedException ex) {
            throw new ProcessingException(ex.getMessage(), ex);
        }
    }

    /**
     * Asynchronously process client request into a response.
     *
     * The method is used by Jersey client runtime to asynchronously send a request
     * and receive a response.
     *
     * @param request Jersey client request to be sent.
     * @param callback Jersey asynchronous connector callback to asynchronously receive
     * the request processing result (either a response or a failure).
     * @return asynchronously executed task handle.
     */
    @Override
    public Future<?> apply(final ClientRequest request, final AsyncConnectorCallback callback) {
        final ByteBufferInputStream entityStream = new ByteBufferInputStream();
        final AtomicBoolean callbackInvoked = new AtomicBoolean(false);

        Throwable failure;
        try {
            return client.executeRequest(translateRequest(request), new AsyncHandler<Void>() {
                private volatile HttpResponseStatus status = null;

                @Override
                public STATE onStatusReceived(final HttpResponseStatus responseStatus) throws Exception {
                    status = responseStatus;
                    return STATE.CONTINUE;
                }

                @Override
                public STATE onHeadersReceived(HttpResponseHeaders headers) throws Exception {
                    if (!callbackInvoked.compareAndSet(false, true))
                        return STATE.ABORT;

                    callback.response(translateResponse(request, this.status, headers, entityStream));
                    return STATE.CONTINUE;
                }

                @Override
                public STATE onBodyPartReceived(HttpResponseBodyPart bodyPart) throws Exception {
                    entityStream.put(bodyPart.getBodyByteBuffer());
                    return STATE.CONTINUE;
                }

                @Override
                public Void onCompleted() throws Exception {
                    entityStream.closeQueue();
                    return null;
                }

                @Override
                public void onThrowable(Throwable t) {
                    entityStream.closeQueue(t);

                    if (callbackInvoked.compareAndSet(false, true)) {
                        t = t instanceof IOException ? new ProcessingException(t.getMessage(), t) : t;
                        callback.failure(t);
                    }
                }
            });
        } catch (IOException ex) {
            failure = new ProcessingException(ex.getMessage(), ex.getCause());
        } catch (Throwable t) {
            failure = t;
        }

        if (callbackInvoked.compareAndSet(false, true))
            callback.failure(failure);

        return Futures.immediateFailedFuture(failure);
    }

    private com.ning.http.client.Request translateRequest(ClientRequest requestContext) {
        final RequestBuilder builder = new RequestBuilder(requestContext.getMethod()); // method

        builder.setUrl(requestContext.getUri().toString());              // url

        builder.setFollowRedirects(PropertiesHelper.getValue(requestContext.getConfiguration().getProperties(), ClientProperties.FOLLOW_REDIRECTS, true));

        final com.ning.http.client.Request.EntityWriter entity = this.getHttpEntity(requestContext);
        if (entity != null)
            builder.setBody(entity);

        MultivaluedMap<String, Object> headers = requestContext.getHeaders();
        Map<String, Collection<String>> headers1 = new HashMap<String, Collection<String>>();
        for (Map.Entry<String, List<Object>> entry : headers.entrySet()) {
            List<String> vals = new ArrayList<String>(entry.getValue().size());
            for (Object o : entry.getValue())
                vals.add(o.toString());
            headers1.put(entry.getKey(), vals);
        }
        builder.setHeaders(headers1);                                     // headers

        com.ning.http.client.Request result = builder.build();
        //writeOutBoundHeaders(request.getHeaders(), result);

        return result;
    }

    private com.ning.http.client.Request.EntityWriter getHttpEntity(final ClientRequest requestContext) {
        final Object entity = requestContext.getEntity();

        if (entity == null)
            return null;

        return new com.ning.http.client.Request.EntityWriter() {
            @Override
            public void writeEntity(final OutputStream out) throws IOException {
                requestContext.setStreamProvider(new OutboundMessageContext.StreamProvider() {
                    @Override
                    public OutputStream getOutputStream(int contentLength) throws IOException {
                        return out;
                    }
                });
                requestContext.writeEntity();
            }
        };
    }

    private static void writeOutBoundHeaders(final MultivaluedMap<String, Object> headers, final com.ning.http.client.Request request) {
        for (Map.Entry<String, List<Object>> e : headers.entrySet()) {
            List<Object> vs = e.getValue();
            if (vs.size() == 1)
                request.getHeaders().add(e.getKey(), vs.get(0).toString());
            else {
                StringBuilder b = new StringBuilder();
                for (Object v : e.getValue()) {
                    if (b.length() > 0)
                        b.append(',');

                    b.append(v);
                }
                request.getHeaders().add(e.getKey(), b.toString());
            }
        }
    }

    private ClientResponse translateResponse(final ClientRequest requestContext,
            final HttpResponseStatus status,
            final HttpResponseHeaders headers,
            final NonBlockingInputStream entityStream) {

        final ClientResponse responseContext = new ClientResponse(new Response.StatusType() {
            @Override
            public int getStatusCode() {
                return status.getStatusCode();
            }

            @Override
            public Response.Status.Family getFamily() {
                return Response.Status.Family.familyOf(status.getStatusCode());
            }

            @Override
            public String getReasonPhrase() {
                return status.getStatusText();
            }
        }, requestContext);

//        for (Map.Entry<String, List<String>> entry : headers.getHeaders().entrySet()) {
//            for (String value : entry.getValue()) {
//                // TODO value.toString?
//                responseContext.getHeaders().add(entry.getKey(), value);
//            }
//        }

        responseContext.headers(Maps.<String, List<String>>filterKeys(headers.getHeaders(), Predicates.notNull()));
        responseContext.setEntityStream(entityStream);

        return responseContext;
    }

    private ClientResponse translateResponse(ClientRequest request, final com.ning.http.client.Response response) throws IOException {
        final ClientResponse responseContext = new ClientResponse(new Response.StatusType() {
            @Override
            public int getStatusCode() {
                return response.getStatusCode();
            }

            @Override
            public Response.Status.Family getFamily() {
                return Response.Status.Family.familyOf(response.getStatusCode());
            }

            @Override
            public String getReasonPhrase() {
                return response.getStatusText();
            }
        }, request);

        responseContext.headers(Maps.<String, List<String>>filterKeys(response.getHeaders(), Predicates.notNull()));
        responseContext.setEntityStream(response.getResponseBodyAsStream());

        return responseContext;
    }
}
