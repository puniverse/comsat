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

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.sql.Clob;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import co.paralleluniverse.common.util.CheckedCallable;
import co.paralleluniverse.fibers.Suspendable;

/**
 * @author crclespainter
 */
public class FiberClob implements Clob {
    private final Clob clob;
    private final ExecutorService executor;

    public FiberClob(final Clob clob, final ExecutorService executor) {
        this.clob = clob;
        this.executor = executor;
    }

    @Override
    @Suspendable
    public long length() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Long, SQLException>() {
            @Override
            public Long call() throws SQLException {
                return clob.length();
            }
        });
    }

    @Override
    @Suspendable
    public String getSubString(final long pos, final int length) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<String, SQLException>() {
            @Override
            public String call() throws SQLException {
                return clob.getSubString(pos, length);
            }
        });
    }

    @Override
    @Suspendable
    public Reader getCharacterStream() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Reader, SQLException>() {
            @Override
            public Reader call() throws SQLException {
                return clob.getCharacterStream();
            }
        });
    }

    @Override
    @Suspendable
    public InputStream getAsciiStream() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<InputStream, SQLException>() {
            @Override
            public InputStream call() throws SQLException {
                return clob.getAsciiStream();
            }
        });
    }

    @Override
    @Suspendable
    public long position(final String searchstr, final long start) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Long, SQLException>() {
            @Override
            public Long call() throws SQLException {
                return clob.position(searchstr, start);
            }
        });
    }

    @Override
    @Suspendable
    public long position(final Clob searchstr, final long start) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Long, SQLException>() {
            @Override
            public Long call() throws SQLException {
                return clob.position(searchstr, start);
            }
        });
    }

    @Override
    @Suspendable
    public int setString(final long pos, final String str) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Integer, SQLException>() {
            @Override
            public Integer call() throws SQLException {
                return clob.setString(pos, str);
            }
        });
    }

    @Override
    @Suspendable
    public int setString(final long pos, final String str, final int offset, final int len) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Integer, SQLException>() {
            @Override
            public Integer call() throws SQLException {
                return clob.setString(pos, str, offset, len);
            }
        });
    }

    @Override
    @Suspendable
    public OutputStream setAsciiStream(final long pos) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<OutputStream, SQLException>() {
            @Override
            public OutputStream call() throws SQLException {
                return clob.setAsciiStream(pos);
            }
        });
    }

    @Override
    @Suspendable
    public Writer setCharacterStream(final long pos) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Writer, SQLException>() {
            @Override
            public Writer call() throws SQLException {
                return clob.setCharacterStream(pos);
            }
        });
    }

    @Override
    @Suspendable
    public void truncate(final long len) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                clob.truncate(len);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void free() throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                clob.free();
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public Reader getCharacterStream(final long pos, final long length) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Reader, SQLException>() {
            @Override
            public Reader call() throws SQLException {
                return clob.getCharacterStream(pos, length);
            }
        });
    }
}
