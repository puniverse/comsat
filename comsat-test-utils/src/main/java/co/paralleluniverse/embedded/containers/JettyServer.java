/*
 * COMSAT
 * Copyright (C) 2014-2016, Parallel Universe Software Co. All rights reserved.
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
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.websocket.jsr356.server.deploy.WebSocketServerContainerInitializer;

public final class JettyServer extends AbstractEmbeddedServer {
    private Server server;
    private ServletContextHandler context;
    private boolean wsEnabled;

    private void build() {
        if (server != null)
            return;
        this.server = new Server(new QueuedThreadPool(nThreads, nThreads));
        final ServerConnector http = new ServerConnector(server);
        http.setPort(port);
        http.setAcceptQueueSize(maxConn);
        server.addConnector(http);
        this.context = new ServletContextHandler(ServletContextHandler.SESSIONS);
    }

    @Override
    public final ServletDesc addServlet(String name, Class<? extends Servlet> servletClass, String mapping) {
        if (context == null)
            build();
        ServletHolder sh = new ServletHolder(servletClass);
        context.addServlet(sh, mapping);
        return new JettyServletDesc(sh);
    }

    @Override
    public final void addServletContextListener(Class<? extends ServletContextListener> scl) {
        if (context == null)
            build();
        try {
            context.addEventListener(scl.newInstance());
        } catch (InstantiationException | IllegalAccessException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public final void start() throws Exception {
        if (context==null)
            build();
        server.setHandler(context);
        if (wsEnabled)
            WebSocketServerContainerInitializer.configureContext(context);
        server.start();
    }

    @Override
    public final void stop() throws Exception {
        server.stop();
    }

    @Override
    public final void enableWebsockets() throws Exception {
        this.wsEnabled = true;
    }

    @Override
    public final void setResourceBase(final String resourceBaseUrl) {
        if (context==null)
            build();
        context.setResourceBase(resourceBaseUrl);
    }

    private static final class JettyServletDesc implements ServletDesc {
        private final ServletHolder impl;

        public JettyServletDesc(ServletHolder sh) {
            this.impl = sh;
        }

        @Override
        public final ServletDesc setInitParameter(String name, String value) {
            impl.setInitParameter(name, value);
            return this;
        }

        @Override
        public final ServletDesc setLoadOnStartup(int load) {
            impl.setInitOrder(load);
            return this;
        }
    }
}
