/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.example;

//import co.paralleluniverse.fibers.servlet.HttpServlet;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

/**
 *
 * @author eitan
 */
public class JettyMain {
    public static final String BASE_URI = "http://localhost:8080/";

    public static void main(String[] args) throws Exception {
        System.setProperty("co.paralleluniverse.debugMode", "true");
        System.setProperty("co.paralleluniverse.globalFlightRecorder", "true");
        System.setProperty("co.paralleluniverse.flightRecorderDumpFile", "~/jersey.log");
        System.setProperty("co.paralleluniverse.monitoring.flightRecorderSize", "200000");
        Server server = new Server(8080);
        ServletContextHandler root = new ServletContextHandler(server, "/", ServletContextHandler.SESSIONS);

//        final ServletHolder sh = new ServletHolder(ServletContainer.class);
//        final ServletHolder sh = new ServletHolder(co.paralleluniverse.fibers.jersey.ServletContainer.class);
        final ServletHolder sh = new ServletHolder(new co.paralleluniverse.fibers.jersey.ServletContainer());
        sh.setInitParameter("jersey.config.server.provider.packages", "com.example");
        root.addServlet(sh, "/*");
        server.start();

        System.out.println("Jersey app started. Hit enter to stop it...");
        System.in.read();

        server.stop();
    }
}
