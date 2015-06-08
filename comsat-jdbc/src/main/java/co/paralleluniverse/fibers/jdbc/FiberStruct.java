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
import java.sql.SQLException;
import java.sql.Struct;
import java.util.Map;
import java.util.concurrent.ExecutorService;

/**
 * @author crclespainter
 */
public class FiberStruct implements Struct {
    private final Struct struct;
    private final ExecutorService executor;

    public FiberStruct(final Struct struct, final ExecutorService executor) {
        this.struct = struct;
        this.executor = executor;
    }

    @Override
    @Suspendable
    public String getSQLTypeName() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<String, SQLException>() {
            @Override
            public String call() throws SQLException {
                return struct.getSQLTypeName();
            }
        });
    }

    @Override
    @Suspendable
    public Object[] getAttributes() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Object[], SQLException>() {
            @Override
            public Object[] call() throws SQLException {
                return struct.getAttributes();
            }
        });
    }

    @Override
    @Suspendable
    public Object[] getAttributes(final Map<String, Class<?>> map) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Object[], SQLException>() {
            @Override
            public Object[] call() throws SQLException {
                return struct.getAttributes(map);
            }
        });
    }

    @Override
    public int hashCode() {
        return struct.hashCode();
    }

    @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
    @Override
    public boolean equals(Object obj) {
        return struct.equals(obj);
    }

    @Override
    public String toString() {
        return struct.toString();
    }
}
