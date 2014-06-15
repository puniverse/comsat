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
package co.paralleluniverse.fibers.jdbi;

import co.paralleluniverse.embedded.db.H2JdbcDatasource;
import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.SuspendableRunnable;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import javax.sql.DataSource;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.IDBI;
import org.skife.jdbi.v2.util.StringMapper;

@RunWith(Parameterized.class)
public class FiberFluentAPITest {

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
            {H2JdbcDatasource.class},});
    }
    private final Class<? extends DataSource> cls;
    private IDBI jdbi;

    public FiberFluentAPITest(Class<? extends DataSource> cls) {
        this.cls = cls;
    }

    @Before
    public void setUp() throws Exception {
        DataSource ds = cls.newInstance();
        // snippet creation
        this.jdbi = new FiberDBI(ds);
        // end of snippet
    }

    @Test
    public void testOpen() throws IOException, InterruptedException, Exception {
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                try (Handle h = jdbi.open()) {
                    h.execute("create table if not exists testOpen (id int primary key, name varchar(100))");
                    h.execute("drop table testOpen");
                }
            }
        }).start().join();
    }

    @Test
    public void testQueryFirst() throws IOException, InterruptedException, Exception {
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                // snippet usage
                try (Handle h = jdbi.open()) {
                    h.execute("create table  if not exists testQueryFirst (id int primary key, name varchar(100))");
                    for (int i = 0; i < 100; i++)
                        h.execute("insert into testQueryFirst (id, name) values (?, ?)", i, "stranger " + i);
                    assertEquals("stranger 37", h.createQuery("select name from testQueryFirst where id = :id")
                            .bind("id", 37).map(StringMapper.FIRST).first());
                    h.execute("drop table testQueryFirst");
                }
                // end of snippet
            }
        }).start().join();
    }

    @Test
    public void testQueryList() throws IOException, InterruptedException, Exception {
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                try (Handle h = jdbi.open()) {
                    h.execute("create table if not exists testQueryList (id int primary key, name varchar(100))");
                    for (int i = 0; i < 100; i++)
                        h.execute("insert into testQueryList (id, name) values (?, ?)", i, "stranger " + i);
                    assertEquals(37, h.createQuery("select name from testQueryList where id < :id order by id")
                            .bind("id", 37).map(StringMapper.FIRST).list().size());
                    h.execute("drop table testQueryList");
                }
            }
        }).start().join();
    }
}
