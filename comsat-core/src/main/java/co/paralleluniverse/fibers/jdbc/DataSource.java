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
import com.google.common.util.concurrent.MoreExecutors;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

/**
 *
 * @author eitan
 */
public class DataSource {
    private final javax.sql.DataSource ds;
    private final ListeningExecutorService exec;

    public DataSource(javax.sql.DataSource ds, ListeningExecutorService exec) {
        this.ds = ds;
        this.exec = exec;
    }

    public DataSource(javax.sql.DataSource ds, ExecutorService exec) {
        this(ds, MoreExecutors.listeningDecorator(exec));
    }

    public DataSource(javax.sql.DataSource ds, final int numThreads) {
        this(ds, Executors.newFixedThreadPool(numThreads));
    }

    public Connection getConnection() throws SQLException, SuspendExecution {
        try {
            return AsyncListenableFuture.get(exec.submit(new Callable<Connection>() {
                @Override
                public Connection call() throws Exception {
                    return new Connection(ds.getConnection(), exec);
                }
            }));
        } catch (InterruptedException | ExecutionException ex) {
            throw new RuntimeException(ex);
        }
    }

    public Connection getConnection(final String username, final String password) throws SQLException, SuspendExecution {
        try {
            return AsyncListenableFuture.get(exec.submit(new Callable<Connection>() {
                @Override
                public Connection call() throws Exception {
                    return new Connection(ds.getConnection(username, password), exec);
                }
            }));
        } catch (InterruptedException | ExecutionException ex) {
            throw new RuntimeException(ex);
        }
    }

    public PrintWriter getLogWriter() throws SQLException {
        return ds.getLogWriter();
    }

    public void setLogWriter(PrintWriter out) throws SQLException {
        ds.setLogWriter(out);
    }

    public void setLoginTimeout(int seconds) throws SQLException {
        ds.setLoginTimeout(seconds);
    }

    public int getLoginTimeout() throws SQLException {
        return ds.getLoginTimeout();
    }

    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return ds.getParentLogger();
    }

    public <T> T unwrap(Class<T> iface) throws SQLException {
        return ds.unwrap(iface);
    }

    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return ds.isWrapperFor(iface);
    }

    public int hashCode() {
        return ds.hashCode();
    }

    public boolean equals(Object obj) {
        return ds.equals(obj);
    }

    public String toString() {
        return ds.toString();
    }
}
