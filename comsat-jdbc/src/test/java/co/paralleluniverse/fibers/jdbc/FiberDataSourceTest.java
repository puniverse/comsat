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
import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import javax.sql.DataSource;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class FiberDataSourceTest {

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
            {H2JdbcDatasource.class},});
    }
    private final Class<? extends DataSource> cls;
    private DataSource ds;

    public FiberDataSourceTest(Class<? extends DataSource> cls) {
        this.cls = cls;
    }

    @Before
    public void setUp() throws Exception {
        this.ds = cls.newInstance();
    }

    @Test
    public void testGetConnection() throws IOException, InterruptedException, Exception {
        final DataSource fiberDs = new FiberDataSource(ds);
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                try {
                    fiberDs.getConnection().close();
                } catch (SQLException ex) {
                    Assert.fail(ex.getMessage());
                }
            }
        }).start().join();
    }

    @Test
    public void testGetConnectionUsername() throws IOException, InterruptedException, Exception {
        final DataSource fiberDs = new FiberDataSource(ds);
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                try {
                    fiberDs.getConnection("", "").close();
                } catch (SQLException ex) {
                    Assert.fail(ex.getMessage());
                }
            }
        }).start().join();
    }
}
