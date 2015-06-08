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
import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Date;
import java.sql.NClob;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLXML;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.concurrent.ExecutorService;

/**
 * @author eitan
 */
public class FiberPreparedStatement extends FiberStatement implements PreparedStatement {
    FiberPreparedStatement(final java.sql.PreparedStatement ps, final ExecutorService exec) {
        super(ps, exec);
    }

    protected PreparedStatement stmt() {
        return (PreparedStatement) stmt;
    }

    @Override
    @Suspendable
    public FiberResultSet executeQuery() throws SQLException {
        final ResultSet result = JDBCFiberAsync.exec(executor, new CheckedCallable<ResultSet, SQLException>() {
            @Override
            public ResultSet call() throws SQLException {
                return stmt().executeQuery();
            }
        });
        return new FiberResultSet(result, executor);
    }

    @Override
    @Suspendable
    public int executeUpdate() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Integer, SQLException>() {
            @Override
            public Integer call() throws SQLException {
                return stmt().executeUpdate();
            }
        });
    }

    @Override
    @Suspendable
    public boolean execute() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Boolean, SQLException>() {
            @Override
            public Boolean call() throws SQLException {
                return stmt().execute();
            }
        });
    }

    @Override
    @Suspendable
    public void setNull(final int parameterIndex, final int sqlType) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                stmt().setNull(parameterIndex, sqlType);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void setBoolean(final int parameterIndex, final boolean x) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                stmt().setBoolean(parameterIndex, x);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void setByte(final int parameterIndex, final byte x) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                stmt().setByte(parameterIndex, x);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void setShort(final int parameterIndex, final short x) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                stmt().setShort(parameterIndex, x);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void setInt(final int parameterIndex, final int x) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                stmt().setInt(parameterIndex, x);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void setLong(final int parameterIndex, final long x) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                stmt().setLong(parameterIndex, x);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void setFloat(final int parameterIndex, final float x) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                stmt().setFloat(parameterIndex, x);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void setDouble(final int parameterIndex, final double x) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                stmt().setDouble(parameterIndex, x);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void setBigDecimal(final int parameterIndex, final BigDecimal x) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                stmt().setBigDecimal(parameterIndex, x);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void setString(final int parameterIndex, final String x) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                stmt().setString(parameterIndex, x);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void setBytes(final int parameterIndex, final byte[] x) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                stmt().setBytes(parameterIndex, x);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void setDate(final int parameterIndex, final Date x) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                stmt().setDate(parameterIndex, x);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void setTime(final int parameterIndex, final Time x) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                stmt().setTime(parameterIndex, x);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void setTimestamp(final int parameterIndex, final Timestamp x) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                stmt().setTimestamp(parameterIndex, x);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void setAsciiStream(final int parameterIndex, final InputStream x, final int length) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                stmt().setAsciiStream(parameterIndex, x, length);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void setUnicodeStream(final int parameterIndex, final InputStream x, final int length) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                stmt().setUnicodeStream(parameterIndex, x, length);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void setBinaryStream(final int parameterIndex, final InputStream x, final int length) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                stmt().setBinaryStream(parameterIndex, x, length);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void clearParameters() throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                stmt().clearParameters();
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void setObject(final int parameterIndex, final Object x, final int targetSqlType) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                stmt().setObject(parameterIndex, x, targetSqlType);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void setObject(final int parameterIndex, final Object x) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                stmt().setObject(parameterIndex, x);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void addBatch() throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                stmt().addBatch();
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void setCharacterStream(final int parameterIndex, final Reader reader, final int length) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                stmt().setCharacterStream(parameterIndex, reader, length);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void setRef(final int parameterIndex, final Ref x) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                stmt().setRef(parameterIndex, x);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void setBlob(final int parameterIndex, final Blob x) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                stmt().setBlob(parameterIndex, x);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void setClob(final int parameterIndex, final Clob x) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                stmt().setClob(parameterIndex, x);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void setArray(final int parameterIndex, final Array x) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                stmt().setArray(parameterIndex, x);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public ResultSetMetaData getMetaData() throws SQLException {
        final ResultSetMetaData meta = JDBCFiberAsync.exec(executor, new CheckedCallable<ResultSetMetaData, SQLException>() {
            @Override
            public ResultSetMetaData call() throws SQLException {
                return stmt().getMetaData();
            }
        });
        return new FiberResultSetMetaData(meta, executor);
    }

    @Override
    @Suspendable
    public void setDate(final int parameterIndex, final Date x, final Calendar cal) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                stmt().setDate(parameterIndex, x, cal);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void setTime(final int parameterIndex, final Time x, final Calendar cal) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                stmt().setTime(parameterIndex, x, cal);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void setTimestamp(final int parameterIndex, final Timestamp x, final Calendar cal) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                stmt().setTimestamp(parameterIndex, x, cal);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void setNull(final int parameterIndex, final int sqlType, final String typeName) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                stmt().setNull(parameterIndex, sqlType, typeName);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void setURL(final int parameterIndex, final URL x) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                stmt().setURL(parameterIndex, x);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public ParameterMetaData getParameterMetaData() throws SQLException {
        final ParameterMetaData pmeta = JDBCFiberAsync.exec(executor, new CheckedCallable<ParameterMetaData, SQLException>() {
            @Override
            public ParameterMetaData call() throws SQLException {
                return stmt().getParameterMetaData();
            }
        });
        return new FiberParameterMetadata(pmeta, executor);
    }

    @Override
    @Suspendable
    public void setRowId(final int parameterIndex, final RowId x) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                stmt().setRowId(parameterIndex, x);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void setNString(final int parameterIndex, final String value) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                stmt().setNString(parameterIndex, value);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void setNCharacterStream(final int parameterIndex, final Reader value, final long length) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                stmt().setNCharacterStream(parameterIndex, value, length);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void setNClob(final int parameterIndex, final NClob value) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                stmt().setNClob(parameterIndex, value);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void setClob(final int parameterIndex, final Reader reader, final long length) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                stmt().setClob(parameterIndex, reader, length);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void setBlob(final int parameterIndex, final InputStream inputStream, final long length) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                stmt().setBlob(parameterIndex, inputStream, length);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void setNClob(final int parameterIndex, final Reader reader, final long length) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                stmt().setNClob(parameterIndex, reader, length);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void setSQLXML(final int parameterIndex, final SQLXML xmlObject) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                stmt().setSQLXML(parameterIndex, xmlObject);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void setObject(final int parameterIndex, final Object x, final int targetSqlType, final int scaleOrLength) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                stmt().setObject(parameterIndex, x, targetSqlType, scaleOrLength);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void setAsciiStream(final int parameterIndex, final InputStream x, final long length) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                stmt().setAsciiStream(parameterIndex, x, length);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void setBinaryStream(final int parameterIndex, final InputStream x, final long length) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                stmt().setBinaryStream(parameterIndex, x, length);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void setCharacterStream(final int parameterIndex, final Reader reader, final long length) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                stmt().setCharacterStream(parameterIndex, reader, length);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void setAsciiStream(final int parameterIndex, final InputStream x) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                stmt().setAsciiStream(parameterIndex, x);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void setBinaryStream(final int parameterIndex, final InputStream x) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                stmt().setBinaryStream(parameterIndex, x);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void setCharacterStream(final int parameterIndex, final Reader reader) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                stmt().setCharacterStream(parameterIndex, reader);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void setNCharacterStream(final int parameterIndex, final Reader value) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                stmt().setNCharacterStream(parameterIndex, value);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void setClob(final int parameterIndex, final Reader reader) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                stmt().setClob(parameterIndex, reader);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void setBlob(final int parameterIndex, final InputStream inputStream) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                stmt().setBlob(parameterIndex, inputStream);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void setNClob(final int parameterIndex, final Reader reader) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                stmt().setNClob(parameterIndex, reader);
                return null;
            }
        });
    }

    @Override
    public int hashCode() {
        return stmt().hashCode();
    }

    @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
    @Override
    public boolean equals(Object obj) {
        return stmt().equals(obj);
    }

    @Override
    public String toString() {
        return stmt().toString();
    }
}
