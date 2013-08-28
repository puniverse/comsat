/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.example;

//import co.paralleluniverse.fibers.servlet.HttpServlet;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.servlet.FiberHttpServlet;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.AsyncContext;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Path;
import org.apache.catalina.Context;
import org.apache.catalina.Wrapper;
import org.apache.catalina.startup.Tomcat;
import org.eclipse.jetty.io.Connection;
import org.eclipse.jetty.io.EndPoint;
import org.eclipse.jetty.server.ConnectionFactory;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

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
        new PerfTestServer(new JettyServer()).start();
//        new PerfTestServer(new TomcatServer()).start();
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
        }, "/fiber/*", null);
        root.addServlet(new HttpServlet() {
            @Override
            protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
                try {
                    doWork(req, resp);
                } catch (SuspendExecution ex) {
                    throw new AssertionError(ex);
                }
            }
        }, "/sync/*", null);
//        final ExecutorService exec = Executors.newFixedThreadPool(50);
        final ScheduledExecutorService exec = Executors.newScheduledThreadPool(20, new ThreadFactoryBuilder().setDaemon(true).build());
        root.addServlet(new HttpServlet() {
            @Override
            protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
                req.setAttribute("org.apache.catalina.ASYNC_SUPPORTED", true);
                final AsyncContext startAsync = req.startAsync();
                String s;
                try {
                    final int sleepTime = Integer.parseInt(req.getParameter("sleep"));
                    exec.schedule(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                resp.getWriter().println("sleeped " + sleepTime);
                            } catch (IOException ex) {
                                Logger.getLogger(PerfTestServer.class.getName()).log(Level.SEVERE, null, ex);
                            }
                            startAsync.complete();
                        }
                    }, sleepTime, TimeUnit.MILLISECONDS);
                } catch (NumberFormatException ex) {
                    resp.getWriter().println("no sleep param");
                }


//                new Fiber<Void>(new SuspendableRunnable() {
//                    @Override
//                    public void run() throws SuspendExecution {
//                        try {
//                            doWork(req, resp);
//                        } catch (IOException ex) {
////                        } catch (InterruptedException ex) {
//                            throw new AssertionError(ex);
//                        } finally {
//                            startAsync.complete();
//                        }
//                    }
//                }).start();
            }
        }, "/async/*", null);

        root.addServlet(new co.paralleluniverse.fibers.jersey.ServletContainer(), "/jersey/fiber/*", COM_EXAMPLE);
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
            jetty = new Server();
            ServerConnector asyncConn = new ServerConnector(jetty, new QueuedThreadPool(30), null, null, 2, 0, new HttpConnectionFactory());
            asyncConn.setPort(8080);
            asyncConn.setAcceptQueueSize(100000);            
            jetty.addConnector(asyncConn);

            ServerConnector syncConn = new ServerConnector(jetty, new QueuedThreadPool(200), null, null, 2, 0, new HttpConnectionFactory());
            syncConn.setPort(8081);
            syncConn.setAcceptQueueSize(100000);            
            jetty.addConnector(syncConn);
            
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
            tomcat.getConnector().setAttribute("maxThreads", 50);
            tomcat.getConnector().setAttribute("acceptCount", 10000);
            tomcat.getConnector().setAttribute("acceptorThreadCount", 2);
            //acceptorThreadCount
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
