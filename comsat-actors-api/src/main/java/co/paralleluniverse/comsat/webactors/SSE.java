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
package co.paralleluniverse.comsat.webactors;

import java.nio.charset.Charset;

/**
 *
 * @author pron
 */
public final class SSE {
    //
    // see http://www.html5rocks.com/en/tutorials/eventsource/basics/
    //
    public static HttpResponse.Builder startSSE(HttpRequest request) {
        if (request.shouldClose())
            throw new IllegalStateException("HttpRequest.openChannel() has not been called");
        return new HttpResponse.Builder(request)
                .setContentType("text/event-stream")
                .setCharacterEncoding(Charset.forName("UTF-8"));
    }

    public static HttpResponse.Builder startSSE(HttpRequest request, long reconnectTimeout) {
        if (request.shouldClose())
            throw new IllegalStateException("HttpRequest.openChannel() has not been called");
        return new HttpResponse.Builder(request, retryString(reconnectTimeout) + '\n')
                .setContentType("text/event-stream")
                .setCharacterEncoding(Charset.forName("UTF-8"));
    }

    /**
     * Returns the SSE last-event-id value from the request (the {@code Last-Event-ID} header).
     *
     * @param request the request
     * @return the SSE last-event-id value from the request, or {@code -1} if not specified.
     */
    public static long getLastEventId(HttpRequest request) {
        String str = request.getHeader("Last-Event-ID");
        if (str == null)
            return -1;
        return Long.parseLong(str);
    }

    /**
     * Encodes a given payload as an SSE event message. The returned value can be used as the body of a {@link WebDataMessage}.
     *
     * @param payload the message payload
     * @return the payload encoded as an SSE event
     */
    public static String sseMessage(String payload) {
        return dataString(payload) + '\n';
    }

    /**
     * Encodes a given payload and id as an SSE event message. The returned value can be used as the body of a {@link WebDataMessage}.
     *
     * @param id      the SSE event id
     * @param payload the message payload
     * @return the id and payload encoded as an SSE event
     */
    public static String sseMessage(long id, String payload) {
        return idString(id) + dataString(payload) + '\n';
    }

    public static String sseMessage(long reconnectTimeout, long id, String payload) {
        return retryString(reconnectTimeout) + idString(id) + dataString(payload) + '\n';
    }

    private static String idString(long id) {
        return "id: " + id + '\n';
    }

    private static String retryString(long reconnectTimeout) {
        return "retry: " + reconnectTimeout + '\n';
    }

    private static String dataString(String payload) {
        String message = payload.trim();
        if (message.charAt(message.length() - 1) == '\n')
            message = message.substring(0, message.length() - 1);
        message = message.replaceAll("\n", "\ndata: ");
        message = "data: " + message + '\n';
        return message;
    }

    private SSE() {
    }
}
