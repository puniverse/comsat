package co.paralleluniverse.examples.test;

import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.servlet.FiberHttpServlet;
import co.paralleluniverse.strands.Strand;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

@WebServlet(urlPatterns = "/")
public class MyFiberServlet extends FiberHttpServlet {
    final static DataSource ds = lookupDataSourceJDBC("jdbc/fiberds");

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException, SuspendExecution {
        try (PrintWriter out = resp.getWriter()) {
            Strand.sleep(100);
            try (Connection connection = ds.getConnection()) {
                out.print("conn " + connection);
            }
        } catch (InterruptedException ex) {
        } catch (SQLException ex) {
            Logger.getLogger(MyFiberServlet.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static DataSource lookupDataSourceJDBC(final String name) {
        try {
            Context envCtx = (Context) new InitialContext().lookup("java:comp/env");
            return (DataSource) envCtx.lookup(name);
        } catch (NamingException ex) {
            throw new RuntimeException(ex);
        }
    }
}
