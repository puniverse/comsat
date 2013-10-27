
import co.paralleluniverse.concurrent.util.ThreadUtil;
import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.jdbc.*;
import co.paralleluniverse.fibers.servlet.FiberHttpServlet;
import co.paralleluniverse.strands.SuspendableRunnable;
import com.mysql.jdbc.jdbc2.optional.MysqlConnectionPoolDataSource;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.concurrent.ExecutionException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

public class JdbcServlet extends FiberHttpServlet {
    private final DataSource ds;
    private long firstConnTime = 0;

    public JdbcServlet() throws ExecutionException, InterruptedException {
        final MysqlConnectionPoolDataSource mysqlDS = new MysqlConnectionPoolDataSource();
        mysqlDS.setServerName("localhost");
        this.ds = new AsyncDataSource(mysqlDS, 10);
        initDB();
    }

    private void initDB() throws ExecutionException, InterruptedException {
        new Fiber(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                long nanoTime = System.nanoTime();
                try (Connection conn = ds.getConnection("root", "root")) {
                    long nanoTime1 = System.nanoTime();
                    firstConnTime = nanoTime1 - nanoTime;
                    initTestDb(conn, false); // DROP OLD
                    initTestDb(conn, true); // CREATE NEW
                } catch (SQLException ex) {
                    throw new RuntimeException(ex);
                }
            }
        }).start().join();
    }

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        final PrintWriter out = new PrintWriter(System.out);
        final JdbcServlet myServlet = new JdbcServlet();
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                myServlet.dsJdbcPs(out);
            }
        }).start().join();
        System.out.println("finished");

        Thread.sleep(3000);
        ThreadUtil.dumpThreads();
    }

    private static void initTestDb(Connection conn, final boolean init) throws SQLException, SuspendExecution {
        try (final Statement stmt = conn.createStatement()) {
            if (init) {
                stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS PUNIVERSE");
                stmt.executeUpdate("CREATE TABLE IF NOT EXISTS PUNIVERSE.PERSONS"
                        + "(name CHAR(32), age INT(8), PRIMARY KEY (name))");
                StringBuilder sb = new StringBuilder("INSERT IGNORE INTO PUNIVERSE.PERSONS VALUES");
                for (int i = 0; i < 10000; i++) {
                    sb.append(i == 0 ? "" : ",").append("('name").append(i).append("',").append(i).append(")");
                }
                stmt.executeUpdate(sb.toString());
                String createProcedure = "CREATE PROCEDURE PUNIVERSE.getByAge("
                        + "IN p_age INT(8)"
                        //                        + ",o_name OUT PUNIVERSE.PERSONS.name%TYPE"
                        //                        + ",o_age OUT PUNIVERSE.PERSONS.age%TYPE"
                        + ")  BEGIN "
                        + "SELECT * "
                        //                        + "INTO o_name,o_age "
                        + "from PUNIVERSE.PERSONS WHERE age >= p_age; END;";
                stmt.executeUpdate(createProcedure);
            } else {
                stmt.executeUpdate("DROP DATABASE IF EXISTS PUNIVERSE");
            }
        }
    }

    private void dsJdbcPs(final PrintWriter out) throws RuntimeException, SuspendExecution {
        try {
            try (Connection conn = ds.getConnection("root", "root")) {
                out.println("time is " + new Date());
                long lt = System.nanoTime();
                try (final PreparedStatement stmt = conn.prepareStatement("select * from PUNIVERSE.PERSONS where age >= ?")) {
                    long ct = System.nanoTime();
                    out.println("ConnectionTime: " + (ct - lt) + " instead of " + firstConnTime);
                    lt = ct;
                    stmt.setInt(1, 5000);
                    ResultSet res = stmt.executeQuery();
                    ct = System.nanoTime();
                    out.println("execQueryTime: " + (ct - lt));
                    lt = ct;
                    int c = 0;
                    while (res.next()) {
                        c++;
                    }
                    out.println("count = " + c);
                    stmt.setInt(1, 7000);
                    res = stmt.executeQuery();
                    ct = System.nanoTime();
                    out.println("execQueryTime: " + (ct - lt));
                    lt = ct;
                    c = 0;
                    while (res.next()) {
                        c++;
                    }
                    out.println("count = " + c);
                    ct = System.nanoTime();
                    out.println("rsTime: " + (ct - lt));
                }
                try (final CallableStatement cs = conn.prepareCall("{call PUNIVERSE.getByAge(?)}")) {
                    cs.setInt(1, 35);
                    ResultSet res = cs.executeQuery();
                    int c = 0;
                    while (res.next()) {
                        c++;
                    }
                    out.println("result count from callable is " + c);


                }
                out.flush();

            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException, SuspendExecution {
        dsJdbcPs(resp.getWriter());
//        dsJdbc(resp.getWriter());
    }
}
