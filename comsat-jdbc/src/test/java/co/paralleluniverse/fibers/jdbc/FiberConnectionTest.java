/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.fibers.jdbc;

import co.paralleluniverse.embedded.db.H2JdbcDatasource;
import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.SuspendableRunnable;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.Executors;
import javax.sql.DataSource;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class FiberConnectionTest {

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
            {H2JdbcDatasource.class},});
    }
    private final Class<? extends DataSource> cls;
    private Connection conn;

    public FiberConnectionTest(Class<? extends DataSource> cls) {
        this.cls = cls;
    }

    @Before
    public void setUp() throws Exception {
        final Connection connection = cls.newInstance().getConnection();
        this.conn = new FiberConnection(connection,
                MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(10, new ThreadFactoryBuilder().setNameFormat("jdbc-worker-%d").setDaemon(true).build())));
    }

    @After
    public void tearDown() throws Exception {
        conn.close();
    }

    @Test
    public void testCreateStatement() throws IOException, InterruptedException, Exception {
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                try {
                    conn.createStatement().close();
                } catch (SQLException ex) {
                    Assert.fail(ex.getMessage());
                }
            }
        }).start().join();
    }

    @Test
    public void testPrepareStatement() throws IOException, InterruptedException, Exception {
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                try {
                    conn.prepareStatement("create table tablename");
                } catch (SQLException ex) {
                    Assert.fail(ex.getMessage());
                }
            }
        }).start().join();
    }

    @Test
    public void testPrepareCall() throws IOException, InterruptedException, Exception {
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                try {
                    conn.prepareCall("create table tablename");
                } catch (SQLException ex) {
                    Assert.fail(ex.getMessage());
                }
            }
        }).start().join();
    }

    @Test
    public void testCommit() throws IOException, InterruptedException, Exception {
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                try {
                    conn.setAutoCommit(false);
                    conn.createStatement().execute("drop table if exists testCommit");
                    conn.createStatement().execute("create table testCommit (id int primary key, name varchar(100))");
                    conn.createStatement().execute("insert into testCommit (id, name) values (1, 'name')");
                    conn.commit();
                    Assert.assertTrue(conn.createStatement().executeQuery("select * from testCommit").next());
                    conn.createStatement().execute("drop table testCommit");
                } catch (SQLException ex) {
                    Assert.fail(ex.getMessage());
                }
            }
        }).start().join();
    }

    @Test
    public void testRollback() throws IOException, InterruptedException, Exception {
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                try {
                    conn.setAutoCommit(false);
                    conn.createStatement().execute("drop table if exists testRollback");
                    conn.createStatement().execute("create table testRollback (id int primary key, name varchar(100))");
                    conn.createStatement().execute("insert into testRollback (id, name) values (1, 'name')");
                    conn.rollback();
                    Assert.assertFalse(conn.createStatement().executeQuery("select * from testRollback").next());
                    conn.createStatement().execute("drop table testRollback");
                } catch (SQLException ex) {
                    Assert.fail(ex.getMessage());
                }
            }
        }).start().join();
    }
}
