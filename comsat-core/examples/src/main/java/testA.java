
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author eitan
 */
public class testA {
    public static final String BASE_URI = "http://localhost:8080/";

    public static void main(String[] args) throws Exception {

        Server server = new Server(8080);
        server.setHandler(new AbstractHandler() {
            @Override
            public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
//                response.setContentType("text/html;charset=utf-8");
  //              response.setStatus(HttpServletResponse.SC_OK);
                baseRequest.setHandled(true);
                response.getWriter().println("<h1>Hello World</h1>");
            }
        });


//        ServletContextHandler root = new ServletContextHandler(server, "/", ServletContextHandler.SESSIONS);
//        final ServletHolder sh = new ServletHolder(ServletContainer.class);
//
//        sh.setInitParameter("com.sun.jersey.config.property.packages", "com.example");
//        root.addServlet(sh, "/*");
        server.start();

        System.out.println("Jersey app started. Hit enter to stop it...");
        System.in.read();

        server.stop();
    }
}
