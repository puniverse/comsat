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
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;

/**
 * @author crclespainter
 */
public class FiberResultSetMetaData implements ResultSetMetaData {
    private final ResultSetMetaData resultMeta;
    private final ExecutorService executor;

    public FiberResultSetMetaData(final ResultSetMetaData resultMeta, final ExecutorService executor) {
        this.resultMeta = resultMeta;
        this.executor = executor;
    }

    @Override
    @Suspendable
    public int getColumnCount() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Integer, SQLException>() {
            @Override
            public Integer call() throws SQLException {
                return resultMeta.getColumnCount();
            }
        });
    }

    @Override
    @Suspendable
    public boolean isAutoIncrement(final int column) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Boolean, SQLException>() {
            @Override
            public Boolean call() throws SQLException {
                return resultMeta.isAutoIncrement(column);
            }
        });
    }

    @Override
    @Suspendable
    public boolean isCaseSensitive(final int column) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Boolean, SQLException>() {
            @Override
            public Boolean call() throws SQLException {
                return resultMeta.isCaseSensitive(column);
            }
        });
    }

    @Override
    @Suspendable
    public boolean isSearchable(final int column) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Boolean, SQLException>() {
            @Override
            public Boolean call() throws SQLException {
                return resultMeta.isSearchable(column);
            }
        });
    }

    @Override
    @Suspendable
    public boolean isCurrency(final int column) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Boolean, SQLException>() {
            @Override
            public Boolean call() throws SQLException {
                return resultMeta.isCurrency(column);
            }
        });
    }

    @Override
    @Suspendable
    public int isNullable(final int column) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Integer, SQLException>() {
            @Override
            public Integer call() throws SQLException {
                return resultMeta.isNullable(column);
            }
        });
    }

    @Override
    @Suspendable
    public boolean isSigned(final int column) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Boolean, SQLException>() {
            @Override
            public Boolean call() throws SQLException {
                return resultMeta.isSigned(column);
            }
        });
    }

    @Override
    @Suspendable
    public int getColumnDisplaySize(final int column) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Integer, SQLException>() {
            @Override
            public Integer call() throws SQLException {
                return resultMeta.getColumnDisplaySize(column);
            }
        });
    }

    @Override
    @Suspendable
    public String getColumnLabel(final int column) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<String, SQLException>() {
            @Override
            public String call() throws SQLException {
                return resultMeta.getColumnLabel(column);
            }
        });
    }

    @Override
    @Suspendable
    public String getColumnName(final int column) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<String, SQLException>() {
            @Override
            public String call() throws SQLException {
                return resultMeta.getColumnName(column);
            }
        });
    }

    @Override
    @Suspendable
    public String getSchemaName(final int column) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<String, SQLException>() {
            @Override
            public String call() throws SQLException {
                return resultMeta.getSchemaName(column);
            }
        });
    }

    @Override
    @Suspendable
    public int getPrecision(final int column) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Integer, SQLException>() {
            @Override
            public Integer call() throws SQLException {
                return resultMeta.getPrecision(column);
            }
        });
    }

    @Override
    @Suspendable
    public int getScale(final int column) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Integer, SQLException>() {
            @Override
            public Integer call() throws SQLException {
                return resultMeta.getScale(column);
            }
        });
    }

    @Override
    @Suspendable
    public String getTableName(final int column) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<String, SQLException>() {
            @Override
            public String call() throws SQLException {
                return resultMeta.getTableName(column);
            }
        });
    }

    @Override
    @Suspendable
    public String getCatalogName(final int column) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<String, SQLException>() {
            @Override
            public String call() throws SQLException {
                return resultMeta.getCatalogName(column);
            }
        });
    }

    @Override
    @Suspendable
    public int getColumnType(final int column) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Integer, SQLException>() {
            @Override
            public Integer call() throws SQLException {
                return resultMeta.getColumnType(column);
            }
        });
    }

    @Override
    @Suspendable
    public String getColumnTypeName(final int column) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<String, SQLException>() {
            @Override
            public String call() throws SQLException {
                return resultMeta.getColumnTypeName(column);
            }
        });
    }

    @Override
    @Suspendable
    public boolean isReadOnly(final int column) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Boolean, SQLException>() {
            @Override
            public Boolean call() throws SQLException {
                return resultMeta.isReadOnly(column);
            }
        });
    }

    @Override
    @Suspendable
    public boolean isWritable(final int column) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Boolean, SQLException>() {
            @Override
            public Boolean call() throws SQLException {
                return resultMeta.isWritable(column);
            }
        });
    }

    @Override
    @Suspendable
    public boolean isDefinitelyWritable(final int column) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Boolean, SQLException>() {
            @Override
            public Boolean call() throws SQLException {
                return resultMeta.isDefinitelyWritable(column);
            }
        });
    }

    @Override
    @Suspendable
    public String getColumnClassName(final int column) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<String, SQLException>() {
            @Override
            public String call() throws SQLException {
                return resultMeta.getColumnClassName(column);
            }
        });
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        return resultMeta.unwrap(iface);
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return resultMeta.isWrapperFor(iface);
    }
}
