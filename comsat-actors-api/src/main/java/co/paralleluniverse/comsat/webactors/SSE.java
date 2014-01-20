/*
 * COMSAT
 * Copyright (c) 2013-2014, Parallel Universe Software Co. All rights reserved.
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
 * Utility classes for SSE (<a href="http://dev.w3.org/html5/eventsource/">Server-Sent Events</a>).
 * To start an SSE stream in response to an {@link HttpRequest}, do the following:
 *
 * ```java
 * request.getFrom().send(new HttpResponse(self(), SSE.startSSE(request)));
 * ```
 * This will result in a {@link HttpStreamOpened} message being sent to the web actor from a newly 
 * created actor that represents the SSE connection. To send SSE events, simply send {@link WebDataMessage}s
 * to that actor:
 * 
 * ```java
 * // send events
 * sseActor.send(new WebDataMessage(self(), SSE.event("this is an SSE event!")));
 * ```
 * 
 * You might want to consider wrapping the actor sending {@link HttpStreamOpened} with a 
 * {@link co.paralleluniverse.strands.channels.Channels#map(co.paralleluniverse.strands.channels.SendPort, com.google.common.base.Function) mapping channel}
 * to transform a specialized message class into {@link WebDataMessage} using the methods in this class.
 * 
 * For a good tutorial on SSE, please see: <a href="http://www.html5rocks.com/en/tutorials/eventsource/basics/">Stream Updates with Server-Sent Events</a>,
 * by Eric Bidelman
 */
public final class SSE {
    /*
     *see http://www.html5rocks.com/en/tutorials/eventsource/basics/
     */
    /**
     * This method returns a new {@link HttpResponse HttpResponse} with
     * its {@link HttpResponse.Builder#setContentType(String) content type}
     * and {@link HttpResponse.Builder#setCharacterEncoding(java.nio.charset.Charset) character encoding} set
     * in compliance with to the SSE spec, and an empty body.
     *
     * @param request the {@link HttpRequest} in response to which we wish to start an SSE stream.
     * @return an {@link HttpResponse.Builder HttpResponse.Builder} (which can have other metadata, such as headers or cookies added to).
     */
    public static HttpResponse.Builder startSSE(HttpRequest request) {
        return new HttpResponse.Builder(request)
                .setContentType("text/event-stream")
                .setCharacterEncoding(Charset.forName("UTF-8"))
                .startActor();
    }

    /**
     * This method returns a new {@link HttpResponse HttpResponse} with
     * its {@link HttpResponse.Builder#setContentType(String) content type}
     * and {@link HttpResponse.Builder#setCharacterEncoding(java.nio.charset.Charset) character encoding} set
     * in compliance with to the SSE spec, and a body encoding a {@link #reconnectTimeout(long) reconnection timeout} indication.
     *
     * @param request          the {@link HttpRequest} in response to which we wish to start an SSE stream.
     * @param reconnectTimeout the amount of time, in milliseconds, the client should wait before attempting to reconnect
     *                         after the connection has closed (will be encoded in the message body as {@code retry: ...})
     * @return an {@link HttpResponse.Builder HttpResponse.Builder} (which can have other metadata, such as headers or cookies added to).
     */
    public static HttpResponse.Builder startSSE(HttpRequest request, long reconnectTimeout) {
        return new HttpResponse.Builder(request, retryString(reconnectTimeout) + '\n')
                .setContentType("text/event-stream")
                .setCharacterEncoding(Charset.forName("UTF-8"))
                .startActor();
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
     * @param id        the SSE event id (will be encoded in the message as {@code id: ...})
     * @param eventType the name of the type of the event (will be encoded in the message as {@code event: ...})
     * @param payload   the message payload (will be encoded in the message as {@code data: ...})
     * @return the payload encoded as an SSE event
     */
    public static String event(long id, String eventType, String payload) {
        return idString(id) + eventString(eventType) + dataString(payload) + '\n';
    }

    /**
     * Encodes a given payload as an SSE event message. The returned value can be used as the body of a {@link WebDataMessage}.
     *
     * @param eventType the name of the type of the event (will be encoded in the message as {@code event: ...})
     * @param payload   the message payload (will be encoded in the message as {@code data: ...})
     * @return the payload encoded as an SSE event
     */
    public static String event(String eventType, String payload) {
        return dataString(payload) + '\n';
    }

    /**
     * Encodes a given payload and id as an SSE event message. The returned value can be used as the body of a {@link WebDataMessage}.
     *
     * @param id      the SSE event id (will be encoded in the message as {@code id: ...})
     * @param payload the message payload (will be encoded in the message as {@code data: ...})
     * @return the id and payload encoded as an SSE event
     */
    public static String event(long id, String payload) {
        return idString(id) + dataString(payload) + '\n';
    }

    /**
     * Encodes a given payload as an SSE event message. The returned value can be used as the body of a {@link WebDataMessage}.
     *
     * @param payload the message payload the message payload (will be encoded in the message as {@code data: ...})
     * @return the payload encoded as an SSE event
     */
    public static String event(String payload) {
        return dataString(payload) + '\n';
    }

    /**
     * Encodes an indication to the client to attempt a reconnect if the connection is closed within the given time.
     * This string may be concatenated ahead of a string encoding an SSE event, like so: {@code reconnectTimeout(t) + event(x)}).
     *
     * @param reconnectTimeout the amount of time, in milliseconds, the client should wait before attempting to reconnect
     *                         after the connection has closed (will be encoded in the message as {@code retry: ...})
     * @return a string encoding the reconnection timeout indication
     */
    public static String reconnectTimeout(long reconnectTimeout) {
        return retryString(reconnectTimeout);
    }

    private static String idString(long id) {
        return "id: " + id + '\n';
    }

    private static String eventString(String eventName) {
        return "event: " + eventName + '\n';
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
