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

import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import co.paralleluniverse.common.util.CheckedCallable;
import co.paralleluniverse.fibers.Suspendable;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Blob;

/**
 * @author crclespainter
 */
class FiberBlob implements Blob {
    private final Blob blob;
    private final ExecutorService executor;

    public FiberBlob(final Blob blob, final ExecutorService executor) {
        this.blob = blob;
        this.executor = executor;
    }

    @Override
    @Suspendable
    public byte[] getBytes(final long pos, final int length) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<byte[], SQLException>() {
            @Override
            public byte[] call() throws SQLException {
                return blob.getBytes(pos, length);
            }
        });
    }

    @Override
    @Suspendable
    public InputStream getBinaryStream() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<InputStream, SQLException>() {
            @Override
            public InputStream call() throws SQLException {
                return blob.getBinaryStream();
            }
        });
    }

    @Override
    @Suspendable
    public long position(final byte[] pattern, final long start) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Long, SQLException>() {
            @Override
            public Long call() throws SQLException {
                return blob.position(pattern, start);
            }
        });
    }

    @Override
    @Suspendable
    public long position(final Blob pattern, final long start) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Long, SQLException>() {
            @Override
            public Long call() throws SQLException {
                return blob.position(pattern, start);
            }
        });
    }

    @Override
    @Suspendable
    public int setBytes(final long pos, final byte[] bytes) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Integer, SQLException>() {
            @Override
            public Integer call() throws SQLException {
                return blob.setBytes(pos, bytes);
            }
        });
    }

    @Override
    @Suspendable
    public int setBytes(final long pos, final byte[] bytes, final int offset, final int len) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Integer, SQLException>() {
            @Override
            public Integer call() throws SQLException {
                return blob.setBytes(pos, bytes, offset, len);
            }
        });
    }

    @Override
    @Suspendable
    public OutputStream setBinaryStream(final long pos) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<OutputStream, SQLException>() {
            @Override
            public OutputStream call() throws SQLException {
                return blob.setBinaryStream(pos);
            }
        });
    }

    @Override
    @Suspendable
    public InputStream getBinaryStream(final long pos, final long length) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<InputStream, SQLException>() {
            @Override
            public InputStream call() throws SQLException {
                return blob.getBinaryStream(pos, length);
            }
        });        
    }

    @Override
    @Suspendable
    public long length() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Long, SQLException>() {
            @Override
            public Long call() throws SQLException {
                return blob.length();
            }
        });
    }

    @Override
    @Suspendable
    public void truncate(final long len) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                blob.truncate(len);
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
                blob.free();
                return null;
            }
        });
    }
}
