/*
 * COMSAT
 * Copyright (C) 2014, Parallel Universe Software Co. All rights reserved.
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
package co.paralleluniverse.embedded.containers;

import javax.servlet.Servlet;
import javax.servlet.ServletContextListener;


public interface EmbeddedServer {
    EmbeddedServer setPort(int port);
    EmbeddedServer setNumThreads(int nThreads);
    EmbeddedServer setMaxConnections(int maxConn);
    ServletDesc addServlet(String name, Class<? extends Servlet> servletClass, String mapping);
    void addServletContextListener(Class <? extends ServletContextListener> scl);
    
    void start() throws Exception;
    void stop() throws Exception;
    
    interface ServletDesc {
        ServletDesc setInitParameter(String name, String value);
        ServletDesc setLoadOnStartup(int load);
    }
}
