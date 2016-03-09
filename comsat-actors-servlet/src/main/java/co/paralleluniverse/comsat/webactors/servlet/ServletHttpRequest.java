/*
 * COMSAT
 * Copyright (c) 2013-2016, Parallel Universe Software Co. All rights reserved.
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
package co.paralleluniverse.comsat.webactors.servlet;

import co.paralleluniverse.actors.ActorRef;
import co.paralleluniverse.comsat.webactors.Cookie;
import static co.paralleluniverse.comsat.webactors.Cookie.*;
import co.paralleluniverse.comsat.webactors.HttpRequest;
import co.paralleluniverse.comsat.webactors.HttpResponse;
import co.paralleluniverse.comsat.webactors.WebMessage;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.io.ByteStreams;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Map;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Wraps a {@link HttpServletRequest} as a {@link HttpRequest}
 */
final class ServletHttpRequest extends HttpRequest {
    final HttpServletRequest request;
    final HttpServletResponse response;

    private ListMultimap<String, String> headers;
    private Multimap<String, String> params;
    private Map<String, Object> attrs;
    private final ActorRef<? super HttpResponse> sender;
    private String strBody;
    private byte[] binBody;
    private Collection<Cookie> cookies;
    private String sourceAddress;

    /**
     * Constructs a {@code ServletHttpRequest} message
     *
     * @param sender this message's sender
     * @param request the {@link HttpServletRequest}
     * @param response the {@link HttpServletResponse}
     */
    public ServletHttpRequest(ActorRef<? super HttpResponse> sender, HttpServletRequest request, HttpServletResponse response) {
        this.sender = sender;
        this.request = request;
        this.response = response;
    }

    @Override
    public final String getSourceAddress() {
        if (sourceAddress == null)
            sourceAddress = request.getRemoteAddr();
        return sourceAddress;
    }

    //    @Override
//    public String getRequestURL() {
//        return request.getRequestURL().toString();
//    }
    @Override
    public final String getStringBody() {
        if (strBody == null) {
            if (binBody != null)
                return null;
            final byte[] ba = readBody();
            final String enc = request.getCharacterEncoding();
            try {
                this.strBody = enc!=null ?  new String(ba, enc) : new String(ba);
            } catch (final UnsupportedEncodingException e) {
                throw new UnsupportedCharsetException(enc);
            }
        }
        return strBody;
    }

    @Override
    public final ByteBuffer getByteBufferBody() {
        if (binBody == null) {
            if (strBody != null)
                return null;
            this.binBody = readBody();
        }
        return ByteBuffer.wrap(binBody).asReadOnlyBuffer();
    }

    private byte[] readBody() {
        try {
            final ServletInputStream is = request.getInputStream();
            final int length = request.getContentLength();
            final byte[] ba;
            if (length < 0)
                ba = ByteStreams.toByteArray(is);
            else {
                ba = new byte[length];
                ByteStreams.readFully(is, ba);
            }
            return ba;
        } catch (final IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public final ListMultimap<String, String> getHeaders() {
        if (headers == null) {
            final ImmutableListMultimap.Builder<String, String> mm = ImmutableListMultimap.builder();// LinkedHashMultimap.create();
            for (final Enumeration<String> hs = request.getHeaderNames(); hs.hasMoreElements();) {
                final String h = hs.nextElement();
                for (final Enumeration<String> hv = request.getHeaders(h); hv.hasMoreElements();)
                    mm.put(h, hv.nextElement());
            }
            this.headers = mm.build();
        }
        return headers;
    }

    @Override
    public final Multimap<String, String> getParameters() {
        if (params == null) {
            final ImmutableListMultimap.Builder<String, String> mm = ImmutableListMultimap.builder();
            for (final Enumeration<String> ps = request.getParameterNames(); ps.hasMoreElements();) {
                final String p = ps.nextElement();
                final String[] pvs = request.getParameterValues(p);
                if (pvs != null) {
                    for (final String pv : pvs)
                        mm.put(p, pv);
                }
            }
            this.params = mm.build();
        }
        return params;
    }

    @Override
    public final Map<String, Object> getAttributes() {
        if (attrs == null) {
            final ImmutableMap.Builder<String, Object> m = ImmutableMap.builder();
            for (final Enumeration<String> as = request.getAttributeNames(); as.hasMoreElements();) {
                final String a = as.nextElement();
                final Object v = request.getAttribute(a);
                m.put(a, v);
            }
            this.attrs = m.build();
        }
        return attrs;
    }

    @Override
    public final Collection<Cookie> getCookies() {
        if (cookies == null) {
            final javax.servlet.http.Cookie[] cs = request.getCookies();
            final ImmutableCollection.Builder<Cookie> collb = ImmutableList.builder();
            if (cs != null) {
                for (final javax.servlet.http.Cookie c : cs) {
                    collb.add(cookie(c.getName(), c.getValue())
                            .setComment(c.getComment())
                            .setDomain(c.getDomain())
                            .setMaxAge(c.getMaxAge())
                            .setHttpOnly(c.isHttpOnly())
                            .setPath(c.getPath())
                            .setSecure(c.getSecure())
                            .setVersion(c.getVersion())
                            .build());
                }
            }
            this.cookies = collb.build();
        }
        return cookies;
    }

    @Override
    public final long getDateHeader(String name) {
        return request.getDateHeader(name);
    }

    @Override
    public final String getMethod() {
        return request.getMethod();
    }

    @Override
    public final String getScheme() {
        return request.getScheme();
    }

    
    @Override
    public final String getQueryString() {
        return request.getQueryString();
    }

    @Override
    public final String getServerName() {
        return request.getServerName();
    }

    @Override
    public final int getServerPort() {
        return request.getServerPort();
    }

    @Override
    public final String getContextPath() {
        return request.getContextPath();
    }

    @Override
    public final String getRequestURI() {
        return request.getRequestURI();
    }

    @Override
    public final int getContentLength() {
        return request.getContentLength();
    }

    @Override
    public final String getContentType() {
        return request.getContentType();
    }

    @Override
    public final String getPathInfo() {
        return request.getPathInfo();
    }

    @Override
    public final Charset getCharacterEncoding() {
        return request.getCharacterEncoding() != null ? Charset.forName(request.getCharacterEncoding()) : null;
    }

    @Override
    public final ActorRef<WebMessage> getFrom() {
        //noinspection unchecked
        return (ActorRef<WebMessage>) sender;
    }
}
