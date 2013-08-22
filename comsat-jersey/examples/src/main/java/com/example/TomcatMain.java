/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.example;

//import co.paralleluniverse.fibers.servlet.HttpServlet;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.servlet.FiberHttpServlet;
import co.paralleluniverse.strands.Strand;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.catalina.Context;
import org.apache.catalina.startup.Tomcat;

/**
 *
 * @author eitan
 */
public class TomcatMain {
    public static final String BASE_URI = "http://localhost:8080/";
//    private final static ExecutorService exec = Executors.newFixedThreadPool(50);
    private static final int SLEEP = 100;

    public static void main(String[] args) throws Exception {
//        System.setProperty("co.paralleluniverse.debugMode", "true");
//        System.setProperty("co.paralleluniverse.globalFlightRecorder", "true");
//        System.setProperty("co.paralleluniverse.flightRecorderDumpFile", "~/jersey.log");
//        System.setProperty("co.paralleluniverse.monitoring.flightRecorderSize", "200000");
        Tomcat tomcat = new Tomcat();
        File baseDir = new File("tomcat");
        tomcat.setBaseDir(baseDir.getAbsolutePath());

        File applicationDir = new File(baseDir + "/webapps", "/ROOT");
        if (!applicationDir.exists()) {
            applicationDir.mkdirs();
        }

        try {
            Context appContext = tomcat.addWebapp("/", "ROOT");

            // A Jetty AbstractHandler is an HttpServlet here:
            Tomcat.addServlet(appContext, "helloWorldServlet", new FiberHttpServlet() {
                @Override
                protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException, SuspendExecution {
                    try {
                        Strand.sleep(SLEEP);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(TomcatMain.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    resp.getWriter().println("hi from servletB");
                }
            });
            appContext.addServletMapping("/helloworld", "helloWorldServlet");

            Tomcat.addServlet(appContext, "helloWorldServletSync", new HttpServlet() {
                protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
                    try {
                        Strand.sleep(SLEEP);
                    } catch (SuspendExecution|InterruptedException ex) {
                        Logger.getLogger(TomcatMain.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    resp.getWriter().println("hi from servletB");
                }
            });
            appContext.addServletMapping("/helloworldsync", "helloWorldServletSync");

            tomcat.start();
            System.out.println("Tomcat server: http://" + tomcat.getHost().getName() + ":" + 8080 + "/");
            System.in.read();

            tomcat.stop();
//            tomcat.getServer().await();
        } catch (ServletException e) {
            e.printStackTrace();
        }


//        System.out.println("Jersey app started. Hit enter to stop it...");
    }
}
