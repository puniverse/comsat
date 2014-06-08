package co.paralleluniverse.fibers.jooq;

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
import co.paralleluniverse.embedded.db.H2JdbcDatasource;
import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.jdbc.FiberDataSource;
import co.paralleluniverse.strands.SuspendableRunnable;
import java.io.IOException;
import java.sql.Connection;
import java.util.Arrays;
import java.util.Collection;
import javax.sql.DataSource;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.RecordMapper;
import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.table;
import static org.jooq.impl.DSL.using;
import org.junit.After;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class JooqContextTest {

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
            {H2JdbcDatasource.class},});
    }
    private final Class<? extends DataSource> cls;
    private Connection conn;
    private DSLContext ctx;

    public JooqContextTest(Class<? extends DataSource> cls) {
        this.cls = cls;
    }

    @Before
    public void setUp() throws Exception {
        this.conn = new FiberDataSource(cls.newInstance()).getConnection();
        conn.createStatement().execute("create table something (id int primary key, name varchar(100))");
        this.ctx = using(conn);

    }

    @After
    public void tearDown() throws Exception {
        conn.createStatement().execute("drop table something");
        conn.close();
    }

    @Test
    public void testInsertSelect() throws IOException, InterruptedException, Exception {
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                for (int i = 0; i < 100; i++)
                    ctx.insertInto(table("something"), field("id"), field("name")).values(i, "name" + i).execute();
                for (int i = 0; i < 50; i++) {
                    Something something = ctx.select(field("id"), field("name")).from(table("something")).where(field("id", Integer.class).eq(i)).fetchOne().map(Something.mapper);
                    assertEquals("name" + i, something.name);
                }
            }
        }).start().join();
    }

    public static class Something {
        public final int id;
        public final String name;
        public static RecordMapper<Record, Something> mapper = new RecordMapper<Record, Something>() {
            @Override
            public Something map(Record r) {
                return new Something(r.getValue(field("id", Integer.class)), r.getValue(field("name", String.class)));
            }
        };

        public Something(int id, String name) {
            this.id = id;
            this.name = name;
        }
    }

}
