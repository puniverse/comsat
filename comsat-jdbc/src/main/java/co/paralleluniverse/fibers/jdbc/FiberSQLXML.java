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
import java.io.Reader;
import java.io.Writer;
import java.sql.SQLXML;
import javax.xml.transform.Result;
import javax.xml.transform.Source;

/**
 * @author crclespainter
 */
public class FiberSQLXML implements SQLXML {
    private final SQLXML sqlXML;
    private final ExecutorService executor;

    public FiberSQLXML(final SQLXML sqlXML, final ExecutorService executor) {
        this.sqlXML = sqlXML;
        this.executor = executor;
    }

    @Override
    @Suspendable
    public InputStream getBinaryStream() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<InputStream, SQLException>() {
            @Override
            public InputStream call() throws SQLException {
                return sqlXML.getBinaryStream();
            }
        });
    }

    @Override
    @Suspendable
    public void free() throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                sqlXML.free();
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public OutputStream setBinaryStream() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<OutputStream, SQLException>() {
            @Override
            public OutputStream call() throws SQLException {
                return sqlXML.setBinaryStream();
            }
        });
    }

    @Override
    @Suspendable
    public Reader getCharacterStream() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Reader, SQLException>() {
            @Override
            public Reader call() throws SQLException {
                return sqlXML.getCharacterStream();
            }
        });
    }

    @Override
    @Suspendable
    public Writer setCharacterStream() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Writer, SQLException>() {
            @Override
            public Writer call() throws SQLException {
                return sqlXML.setCharacterStream();
            }
        });
    }

    @Override
    @Suspendable
    public String getString() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<String, SQLException>() {
            @Override
            public String call() throws SQLException {
                return sqlXML.getString();
            }
        });
    }

    @Override
    @Suspendable
    public void setString(final String value) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                sqlXML.setString(value);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public <T extends Source> T getSource(final Class<T> sourceClass) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<T, SQLException>() {
            @Override
            public T call() throws SQLException {
                return sqlXML.getSource(sourceClass);
            }
        });
    }

    @Override
    @Suspendable
    public <T extends Result> T setResult(final Class<T> resultClass) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<T, SQLException>() {
            @Override
            public T call() throws SQLException {
                return sqlXML.setResult(resultClass);
            }
        });
    }

    @Override
    public int hashCode() {
        return sqlXML.hashCode();
    }

    @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
    @Override
    public boolean equals(Object obj) {
        return sqlXML.equals(obj);
    }

    @Override
    public String toString() {
        return sqlXML.toString();
    }
}
