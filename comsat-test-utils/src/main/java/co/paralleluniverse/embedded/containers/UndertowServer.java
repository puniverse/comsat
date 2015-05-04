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

import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.RequestLimit;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.*;
import javax.servlet.Servlet;
import javax.servlet.ServletContextListener;

public class UndertowServer extends AbstractEmbeddedServer {
    private static final String ANY_LOCAL_ADDRESS = "0.0.0.0"; // not "localhost"!
    private DeploymentInfo deployment;
    private Undertow server;

    private void build() {
        if (deployment != null)
            return;
        this.deployment = Servlets.deployment().setDeploymentName("")
                .setClassLoader(ClassLoader.getSystemClassLoader())
                .setContextPath("/");
    }

    @Override
    public ServletDesc addServlet(String name, Class<? extends Servlet> servletClass, String mapping) {
        build();
        ServletInfo info = Servlets.servlet(name, servletClass).addMapping(mapping).setAsyncSupported(true);
        deployment.addServlet(info);
        return new UndertowServletDesc(info);
    }

    @Override
    public void start() throws Exception {
        DeploymentManager servletsContainer = Servlets.defaultContainer().addDeployment(deployment);
        servletsContainer.deploy();
        HttpHandler handler = servletsContainer.start();
        handler = Handlers.requestLimitingHandler(new RequestLimit(maxConn), handler);
        this.server = Undertow.builder()
                .setHandler(handler)
                .setIoThreads(nThreads)
                .addHttpListener(port, ANY_LOCAL_ADDRESS)
                .build();
        new Thread(new Runnable() {
            @Override
            public void run() {
                server.start();
            }
        }).start();
        waitUrlAvailable("http://localhost:" + port);
    }

    @Override
    public void stop() throws Exception {
        if (server != null)
            server.stop();
    }

    @Override
    public void addServletContextListener(Class<? extends ServletContextListener> scl) {
        // TODO
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void enableWebsockets() throws Exception {
        // TODO
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setResourceBase(String resourceBaseUrl) {
        // TODO
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private static class UndertowServletDesc implements ServletDesc {
        private final ServletInfo impl;

        public UndertowServletDesc(ServletInfo info) {
            this.impl = info;
        }

        @Override
        public ServletDesc setInitParameter(String name, String value) {
            impl.addInitParam(name, value);
            return this;
        }

        @Override
        public ServletDesc setLoadOnStartup(int load) {
            impl.setLoadOnStartup(load);
            return this;
        }
    }
}
