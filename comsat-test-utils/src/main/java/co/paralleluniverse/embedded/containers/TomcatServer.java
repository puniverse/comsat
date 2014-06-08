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

import java.io.File;
import javax.servlet.Servlet;
import org.apache.catalina.Context;
import org.apache.catalina.Wrapper;
import org.apache.catalina.startup.Tomcat;

public class TomcatServer extends AbstractEmbeddedServer {
    private final Tomcat tomcat;
    private Context context;

    public TomcatServer() {
        this.tomcat = new Tomcat();
    }

    @Override
    public ServletDesc addServlet(String name, Class<? extends Servlet> servletClass, String mapping) {
        if (context == null) {
            context = tomcat.addContext("/", new File("./build").getAbsolutePath());
            tomcat.setPort(port);
            tomcat.getConnector().setAttribute("maxThreads", nThreads);
            tomcat.getConnector().setAttribute("acceptCount", maxConn);
        }

        Wrapper w = Tomcat.addServlet(context, name, servletClass.getName());
        w.addMapping(mapping);
        return new TomcatServletDesc(w);
    }

    @Override
    public void start() throws Exception {
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                try {
//                } catch (LifecycleException ex) {
//                    throw new RuntimeException(ex);
//                }
//            }
//        }).start();
//        Thread.sleep(100);
                    tomcat.start();
    }

    @Override
    public void stop() throws Exception {
        tomcat.stop();
        tomcat.getConnector().destroy();
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
