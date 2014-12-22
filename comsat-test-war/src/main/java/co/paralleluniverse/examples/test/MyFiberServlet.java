package co.paralleluniverse.examples.test;

import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.Suspendable;
import co.paralleluniverse.fibers.servlet.FiberHttpServlet;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

@WebServlet(urlPatterns = "/", asyncSupported = true)
public class MyFiberServlet extends FiberHttpServlet {
    final static DataSource ds = lookupDataSourceJDBC("jdbc/fiberds");

    @Suspendable
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try (PrintWriter out = resp.getWriter(); Connection connection = ds.getConnection()) {
            Fiber.sleep(10);
            out.print(connection);
        } catch (InterruptedException | SuspendExecution | SQLException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static DataSource lookupDataSourceJDBC(final String name) {
        try {
            Context envCtx = (Context) new InitialContext().lookup("java:comp/env");
            return (DataSource) envCtx.lookup(name);
        } catch (NamingException ex) {
            return null;
        }
    }
}
