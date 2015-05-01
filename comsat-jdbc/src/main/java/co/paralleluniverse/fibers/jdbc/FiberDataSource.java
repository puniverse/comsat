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
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
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
    private final ListeningExecutorService executor;

    /**
     * Wraps a JDBC {@link DataSource}.
     * @param ds The {@link DataSource} to wrap.
     * @param executor The {@link ExecutorService} to use to actually execute JDBC operations.
     * @return 
     */
    public static DataSource wrap(DataSource ds, ExecutorService executor) {
        return new FiberDataSource(ds, MoreExecutors.listeningDecorator(executor));
    }

    /**
     * Wraps a JDBC {@link DataSource}.
     * @param ds The {@link DataSource} to wrap.
     * @param numThreads The number of threads to create in the thread pool that will be used to execute JDBC operations.
     * @return 
     */
    public static DataSource wrap(DataSource ds, final int numThreads) {
        return wrap(ds, Executors.newFixedThreadPool(numThreads, new ThreadFactoryBuilder().setNameFormat("jdbc-worker-%d").setDaemon(true).build()));
    }

    /**
     * Wraps a JDBC {@link DataSource} with fixed 10 threads pool executor.
     * @param ds The {@link DataSource} to wrap.
     * @return 
     */
    public static DataSource wrap(DataSource ds) {
        return wrap(ds, 10);
    }

    protected FiberDataSource(DataSource ds, ListeningExecutorService exec) {
        this.ds = ds;
        this.executor = exec;
    }

    @Override
    @Suspendable
    public Connection getConnection() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<FiberConnection, SQLException>() {
            @Override
            public FiberConnection call() throws SQLException {
                return new FiberConnection(ds.getConnection(), executor);
            }
        });
    }

    @Override
    @Suspendable
    public Connection getConnection(final String username, final String password) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<FiberConnection, SQLException>() {
            @Override
            public FiberConnection call() throws SQLException {
                return new FiberConnection(ds.getConnection(username, password), executor);
            }
        });
    }

    @Override
    @Suspendable
    public PrintWriter getLogWriter() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<PrintWriter, SQLException>() {
            @Override
            public PrintWriter call() throws SQLException {
                return ds.getLogWriter();
            }
        });
    }

    @Override
    @Suspendable
    public void setLogWriter(final PrintWriter out) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                ds.setLogWriter(out);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void setLoginTimeout(final int seconds) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                ds.setLoginTimeout(seconds);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public int getLoginTimeout() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Integer, SQLException>() {
            @Override
            public Integer call() throws SQLException {
                return ds.getLoginTimeout();
            }
        });
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
    @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
    public boolean equals(Object obj) {
        return ds.equals(obj);
    }

    @Override
    public String toString() {
        return ds.toString();
    }
}
