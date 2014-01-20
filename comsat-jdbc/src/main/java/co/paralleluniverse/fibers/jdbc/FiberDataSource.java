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
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;
import javax.sql.DataSource;

/**
 * A JDBC {@link DataSource} that creates connections that can be used in Quasar fibers.
 * This class simply wraps any JDBC data source, and executes any JDBC operations in a separate {@code ExecutorService}.
 *
 * @author eitan
 */
public class FiberDataSource implements DataSource {
    private final DataSource ds;
    private final ListeningExecutorService exec;

    /**
     * Wraps a JDBC {@link DataSource}.
     * @param ds The {@link DataSource} to wrap.
     * @param executor The {@link ExecutorService} to use to actually execute JDBC operations.
     */
    public FiberDataSource(DataSource ds, ExecutorService executor) {
        this(ds, MoreExecutors.listeningDecorator(executor));
    }

    /**
     * Wraps a JDBC {@link DataSource}.
     * @param ds The {@link DataSource} to wrap.
     * @param numThreads The number of threads to create in the thread pool that will be used to execute JDBC operations.
     */
    public FiberDataSource(DataSource ds, final int numThreads) {
        this(ds, Executors.newFixedThreadPool(numThreads, new ThreadFactoryBuilder().setNameFormat("jdbc-worker-%d").setDaemon(true).build()));
    }

    private FiberDataSource(DataSource ds, ListeningExecutorService exec) {
        this.ds = ds;
        this.exec = exec;
    }

    @Override
    @Suspendable
    public Connection getConnection() throws SQLException {
        try {
            return AsyncListenableFuture.get(exec.submit(new Callable<FiberConnection>() {
                @Override
                public FiberConnection call() throws Exception {
                    return new FiberConnection(ds.getConnection(), exec);
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
    public Connection getConnection(final String username, final String password) throws SQLException {
        try {
            return AsyncListenableFuture.get(exec.submit(new Callable<FiberConnection>() {
                @Override
                public FiberConnection call() throws Exception {
                    return new FiberConnection(ds.getConnection(username, password), exec);
                }
            }));
        } catch (InterruptedException | ExecutionException ex) {
            throw Exceptions.rethrowUnwrap(ex, SQLException.class);
        } catch (SuspendExecution ex) {
            throw new AssertionError(ex);
        }
    }

    @Override
    public PrintWriter getLogWriter() throws SQLException {
        return ds.getLogWriter();
    }

    @Override
    public void setLogWriter(PrintWriter out) throws SQLException {
        ds.setLogWriter(out);
    }

    @Override
    public void setLoginTimeout(int seconds) throws SQLException {
        ds.setLoginTimeout(seconds);
    }

    @Override
    public int getLoginTimeout() throws SQLException {
        return ds.getLoginTimeout();
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return ds.getParentLogger();
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        return ds.unwrap(iface);
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return ds.isWrapperFor(iface);
    }

    @Override
    public int hashCode() {
        return ds.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return ds.equals(obj);
    }

    @Override
    public String toString() {
        return ds.toString();
    }
}
