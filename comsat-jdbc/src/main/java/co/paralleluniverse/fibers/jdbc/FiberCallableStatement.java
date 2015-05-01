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
import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Date;
import java.sql.NClob;
import java.sql.Ref;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLXML;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Map;

/**
 * @author eitan
 */
class FiberCallableStatement extends FiberPreparedStatement implements CallableStatement {
    public FiberCallableStatement(CallableStatement cs, ListeningExecutorService exec) {
        super(cs, exec);
    }

    @Override
    protected CallableStatement stmt() {
        return (CallableStatement) super.stmt();
    }

    //Delegations
    @Override
    @Suspendable
    public void registerOutParameter(final int parameterIndex, final int sqlType) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                stmt().registerOutParameter(parameterIndex, sqlType);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void registerOutParameter(final int parameterIndex, final int sqlType, final int scale) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                stmt().registerOutParameter(parameterIndex, sqlType, scale);
                return null;
            }
        });
    }

    @Override
    public boolean wasNull() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Boolean, SQLException>() {
            @Override
            public Boolean call() throws SQLException {
                return stmt().wasNull();
            }
        });
    }

    @Override
    @Suspendable
    public String getString(final int parameterIndex) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<String, SQLException>() {
            @Override
            public String call() throws SQLException {
                return stmt().getString(parameterIndex);
            }
        });
    }

    @Override
    @Suspendable
    public boolean getBoolean(final int parameterIndex) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Boolean, SQLException>() {
            @Override
            public Boolean call() throws SQLException {
                return stmt().getBoolean(parameterIndex);
            }
        });
    }

    @Override
    @Suspendable
    public byte getByte(final int parameterIndex) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Byte, SQLException>() {
            @Override
            public Byte call() throws SQLException {
                return stmt().getByte(parameterIndex);
            }
        });
    }

    @Override
    @Suspendable
    public short getShort(final int parameterIndex) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Short, SQLException>() {
            @Override
            public Short call() throws SQLException {
                return stmt().getShort(parameterIndex);
            }
        });
    }

    @Override
    @Suspendable
    public int getInt(final int parameterIndex) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Integer, SQLException>() {
            @Override
            public Integer call() throws SQLException {
                return stmt().getInt(parameterIndex);
            }
        });
    }

    @Override
    @Suspendable
    public long getLong(final int parameterIndex) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Long, SQLException>() {
            @Override
            public Long call() throws SQLException {
                return stmt().getLong(parameterIndex);
            }
        });
    }

    @Override
    @Suspendable
    public float getFloat(final int parameterIndex) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Float, SQLException>() {
            @Override
            public Float call() throws SQLException {
                return stmt().getFloat(parameterIndex);
            }
        });
    }

    @Override
    @Suspendable
    public double getDouble(final int parameterIndex) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Double, SQLException>() {
            @Override
            public Double call() throws SQLException {
                return stmt().getDouble(parameterIndex);
            }
        });
    }

    @Override
    @Suspendable
    public BigDecimal getBigDecimal(final int parameterIndex, final int scale) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<BigDecimal, SQLException>() {
            @Override
            public BigDecimal call() throws SQLException {
                return stmt().getBigDecimal(parameterIndex, scale);
            }
        });
    }

    @Override
    @Suspendable
    public byte[] getBytes(final int parameterIndex) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<byte[], SQLException>() {
            @Override
            public byte[] call() throws SQLException {
                return stmt().getBytes(parameterIndex);
            }
        });
    }

    @Override
    @Suspendable
    public Date getDate(final int parameterIndex) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Date, SQLException>() {
            @Override
            public Date call() throws SQLException {
                return stmt().getDate(parameterIndex);
            }
        });
    }

    @Override
    @Suspendable
    public Time getTime(final int parameterIndex) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Time, SQLException>() {
            @Override
            public Time call() throws SQLException {
                return stmt().getTime(parameterIndex);
            }
        });
    }

    @Override
    @Suspendable
    public Timestamp getTimestamp(final int parameterIndex) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Timestamp, SQLException>() {
            @Override
            public Timestamp call() throws SQLException {
                return stmt().getTimestamp(parameterIndex);
            }
        });
    }

    @Override
    @Suspendable
    public Object getObject(final int parameterIndex) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Object, SQLException>() {
            @Override
            public Object call() throws SQLException {
                return stmt().getObject(parameterIndex);
            }
        });
    }

    @Override
    @Suspendable
    public BigDecimal getBigDecimal(final int parameterIndex) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<BigDecimal, SQLException>() {
            @Override
            public BigDecimal call() throws SQLException {
                return stmt().getBigDecimal(parameterIndex);
            }
        });
    }

    @Override
    @Suspendable
    public Object getObject(final int parameterIndex, final Map<String, Class<?>> map) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Object, SQLException>() {
            @Override
            public Object call() throws SQLException {
                return stmt().getObject(parameterIndex, map);
            }
        });
    }

    @Override
    @Suspendable
    public Ref getRef(final int parameterIndex) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Ref, SQLException>() {
            @Override
            public Ref call() throws SQLException {
                return stmt().getRef(parameterIndex);
            }
        });
    }

    @Override
    @Suspendable
    public Blob getBlob(final int parameterIndex) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Blob, SQLException>() {
            @Override
            public Blob call() throws SQLException {
                return stmt().getBlob(parameterIndex);
            }
        });
    }

    @Override
    @Suspendable
    public Clob getClob(final int parameterIndex) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Clob, SQLException>() {
            @Override
            public Clob call() throws SQLException {
                return stmt().getClob(parameterIndex);
            }
        });
    }

    @Override
    @Suspendable
    public Array getArray(final int parameterIndex) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Array, SQLException>() {
            @Override
            public Array call() throws SQLException {
                return stmt().getArray(parameterIndex);
            }
        });
    }

    @Override
    @Suspendable
    public Date getDate(final int parameterIndex, final Calendar cal) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Date, SQLException>() {
            @Override
            public Date call() throws SQLException {
                return stmt().getDate(parameterIndex, cal);
            }
        });
    }

    @Override
    @Suspendable
    public Time getTime(final int parameterIndex, final Calendar cal) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Time, SQLException>() {
            @Override
            public Time call() throws SQLException {
                return stmt().getTime(parameterIndex, cal);
            }
        });
    }

    @Override
    @Suspendable
    public Timestamp getTimestamp(final int parameterIndex, final Calendar cal) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Timestamp, SQLException>() {
            @Override
            public Timestamp call() throws SQLException {
                return stmt().getTimestamp(parameterIndex, cal);
            }
        });
    }

    @Override
    @Suspendable
    public void registerOutParameter(final int parameterIndex, final int sqlType, final String typeName) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                stmt().registerOutParameter(parameterIndex, sqlType, typeName);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void registerOutParameter(final String parameterName, final int sqlType) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                stmt().registerOutParameter(parameterName, sqlType);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void registerOutParameter(final String parameterName, final int sqlType, final int scale) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                stmt().registerOutParameter(parameterName, sqlType, scale);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void registerOutParameter(final String parameterName, final int sqlType, final String typeName) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                stmt().registerOutParameter(parameterName, sqlType, typeName);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public URL getURL(final int parameterIndex) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<URL, SQLException>() {
            @Override
            public URL call() throws SQLException {
                return stmt().getURL(parameterIndex);
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
    public String getString(final String parameterName) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<String, SQLException>() {
            @Override
            public String call() throws SQLException {
                return stmt().getString(parameterName);
            }
        });
    }

    @Override
    @Suspendable
    public boolean getBoolean(final String parameterName) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Boolean, SQLException>() {
            @Override
            public Boolean call() throws SQLException {
                return stmt().getBoolean(parameterName);
            }
        });
    }

    @Override
    @Suspendable
    public byte getByte(final String parameterName) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Byte, SQLException>() {
            @Override
            public Byte call() throws SQLException {
                return stmt().getByte(parameterName);
            }
        });
    }

    @Override
    @Suspendable
    public short getShort(final String parameterName) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Short, SQLException>() {
            @Override
            public Short call() throws SQLException {
                return stmt().getShort(parameterName);
            }
        });
    }

    @Override
    @Suspendable
    public int getInt(final String parameterName) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Integer, SQLException>() {
            @Override
            public Integer call() throws SQLException {
                return stmt().getInt(parameterName);
            }
        });
    }

    @Override
    @Suspendable
    public long getLong(final String parameterName) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Long, SQLException>() {
            @Override
            public Long call() throws SQLException {
                return stmt().getLong(parameterName);
            }
        });
    }

    @Override
    @Suspendable
    public float getFloat(final String parameterName) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Float, SQLException>() {
            @Override
            public Float call() throws SQLException {
                return stmt().getFloat(parameterName);
            }
        });
    }

    @Override
    @Suspendable
    public double getDouble(final String parameterName) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Double, SQLException>() {
            @Override
            public Double call() throws SQLException {
                return stmt().getDouble(parameterName);
            }
        });
    }

    @Override
    @Suspendable
    public byte[] getBytes(final String parameterName) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<byte[], SQLException>() {
            @Override
            public byte[] call() throws SQLException {
                return stmt().getBytes(parameterName);
            }
        });
    }

    @Override
    @Suspendable
    public Date getDate(final String parameterName) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Date, SQLException>() {
            @Override
            public Date call() throws SQLException {
                return stmt().getDate(parameterName);
            }
        });
    }

    @Override
    @Suspendable
    public Time getTime(final String parameterName) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Time, SQLException>() {
            @Override
            public Time call() throws SQLException {
                return stmt().getTime(parameterName);
            }
        });
    }

    @Override
    @Suspendable
    public Timestamp getTimestamp(final String parameterName) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Timestamp, SQLException>() {
            @Override
            public Timestamp call() throws SQLException {
                return stmt().getTimestamp(parameterName);
            }
        });
    }

    @Override
    @Suspendable
    public Object getObject(final String parameterName) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Object, SQLException>() {
            @Override
            public Object call() throws SQLException {
                return stmt().getObject(parameterName);
            }
        });
    }

    @Override
    @Suspendable
    public BigDecimal getBigDecimal(final String parameterName) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<BigDecimal, SQLException>() {
            @Override
            public BigDecimal call() throws SQLException {
                return stmt().getBigDecimal(parameterName);
            }
        });
    }

    @Override
    @Suspendable
    public Object getObject(final String parameterName, final Map<String, Class<?>> map) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Object, SQLException>() {
            @Override
            public Object call() throws SQLException {
                return stmt().getObject(parameterName, map);
            }
        });
    }

    @Override
    @Suspendable
    public Ref getRef(final String parameterName) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Ref, SQLException>() {
            @Override
            public Ref call() throws SQLException {
                return stmt().getRef(parameterName);
            }
        });
    }

    @Override
    @Suspendable
    public Blob getBlob(final String parameterName) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Blob, SQLException>() {
            @Override
            public Blob call() throws SQLException {
                return stmt().getBlob(parameterName);
            }
        });
    }

    @Override
    @Suspendable
    public Clob getClob(final String parameterName) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Clob, SQLException>() {
            @Override
            public Clob call() throws SQLException {
                return stmt().getClob(parameterName);
            }
        });
    }

    @Override
    @Suspendable
    public Array getArray(final String parameterName) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Array, SQLException>() {
            @Override
            public Array call() throws SQLException {
                return stmt().getArray(parameterName);
            }
        });
    }

    @Override
    @Suspendable
    public Date getDate(final String parameterName, final Calendar cal) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Date, SQLException>() {
            @Override
            public Date call() throws SQLException {
                return stmt().getDate(parameterName, cal);
            }
        });
    }

    @Override
    @Suspendable
    public Time getTime(final String parameterName, final Calendar cal) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Time, SQLException>() {
            @Override
            public Time call() throws SQLException {
                return stmt().getTime(parameterName, cal);
            }
        });
    }

    @Override
    @Suspendable
    public Timestamp getTimestamp(final String parameterName, final Calendar cal) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Timestamp, SQLException>() {
            @Override
            public Timestamp call() throws SQLException {
                return stmt().getTimestamp(parameterName, cal);
            }
        });
    }

    @Override
    @Suspendable
    public URL getURL(final String parameterName) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<URL, SQLException>() {
            @Override
            public URL call() throws SQLException {
                return stmt().getURL(parameterName);
            }
        });
    }

    @Override
    @Suspendable
    public RowId getRowId(final int parameterIndex) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<RowId, SQLException>() {
            @Override
            public RowId call() throws SQLException {
                return stmt().getRowId(parameterIndex);
            }
        });
    }

    @Override
    @Suspendable
    public RowId getRowId(final String parameterName) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<RowId, SQLException>() {
            @Override
            public RowId call() throws SQLException {
                return stmt().getRowId(parameterName);
            }
        });
    }

    @Override
    @Suspendable
    public NClob getNClob(final int parameterIndex) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<NClob, SQLException>() {
            @Override
            public NClob call() throws SQLException {
                return stmt().getNClob(parameterIndex);
            }
        });
    }

    @Override
    @Suspendable
    public NClob getNClob(final String parameterName) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<NClob, SQLException>() {
            @Override
            public NClob call() throws SQLException {
                return stmt().getNClob(parameterName);
            }
        });
    }

    @Override
    @Suspendable
    public SQLXML getSQLXML(final int parameterIndex) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<SQLXML, SQLException>() {
            @Override
            public SQLXML call() throws SQLException {
                return stmt().getSQLXML(parameterIndex);
            }
        });
    }

    @Override
    @Suspendable
    public SQLXML getSQLXML(final String parameterName) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<SQLXML, SQLException>() {
            @Override
            public SQLXML call() throws SQLException {
                return stmt().getSQLXML(parameterName);
            }
        });
    }

    @Override
    @Suspendable
    public String getNString(final int parameterIndex) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<String, SQLException>() {
            @Override
            public String call() throws SQLException {
                return stmt().getString(parameterIndex);
            }
        });
    }

    @Override
    @Suspendable
    public String getNString(final String parameterName) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<String, SQLException>() {
            @Override
            public String call() throws SQLException {
                return stmt().getString(parameterName);
            }
        });
    }

    @Override
    @Suspendable
    public Reader getNCharacterStream(final int parameterIndex) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Reader, SQLException>() {
            @Override
            public Reader call() throws SQLException {
                return stmt().getNCharacterStream(parameterIndex);
            }
        });
    }

    @Override
    @Suspendable
    public Reader getNCharacterStream(final String parameterName) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Reader, SQLException>() {
            @Override
            public Reader call() throws SQLException {
                return stmt().getNCharacterStream(parameterName);
            }
        });
    }

    @Override
    @Suspendable
    public Reader getCharacterStream(final int parameterIndex) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Reader, SQLException>() {
            @Override
            public Reader call() throws SQLException {
                return stmt().getCharacterStream(parameterIndex);
            }
        });
    }

    @Override
    @Suspendable
    public Reader getCharacterStream(final String parameterName) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Reader, SQLException>() {
            @Override
            public Reader call() throws SQLException {
                return stmt().getCharacterStream(parameterName);
            }
        });
    }

    @Override
    @Suspendable
    public <T> T getObject(final int parameterIndex, final Class<T> type) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<T, SQLException>() {
            @Override
            public T call() throws SQLException {
                return stmt().getObject(parameterIndex, type);
            }
        });
    }

    @Override
    @Suspendable
    public <T> T getObject(final String parameterName, final Class<T> type) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<T, SQLException>() {
            @Override
            public T call() throws SQLException {
                return stmt().getObject(parameterName, type);
            }
        });
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
    @Suspendable
    public void setURL(final String parameterName, final URL val) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                stmt().setURL(parameterName, val);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void setNull(final String parameterName, final int sqlType) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                stmt().setNull(parameterName, sqlType);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void setBoolean(final String parameterName, final boolean x) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                stmt().setBoolean(parameterName, x);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void setByte(final String parameterName, final byte x) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                stmt().setByte(parameterName, x);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void setShort(final String parameterName, final short x) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                stmt().setShort(parameterName, x);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void setInt(final String parameterName, final int x) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                stmt().setInt(parameterName, x);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void setLong(final String parameterName, final long x) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                stmt().setLong(parameterName, x);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void setFloat(final String parameterName, final float x) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                stmt().setFloat(parameterName, x);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void setDouble(final String parameterName, final double x) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                stmt().setDouble(parameterName, x);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void setBigDecimal(final String parameterName, final BigDecimal x) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                stmt().setBigDecimal(parameterName, x);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void setString(final String parameterName, final String x) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                stmt().setString(parameterName, x);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void setBytes(final String parameterName, final byte[] x) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                stmt().setBytes(parameterName, x);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void setDate(final String parameterName, final Date x) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                stmt().setDate(parameterName, x);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void setTime(final String parameterName, final Time x) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                stmt().setTime(parameterName, x);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void setTimestamp(final String parameterName, final Timestamp x) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                stmt().setTimestamp(parameterName, x);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void setAsciiStream(final String parameterName, final InputStream x, final int length) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                stmt().setAsciiStream(parameterName, x, length);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void setBinaryStream(final String parameterName, final InputStream x, final int length) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                stmt().setBinaryStream(parameterName, x, length);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void setObject(final String parameterName, final Object x, final int targetSqlType, final int scale) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                stmt().setObject(parameterName, x, targetSqlType, scale);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void setObject(final String parameterName, final Object x, final int targetSqlType) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                stmt().setObject(parameterName, x, targetSqlType);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void setObject(final String parameterName, final Object x) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                stmt().setObject(parameterName, x);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void setCharacterStream(final String parameterName, final Reader reader, final int length) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                stmt().setCharacterStream(parameterName, reader, length);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void setDate(final String parameterName, final Date x, final Calendar cal) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                stmt().setDate(parameterName, x, cal);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void setTime(final String parameterName, final Time x, final Calendar cal) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                stmt().setTime(parameterName, x, cal);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void setTimestamp(final String parameterName, final Timestamp x, final Calendar cal) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                stmt().setTimestamp(parameterName, x, cal);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void setNull(final String parameterName, final int sqlType, final String typeName) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                stmt().setNull(parameterName, sqlType, typeName);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void setRowId(final String parameterName, final RowId x) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                stmt().setRowId(parameterName, x);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void setNString(final String parameterName, final String value) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                stmt().setNString(parameterName, value);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void setNCharacterStream(final String parameterName, final Reader value, final long length) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                stmt().setNCharacterStream(parameterName, value, length);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void setNClob(final String parameterName, final NClob value) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                stmt().setNClob(parameterName, value);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void setClob(final String parameterName, final Reader reader, final long length) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                stmt().setClob(parameterName, reader, length);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void setBlob(final String parameterName, final InputStream inputStream, final long length) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                stmt().setBlob(parameterName, inputStream, length);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void setNClob(final String parameterName, final Reader reader, final long length) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                stmt().setNClob(parameterName, reader, length);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void setSQLXML(final String parameterName, final SQLXML xmlObject) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                stmt().setSQLXML(parameterName, xmlObject);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void setBlob(final String parameterName, final Blob x) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                stmt().setBlob(parameterName, x);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void setClob(final String parameterName, final Clob x) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                stmt().setClob(parameterName, x);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void setAsciiStream(final String parameterName, final InputStream x, final long length) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                stmt().setAsciiStream(parameterName, x, length);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void setBinaryStream(final String parameterName, final InputStream x, final long length) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                stmt().setBinaryStream(parameterName, x, length);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void setCharacterStream(final String parameterName, final Reader reader, final long length) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                stmt().setCharacterStream(parameterName, reader, length);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void setAsciiStream(final String parameterName, final InputStream x) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                stmt().setAsciiStream(parameterName, x);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void setBinaryStream(final String parameterName, final InputStream x) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                stmt().setBinaryStream(parameterName, x);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void setCharacterStream(final String parameterName, final Reader reader) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                stmt().setCharacterStream(parameterName, reader);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void setNCharacterStream(final String parameterName, final Reader value) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                stmt().setNCharacterStream(parameterName, value);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void setClob(final String parameterName, final Reader reader) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                stmt().setClob(parameterName, reader);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void setBlob(final String parameterName, final InputStream inputStream) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                stmt().setBlob(parameterName, inputStream);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void setNClob(final String parameterName, final Reader reader) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                stmt().setNClob(parameterName, reader);
                return null;
            }
        });
    }
}
