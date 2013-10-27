/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

import co.paralleluniverse.fibers.servlet.FiberHttpServlet;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.servlet.AsyncContext;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

/**
 *
 * @author eitan
 */
public class SimplestServer {
    private final static ExecutorService exec = Executors.newCachedThreadPool();

    public static void main(String[] args) throws Exception {
        final Server server = new Server(8080);

        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
//        context.setContextPath("/");
        server.setHandler(context);
//        context.addServlet(new ServletHolder(new JdbcServlet()), "/root");
        context.addServlet(new ServletHolder(new JdbcServlet()), "/jdbc");
        context.addServlet(new ServletHolder(new HttpServlet() {
            @Override
            protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
                exec.execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            server.stop();
                        } catch (Exception ex) {
                            System.out.println(ex.toString());
                        }
                    }
                });
            }
        }), "/shutdown");
        context.addServlet(new ServletHolder(new HttpServlet() {
            @Override
            protected void doGet(HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
                resp.setContentType("text/html");
                resp.getWriter().println("hi from asynkA<br/>");
                req.startAsync();
                System.out.println("ac");
                req.getAsyncContext().addListener(new AsyncListener() {
                    @Override
                    public void onComplete(AsyncEvent event) throws IOException {
                        resp.getWriter().println("dddd  ac completed: " + event + ".<br/>");
//                        for (StackTraceElement ste : Thread.getAllStackTraces().get(Thread.currentThread())) {
//                            resp.getWriter().println("ste: " + ste + ".<br/>");                        
//                        }
                    }

                    @Override
                    public void onTimeout(AsyncEvent event) throws IOException {
                        throw new UnsupportedOperationException("Not supported yet.");
                    }

                    @Override
                    public void onError(AsyncEvent event) throws IOException {
                        throw new UnsupportedOperationException("Not supported yet.");
                    }

                    @Override
                    public void onStartAsync(AsyncEvent event) throws IOException {
                        event.getAsyncContext().addListener(this);
                        resp.getWriter().println("ac started: " + event + ".<br/>");
                    }
                });
                resp.getWriter().println("<B>bef:"+req.isAsyncStarted()+"</B><br/>");
                req.getAsyncContext().dispatch("/asyncB");
                resp.getWriter().println("<B>aft:"+req.isAsyncStarted()+"</B><br/>");
                
                resp.getWriter().println("bye from asyncA<br/>");
                resp.getWriter().println("<a href=/shutdown>shutdown</a>");
            }
        }), "/asyncA");
        context.addServlet(new ServletHolder(new javax.servlet.http.HttpServlet() {
            @Override
            protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
                AsyncContext ac = req.startAsync();
                resp.getWriter().println("hi from asyncB");
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ex) {
                }
                resp.getWriter().println("bye from asybcB");
                ac.complete();
            }
        }), "/asyncB");
        context.addServlet(new ServletHolder(new HttpServlet() {
            @Override
            protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
                resp.getWriter().println("hi from servletA");
//                req.getRequestDispatcher("/servletB").include(req, resp);
                resp.getWriter().println("bye from servletA");
            }
        }), "/servletA");
        context.addServlet(new ServletHolder(new HttpServlet() {
            @Override
            protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
                final AsyncContext ac = req.startAsync();
//                final RequestDispatcher rd = req.getRequestDispatcher("servletA");
                final ServletContext sc = getServletContext();
//                RequestDispatcher requestDispatcher = sc.getRequestDispatcher("/servletA");
                //              assert requestDispatcher!=null;
//                ac.start(null); 
                exec.execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(50);
                            resp.getWriter().println("hi from servletBkdkdkd");

                            sc.getRequestDispatcher("/servletA").forward(req, resp);
                            ac.dispatch("/servletA");
//                            resp.reset();    
//                            ac.getRequest().getRequestDispatcher("/servletA").include(ac.getRequest(), ac.getResponse());
//                            ac.dispatch("/servletA");
                            resp.getWriter().println("bye from servletB");
                        } catch (InterruptedException | ServletException | IOException ex) {
                            System.out.println(ex.toString());
                            throw new RuntimeException(ex);
                        } finally {
                            //ac.complete();
                        }
                    }
                });
            }
        }), "/servletB");
//        context.addServlet(new ServletHolder(new HttpServlet() {
        context.addServlet(new ServletHolder(new FiberHttpServlet() {
            @Override
            protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
                resp.getWriter().println("hi from servletA");
//                resp = new CharResponseWrapper(resp); // uncomment this to check exception on wrapped response
                getServletConfig().getServletContext().getRequestDispatcher("/fiberB").forward(req, resp);
//                getServletContext().getRequestDispatcher("/fiberB").forward(req, resp);
//                req.getRequestDispatcher("fiberB").forward(req, resp);
//                req.getRequestDispatcher("/fiberB").forward(req, resp);
                
            }
        }), "/fiberA");
        context.addServlet(new ServletHolder(new FiberHttpServlet() {
            @Override
            protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
                resp.getWriter().println("hi from servletB");
            }
        }), "/fiberB");

        server.start();
        server.join();
        System.exit(0);
    }

    static public class CharResponseWrapper extends HttpServletResponseWrapper {
        private CharArrayWriter output;
        private PrintWriter printWriter;

        public String toString() {
            return output.toString();
        }

        public CharResponseWrapper(HttpServletResponse response) {
            super(response);
            output = new CharArrayWriter();
            printWriter = new PrintWriter(output);
        }

        public PrintWriter getWriter() {
            return printWriter;
        }
    }
}