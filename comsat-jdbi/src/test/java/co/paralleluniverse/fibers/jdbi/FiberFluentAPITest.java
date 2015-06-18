/*
 * COMSAT
 * Copyright (C) 2014-2015, Parallel Universe Software Co. All rights reserved.
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
import co.paralleluniverse.fibers.Suspendable;
import co.paralleluniverse.strands.SuspendableRunnable;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import javax.sql.DataSource;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.IDBI;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.mixins.Transactional;
import org.skife.jdbi.v2.tweak.HandleCallback;
import org.skife.jdbi.v2.util.StringMapper;

@RunWith(Parameterized.class)
public class FiberFluentAPITest {

    @Suspendable
    public interface TestDAO extends Transactional<TestDAO> {
        @SqlUpdate("insert into test (id, name) values (:id, :name)")
        void insert(@Bind("id")long id, @Bind("name") String name);
    }

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

    @Test
    public void testAttach() throws IOException, InterruptedException, Exception {
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                try (Handle h = jdbi.open()) {
                    createTest(h);
                    TestDAO dao = h.attach(TestDAO.class);
                    dao.insert(1, "Name1");
                    dropTest(h);
                }
            }
        }).start().join();
    }

    @Test
    public void testOnDemand() throws IOException, InterruptedException, Exception {
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                try (Handle h = jdbi.open()) {
                    createTest(h);
                }

                TestDAO dao = jdbi.onDemand(TestDAO.class);
                dao.insert(2, "Name2");
            
                try (Handle h = jdbi.open()) {
                    dropTest(h);
                }
            }
        }).start().join();
    }

    @Test
    public void testTransactionalOnDemand() throws IOException, InterruptedException, Exception {
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                try (Handle h = jdbi.open()) {
                    createTest(h);
                }

                TestDAO dao = jdbi.onDemand(TestDAO.class);
                dao.begin();
                dao.insert(3, "Name3");
                dao.commit();

                try (Handle h = jdbi.open()) {
                    dropTest(h);
                }
            }
        }).start().join();
    }

    @Test
    public void testWithHandle() throws IOException, InterruptedException, Exception {
        jdbi.withHandle(new HandleCallback<Void>() {
                @Override
                @Suspendable
                public Void withHandle(Handle h) throws Exception {
                    createTest(h);
                    final TestDAO dao = h.attach(TestDAO.class);
                    dao.insert(4, "Name4");
                    dropTest(h);
                    return null;
                }
            }
        );
    }

    @Test
    public void testTransactionalWithHandle() throws IOException, InterruptedException, Exception {
        jdbi.withHandle(new HandleCallback<Void>() {
                @Override
                @Suspendable
                public Void withHandle(Handle h) throws Exception {
                    createTest(h);
                    final TestDAO dao = h.attach(TestDAO.class);
                    dao.begin();
                    dao.insert(5, "Name5");
                    dao.commit();
                    dropTest(h);
                    return null;
                }
            }
        );
    }

    @Suspendable
    private static void dropTest(Handle h) {
        h.execute("drop table test");
    }

    @Suspendable
    private static void createTest(Handle h) {
        h.execute("create table if not exists test (id int primary key, name varchar(100))");
    }
}
