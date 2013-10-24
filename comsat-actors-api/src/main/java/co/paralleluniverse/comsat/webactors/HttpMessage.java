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
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Map;

public interface HttpMessage extends WebMessage {
    ////
    public Map<String, String[]> getParameterMap();

    public Map<String, String> getHeaderMap();

    public Map<String, String> getAtrributesMap();

    public Collection<Cookie> getCookies();

    public String getMethod();

    public long getDateHeader(String name);

    public String getPathInfo();

    public String getContextPath();

    public String getQueryString();

    public String getRequestURI();

    public String getRequestURL();

    public String getServletPath();

    public int getContentLength();
    
    public String getBody();
    
    public ByteBuffer getBinaryBody();

    public String getContentType();

    public String getServerName();

    public int getServerPort();

    //////////////////
    /// response methods
    @Override
    public SendPort<HttpResponse> sender();
}
