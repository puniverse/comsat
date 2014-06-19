package co.paralleluniverse.examples.test;

import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.servlet.FiberHttpServlet;
import co.paralleluniverse.strands.Strand;
import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(urlPatterns = "/")
public class MyFiberServlet extends FiberHttpServlet {
//    final static Client httpClient = AsyncClientBuilder.newClient();
//    final static DataSource ds = BlockingCallsExample.lookupDataSourceJDBC();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException, SuspendExecution {
        try (PrintWriter out = resp.getWriter()) {
            Strand.sleep(100);
            out.print("testGet");
//            out.println(BlockingCallsExample.doSleep());
//            out.println(BlockingCallsExample.callSomeRS(httpClient));
//            out.println(BlockingCallsExample.executeSomeSql(ds));
        } catch (InterruptedException ex) {
        }
    }
}
