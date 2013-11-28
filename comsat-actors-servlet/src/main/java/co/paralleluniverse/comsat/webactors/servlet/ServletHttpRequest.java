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
package co.paralleluniverse.comsat.webactors.servlet;

import co.paralleluniverse.actors.ActorRef;
import co.paralleluniverse.comsat.webactors.Cookie;
import static co.paralleluniverse.comsat.webactors.Cookie.*;
import co.paralleluniverse.comsat.webactors.HttpRequest;
import co.paralleluniverse.comsat.webactors.HttpResponse;
import co.paralleluniverse.comsat.webactors.WebDataMessage;
import co.paralleluniverse.strands.channels.SendPort;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
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
public class ServletHttpRequest extends HttpRequest {
    final HttpServletRequest request;
    final HttpServletResponse response;
    private Multimap<String, String> headers;
    private Multimap<String, String> params;
    private Map<String, Object> attrs;
    private final ActorRef<? super HttpResponse> sender;
    private String strBody;
    private byte[] binBody;
    private SendPort<WebDataMessage> channel;
    private Collection<Cookie> cookies;

    /**
     * Constructs a {@code ServletHttpRequest} message
     *
     * @param sender   this message's sender
     * @param request  the {@link HttpServletRequest}
     * @param response the {@link HttpServletResponse}
     */
    public ServletHttpRequest(ActorRef<? super HttpResponse> sender, HttpServletRequest request, HttpServletResponse response) {
        this.sender = sender;
        this.request = request;
        this.response = response;
    }

//    @Override
//    public String getRequestURL() {
//        return request.getRequestURL().toString();
//    }

    @Override
    public String getStringBody() {
        if (strBody == null) {
            if (binBody != null)
                return null;
            byte[] ba = readBody();
            String enc = request.getCharacterEncoding();
            try {
                this.strBody = new String(ba, enc);
            } catch (UnsupportedEncodingException e) {
                throw new UnsupportedCharsetException(enc);
            }
        }
        return strBody;
    }

    @Override
    public ByteBuffer getByteBufferBody() {
        if (binBody == null) {
            if (strBody != null)
                return null;
            this.binBody = readBody();
        }
        return ByteBuffer.wrap(binBody).asReadOnlyBuffer();
    }

    private byte[] readBody() {
        try {
            ServletInputStream is = request.getInputStream();
            int length = request.getContentLength();
            byte[] ba;
            if (length < 0)
                ba = ByteStreams.toByteArray(is);
            else {
                ba = new byte[length];
                ByteStreams.readFully(is, ba);
            }
            return ba;
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public Multimap<String, String> getHeaders() {
        if (headers == null) {
            ImmutableListMultimap.Builder<String, String> mm = ImmutableListMultimap.builder();// LinkedHashMultimap.create();
            for (Enumeration<String> hs = request.getHeaderNames(); hs.hasMoreElements();) {
                String h = hs.nextElement();
                for (Enumeration<String> hv = request.getHeaders(h); hv.hasMoreElements();)
                    mm.put(h, hv.nextElement());
            }
            this.headers = mm.build();
        }
        return headers;
    }

    @Override
    public Multimap<String, String> getParameters() {
        if (params == null) {
            ImmutableListMultimap.Builder<String, String> mm = ImmutableListMultimap.builder();
            for (Enumeration<String> ps = request.getParameterNames(); ps.hasMoreElements();) {
                String p = ps.nextElement();
                String[] pvs = request.getParameterValues(p);
                if (pvs != null) {
                    for (String pv : pvs)
                        mm.put(p, pv);
                }
            }
            this.params = mm.build();
        }
        return params;
    }

    @Override
    public Map<String, Object> getAtrributes() {
        if (attrs == null) {
            ImmutableMap.Builder<String, Object> m = ImmutableMap.builder();
            for (Enumeration<String> as = request.getAttributeNames(); as.hasMoreElements();) {
                String a = as.nextElement();
                Object v = request.getAttribute(a);
                m.put(a, v);
            }
            this.attrs = m.build();
        }
        return attrs;
    }

    @Override
    public Collection<Cookie> getCookies() {
        if (cookies == null) {
            final javax.servlet.http.Cookie[] cs = request.getCookies();
            ImmutableCollection.Builder<Cookie> collb = ImmutableList.builder();
            if (cs != null) {
                for (javax.servlet.http.Cookie c : cs) {
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
    public long getDateHeader(String name) {
        return request.getDateHeader(name);
    }

    @Override
    public String getMethod() {
        return request.getMethod();
    }

    @Override
    public String getQueryString() {
        return request.getQueryString();
    }

    @Override
    public String getServerName() {
        return request.getServerName();
    }

    @Override
    public int getServerPort() {
        return request.getServerPort();
    }

    @Override
    public String getContextPath() {
        return request.getContextPath();
    }

    @Override
    public String getRequestURI() {
        return request.getRequestURI();
    }

    @Override
    public int getContentLength() {
        return request.getContentLength();
    }

    @Override
    public String getContentType() {
        return request.getContentType();
    }

    @Override
    public String getPathInfo() {
        return request.getPathInfo();
    }

    @Override
    public Charset getCharacterEncoding() {
        return request.getCharacterEncoding() != null ? Charset.forName(request.getCharacterEncoding()) : null;
    }

    @Override
    public ActorRef<HttpResponse> sender() {
        return (ActorRef<HttpResponse>) sender;
    }

    @Override
    public SendPort<WebDataMessage> openChannel() {
        if (channel == null)
            channel = WebActorServlet.openChannel(request, response);
        return channel;
    }

    @Override
    protected boolean shouldClose() {
        return channel == null;
    }
}
