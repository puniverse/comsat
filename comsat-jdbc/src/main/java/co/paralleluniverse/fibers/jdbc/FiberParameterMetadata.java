/*
 * COMSAT
 * Copyright (c) 2015, Parallel Universe Software Co. All rights reserved.
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
import java.sql.ParameterMetaData;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;

/**
 * @author circlespainter
 */
public class FiberParameterMetadata implements ParameterMetaData {
    protected final ParameterMetaData pmeta;
    protected final ExecutorService executor;

    public FiberParameterMetadata(ParameterMetaData pmeta, ExecutorService executor) {
        this.pmeta = pmeta;
        this.executor = executor;
    }

    @Override
    @Suspendable
    public int getParameterCount() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Integer, SQLException>() {
            @Override
            public Integer call() throws SQLException {
                return pmeta.getParameterCount();
            }
        });
    }

    @Override
    @Suspendable
    public int isNullable(final int param) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Integer, SQLException>() {
            @Override
            public Integer call() throws SQLException {
                return pmeta.isNullable(param);
            }
        });
    }

    @Override
    @Suspendable
    public boolean isSigned(final int param) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Boolean, SQLException>() {
            @Override
            public Boolean call() throws SQLException {
                return pmeta.isSigned(param);
            }
        });
    }

    @Override
    @Suspendable
    public int getPrecision(final int param) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Integer, SQLException>() {
            @Override
            public Integer call() throws SQLException {
                return pmeta.getPrecision(param);
            }
        });
    }

    @Override
    @Suspendable
    public int getScale(final int param) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Integer, SQLException>() {
            @Override
            public Integer call() throws SQLException {
                return pmeta.getScale(param);
            }
        });
    }

    @Override
    @Suspendable
    public int getParameterType(final int param) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Integer, SQLException>() {
            @Override
            public Integer call() throws SQLException {
                return pmeta.getParameterType(param);
            }
        });
    }

    @Override
    @Suspendable
    public String getParameterTypeName(final int param) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<String, SQLException>() {
            @Override
            public String call() throws SQLException {
                return pmeta.getParameterTypeName(param);
            }
        });
    }

    @Override
    @Suspendable
    public String getParameterClassName(final int param) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<String, SQLException>() {
            @Override
            public String call() throws SQLException {
                return pmeta.getParameterClassName(param);
            }
        });
    }

    @Override
    @Suspendable
    public int getParameterMode(final int param) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Integer, SQLException>() {
            @Override
            public Integer call() throws SQLException {
                return pmeta.getParameterMode(param);
            }
        });
    }

    @Override
    public <T> T unwrap(final Class<T> iface) throws SQLException {
        return pmeta.unwrap(iface);
    }

    @Override
    public boolean isWrapperFor(final Class<?> iface) throws SQLException {
        return pmeta.isWrapperFor(iface);
    }
    
}
