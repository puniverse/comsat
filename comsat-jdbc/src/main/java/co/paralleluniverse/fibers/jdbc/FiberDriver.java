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
package co.paralleluniverse.fibers.jdbc;

import co.paralleluniverse.common.util.CheckedCallable;
import co.paralleluniverse.fibers.FiberAsync;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.Suspendable;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

public class FiberDriver implements Driver {

    @Suspendable
    @Override
    public Connection connect(final String url, final Properties info) throws SQLException {
        final String dbURL = url.replaceFirst("fiber:", "");
        int threadCount = Integer.parseInt(info.getProperty(THREADS_COUNT, "10"));
        info.remove(THREADS_COUNT);
        ExecutorService es = Executors.newFixedThreadPool(threadCount, new ThreadFactoryBuilder().setNameFormat("jdbc-worker-%d").setDaemon(true).build());
        try {
            Connection con = FiberAsync.runBlocking(es, new CheckedCallable<Connection, SQLException>() {
                @Override
                public Connection call() throws SQLException {
                    return DriverManager.getConnection(dbURL, info);
                }
            });
            return new FiberConnection(con, MoreExecutors.listeningDecorator(es));

        } catch (SuspendExecution ex) {
            throw new AssertionError(ex);
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        }
    }
    private static final String THREADS_COUNT = "threadsCount";
    private static final String RAW_DATA_SOURCE_URL = "rawDataSourceURL";

    @Override
    public boolean acceptsURL(String url) throws SQLException {
        return url.startsWith("jdbc:fiber:");
    }

    @Override
    public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException {
        String dbURL = info.getProperty(RAW_DATA_SOURCE_URL);
        if (dbURL == null)
            throw new SQLException("no rawDataSourceURL parameter");
        info.remove(RAW_DATA_SOURCE_URL);
        return DriverManager.getDriver(url).getPropertyInfo(url, info);
    }

    @Override
    public int getMajorVersion() {
        return 2;
    }

    @Override
    public int getMinorVersion() {
        return 0;
    }

    @Override
    public boolean jdbcCompliant() {
        return false;
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        throw new SQLFeatureNotSupportedException("Not supported yet.");
    }

}
