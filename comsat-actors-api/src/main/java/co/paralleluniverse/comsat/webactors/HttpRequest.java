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

import co.paralleluniverse.strands.channels.SendPort;
import com.google.common.collect.Multimap;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Map;

public abstract class HttpRequest implements WebMessage {
    ////
    public abstract Multimap<String, String> getParameters();

    public abstract Multimap<String, String> getHeaders();

    public abstract Map<String, Object> getAtrributes();

    public Collection<String> getHeaders(String name) {
        return getHeaders().get(name);
    }

    public String getHeader(String name) {
        return first(getHeaders().get(name));
    }

    public Collection<String> getParametersValues(String name) {
        return getParameters().get(name);
    }

    public String getParameter(String name) {
        return first(getParameters().get(name));
    }

    public Object getAttribute(String name) {
        return getAtrributes().get(name);
    }
    
    private static <V> V first(Collection<V> c) {
        if (c == null || c.isEmpty())
            return null;
        return c.iterator().next();
    }
    
    public abstract Collection<Cookie> getCookies();

    public abstract String getMethod();

    public abstract long getDateHeader(String name);

    public abstract String getPathInfo();

    public abstract String getContextPath();

    public abstract String getQueryString();

    public abstract String getRequestURI();

    public abstract String getRequestURL();

    public abstract String getServletPath();

    public abstract int getContentLength();

    public abstract String getBody();

    public abstract ByteBuffer getBinaryBody();

    public abstract String getContentType();

    public abstract String getServerName();

    public abstract int getServerPort();

    //////////////////
    /// response methods
    @Override
    public abstract SendPort<HttpResponse> sender();
}
