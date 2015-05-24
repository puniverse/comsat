/*
 * COMSAT
 * Copyright (c) 2013-2015, Parallel Universe Software Co. All rights reserved.
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

import co.paralleluniverse.common.util.CheckedCallable;
import co.paralleluniverse.fibers.Suspendable;
import java.sql.Ref;
// import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ExecutorService;

/**
 * @author crclespainter
 */
class FiberRef implements Ref {
    private final Ref ref;
    private final ExecutorService executor;

    public FiberRef(final Ref ref, final ExecutorService executor) {
        this.ref = ref;
        this.executor = executor;
    }

    @Override
    @Suspendable
    public String getBaseTypeName() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<String, SQLException>() {
            @Override
            public String call() throws SQLException {
                return ref.getBaseTypeName();
            }
        });
    }

    @Override
    public Object getObject(final Map<String, Class<?>> map) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Object, SQLException>() {
            @Override
            public Object call() throws SQLException {
                return ref.getObject(map);
            }
        });
    }

    @Override
    public Object getObject() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Object, SQLException>() {
            @Override
            public Object call() throws SQLException {
                return ref.getObject();
            }
        });
    }

    @Override
    public void setObject(final Object value) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                ref.setObject(value);
                return null;
            }
        });
    }
}
