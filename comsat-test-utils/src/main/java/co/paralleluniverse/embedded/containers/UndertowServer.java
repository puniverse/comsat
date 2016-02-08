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

import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.DefaultByteBufferPool;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.RequestLimit;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.*;
import io.undertow.servlet.core.CompositeThreadSetupAction;
import io.undertow.servlet.util.ConstructorInstanceFactory;
import io.undertow.websockets.jsr.JsrWebSocketFilter;
import io.undertow.websockets.jsr.ServerWebSocketContainer;
import java.util.Collections;
import javax.servlet.DispatcherType;
import javax.servlet.Servlet;
import javax.servlet.ServletContextListener;
import org.xnio.OptionMap;
import org.xnio.Options;
import org.xnio.Xnio;
import org.xnio.XnioWorker;

public final class UndertowServer extends AbstractEmbeddedServer {
    private static final String ANY_LOCAL_ADDRESS = "0.0.0.0"; // not "localhost"!

    private DeploymentInfo deployment;
    private Undertow server;

    private void build() {
        if (deployment != null)
            return;

        this.deployment =
            Servlets.deployment().setDeploymentName("")
                .setClassLoader(ClassLoader.getSystemClassLoader())
                .setContextPath("/");
    }

    @Override
    public final ServletDesc addServlet(String name, Class<? extends Servlet> servletClass, String mapping) {
        build();
        final ServletInfo info = Servlets.servlet(name, servletClass).addMapping(mapping).setAsyncSupported(true);
        deployment.addServlet(info);
        return new UndertowServletDesc(info);
    }

    @Override
    public final void start() throws Exception {
        final DeploymentManager servletsContainer = Servlets.defaultContainer().addDeployment(deployment);
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
            public final void run() {
                server.start();
            }
        }).start();
        waitUrlAvailable("http://localhost:" + port);
    }

    @Override
    public final void stop() throws Exception {
        if (server != null)
            server.stop();
    }

    @Override
    public final void addServletContextListener(Class<? extends ServletContextListener> scl) {
        build();
        final ListenerInfo li = Servlets.listener(scl);
        deployment.addListener(li);
    }

    @Override
    public final void enableWebsockets() throws Exception {
        final Xnio xnio = Xnio.getInstance("nio", this.getClass().getClassLoader());
        final XnioWorker worker = xnio.createWorker(OptionMap.builder()
                        .set(Options.WORKER_IO_THREADS, 8)
                        .set(Options.CONNECTION_HIGH_WATER, 1000000)
                        .set(Options.CONNECTION_LOW_WATER, 1000000)
                        .set(Options.WORKER_TASK_CORE_THREADS, 30)
                        .set(Options.WORKER_TASK_MAX_THREADS, 30)
                        .set(Options.TCP_NODELAY, true)
                        .set(Options.CORK, true)
                        .getMap());
        final ClassIntrospecter ci = new ClassIntrospecter() {
            @Override
            public final <T> InstanceFactory<T> createInstanceFactory(final Class<T> clazz) {
                try {
                    return new ConstructorInstanceFactory<>(clazz.getDeclaredConstructor());
                } catch (NoSuchMethodException e) {
                    throw new RuntimeException(e);
                }
            }
        };
        //noinspection unchecked
        final ServerWebSocketContainer wsc =
            new ServerWebSocketContainer (
                ci, worker, new DefaultByteBufferPool(true, 100),
                new CompositeThreadSetupAction(Collections.EMPTY_LIST), false, false
            );
        final FilterInfo fi = new FilterInfo("filter", JsrWebSocketFilter.class);
        fi.setAsyncSupported(true);
        deployment
            .addFilter(fi)
            .addFilterUrlMapping("filter", "/*", DispatcherType.REQUEST)
            .addServletContextAttribute(javax.websocket.server.ServerContainer.class.getName(), wsc);
    }

    @Override
    public final void setResourceBase(String resourceBaseUrl) {
        // TODO
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private static final class UndertowServletDesc implements ServletDesc {
        private final ServletInfo impl;

        public UndertowServletDesc(ServletInfo info) {
            this.impl = info;
        }

        @Override
        public final ServletDesc setInitParameter(String name, String value) {
            impl.addInitParam(name, value);
            return this;
        }

        @Override
        public final ServletDesc setLoadOnStartup(int load) {
            impl.setLoadOnStartup(load);
            return this;
        }
    }
}
