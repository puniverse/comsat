/*
 * COMSAT
 * Copyright (C) 2013, Parallel Universe Software Co. All rights reserved.
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

import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.futures.AsyncListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Wrapper;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

/**
 *
 * @author eitan
 */
public class Statement implements Wrapper, AutoCloseable {
    private final java.sql.Statement stmt;
    final ListeningExecutorService exec;

    public Statement(java.sql.Statement stmt, ListeningExecutorService exec) {
        this.stmt = stmt;
        this.exec = exec;
    }

    public ResultSet executeQuery(final String sql) throws SQLException, SuspendExecution {
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
            throw new RuntimeException(ex);
        }
    }

    java.sql.Statement stmt() {
        return stmt;
    }
    
    public int executeUpdate(final String sql) throws SQLException, SuspendExecution {
        try {
            return AsyncListenableFuture.get(exec.submit(new Callable<Integer>() {
                @Override
                public Integer call() throws Exception {
                    return stmt.executeUpdate(sql);
                }
            }));
        } catch (InterruptedException | ExecutionException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void close() throws SQLException {
        stmt.close();
    }

    public int getMaxFieldSize() throws SQLException {
        return stmt.getMaxFieldSize();
    }

    public void setMaxFieldSize(int max) throws SQLException {
        stmt.setMaxFieldSize(max);
    }

    public int getMaxRows() throws SQLException {
        return stmt.getMaxRows();
    }

    public void setMaxRows(int max) throws SQLException {
        stmt.setMaxRows(max);
    }

    public void setEscapeProcessing(boolean enable) throws SQLException {
        stmt.setEscapeProcessing(enable);
    }

    public int getQueryTimeout() throws SQLException {
        return stmt.getQueryTimeout();
    }

    public void setQueryTimeout(int seconds) throws SQLException {
        stmt.setQueryTimeout(seconds);
    }

    public void cancel() throws SQLException {
        stmt.cancel();
    }

    public SQLWarning getWarnings() throws SQLException {
        return stmt.getWarnings();
    }

    public void clearWarnings() throws SQLException {
        stmt.clearWarnings();
    }

    public void setCursorName(String name) throws SQLException {
        stmt.setCursorName(name);
    }

    public boolean execute(String sql) throws SQLException {
        return stmt.execute(sql);
    }

    public ResultSet getResultSet() throws SQLException, SuspendExecution {
        try {
            return AsyncListenableFuture.get(exec.submit(new Callable<ResultSet>() {
                @Override
                public ResultSet call() throws Exception {
                    return stmt().getResultSet();
                }
            }));
        } catch (InterruptedException | ExecutionException ex) {
            throw new RuntimeException(ex);
        }
    }

    public int getUpdateCount() throws SQLException {
        return stmt.getUpdateCount();
    }

    public boolean getMoreResults() throws SQLException {
        return stmt.getMoreResults();
    }

    public void setFetchDirection(int direction) throws SQLException {
        stmt.setFetchDirection(direction);
    }

    public int getFetchDirection() throws SQLException {
        return stmt.getFetchDirection();
    }

    public void setFetchSize(int rows) throws SQLException {
        stmt.setFetchSize(rows);
    }

    public int getFetchSize() throws SQLException {
        return stmt.getFetchSize();
    }

    public int getResultSetConcurrency() throws SQLException {
        return stmt.getResultSetConcurrency();
    }

    public int getResultSetType() throws SQLException {
        return stmt.getResultSetType();
    }

    public void addBatch(String sql) throws SQLException {
        stmt.addBatch(sql);
    }

    public void clearBatch() throws SQLException {
        stmt.clearBatch();
    }

    public int[] executeBatch() throws SQLException, SuspendExecution {
        try {
            return AsyncListenableFuture.get(exec.submit(new Callable<int[]>() {
                @Override
                public int[] call() throws Exception {
                    return stmt().executeBatch();
                }
            }));
        } catch (InterruptedException | ExecutionException ex) {
            throw new RuntimeException(ex);
        }
    }

    public Connection getConnection() throws SQLException {
        return stmt.getConnection();
    }

    public boolean getMoreResults(int current) throws SQLException {
        return stmt.getMoreResults(current);
    }

    public ResultSet getGeneratedKeys() throws SQLException {
        return stmt.getGeneratedKeys();
    }

    public int executeUpdate(final String sql, final int autoGeneratedKeys) throws SQLException, SuspendExecution {
        try {
            return AsyncListenableFuture.get(exec.submit(new Callable<Integer>() {
                @Override
                public Integer call() throws Exception {
                    return stmt().executeUpdate(sql, autoGeneratedKeys);
                }
            }));
        } catch (InterruptedException | ExecutionException ex) {
            throw new RuntimeException(ex);
        }
    }

    public int executeUpdate(final String sql, final int[] columnIndexes) throws SQLException, SuspendExecution {
        try {
            return AsyncListenableFuture.get(exec.submit(new Callable<Integer>() {
                @Override
                public Integer call() throws Exception {
                    return stmt().executeUpdate(sql, columnIndexes);
                }
            }));
        } catch (InterruptedException | ExecutionException ex) {
            throw new RuntimeException(ex);
        }
    }

    public int executeUpdate(final String sql, final String[] columnNames) throws SQLException, SuspendExecution {
        try {
            return AsyncListenableFuture.get(exec.submit(new Callable<Integer>() {
                @Override
                public Integer call() throws Exception {
                    return stmt().executeUpdate(sql, columnNames);
                }
            }));
        } catch (InterruptedException | ExecutionException ex) {
            throw new RuntimeException(ex);
        }
    }

    public boolean execute(final String sql, final int autoGeneratedKeys) throws SQLException, SuspendExecution {
        try {
            return AsyncListenableFuture.get(exec.submit(new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    return stmt().execute(sql, autoGeneratedKeys);
                }
            }));
        } catch (InterruptedException | ExecutionException ex) {
            throw new RuntimeException(ex);
        }
    }

    public boolean execute(final String sql, final int[] columnIndexes) throws SQLException, SuspendExecution {
        try {
            return AsyncListenableFuture.get(exec.submit(new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    return stmt().execute(sql, columnIndexes);
                }
            }));
        } catch (InterruptedException | ExecutionException ex) {
            throw new RuntimeException(ex);
        }
    }

    public boolean execute(final String sql, final String[] columnNames) throws SQLException, SuspendExecution {
        try {
            return AsyncListenableFuture.get(exec.submit(new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    return stmt().execute(sql, columnNames);
                }
            }));
        } catch (InterruptedException | ExecutionException ex) {
            throw new RuntimeException(ex);
        }
    }

    public int getResultSetHoldability() throws SQLException {
        return stmt.getResultSetHoldability();
    }

    public boolean isClosed() throws SQLException {
        return stmt.isClosed();
    }

    public void setPoolable(boolean poolable) throws SQLException {
        stmt.setPoolable(poolable);
    }

    public boolean isPoolable() throws SQLException {
        return stmt.isPoolable();
    }

    public void closeOnCompletion() throws SQLException {
        stmt.closeOnCompletion();
    }

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
