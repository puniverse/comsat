//
//import co.paralleluniverse.fibers.SuspendExecution;
//import co.paralleluniverse.fibers.servlet.FiberHttpServlet;
//import co.paralleluniverse.strands.Strand;
//import java.io.IOException;
//import java.util.Date;
//import javax.servlet.ServletException;
//import javax.servlet.http.HttpServlet;
//import javax.servlet.http.HttpServletRequest;
//import javax.servlet.http.HttpServletResponse;
//
///*
// * To change this template, choose Tools | Templates
// * and open the template in the editor.
// */
//public class HelloServlet extends FiberHttpServlet {
//    private String greeting = "Hello World";
//
//    public HelloServlet() {
//    }
//
//    public HelloServlet(String greeting) {
//        this.greeting = greeting;
//    }
//
//    @Override
//    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException, SuspendExecution {
//        try {
//            Strand.sleep(1000);
//        } catch (InterruptedException ex) {
//        }
//        response.setContentType("text/html");
//        response.setStatus(HttpServletResponse.SC_OK);
//        response.getWriter().println("<h1>" + greeting + "</h1>");
//        response.getWriter().println("session=" + request.getSession(true).getId());
//        response.getWriter().println("<h1>finished computing on" + new Date() + "</h1>");
//    }
//}