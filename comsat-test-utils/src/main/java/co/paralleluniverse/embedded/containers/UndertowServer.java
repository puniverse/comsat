package co.paralleluniverse.embedded.containers;

import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.RequestLimit;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.*;
import javax.servlet.Servlet;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.impl.client.HttpClients;

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
        for (;;) {
            Thread.sleep(10);
            try {
                if (HttpClients.createDefault().execute(new HttpGet("http://localhost:" + port)).getStatusLine().getStatusCode()>-100)
                    break;
            }catch(HttpHostConnectException ex) {          
            }
        }
    }

    @Override
    public void stop() throws Exception {
        if (server != null)
            server.stop();
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
