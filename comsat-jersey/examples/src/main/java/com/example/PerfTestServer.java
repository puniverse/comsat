/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.example;

//import co.paralleluniverse.fibers.servlet.HttpServlet;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.servlet.FiberHttpServlet;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Path;
import org.apache.catalina.Context;
import org.apache.catalina.Wrapper;
import org.apache.catalina.startup.Tomcat;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

/**
 *
 * @author eitan
 */
@Path("newresource")
public class PerfTestServer {
    public static final String JERSEY_PROVIDER_PACKAGES = "jersey.config.server.provider.packages";
    public static final String COM_EXAMPLE = "com.example";
    private final ServletServer server;
    private final NewResource newResource = new NewResource();

    public static void main(String[] args) throws Exception {
//        new PerfTestServer(new JettyServer()).start();
        new PerfTestServer(new TomcatServer()).start();
    }

    public PerfTestServer(ServletServer server) {
        this.server = server;
    }

    public void start() throws Exception {
        registerServletsOn(server);
        server.start();
        System.out.println("Jersey app started. Hit enter to stop it...");
        System.in.read();
        server.stop();
    }

    private void registerServletsOn(ServletServer root) {
        root.addServlet(new FiberHttpServlet() {
            @Override
            protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException, SuspendExecution {
                doWork(req, resp);
            }
        }, "/fiber/*",null);
        root.addServlet(new HttpServlet() {
            @Override
            protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
                try {
                    doWork(req, resp);
                } catch (SuspendExecution ex) {
                    throw new AssertionError(ex);
                }
            }
        }, "/sync/*",null);

        root.addServlet(new co.paralleluniverse.fibers.jersey.ServletContainer(), "/jersey/fiber/*",COM_EXAMPLE);
        root.addServlet(new org.glassfish.jersey.servlet.ServletContainer(), "/jersey/sync/*", COM_EXAMPLE);
    }

    void doWork(HttpServletRequest req, HttpServletResponse resp) throws IOException, SuspendExecution {
        String s;
        try {
            int sleepTime = Integer.parseInt(req.getParameter("sleep"));
            s = newResource.doWork(sleepTime);
        } catch (NumberFormatException ex) {
            s = "Can't find 'sleep' parameter";
        }
        resp.getWriter().println(s);
    }

    public interface ServletServer {
        public void addServlet(Servlet servlet, String path, String providerPackages);

        void start() throws Exception;

        void stop() throws Exception;
    }

    static class JettyServer implements ServletServer {
        Server jetty;
        ServletContextHandler context;

        public JettyServer() {
            jetty = new Server(8080);
            context = new ServletContextHandler(jetty, "/", ServletContextHandler.SESSIONS);
        }

        @Override
        public void addServlet(Servlet servlet, String path, String providerPackages) {
            final ServletHolder sh = new ServletHolder(servlet);
            if (providerPackages != null)
                sh.setInitParameter(JERSEY_PROVIDER_PACKAGES, providerPackages);
            context.addServlet(sh, path);
        }

        @Override
        public void start() throws Exception {
            jetty.start();
        }

        @Override
        public void stop() throws Exception {
            jetty.stop();
        }
    }

    static class TomcatServer implements ServletServer {
        private final Tomcat tomcat;
        private final Context appContext;

        public TomcatServer() {
            tomcat = new Tomcat();
            File baseDir = new File("tomcat");
            tomcat.setBaseDir(baseDir.getAbsolutePath());

            File applicationDir = new File(baseDir + "/webapps", "/ROOT");
            if (!applicationDir.exists()) {
                applicationDir.mkdirs();
            }
            try {
                appContext = tomcat.addWebapp("/", "ROOT");
            } catch (ServletException ex) {
                throw new RuntimeException(ex);
            }
        }

        @Override
        public void addServlet(Servlet servlet, String path, String providerPackages) {
            Wrapper wrapper = Tomcat.addServlet(appContext, path, servlet);
            if (providerPackages != null)
                wrapper.addInitParameter(JERSEY_PROVIDER_PACKAGES, providerPackages);
            appContext.addServletMapping(path, path);
        }

        @Override
        public void start() throws Exception {
            tomcat.start();
        }

        @Override
        public void stop() throws Exception {
            tomcat.stop();
        }
    }
}
