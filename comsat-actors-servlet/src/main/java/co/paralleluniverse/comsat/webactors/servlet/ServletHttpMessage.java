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
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.comsat.webactors.servlet;

import co.paralleluniverse.comsat.webactors.Cookie;
import co.paralleluniverse.comsat.webactors.HttpMessage;
import co.paralleluniverse.comsat.webactors.HttpResponse;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.channels.SendPort;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ServletHttpMessage extends HttpMessage {
    private final HttpServletRequest request;
    private final HttpServletResponse response;
    private Multimap<String, String> headers;
    private Multimap<String, String> params;
    private Map<String, Object> attrs;

    @Override
    public Collection<Cookie> getCookies() {
        final javax.servlet.http.Cookie[] cookies = request.getCookies();
        List<Cookie> list = new ArrayList<>();
        for (int i = 0; i < cookies.length; i++) {
            final javax.servlet.http.Cookie c = cookies[i];
            list.add(new Cookie(c.getName(), c.getValue()).setComment(c.getComment()).setDomain(c.getDomain()).
                    setMaxAge(c.getMaxAge()).setHttpOnly(c.isHttpOnly()).setPath(c.getPath()).setSecure(c.getSecure()).
                    setVersion(c.getVersion()));
        }
        return list;
    }

    @Override
    public String getRequestURL() {
        return request.getRequestURL().toString();
    }

    @Override
    public String getBody() {
        try {
            StringBuilder sb = new StringBuilder();
            ServletInputStream inputStream = request.getInputStream();
            byte[] b = new byte[1024];
            int len = -1;
            while ((len = inputStream.read(b)) != -1)
                sb.append(Arrays.toString(b));
            return sb.toString();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public ByteBuffer getBinaryBody() {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ServletInputStream inputStream = request.getInputStream();
            byte[] b = new byte[1024];
            while (inputStream.read(b) != -1)
                bos.write(b);

            return ByteBuffer.wrap(bos.toByteArray());
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public ServletHttpMessage(HttpServletRequest request, HttpServletResponse response) {
        this.request = request;
        this.response = response;
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
    public String getServletPath() {
        return request.getServletPath();
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
    public SendPort<HttpResponse> sender() {
        return new SendPort<HttpResponse>() {
            @Override
            public void send(HttpResponse message) throws SuspendExecution, InterruptedException {
                trySend(message);
            }

            @Override
            public boolean send(HttpResponse message, long timeout, TimeUnit unit) throws SuspendExecution, InterruptedException {
                return trySend(message);
            }

            @Override
            public boolean trySend(HttpResponse message) {
                try {
                    if (!request.isAsyncStarted())
                        throw new RuntimeException("Request is already commited, cannot send again");

                    final PrintWriter writer;
                    final ServletOutputStream out;

                    if (message.isBinary()) {
                        out = response.getOutputStream();
                        writer = null;
                        ByteBuffer bb = message.getBinBody();
//                        WritableByteChannel wc = Channels.newChannel(out);
//                        wc.write(bb);
                        if (bb.hasArray()) {
                            out.write(bb.array(), bb.arrayOffset() + bb.position(), bb.remaining());
                            bb.position(bb.limit());
                        } else {
                            byte[] arr = new byte[bb.remaining()];
                            bb.get(arr);
                            out.write(arr);
                        }
                    } else {
                        writer = response.getWriter();
                        out = null;
                        writer.print(message.getStringBody());
                    }

                    if (message.getContentType() != null)
                        response.setContentType(message.getContentType());
                    // TODO: content length

                    if (message.getCharacterEncoding() != null)
                        response.setCharacterEncoding(message.getCharacterEncoding());
                    response.setStatus(message.getStatus());
                    if (message.getCookies() != null) {
                        for (Cookie wc : message.getCookies())
                            response.addCookie(getServletCookie(wc));
                    }
                    if (message.getHeaders() != null) {
                        for (Map.Entry<String, String> h : message.getHeaders().entries())
                            response.addHeader(h.getKey(), h.getValue());
                    }
                    if (message.getError() != null) {
                        response.sendError(message.getStatus(), message.getError().toString());
                    }
                    if (message.getRedirectPath() != null)
                        response.sendRedirect(message.getRedirectPath());

                    if (message.isBinary())
                        out.close();
                    else
                        writer.close();
                    request.getAsyncContext().complete();
                    return true;
                } catch (IOException ex) {
                    request.getServletContext().log("IOException", ex);
                    try {
                        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    } catch (IOException ex2) {
                        request.getServletContext().log("IOException", ex2);
                    }
                    throw new RuntimeException(ex);
                }
            }

            @Override
            public void close() {
            }
        };
    }

    public static javax.servlet.http.Cookie getServletCookie(Cookie wc) {
        javax.servlet.http.Cookie c = new javax.servlet.http.Cookie(wc.getName(), wc.getValue());
        c.setComment(wc.getComment());
        if (wc.getDomain() != null)
            c.setDomain(wc.getDomain());
        c.setMaxAge(wc.getMaxAge());
        c.setPath(wc.getPath());
        c.setSecure(wc.isSecure());
        c.setHttpOnly(wc.isHttpOnly());
        c.setVersion(wc.getVersion());
        return c;
    }
}
