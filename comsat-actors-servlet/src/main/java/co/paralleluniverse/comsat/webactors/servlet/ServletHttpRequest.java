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

import co.paralleluniverse.common.util.Exceptions;
import co.paralleluniverse.comsat.webactors.Cookie;
import static co.paralleluniverse.comsat.webactors.Cookie.*;
import co.paralleluniverse.comsat.webactors.HttpRequest;
import co.paralleluniverse.comsat.webactors.HttpResponse;
import co.paralleluniverse.comsat.webactors.WebMessage;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.channels.ChannelClosedException;
import co.paralleluniverse.strands.channels.SendPort;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.servlet.AsyncContext;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ServletHttpRequest extends HttpRequest {
    private final HttpServletRequest request;
    private Multimap<String, String> headers;
    private Multimap<String, String> params;
    private Map<String, Object> attrs;
    private final SendPort<WebMessage> sender;

    public ServletHttpRequest(HttpServletRequest request, HttpServletResponse response) {
        this.request = request;
        this.sender = new Peer(request.getAsyncContext());
    }

    @Override
    public Collection<Cookie> getCookies() {
        final javax.servlet.http.Cookie[] cookies = request.getCookies();
        List<Cookie> list = new ArrayList<>();
        for (int i = 0; i < cookies.length; i++) {
            final javax.servlet.http.Cookie c = cookies[i];
            list.add(cookie(c.getName(), c.getValue()).setComment(c.getComment()).setDomain(c.getDomain()).
                    setMaxAge(c.getMaxAge()).setHttpOnly(c.isHttpOnly()).setPath(c.getPath()).setSecure(c.getSecure()).
                    setVersion(c.getVersion()).build());
        }
        return list;
    }

    @Override
    public String getRequestURL() {
        return request.getRequestURL().toString();
    }

    @Override
    public String getStringBody() {
        try {
            StringBuilder sb = new StringBuilder();
            BufferedReader reader = request.getReader();
            char[] b = new char[1024];
            int len;
            while ((len = reader.read(b)) != -1)
                sb.append(b, 0, len);
            return sb.toString();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public ByteBuffer getByteBufferBody() {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ServletInputStream inputStream = request.getInputStream();
            byte[] b = new byte[1024];
            int len;
            while ((len = inputStream.read(b)) != -1)
                bos.write(b, 0, len);

            return ByteBuffer.wrap(bos.toByteArray());
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

    private static class Peer implements SendPort<WebMessage> {
        private final AsyncContext ctx;
        private Throwable exception;

        public Peer(AsyncContext ctx) {
            this.ctx = ctx;
        }

        @Override
        public void send(WebMessage message) throws SuspendExecution, InterruptedException {
            if (!trySend(message)) {
                if (exception == null)
                    throw new ChannelClosedException(this, exception);
                throw Exceptions.rethrow(exception);
            }
        }

        @Override
        public boolean send(WebMessage message, long timeout, TimeUnit unit) throws SuspendExecution, InterruptedException {
            send(message);
            return true;
        }

        @Override
        public boolean trySend(WebMessage message) {
            try {
                if (!ctx.getRequest().isAsyncStarted())
                    return false;

                final HttpServletResponse response = (HttpServletResponse) ctx.getResponse();

                if (message instanceof HttpResponse) {
                    final HttpResponse msg = (HttpResponse) message;
                    if (!response.isCommitted()) {
                        if (msg.getCookies() != null) {
                            for (Cookie wc : msg.getCookies())
                                response.addCookie(getServletCookie(wc));
                        }
                        if (msg.getHeaders() != null) {
                            for (Map.Entry<String, String> h : msg.getHeaders().entries())
                                response.addHeader(h.getKey(), h.getValue());
                        }
                    }

                    if (msg.getError() != null) {
                        response.sendError(msg.getStatus(), msg.getError().toString());
                        close();
                        return true;
                    }
                    if (msg.getRedirectPath() != null) {
                        response.sendRedirect(msg.getRedirectPath());
                        close();
                        return true;
                    }

                    if (!response.isCommitted()) {
                        response.setStatus(msg.getStatus());

                        if (msg.getContentType() != null)
                            response.setContentType(msg.getContentType());
                        if (msg.getCharacterEncoding() != null)
                            response.setCharacterEncoding(msg.getCharacterEncoding());
                    }
                }

                ServletOutputStream out = writeBody(message, response);
                out.flush(); // commits the response

                if (message instanceof HttpResponse && ((HttpResponse) message).shouldClose()) {
                    out.close();
                    close();
                }
                return true;
            } catch (IOException ex) {
                ctx.getRequest().getServletContext().log("IOException", ex);
                close();
                this.exception = ex;
//                try {
//                    response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
//                } catch (IOException ex2) {
//                    request.getServletContext().log("IOException", ex2);
//                }

                return false;
            }
        }

        private ServletOutputStream writeBody(WebMessage message, HttpServletResponse response) throws IOException {
            final byte[] arr;
            final int offset;
            final int length;
            ByteBuffer bb;
            String sb;
            if ((bb = message.getByteBufferBody()) != null) {
//                        WritableByteChannel wc = Channels.newChannel(out);
//                        wc.write(bb);
                if (bb.hasArray()) {
                    arr = bb.array();
                    offset = bb.arrayOffset() + bb.position();
                    length = bb.remaining();
                    bb.position(bb.limit());
                } else {
                    arr = new byte[bb.remaining()];
                    bb.get(arr);
                    offset = 0;
                    length = arr.length;
                }
            } else if ((sb = message.getStringBody()) != null) {
                arr = sb.getBytes(response.getCharacterEncoding());
                offset = 0;
                length = arr.length;
            } else {
                arr = null;
                offset = 0;
                length = 0;
            }

            if (!response.isCommitted())
                response.setContentLength(length);

            final ServletOutputStream out = response.getOutputStream();
            if (arr != null)
                out.write(arr, offset, length);
            return out;
        }

        @Override
        public void close() {
            ctx.complete();
        }
    }

    @Override
    public SendPort<WebMessage> sender() {
        return sender;
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
