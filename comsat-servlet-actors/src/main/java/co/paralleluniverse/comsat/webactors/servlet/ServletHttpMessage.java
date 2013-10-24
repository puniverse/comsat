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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ServletHttpMessage implements HttpMessage {
    private final HttpServletRequest request;
    private final HttpServletResponse response;

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
            int len = -1;
            while ((len = inputStream.read(b)) != -1)
                bos.write(b);

            return ByteBuffer.wrap(bos.toByteArray());
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public interface EnumGettersForRoMap<K, V> {
        public V get(K key);

        public Enumeration<K> getKeys();
    }

    public <K, V> Map<K, V> getMapFromEnums(final EnumGettersForRoMap<K, V> etrm) {
        return new Map<K, V>() {
            @Override
            public int size() {
                int i = 0;
                Enumeration keys = etrm.getKeys();
                while (keys.hasMoreElements()) {
                    i++;
                    keys.nextElement();
                }
                return i;
            }

            @Override
            public boolean isEmpty() {
                return !etrm.getKeys().hasMoreElements();
            }

            @Override
            public boolean containsKey(Object key) {
                Enumeration keys = etrm.getKeys();
                while (keys.hasMoreElements())
                    if (keys.nextElement().equals(key))
                        return true;
                return false;
            }

            @Override
            public boolean containsValue(Object value) {
                Enumeration<K> keys = etrm.getKeys();
                while (keys.hasMoreElements())
                    if (etrm.get(keys.nextElement()).equals(value))
                        return true;
                return false;
            }

            @Override
            public V get(Object key) {
                return etrm.get((K) key);
            }

            @Override
            public V put(K key, V value) {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            @Override
            public V remove(Object key) {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            @Override
            public void putAll(Map<? extends K, ? extends V> m) {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            @Override
            public void clear() {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            @Override
            public Set<K> keySet() {
                return new HashSet<>(Collections.list(etrm.getKeys()));
            }

            @Override
            public Collection<V> values() {
                ArrayList<V> values = new ArrayList<>();
                Enumeration<K> keys = etrm.getKeys();
                while (keys.hasMoreElements())
                    values.add(etrm.get(keys.nextElement()));
                return values;
            }

            @Override
            public Set<Entry<K, V>> entrySet() {
                final Set<Entry<K, V>> hashSet = new HashSet<>();
                Enumeration<K> keys = etrm.getKeys();
                while (keys.hasMoreElements()) {
                    final K key = keys.nextElement();
                    hashSet.add(new Entry<K, V>() {
                        @Override
                        public K getKey() {
                            return key;
                        }

                        @Override
                        public V getValue() {
                            return etrm.get(key);
                        }

                        @Override
                        public V setValue(V value) {
                            throw new UnsupportedOperationException("Not supported yet.");
                        }
                    });
                }
                return hashSet;
            }
        };
    }

    public ServletHttpMessage(HttpServletRequest request, HttpServletResponse response) {
        this.request = request;
        this.response = response;
    }

    @Override
    public Map<String, String> getHeaderMap() {

        return getMapFromEnums(new EnumGettersForRoMap<String, String>() {
            @Override
            public String get(String key) {
                return request.getHeader(key);
            }

            @Override
            public Enumeration<String> getKeys() {
                return request.getHeaderNames();
            }
        });
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
    public Map<String, String[]> getParameterMap() {
        return request.getParameterMap();
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
    public Map<String, String> getAtrributesMap() {
        return getMapFromEnums(new EnumGettersForRoMap<String, String>() {
            @Override
            public String get(String key) {
                return request.getParameter(key);
            }

            @Override
            public Enumeration<String> getKeys() {
                return request.getParameterNames();
            }
        });
    }

    @Override
    public SendPort<HttpResponse> sender() {
        try {
            final PrintWriter writer = response.getWriter();
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
                    if (!request.isAsyncStarted()) 
                        throw new RuntimeException("Request is already commited, cannot send again");
                    writer.print(message.getString());
                    response.setStatus(message.getStatus());
                    if (message.getCookies() != null) {
                        for (Cookie wc : message.getCookies())
                            response.addCookie(getServletCookie(wc));
                    }
                    if (message.getHeaders() != null) {
                        for (Map.Entry<String, String> h : message.getHeaders())
                            response.addHeader(h.getKey(), h.getValue());
                    }
                    if (message.getError() != null) {
                        try {
                            response.sendError(message.getStatus(), message.getError().toString());
                        } catch (IOException ex) {
                            Logger.getLogger(ServletHttpMessage.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                    if (message.getRedirectPath() != null) {
                        try {
                            response.sendRedirect(message.getRedirectPath());
                        } catch (IOException ex) {
                            Logger.getLogger(ServletHttpMessage.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                    writer.close();
                    request.getAsyncContext().complete();
                    return true;
                }

                @Override
                public void close() {
                }
            };
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }


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
