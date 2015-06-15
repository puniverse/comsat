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
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.util.concurrent.ExecutorService;

/**
 * @author eitan
 */
public class FiberStatement implements Statement {
    protected final Statement stmt;
    protected final ExecutorService executor;

    public FiberStatement(final Statement stmt, final ExecutorService exec) {
        this.stmt = stmt;
        this.executor = exec;
    }

    @Override
    @Suspendable
    public FiberResultSet executeQuery(final String sql) throws SQLException {
        final ResultSet result = JDBCFiberAsync.exec(executor, new CheckedCallable<ResultSet, SQLException>() {
            @Override
            public ResultSet call() throws SQLException {
//                    int fetchSize = stmt.getFetchSize();
//                    stmt.setFetchSize(99999);
                final ResultSet executeQuery = stmt.executeQuery(sql);
//                    stmt.setFetchSize(fetchSize);
                return executeQuery;
            }
        });
        return new FiberResultSet(result, executor);
    }

    @Override
    @Suspendable
    public int executeUpdate(final String sql) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Integer, SQLException>() {
            @Override
            public Integer call() throws SQLException {
                return stmt.executeUpdate(sql);
            }
        });
    }

    @Override
    @Suspendable
    public void close() throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                stmt.close();
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public int getMaxFieldSize() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Integer, SQLException>() {
            @Override
            public Integer call() throws SQLException {
                return stmt.getMaxFieldSize();
            }
        });
    }

    @Override
    @Suspendable
    public void setMaxFieldSize(final int max) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                stmt.setMaxFieldSize(max);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public int getMaxRows() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Integer, SQLException>() {
            @Override
            public Integer call() throws SQLException {
                return stmt.getMaxRows();
            }
        });
    }

    @Override
    @Suspendable
    public void setMaxRows(final int max) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                stmt.setMaxRows(max);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void setEscapeProcessing(final boolean enable) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                stmt.setEscapeProcessing(enable);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public int getQueryTimeout() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Integer, SQLException>() {
            @Override
            public Integer call() throws SQLException {
                return stmt.getQueryTimeout();
            }
        });
    }

    @Override
    @Suspendable
    public void setQueryTimeout(final int seconds) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                stmt.setQueryTimeout(seconds);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void cancel() throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                stmt.cancel();
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public SQLWarning getWarnings() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<SQLWarning, SQLException>() {
            @Override
            public SQLWarning call() throws SQLException {
                return stmt.getWarnings();
            }
        });
    }

    @Override
    @Suspendable
    public void clearWarnings() throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                stmt.clearWarnings();
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void setCursorName(final String name) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                stmt.setCursorName(name);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public FiberResultSet getResultSet() throws SQLException {
        final ResultSet result = JDBCFiberAsync.exec(executor, new CheckedCallable<ResultSet, SQLException>() {
            @Override
            public ResultSet call() throws SQLException {
                return stmt.getResultSet();
            }
        });
        return new FiberResultSet(result, executor);
    }

    @Override
    @Suspendable
    public int getUpdateCount() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Integer, SQLException>() {
            @Override
            public Integer call() throws SQLException {
                return stmt.getUpdateCount();
            }
        });
    }

    @Override
    @Suspendable
    public boolean getMoreResults() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Boolean, SQLException>() {
            @Override
            public Boolean call() throws SQLException {
                return stmt.getMoreResults();
            }
        });
    }

    @Override
    @Suspendable
    public void setFetchDirection(final int direction) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                stmt.setFetchDirection(direction);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public int getFetchDirection() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Integer, SQLException>() {
            @Override
            public Integer call() throws SQLException {
                return stmt.getFetchDirection();
            }
        });
    }

    @Override
    @Suspendable
    public void setFetchSize(final int rows) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                stmt.setFetchSize(rows);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public int getFetchSize() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Integer, SQLException>() {
            @Override
            public Integer call() throws SQLException {
                return stmt.getFetchSize();
            }
        });
    }

    @Override
    @Suspendable
    public int getResultSetConcurrency() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Integer, SQLException>() {
            @Override
            public Integer call() throws SQLException {
                return stmt.getResultSetConcurrency();
            }
        });
    }

    @Override
    @Suspendable
    public int getResultSetType() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Integer, SQLException>() {
            @Override
            public Integer call() throws SQLException {
                return stmt.getResultSetType();
            }
        });
    }

    @Override
    @Suspendable
    public void addBatch(final String sql) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                stmt.addBatch(sql);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void clearBatch() throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                stmt.clearBatch();
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public int[] executeBatch() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<int[], SQLException>() {
            @Override
            public int[] call() throws SQLException {
                return stmt.executeBatch();
            }
        });
    }

    @Override
    @Suspendable
    public FiberConnection getConnection() throws SQLException {
        final Connection conn = JDBCFiberAsync.exec(executor, new CheckedCallable<Connection, SQLException>() {
            @Override
            public Connection call() throws SQLException {
                return stmt.getConnection();
            }
        });
        return new FiberConnection(conn, executor);
    }

    @Override
    @Suspendable
    public boolean getMoreResults(final int current) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Boolean, SQLException>() {
            @Override
            public Boolean call() throws SQLException {
                return stmt.getMoreResults();
            }
        });
    }

    @Override
    @Suspendable
    public ResultSet getGeneratedKeys() throws SQLException {
        final ResultSet result = JDBCFiberAsync.exec(executor, new CheckedCallable<ResultSet, SQLException>() {
            @Override
            public ResultSet call() throws SQLException {
                return stmt.getGeneratedKeys();
            }
        });
        return new FiberResultSet(result, executor);
    }

    @Override
    @Suspendable
    public int executeUpdate(final String sql, final int autoGeneratedKeys) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Integer, SQLException>() {
            @Override
            public Integer call() throws SQLException {
                return stmt.executeUpdate(sql, autoGeneratedKeys);
            }
        });
    }

    @Override
    @Suspendable
    public int executeUpdate(final String sql, final int[] columnIndexes) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Integer, SQLException>() {
            @Override
            public Integer call() throws SQLException {
                return stmt.executeUpdate(sql, columnIndexes);
            }
        });
    }

    @Override
    @Suspendable
    public int executeUpdate(final String sql, final String[] columnNames) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Integer, SQLException>() {
            @Override
            public Integer call() throws SQLException {
                return stmt.executeUpdate(sql, columnNames);
            }
        });
    }

    @Suspendable
    @Override
    public boolean execute(final String sql) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Boolean, SQLException>() {
            @Override
            public Boolean call() throws SQLException {
                return stmt.execute(sql);
            }
        });
    }

    @Override
    @Suspendable
    public boolean execute(final String sql, final int autoGeneratedKeys) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Boolean, SQLException>() {
            @Override
            public Boolean call() throws SQLException {
                return stmt.execute(sql, autoGeneratedKeys);
            }
        });
    }

    @Override
    @Suspendable
    public boolean execute(final String sql, final int[] columnIndexes) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Boolean, SQLException>() {
            @Override
            public Boolean call() throws SQLException {
                return stmt.execute(sql, columnIndexes);
            }
        });
    }

    @Override
    @Suspendable
    public boolean execute(final String sql, final String[] columnNames) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Boolean, SQLException>() {
            @Override
            public Boolean call() throws SQLException {
                return stmt.execute(sql, columnNames);
            }
        });
    }

    @Override
    @Suspendable
    public int getResultSetHoldability() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Integer, SQLException>() {
            @Override
            public Integer call() throws SQLException {
                return stmt.getResultSetHoldability();
            }
        });
    }

    @Override
    @Suspendable
    public boolean isClosed() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Boolean, SQLException>() {
            @Override
            public Boolean call() throws SQLException {
                return stmt.isClosed();
            }
        });
    }

    @Override
    @Suspendable
    public void setPoolable(final boolean poolable) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                stmt.setPoolable(poolable);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public boolean isPoolable() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Boolean, SQLException>() {
            @Override
            public Boolean call() throws SQLException {
                return stmt.isPoolable();
            }
        });
    }

    @Override
    @Suspendable
    public void closeOnCompletion() throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                stmt.closeOnCompletion();
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public boolean isCloseOnCompletion() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Boolean, SQLException>() {
            @Override
            public Boolean call() throws SQLException {
                return stmt.isCloseOnCompletion();
            }
        });
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        return stmt.unwrap(iface);
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return stmt.isWrapperFor(iface);
    }

    @Override
    public int hashCode() {
        return stmt.hashCode();
    }

    @Override
    @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
    public boolean equals(Object obj) {
        return stmt.equals(obj);
    }

    @Override
    public String toString() {
        return stmt.toString();
    }
}
