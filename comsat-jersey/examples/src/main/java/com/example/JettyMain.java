/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.example;

//import co.paralleluniverse.fibers.servlet.HttpServlet;
import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.servlet.FiberHttpServlet;
import co.paralleluniverse.strands.Strand;
import co.paralleluniverse.strands.SuspendableRunnable;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.AsyncContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

/**
 *
 * @author eitan
 */
public class JettyMain {
    public static final String BASE_URI = "http://localhost:8080/";
//    private final static ExecutorService exec = Executors.newFixedThreadPool(50);
    private static final int SLEEP = 100;

    public static void main(String[] args) throws Exception {
//        System.setProperty("co.paralleluniverse.debugMode", "true");
//        System.setProperty("co.paralleluniverse.globalFlightRecorder", "true");
//        System.setProperty("co.paralleluniverse.flightRecorderDumpFile", "~/jersey.log");
//        System.setProperty("co.paralleluniverse.monitoring.flightRecorderSize", "200000");
        Server server = new Server(8080);
        ServletContextHandler root = new ServletContextHandler(server, "/", ServletContextHandler.SESSIONS);

//        final ServletHolder sh = new ServletHolder(ServletContainer.class);
//        final ServletHolder sh = new ServletHolder(co.paralleluniverse.fibers.jersey.ServletContainer.class);
        final ServletHolder sh = new ServletHolder(new co.paralleluniverse.fibers.jersey.ServletContainer());
        sh.setInitParameter("jersey.config.server.provider.packages", "com.example");
        root.addServlet(sh, "/*");
        root.addServlet(new ServletHolder(new FiberHttpServlet() {
            @Override
            protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException, SuspendExecution {
                try {
                    Strand.sleep(SLEEP);
                } catch (InterruptedException ex) {
                    Logger.getLogger(JettyMain.class.getName()).log(Level.SEVERE, null, ex);
                }
                resp.getWriter().println("hi from servletB");
            }
        }), "/fiber/*");
        root.addServlet(new ServletHolder(new HttpServlet() {
            @Override
            protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
                try {
                    Strand.sleep(SLEEP);
                } catch (SuspendExecution | InterruptedException ex) {
                    Logger.getLogger(JettyMain.class.getName()).log(Level.SEVERE, null, ex);
                }
                resp.getWriter().println("hi from servletB");
            }
        }), "/sync/*");
        root.addServlet(new ServletHolder(new HttpServlet() {
            @Override
            protected void doGet(HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
                final AsyncContext ac = req.startAsync();
                //                exec.execute
                new Fiber(new SuspendableRunnable() {
                    @Override
                    public void run() throws SuspendExecution {
                        try {
                            Strand.sleep(SLEEP);
                            resp.getWriter().println("hi from asyncB");
                        } catch (InterruptedException | IOException ex) {
                            Logger.getLogger(JettyMain.class.getName()).log(Level.SEVERE, null, ex);
                        } finally {
                            ac.complete();
                        }

                    }
                }).inheritThreadLocals().start();
            }
        }), "/async/*");
        server.start();

        System.out.println("Jersey app started. Hit enter to stop it...");
        System.in.read();

        server.stop();
    }
}
