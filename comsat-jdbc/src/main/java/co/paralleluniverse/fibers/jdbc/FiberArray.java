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
import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ExecutorService;

/**
 * @author crclespainter
 */
class FiberArray implements Array {
    private final Array array;
    private final ExecutorService executor;

    public FiberArray(final Array array, final ExecutorService executor) {
        this.array = array;
        this.executor = executor;
    }

    @Override
    @Suspendable
    public String getBaseTypeName() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<String, SQLException>() {
            @Override
            public String call() throws SQLException {
                return array.getBaseTypeName();
            }
        });
    }

    @Override
    @Suspendable
    public int getBaseType() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Integer, SQLException>() {
            @Override
            public Integer call() throws SQLException {
                return array.getBaseType();
            }
        });
    }

    @Override
    @Suspendable
    public Object getArray() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Object, SQLException>() {
            @Override
            public Object call() throws SQLException {
                return array.getArray();
            }
        });
    }

    @Override
    @Suspendable
    public Object getArray(final Map<String, Class<?>> map) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Object, SQLException>() {
            @Override
            public Object call() throws SQLException {
                return array.getArray(map);
            }
        });
    }

    @Override
    @Suspendable
    public Object getArray(final long index, final int count) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Object, SQLException>() {
            @Override
            public Object call() throws SQLException {
                return array.getArray(index, count);
            }
        });
    }

    @Override
    @Suspendable
    public Object getArray(final long index, final int count, final Map<String, Class<?>> map) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Object, SQLException>() {
            @Override
            public Object call() throws SQLException {
                return array.getArray(index, count, map);
            }
        });
    }

    @Override
    @Suspendable
    public FiberResultSet getResultSet() throws SQLException {
        final ResultSet result = JDBCFiberAsync.exec(executor, new CheckedCallable<ResultSet, SQLException>() {
            @Override
            public ResultSet call() throws SQLException {
                return array.getResultSet();
            }
        });
        return new FiberResultSet(result, executor);
    }

    @Override
    @Suspendable
    public FiberResultSet getResultSet(final Map<String, Class<?>> map) throws SQLException {
        final ResultSet result = JDBCFiberAsync.exec(executor, new CheckedCallable<ResultSet, SQLException>() {
            @Override
            public ResultSet call() throws SQLException {
                return array.getResultSet(map);
            }
        });
        return new FiberResultSet(result, executor);

    }

    @Override
    @Suspendable
    public FiberResultSet getResultSet(final long index, final int count) throws SQLException {
        final ResultSet result = JDBCFiberAsync.exec(executor, new CheckedCallable<ResultSet, SQLException>() {
            @Override
            public ResultSet call() throws SQLException {
                return array.getResultSet(index, count);
            }
        });
        return new FiberResultSet(result, executor);
    }

    @Override
    @Suspendable
    public FiberResultSet getResultSet(final long index, final int count, final Map<String, Class<?>> map) throws SQLException {
        final ResultSet result = JDBCFiberAsync.exec(executor, new CheckedCallable<ResultSet, SQLException>() {
            @Override
            public ResultSet call() throws SQLException {
                return array.getResultSet(index, count, map);
            }
        });
        return new FiberResultSet(result, executor);
    }

    @Override
    @Suspendable
    public void free() throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                array.free();
                return null;
            }
        });
    }
}
