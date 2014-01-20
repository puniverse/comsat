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

import com.google.common.collect.Multimap;
import java.nio.charset.Charset;
import java.util.Collection;

/**
 * A message sent over an HTTP connection. Can be either a request or a response.
 */
public abstract class HttpMessage extends WebMessage {
    /**
     * A multimap of the headers contained in this message and (all) their values.
     * If the request has no headers, returns an empty multimap.
     */
    public abstract Multimap<String, String> getHeaders();

    /**
     * A collection all of the {@link Cookie} objects the client sent with this message.
     * This method returns an empty collection if no cookies were sent.
     */
    public abstract Collection<Cookie> getCookies();

    /**
     * The length, in bytes, of the message body and made available by the input stream,
     * or {@code -1} if the length is not known or is greater than {@code Integer.MAX_VALUE}.
     */
    public abstract int getContentLength();

    /**
     * The {@link Charset} representing character encoding used in the body of this message.
     * This method returns {@code null} if the message does not specify a character encoding.
     */
    public abstract Charset getCharacterEncoding();

    /**
     * The MIME type of the body of the request, or {@code null} if the type is not known
     * For example, {@code text/html; charset=UTF-8}.
     */
    public abstract String getContentType();

    /**
     * Returns all values associated with the given header name
     *
     * @param name the header name
     * @return all values associated with the given header name
     */
    public Collection<String> getHeaderValues(String name) {
        return getHeaders().get(name);
    }

    /**
     * Returns the value of the given header name.
     * If the header is not found in the message, this method returns {@code null}.
     * If the header has more than one value, this method returns the first value.
     *
     * @param name the header name
     * @return the (first) value of the given header name; {@code null} if the header is not found
     */
    public String getHeader(String name) {
        return first(getHeaders().get(name));
    }

    static <V> V first(Collection<V> c) {
        if (c == null || c.isEmpty())
            return null;
        return c.iterator().next();
    }
}
