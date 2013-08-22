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
import com.ning.http.client.AsyncHandler;
import com.ning.http.client.AsyncHandler.STATE;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.HttpResponseBodyPart;
import com.ning.http.client.HttpResponseHeaders;
import com.ning.http.client.HttpResponseStatus;
import com.ning.http.client.Request;
import com.ning.http.client.RequestBuilder;
import com.ning.http.client.Response;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.core.MultivaluedMap;
import org.glassfish.jersey.client.ClientRequest;
import org.glassfish.jersey.client.ClientResponse;
import org.glassfish.jersey.client.spi.AsyncConnectorCallback;
import org.glassfish.jersey.client.spi.Connector;
import org.glassfish.jersey.message.internal.Statuses;

/**
 *
 * @author pron
 */
public class AsyncHttpConnector implements Connector {
    private final AsyncHttpClient client;

    public AsyncHttpConnector() {
        this.client = new AsyncHttpClient();
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
    public ClientResponse apply(ClientRequest request) throws ProcessingException {
        throw new UnsupportedOperationException("Only async requests supported");
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
    public Future<?> apply(ClientRequest request, AsyncConnectorCallback callback) {
        try {
            return client.executeRequest(translateRequest(request), translateCallback(callback, request));
        } catch (IOException e) {
            throw new ProcessingException(e);
        }
    }

    private Request translateRequest(ClientRequest request) throws IOException {
        final RequestBuilder builder = new RequestBuilder(request.getMethod()); // method
        builder.setUrl(request.getUri().toURL().toString());              // url

        MultivaluedMap<String, Object> headers = request.getHeaders();
        Map<String, Collection<String>> headers1 = new HashMap<String, Collection<String>>();
        for (Map.Entry<String, List<Object>> entry : headers.entrySet()) {
            List<String> vals = new ArrayList<String>(entry.getValue().size());
            for (Object o : entry.getValue())
                vals.add(o.toString());
            headers1.put(entry.getKey(), vals);
        }
        builder.setHeaders(headers1);                                     // headers

        return builder.build();
    }

    private ClientResponse translateResponse(ClientRequest request, com.ning.http.client.Response response) throws IOException {
        final int code = response.getStatusCode();
        final String reasonPhrase = response.getStatusText();
        final javax.ws.rs.core.Response.StatusType status = reasonPhrase == null ? Statuses.from(code) : Statuses.from(code, reasonPhrase);
        ClientResponse responseContext = new ClientResponse(status, request);
        responseContext.headers(Maps.<String, List<String>>filterKeys(response.getHeaders(), Predicates.notNull()));
        responseContext.setEntityStream(response.getResponseBodyAsStream());
        return responseContext;
    }

    private AsyncHandler<Void> translateCallback(final AsyncConnectorCallback callback, final ClientRequest request) {
        return new AsyncHandler<Void>() {
            private final Response.ResponseBuilder builder = new Response.ResponseBuilder();

            @Override
            public STATE onStatusReceived(HttpResponseStatus responseStatus) throws Exception {
                builder.accumulate(responseStatus);
                return STATE.CONTINUE;
            }

            @Override
            public STATE onHeadersReceived(HttpResponseHeaders headers) throws Exception {
                builder.accumulate(headers);
                return STATE.CONTINUE;
            }

            @Override
            public STATE onBodyPartReceived(HttpResponseBodyPart bodyPart) throws Exception {
                builder.accumulate(bodyPart);
                return STATE.CONTINUE;
            }

            @Override
            public Void onCompleted() throws Exception {
                Response response = builder.build();
                ClientResponse cr = translateResponse(request, response);
                callback.response(cr);
                return null;
            }

            @Override
            public void onThrowable(Throwable t) {
                callback.failure(t);
            }
        };
    }

    /**
     * Close connector and release all it's internally associated resources.
     */
    @Override
    public void close() {
        client.close();
    }
}
