package co.paralleluniverse.examples.test;

import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.Suspendable;
import co.paralleluniverse.fibers.servlet.FiberNewHttpServlet;
import co.paralleluniverse.strands.Strand;
import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(urlPatterns = "/test",asyncSupported = true)
public class TestServlet extends FiberNewHttpServlet{
    @Suspendable
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try (PrintWriter out = resp.getWriter()) {
            Strand.sleep(100);
            out.print("hello");
        } catch (SuspendExecution | InterruptedException ex) {
            throw new RuntimeException(ex);
        }
    }
}
