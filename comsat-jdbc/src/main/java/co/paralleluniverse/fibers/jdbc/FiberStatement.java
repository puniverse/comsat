/*
 * COMSAT
 * Copyright (c) 2013-2014, Parallel Universe Software Co. All rights reserved.
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

import co.paralleluniverse.common.util.Exceptions;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.Suspendable;
import co.paralleluniverse.fibers.futures.AsyncListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

/**
 *
 * @author eitan
 */
class FiberStatement implements Statement {
    private final Statement stmt;
    final ListeningExecutorService exec;

    public FiberStatement(Statement stmt, ListeningExecutorService exec) {
        this.stmt = stmt;
        this.exec = exec;
    }

    @Override
    @Suspendable
    public ResultSet executeQuery(final String sql) throws SQLException {
        try {
            return AsyncListenableFuture.get(exec.submit(new Callable<ResultSet>() {
                @Override
                public ResultSet call() throws Exception {
//                    int fetchSize = stmt.getFetchSize();
                    stmt.setFetchSize(99999);
                    final ResultSet executeQuery = stmt.executeQuery(sql);
//                    stmt.setFetchSize(fetchSize);
                    return executeQuery;
                }
            }));
        } catch (InterruptedException | ExecutionException ex) {
            throw Exceptions.rethrowUnwrap(ex, SQLException.class);
        } catch (SuspendExecution ex) {
            throw new AssertionError(ex);
        }
    }

    java.sql.Statement stmt() {
        return stmt;
    }

    @Override
    @Suspendable
    public int executeUpdate(final String sql) throws SQLException {
        try {
            return AsyncListenableFuture.get(exec.submit(new Callable<Integer>() {
                @Override
                public Integer call() throws Exception {
                    return stmt.executeUpdate(sql);
                }
            }));
        } catch (InterruptedException | ExecutionException ex) {
            throw Exceptions.rethrowUnwrap(ex, SQLException.class);
        } catch (SuspendExecution ex) {
            throw new AssertionError(ex);
        }
    }

    @Override
    public void close() throws SQLException {
        stmt.close();
    }

    @Override
    public int getMaxFieldSize() throws SQLException {
        return stmt.getMaxFieldSize();
    }

    @Override
    public void setMaxFieldSize(int max) throws SQLException {
        stmt.setMaxFieldSize(max);
    }

    @Override
    public int getMaxRows() throws SQLException {
        return stmt.getMaxRows();
    }

    @Override
    public void setMaxRows(int max) throws SQLException {
        stmt.setMaxRows(max);
    }

    @Override
    public void setEscapeProcessing(boolean enable) throws SQLException {
        stmt.setEscapeProcessing(enable);
    }

    @Override
    public int getQueryTimeout() throws SQLException {
        return stmt.getQueryTimeout();
    }

    @Override
    public void setQueryTimeout(int seconds) throws SQLException {
        stmt.setQueryTimeout(seconds);
    }

    @Override
    public void cancel() throws SQLException {
        stmt.cancel();
    }

    @Override
    public SQLWarning getWarnings() throws SQLException {
        return stmt.getWarnings();
    }

    @Override
    public void clearWarnings() throws SQLException {
        stmt.clearWarnings();
    }

    @Override
    public void setCursorName(String name) throws SQLException {
        stmt.setCursorName(name);
    }

    @Override
    @Suspendable
    public ResultSet getResultSet() throws SQLException {
        try {
            return AsyncListenableFuture.get(exec.submit(new Callable<ResultSet>() {
                @Override
                public ResultSet call() throws Exception {
                    return stmt().getResultSet();
                }
            }));
        } catch (SuspendExecution ex) {
            throw new AssertionError(ex);
        } catch (InterruptedException | ExecutionException ex) {
            throw Exceptions.rethrowUnwrap(ex, SQLException.class);
        }
    }

    @Override
    public int getUpdateCount() throws SQLException {
        return stmt.getUpdateCount();
    }

    @Override
    public boolean getMoreResults() throws SQLException {
        return stmt.getMoreResults();
    }

    @Override
    public void setFetchDirection(int direction) throws SQLException {
        stmt.setFetchDirection(direction);
    }

    @Override
    public int getFetchDirection() throws SQLException {
        return stmt.getFetchDirection();
    }

    @Override
    public void setFetchSize(int rows) throws SQLException {
        stmt.setFetchSize(rows);
    }

    @Override
    public int getFetchSize() throws SQLException {
        return stmt.getFetchSize();
    }

    @Override
    public int getResultSetConcurrency() throws SQLException {
        return stmt.getResultSetConcurrency();
    }

    @Override
    public int getResultSetType() throws SQLException {
        return stmt.getResultSetType();
    }

    @Override
    public void addBatch(String sql) throws SQLException {
        stmt.addBatch(sql);
    }

    @Override
    public void clearBatch() throws SQLException {
        stmt.clearBatch();
    }

    @Override
    @Suspendable
    public int[] executeBatch() throws SQLException {
        try {
            return AsyncListenableFuture.get(exec.submit(new Callable<int[]>() {
                @Override
                public int[] call() throws Exception {
                    return stmt().executeBatch();
                }
            }));
        } catch (SuspendExecution ex) {
            throw new AssertionError(ex);
        } catch (InterruptedException | ExecutionException ex) {
            throw Exceptions.rethrowUnwrap(ex, SQLException.class);
        }
    }

    @Override
    public Connection getConnection() throws SQLException {
        return stmt.getConnection();
    }

    @Override
    public boolean getMoreResults(int current) throws SQLException {
        return stmt.getMoreResults(current);
    }

    @Override
    public ResultSet getGeneratedKeys() throws SQLException {
        return stmt.getGeneratedKeys();
    }

    @Override
    @Suspendable
    public int executeUpdate(final String sql, final int autoGeneratedKeys) throws SQLException {
        try {
            return AsyncListenableFuture.get(exec.submit(new Callable<Integer>() {
                @Override
                public Integer call() throws Exception {
                    return stmt().executeUpdate(sql, autoGeneratedKeys);
                }
            }));
        } catch (InterruptedException | ExecutionException ex) {
            throw Exceptions.rethrowUnwrap(ex, SQLException.class);
        } catch (SuspendExecution ex) {
            throw new AssertionError(ex);
        }
    }

    @Override
    @Suspendable
    public int executeUpdate(final String sql, final int[] columnIndexes) throws SQLException {
        try {
            return AsyncListenableFuture.get(exec.submit(new Callable<Integer>() {
                @Override
                public Integer call() throws Exception {
                    return stmt().executeUpdate(sql, columnIndexes);
                }
            }));
        } catch (InterruptedException | ExecutionException ex) {
            throw Exceptions.rethrowUnwrap(ex, SQLException.class);
        } catch (SuspendExecution ex) {
            throw new AssertionError(ex);
        }
    }

    @Override
    @Suspendable
    public int executeUpdate(final String sql, final String[] columnNames) throws SQLException {
        try {
            return AsyncListenableFuture.get(exec.submit(new Callable<Integer>() {
                @Override
                public Integer call() throws Exception {
                    return stmt().executeUpdate(sql, columnNames);
                }
            }));
        } catch (InterruptedException | ExecutionException ex) {
            throw Exceptions.rethrowUnwrap(ex, SQLException.class);
        } catch (SuspendExecution ex) {
            throw new AssertionError(ex);
        }
    }

    @Suspendable
    @Override
    public boolean execute(final String sql) throws SQLException {
        try {
            return AsyncListenableFuture.get(exec.submit(new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    return stmt.execute(sql);
                }
            }));
        } catch (InterruptedException | ExecutionException ex) {
            throw Exceptions.rethrowUnwrap(ex, SQLException.class);
        } catch (SuspendExecution ex) {
            throw new AssertionError(ex);
        }
    }

    @Override
    @Suspendable
    public boolean execute(final String sql, final int autoGeneratedKeys) throws SQLException {
        try {
            return AsyncListenableFuture.get(exec.submit(new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    return stmt().execute(sql, autoGeneratedKeys);
                }
            }));
        } catch (InterruptedException | ExecutionException ex) {
            throw Exceptions.rethrowUnwrap(ex, SQLException.class);
        } catch (SuspendExecution ex) {
            throw new AssertionError(ex);
        }
    }

    @Override
    @Suspendable
    public boolean execute(final String sql, final int[] columnIndexes) throws SQLException {
        try {
            return AsyncListenableFuture.get(exec.submit(new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    return stmt().execute(sql, columnIndexes);
                }
            }));
        } catch (InterruptedException | ExecutionException ex) {
            throw Exceptions.rethrowUnwrap(ex, SQLException.class);
        } catch (SuspendExecution ex) {
            throw new AssertionError(ex);
        }
    }

    @Override
    @Suspendable
    public boolean execute(final String sql, final String[] columnNames) throws SQLException {
        try {
            return AsyncListenableFuture.get(exec.submit(new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    return stmt().execute(sql, columnNames);
                }
            }));
        } catch (InterruptedException | ExecutionException ex) {
            throw Exceptions.rethrowUnwrap(ex, SQLException.class);
        } catch (SuspendExecution ex) {
            throw new AssertionError(ex);
        }
    }

    @Override
    public int getResultSetHoldability() throws SQLException {
        return stmt.getResultSetHoldability();
    }

    @Override
    public boolean isClosed() throws SQLException {
        return stmt.isClosed();
    }

    @Override
    public void setPoolable(boolean poolable) throws SQLException {
        stmt.setPoolable(poolable);
    }

    @Override
    public boolean isPoolable() throws SQLException {
        return stmt.isPoolable();
    }

    @Override
    public void closeOnCompletion() throws SQLException {
        stmt.closeOnCompletion();
    }

    @Override
    public boolean isCloseOnCompletion() throws SQLException {
        return stmt.isCloseOnCompletion();
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
    public boolean equals(Object obj) {
        return stmt.equals(obj);
    }

    @Override
    public String toString() {
        return stmt.toString();
    }
}
