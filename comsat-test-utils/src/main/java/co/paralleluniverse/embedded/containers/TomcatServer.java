/*
 * COMSAT
 * Copyright (C) 2014-2015, Parallel Universe Software Co. All rights reserved.
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

import java.io.File;
import javax.servlet.Servlet;
import javax.servlet.ServletContextListener;
import org.apache.catalina.Context;
import org.apache.catalina.Wrapper;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.startup.Tomcat;
import org.apache.tomcat.websocket.server.WsSci;

public class TomcatServer extends AbstractEmbeddedServer {
    private final Tomcat tomcat;
    private Context context;

    public TomcatServer() {
        this.tomcat = new Tomcat();
        this.context = tomcat.addContext("/", new File("./build").getAbsolutePath());
    }

    @Override
    public ServletDesc addServlet(String name, Class<? extends Servlet> servletClass, String mapping) {
        Wrapper w = Tomcat.addServlet(context, name, servletClass.getName());
        w.addMapping(mapping);
        return new TomcatServletDesc(w);
    }

    @Override
    public void start() throws Exception {
        tomcat.setPort(port);
        tomcat.getConnector().setAttribute("maxThreads", nThreads);
        tomcat.getConnector().setAttribute("acceptCount", maxConn);
        tomcat.start();
    }

    @Override
    public void stop() throws Exception {
        tomcat.stop();
        tomcat.getConnector().destroy();
        tomcat.destroy();
    }

    @Override
    public void enableWebsockets() throws Exception {
        context.addServletContainerInitializer(new WsSci(), null);
    }

    @Override
    public void addServletContextListener(Class<? extends ServletContextListener> scl) {
        StandardContext tomcatCtx = (StandardContext) this.context;
        tomcatCtx.addApplicationListener(scl.getName());
    }

    @Override
    public void setResourceBase(String resourceBaseUrl) {
        // TODO
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private static class TomcatServletDesc implements ServletDesc {
        private final Wrapper impl;

        public TomcatServletDesc(Wrapper w) {
            this.impl = w;
        }

        @Override
        public ServletDesc setInitParameter(String name, String value) {
            impl.addInitParameter(name, value);
            return this;
        }

        @Override
        public ServletDesc setLoadOnStartup(int load) {
            impl.setLoadOnStartup(load);
            return this;
        }
    }
}
