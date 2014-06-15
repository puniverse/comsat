/*
 * COMSAT
 * Copyright (C) 2014, Parallel Universe Software Co. All rights reserved.
 *
 * This program and the accompanying materials are dual-licensed under
 * either the terms of the Eclipse Public License v1.0 as published by
 * the Eclipse Foundation
 *
 *   or (per the licensee's choosing)
 *
 * under the terms of the GNU Lesser General Public License version 3.0
 * as published by the Free Software Foundation.
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
import static org.junit.Assert.*;
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
    private final Class<? extends DataSource> dsCls;
    private DataSource ds;

    public FiberDataSourceTest(Class<? extends DataSource> cls) {
        this.dsCls = cls;
    }

    @Before
    public void setUp() throws Exception {
        this.ds = dsCls.newInstance();
    }

    @Test
    public void testGetConnection() throws IOException, InterruptedException, Exception {
        // snippet DataSource wrapping
        final DataSource fiberDs = FiberDataSource.wrap(ds);
        // end of snippet
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                try {
                    // snippet DataSource usage
                    fiberDs.getConnection().close();
                    // end of snippet
                } catch (SQLException ex) {
                    fail(ex.getMessage());
                }
            }
        }).start().join();
    }

    @Test
    public void testGetConnectionUsername() throws IOException, InterruptedException, Exception {
        final DataSource fiberDs = FiberDataSource.wrap(ds);
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                try {
                    fiberDs.getConnection("", "").close();
                } catch (SQLException ex) {
                    fail(ex.getMessage());
                }
            }
        }).start().join();
    }
}
