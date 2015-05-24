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
import java.sql.SQLException;
import java.sql.Savepoint;
import java.util.concurrent.ExecutorService;

/**
 * @author crclespainter
 */
class FiberSavepoint implements Savepoint {
    private final Savepoint savepoint;
    private final ExecutorService executor;

    public FiberSavepoint(final Savepoint savepoint, final ExecutorService executor) {
        this.savepoint = savepoint;
        this.executor = executor;
    }

    @Override
    public int getSavepointId() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Integer, SQLException>() {
            @Override
            public Integer call() throws SQLException {
                return savepoint.getSavepointId();
            }
        });
    }

    @Override
    public String getSavepointName() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<String, SQLException>() {
            @Override
            public String call() throws SQLException {
                return savepoint.getSavepointName();
            }
        });
    }
}
