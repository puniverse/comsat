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
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.RowId;
import java.sql.SQLException;
// 1.8
//
// import java.sql.SQLType;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Map;
import java.util.concurrent.ExecutorService;

/**
 * @author crclespainter
 */
class FiberResultSet implements ResultSet {
    private final ResultSet result;
    private final ExecutorService executor;

    public FiberResultSet(final ResultSet result, final ExecutorService executor) {
        this.result = result;
        this.executor = executor;
    }

    @Override
    @Suspendable
    public boolean next() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Boolean, SQLException>() {
            @Override
            public Boolean call() throws SQLException {
                return result.next();
            }
        });
    }

    @Override
    @Suspendable
    public void close() throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                result.close();
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public boolean wasNull() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Boolean, SQLException>() {
            @Override
            public Boolean call() throws SQLException {
                return result.wasNull();
            }
        });
    }

    @Override
    @Suspendable
    public String getString(final int columnIndex) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<String, SQLException>() {
            @Override
            public String call() throws SQLException {
                return result.getString(columnIndex);
            }
        });
    }

    @Override
    @Suspendable
    public boolean getBoolean(final int columnIndex) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Boolean, SQLException>() {
            @Override
            public Boolean call() throws SQLException {
                return result.getBoolean(columnIndex);
            }
        });
    }

    @Override
    @Suspendable
    public byte getByte(final int columnIndex) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Byte, SQLException>() {
            @Override
            public Byte call() throws SQLException {
                return result.getByte(columnIndex);
            }
        });
    }

    @Override
    @Suspendable
    public short getShort(final int columnIndex) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Short, SQLException>() {
            @Override
            public Short call() throws SQLException {
                return result.getShort(columnIndex);
            }
        });
    }

    @Override
    @Suspendable
    public int getInt(final int columnIndex) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Integer, SQLException>() {
            @Override
            public Integer call() throws SQLException {
                return result.getInt(columnIndex);
            }
        });
    }

    @Override
    @Suspendable
    public long getLong(final int columnIndex) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Long, SQLException>() {
            @Override
            public Long call() throws SQLException {
                return result.getLong(columnIndex);
            }
        });
    }

    @Override
    @Suspendable
    public float getFloat(final int columnIndex) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Float, SQLException>() {
            @Override
            public Float call() throws SQLException {
                return result.getFloat(columnIndex);
            }
        });
    }

    @Override
    @Suspendable
    public double getDouble(final int columnIndex) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Double, SQLException>() {
            @Override
            public Double call() throws SQLException {
                return result.getDouble(columnIndex);
            }
        });
    }

    @Override
    @Deprecated
    @Suspendable
    public BigDecimal getBigDecimal(final int columnIndex, final int scale) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<BigDecimal, SQLException>() {
            @Override
            public BigDecimal call() throws SQLException {
                return result.getBigDecimal(columnIndex, scale);
            }
        });
    }

    @Override
    @Suspendable
    public byte[] getBytes(final int columnIndex) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<byte[], SQLException>() {
            @Override
            public byte[] call() throws SQLException {
                return result.getBytes(columnIndex);
            }
        });
    }

    @Override
    @Suspendable
    public Date getDate(final int columnIndex) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Date, SQLException>() {
            @Override
            public Date call() throws SQLException {
                return result.getDate(columnIndex);
            }
        });
    }

    @Override
    @Suspendable
    public Time getTime(final int columnIndex) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Time, SQLException>() {
            @Override
            public Time call() throws SQLException {
                return result.getTime(columnIndex);
            }
        });
    }

    @Override
    @Suspendable
    public Timestamp getTimestamp(final int columnIndex) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Timestamp, SQLException>() {
            @Override
            public Timestamp call() throws SQLException {
                return result.getTimestamp(columnIndex);
            }
        });
    }

    @Override
    @Suspendable
    public InputStream getAsciiStream(final int columnIndex) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<InputStream, SQLException>() {
            @Override
            public InputStream call() throws SQLException {
                return result.getAsciiStream(columnIndex);
            }
        });
    }

    @Override
    @Deprecated
    @Suspendable
    public InputStream getUnicodeStream(final int columnIndex) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<InputStream, SQLException>() {
            @Override
            public InputStream call() throws SQLException {
                return result.getUnicodeStream(columnIndex);
            }
        });
    }

    @Override
    @Suspendable
    public InputStream getBinaryStream(final int columnIndex) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<InputStream, SQLException>() {
            @Override
            public InputStream call() throws SQLException {
                return result.getBinaryStream(columnIndex);
            }
        });
    }

    @Override
    @Suspendable
    public String getString(final String columnLabel) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<String, SQLException>() {
            @Override
            public String call() throws SQLException {
                return result.getString(columnLabel);
            }
        });
    }

    @Override
    @Suspendable
    public boolean getBoolean(final String columnLabel) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Boolean, SQLException>() {
            @Override
            public Boolean call() throws SQLException {
                return result.getBoolean(columnLabel);
            }
        });
    }

    @Override
    @Suspendable
    public byte getByte(final String columnLabel) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Byte, SQLException>() {
            @Override
            public Byte call() throws SQLException {
                return result.getByte(columnLabel);
            }
        });
    }

    @Override
    @Suspendable
    public short getShort(final String columnLabel) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Short, SQLException>() {
            @Override
            public Short call() throws SQLException {
                return result.getShort(columnLabel);
            }
        });
    }

    @Override
    @Suspendable
    public int getInt(final String columnLabel) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Integer, SQLException>() {
            @Override
            public Integer call() throws SQLException {
                return result.getInt(columnLabel);
            }
        });
    }

    @Override
    @Suspendable
    public long getLong(final String columnLabel) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Long, SQLException>() {
            @Override
            public Long call() throws SQLException {
                return result.getLong(columnLabel);
            }
        });
    }

    @Override
    @Suspendable
    public float getFloat(final String columnLabel) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Float, SQLException>() {
            @Override
            public Float call() throws SQLException {
                return result.getFloat(columnLabel);
            }
        });
    }

    @Override
    @Suspendable
    public double getDouble(final String columnLabel) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Double, SQLException>() {
            @Override
            public Double call() throws SQLException {
                return result.getDouble(columnLabel);
            }
        });
    }

    @Override
    @Deprecated
    @Suspendable
    public BigDecimal getBigDecimal(final String columnLabel, final int scale) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<BigDecimal, SQLException>() {
            @Override
            public BigDecimal call() throws SQLException {
                return result.getBigDecimal(columnLabel, scale);
            }
        });
    }

    @Override
    @Suspendable
    public byte[] getBytes(final String columnLabel) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<byte[], SQLException>() {
            @Override
            public byte[] call() throws SQLException {
                return result.getBytes(columnLabel);
            }
        });
    }

    @Override
    @Suspendable
    public Date getDate(final String columnLabel) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Date, SQLException>() {
            @Override
            public Date call() throws SQLException {
                return result.getDate(columnLabel);
            }
        });
    }

    @Override
    @Suspendable
    public Time getTime(final String columnLabel) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Time, SQLException>() {
            @Override
            public Time call() throws SQLException {
                return result.getTime(columnLabel);
            }
        });
    }

    @Override
    @Suspendable
    public Timestamp getTimestamp(final String columnLabel) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Timestamp, SQLException>() {
            @Override
            public Timestamp call() throws SQLException {
                return result.getTimestamp(columnLabel);
            }
        });
    }

    @Override
    @Suspendable
    public InputStream getAsciiStream(final String columnLabel) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<InputStream, SQLException>() {
            @Override
            public InputStream call() throws SQLException {
                return result.getAsciiStream(columnLabel);
            }
        });
    }

    @Override
    @Deprecated
    @Suspendable
    public InputStream getUnicodeStream(final String columnLabel) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<InputStream, SQLException>() {
            @Override
            public InputStream call() throws SQLException {
                return result.getUnicodeStream(columnLabel);
            }
        });
    }

    @Override
    @Suspendable
    public InputStream getBinaryStream(final String columnLabel) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<InputStream, SQLException>() {
            @Override
            public InputStream call() throws SQLException {
                return result.getBinaryStream(columnLabel);
            }
        });
    }

    @Override
    @Suspendable
    public SQLWarning getWarnings() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<SQLWarning, SQLException>() {
            @Override
            public SQLWarning call() throws SQLException {
                return result.getWarnings();
            }
        });
    }

    @Override
    @Suspendable
    public void clearWarnings() throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                result.clearWarnings();
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public String getCursorName() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<String, SQLException>() {
            @Override
            public String call() throws SQLException {
                return result.getCursorName();
            }
        });
    }

    @Override
    @Suspendable
    public FiberResultSetMetaData getMetaData() throws SQLException {
        final ResultSetMetaData meta = JDBCFiberAsync.exec(executor, new CheckedCallable<ResultSetMetaData, SQLException>() {
            @Override
            public ResultSetMetaData call() throws SQLException {
                return result.getMetaData();
            }
        });
        return new FiberResultSetMetaData(meta, executor);
    }

    @Override
    @Suspendable
    public Object getObject(final int columnIndex) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Object, SQLException>() {
            @Override
            public Object call() throws SQLException {
                return result.getObject(columnIndex);
            }
        });
    }

    @Override
    @Suspendable
    public Object getObject(final String columnLabel) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Object, SQLException>() {
            @Override
            public Object call() throws SQLException {
                return result.getObject(columnLabel);
            }
        });
    }

    @Override
    @Suspendable
    public int findColumn(final String columnLabel) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Integer, SQLException>() {
            @Override
            public Integer call() throws SQLException {
                return result.findColumn(columnLabel);
            }
        });
    }

    @Override
    @Suspendable
    public Reader getCharacterStream(final int columnIndex) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Reader, SQLException>() {
            @Override
            public Reader call() throws SQLException {
                return result.getCharacterStream(columnIndex);
            }
        });
    }

    @Override
    @Suspendable
    public Reader getCharacterStream(final String columnLabel) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Reader, SQLException>() {
            @Override
            public Reader call() throws SQLException {
                return result.getCharacterStream(columnLabel);
            }
        });
    }

    @Override
    @Suspendable
    public BigDecimal getBigDecimal(final int columnIndex) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<BigDecimal, SQLException>() {
            @Override
            public BigDecimal call() throws SQLException {
                return result.getBigDecimal(columnIndex);
            }
        });
    }

    @Override
    @Suspendable
    public BigDecimal getBigDecimal(final String columnLabel) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<BigDecimal, SQLException>() {
            @Override
            public BigDecimal call() throws SQLException {
                return result.getBigDecimal(columnLabel);
            }
        });
    }

    @Override
    @Suspendable
    public boolean isBeforeFirst() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Boolean, SQLException>() {
            @Override
            public Boolean call() throws SQLException {
                return result.isBeforeFirst();
            }
        });
    }

    @Override
    @Suspendable
    public boolean isAfterLast() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Boolean, SQLException>() {
            @Override
            public Boolean call() throws SQLException {
                return result.isAfterLast();
            }
        });
    }

    @Override
    @Suspendable
    public boolean isFirst() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Boolean, SQLException>() {
            @Override
            public Boolean call() throws SQLException {
                return result.isFirst();
            }
        });
    }

    @Override
    @Suspendable
    public boolean isLast() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Boolean, SQLException>() {
            @Override
            public Boolean call() throws SQLException {
                return result.isLast();
            }
        });
    }

    @Override
    @Suspendable
    public void beforeFirst() throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                result.beforeFirst();
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void afterLast() throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                result.afterLast();
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public boolean first() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Boolean, SQLException>() {
            @Override
            public Boolean call() throws SQLException {
                return result.first();
            }
        });
    }

    @Override
    @Suspendable
    public boolean last() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Boolean, SQLException>() {
            @Override
            public Boolean call() throws SQLException {
                return result.last();
            }
        });
    }

    @Override
    @Suspendable
    public int getRow() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Integer, SQLException>() {
            @Override
            public Integer call() throws SQLException {
                return result.getRow();
            }
        });
    }

    @Override
    @Suspendable
    public boolean absolute(final int row) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Boolean, SQLException>() {
            @Override
            public Boolean call() throws SQLException {
                return result.absolute(row);
            }
        });
    }

    @Override
    @Suspendable
    public boolean relative(final int rows) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Boolean, SQLException>() {
            @Override
            public Boolean call() throws SQLException {
                return result.absolute(rows);
            }
        });
    }

    @Override
    @Suspendable
    public boolean previous() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Boolean, SQLException>() {
            @Override
            public Boolean call() throws SQLException {
                return result.previous();
            }
        });
    }

    @Override
    @Suspendable
    public void setFetchDirection(final int direction) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                result.setFetchDirection(direction);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public int getFetchDirection() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Integer, SQLException>() {
            @Override
            public Integer call() throws SQLException {
                return result.getFetchDirection();
            }
        });
    }

    @Override
    @Suspendable
    public void setFetchSize(final int rows) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                result.setFetchSize(rows);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public int getFetchSize() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Integer, SQLException>() {
            @Override
            public Integer call() throws SQLException {
                return result.getFetchSize();
            }
        });
    }

    @Override
    @Suspendable
    public int getType() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Integer, SQLException>() {
            @Override
            public Integer call() throws SQLException {
                return result.getType();
            }
        });
    }

    @Override
    @Suspendable
    public int getConcurrency() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Integer, SQLException>() {
            @Override
            public Integer call() throws SQLException {
                return result.getConcurrency();
            }
        });
    }

    @Override
    @Suspendable
    public boolean rowUpdated() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Boolean, SQLException>() {
            @Override
            public Boolean call() throws SQLException {
                return result.rowUpdated();
            }
        });
    }

    @Override
    @Suspendable
    public boolean rowInserted() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Boolean, SQLException>() {
            @Override
            public Boolean call() throws SQLException {
                return result.rowInserted();
            }
        });
    }

    @Override
    @Suspendable
    public boolean rowDeleted() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Boolean, SQLException>() {
            @Override
            public Boolean call() throws SQLException {
                return result.rowDeleted();
            }
        });
    }

    @Override
    @Suspendable
    public void updateNull(final int columnIndex) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                result.updateNull(columnIndex);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void updateBoolean(final int columnIndex, final boolean x) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                result.updateBoolean(columnIndex, x);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void updateByte(final int columnIndex, final byte x) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                result.updateByte(columnIndex, x);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void updateShort(final int columnIndex, final short x) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                result.updateShort(columnIndex, x);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void updateInt(final int columnIndex, final int x) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                result.updateInt(columnIndex, x);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void updateLong(final int columnIndex, final long x) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                result.updateLong(columnIndex, x);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void updateFloat(final int columnIndex, final float x) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                result.updateFloat(columnIndex, x);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void updateDouble(final int columnIndex, final double x) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                result.updateDouble(columnIndex, x);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void updateBigDecimal(final int columnIndex, final BigDecimal x) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                result.updateBigDecimal(columnIndex, x);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void updateString(final int columnIndex, final String x) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                result.updateString(columnIndex, x);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void updateBytes(final int columnIndex, final byte[] x) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                result.updateBytes(columnIndex, x);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void updateDate(final int columnIndex, final Date x) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                result.updateDate(columnIndex, x);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void updateTime(final int columnIndex, final Time x) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                result.updateTime(columnIndex, x);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void updateTimestamp(final int columnIndex, final Timestamp x) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                result.updateTimestamp(columnIndex, x);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void updateAsciiStream(final int columnIndex, final InputStream x, final int length) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                result.updateAsciiStream(columnIndex, x, length);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void updateBinaryStream(final int columnIndex, final InputStream x, final int length) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                result.updateBinaryStream(columnIndex, x, length);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void updateCharacterStream(final int columnIndex, final Reader x, final int length) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                result.updateCharacterStream(columnIndex, x, length);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void updateObject(final int columnIndex, final Object x, final int scaleOrLength) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                result.updateObject(columnIndex, x, scaleOrLength);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void updateObject(final int columnIndex, final Object x) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                result.updateObject(columnIndex, x);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void updateNull(final String columnLabel) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                result.updateNull(columnLabel);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void updateBoolean(final String columnLabel, final boolean x) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                result.updateBoolean(columnLabel, x);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void updateByte(final String columnLabel, final byte x) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                result.updateByte(columnLabel, x);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void updateShort(final String columnLabel, final short x) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                result.updateShort(columnLabel, x);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void updateInt(final String columnLabel, final int x) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                result.updateInt(columnLabel, x);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void updateLong(final String columnLabel, final long x) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                result.updateLong(columnLabel, x);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void updateFloat(final String columnLabel, final float x) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                result.updateFloat(columnLabel, x);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void updateDouble(final String columnLabel, final double x) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                result.updateDouble(columnLabel, x);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void updateBigDecimal(final String columnLabel, final BigDecimal x) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                result.updateBigDecimal(columnLabel, x);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void updateString(final String columnLabel, final String x) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                result.updateString(columnLabel, x);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void updateBytes(final String columnLabel, final byte[] x) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                result.updateBytes(columnLabel, x);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void updateDate(final String columnLabel, final Date x) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                result.updateDate(columnLabel, x);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void updateTime(final String columnLabel, final Time x) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                result.updateTime(columnLabel, x);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void updateTimestamp(final String columnLabel, final Timestamp x) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                result.updateTimestamp(columnLabel, x);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void updateAsciiStream(final String columnLabel, final InputStream x, final int length) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                result.updateAsciiStream(columnLabel, x, length);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void updateBinaryStream(final String columnLabel, final InputStream x, final int length) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                result.updateBinaryStream(columnLabel, x, length);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void updateCharacterStream(final String columnLabel, final Reader reader, final int length) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                result.updateCharacterStream(columnLabel, reader, length);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void updateObject(final String columnLabel, final Object x, final int scaleOrLength) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                result.updateObject(columnLabel, x, scaleOrLength);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void updateObject(final String columnLabel, final Object x) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                result.updateObject(columnLabel, x);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void insertRow() throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                result.insertRow();
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void updateRow() throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                result.updateRow();
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void deleteRow() throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                result.deleteRow();
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void refreshRow() throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                result.refreshRow();
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void cancelRowUpdates() throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                result.cancelRowUpdates();
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void moveToInsertRow() throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                result.moveToInsertRow();
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void moveToCurrentRow() throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                result.moveToCurrentRow();
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public Statement getStatement() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Statement, SQLException>() {
            @Override
            public Statement call() throws SQLException {
                return result.getStatement();
            }
        });
    }

    @Override
    @Suspendable
    public Object getObject(final int columnIndex, final Map<String, Class<?>> map) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Object, SQLException>() {
            @Override
            public Object call() throws SQLException {
                return result.getObject(columnIndex, map);
            }
        });
    }

    @Override
    @Suspendable
    public Ref getRef(final int columnIndex) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Ref, SQLException>() {
            @Override
            public Ref call() throws SQLException {
                return result.getRef(columnIndex);
            }
        });
    }

    @Override
    @Suspendable
    public Blob getBlob(final int columnIndex) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Blob, SQLException>() {
            @Override
            public Blob call() throws SQLException {
                return result.getBlob(columnIndex);
            }
        });
    }

    @Override
    @Suspendable
    public Clob getClob(final int columnIndex) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Clob, SQLException>() {
            @Override
            public Clob call() throws SQLException {
                return result.getClob(columnIndex);
            }
        });
    }

    @Override
    @Suspendable
    public Array getArray(final int columnIndex) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Array, SQLException>() {
            @Override
            public Array call() throws SQLException {
                return result.getArray(columnIndex);
            }
        });
    }

    @Override
    @Suspendable
    public Object getObject(final String columnLabel, final Map<String, Class<?>> map) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Object, SQLException>() {
            @Override
            public Object call() throws SQLException {
                return result.getObject(columnLabel, map);
            }
        });
    }

    @Override
    @Suspendable
    public Ref getRef(final String columnLabel) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Ref, SQLException>() {
            @Override
            public Ref call() throws SQLException {
                return result.getRef(columnLabel);
            }
        });
    }

    @Override
    @Suspendable
    public Blob getBlob(final String columnLabel) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Blob, SQLException>() {
            @Override
            public Blob call() throws SQLException {
                return result.getBlob(columnLabel);
            }
        });
    }

    @Override
    @Suspendable
    public Clob getClob(final String columnLabel) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Clob, SQLException>() {
            @Override
            public Clob call() throws SQLException {
                return result.getClob(columnLabel);
            }
        });
    }

    @Override
    @Suspendable
    public Array getArray(final String columnLabel) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Array, SQLException>() {
            @Override
            public Array call() throws SQLException {
                return result.getArray(columnLabel);
            }
        });
    }

    @Override
    @Suspendable
    public Date getDate(final int columnIndex, final Calendar cal) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Date, SQLException>() {
            @Override
            public Date call() throws SQLException {
                return result.getDate(columnIndex, cal);
            }
        });
    }

    @Override
    @Suspendable
    public Date getDate(final String columnLabel, final Calendar cal) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Date, SQLException>() {
            @Override
            public Date call() throws SQLException {
                return result.getDate(columnLabel, cal);
            }
        });
    }

    @Override
    @Suspendable
    public Time getTime(final int columnIndex, final Calendar cal) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Time, SQLException>() {
            @Override
            public Time call() throws SQLException {
                return result.getTime(columnIndex, cal);
            }
        });
    }

    @Override
    @Suspendable
    public Time getTime(final String columnLabel, final Calendar cal) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Time, SQLException>() {
            @Override
            public Time call() throws SQLException {
                return result.getTime(columnLabel, cal);
            }
        });
    }

    @Override
    @Suspendable
    public Timestamp getTimestamp(final int columnIndex, final Calendar cal) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Timestamp, SQLException>() {
            @Override
            public Timestamp call() throws SQLException {
                return result.getTimestamp(columnIndex, cal);
            }
        });
    }

    @Override
    @Suspendable
    public Timestamp getTimestamp(final String columnLabel, final Calendar cal) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Timestamp, SQLException>() {
            @Override
            public Timestamp call() throws SQLException {
                return result.getTimestamp(columnLabel, cal);
            }
        });
    }

    @Override
    @Suspendable
    public URL getURL(final int columnIndex) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<URL, SQLException>() {
            @Override
            public URL call() throws SQLException {
                return result.getURL(columnIndex);
            }
        });
    }

    @Override
    @Suspendable
    public URL getURL(final String columnLabel) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<URL, SQLException>() {
            @Override
            public URL call() throws SQLException {
                return result.getURL(columnLabel);
            }
        });
    }

    @Override
    @Suspendable
    public void updateRef(final int columnIndex, final Ref x) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                result.updateRef(columnIndex, x);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void updateRef(final String columnLabel, final Ref x) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                result.updateRef(columnLabel, x);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void updateBlob(final int columnIndex, final Blob x) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                result.updateBlob(columnIndex, x);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void updateBlob(final String columnLabel, final Blob x) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                result.updateBlob(columnLabel, x);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void updateClob(final int columnIndex, final Clob x) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                result.updateClob(columnIndex, x);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void updateClob(final String columnLabel, final Clob x) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                result.updateClob(columnLabel, x);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void updateArray(final int columnIndex, final Array x) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                result.updateArray(columnIndex, x);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void updateArray(final String columnLabel, final Array x) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                result.updateArray(columnLabel, x);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public RowId getRowId(final int columnIndex) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<RowId, SQLException>() {
            @Override
            public RowId call() throws SQLException {
                return result.getRowId(columnIndex);
            }
        });
    }

    @Override
    @Suspendable
    public RowId getRowId(final String columnLabel) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<RowId, SQLException>() {
            @Override
            public RowId call() throws SQLException {
                return result.getRowId(columnLabel);
            }
        });
    }

    @Override
    @Suspendable
    public void updateRowId(final int columnIndex, final RowId x) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                result.updateRowId(columnIndex, x);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void updateRowId(final String columnLabel, final RowId x) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                result.updateRowId(columnLabel, x);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public int getHoldability() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Integer, SQLException>() {
            @Override
            public Integer call() throws SQLException {
                return result.getHoldability();
            }
        });
    }

    @Override
    @Suspendable
    public boolean isClosed() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Boolean, SQLException>() {
            @Override
            public Boolean call() throws SQLException {
                return result.isClosed();
            }
        });
    }

    @Override
    @Suspendable
    public void updateNString(final int columnIndex, final String nString) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                result.updateNString(columnIndex, nString);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void updateNString(final String columnLabel, final String nString) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                result.updateNString(columnLabel, nString);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void updateNClob(final int columnIndex, final NClob nClob) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                result.updateNClob(columnIndex, nClob);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void updateNClob(final String columnLabel, final NClob nClob) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                result.updateNClob(columnLabel, nClob);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public NClob getNClob(final int columnIndex) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<NClob, SQLException>() {
            @Override
            public NClob call() throws SQLException {
                return result.getNClob(columnIndex);
            }
        });
    }

    @Override
    @Suspendable
    public NClob getNClob(final String columnLabel) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<NClob, SQLException>() {
            @Override
            public NClob call() throws SQLException {
                return result.getNClob(columnLabel);
            }
        });
    }

    @Override
    @Suspendable
    public SQLXML getSQLXML(final int columnIndex) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<SQLXML, SQLException>() {
            @Override
            public SQLXML call() throws SQLException {
                return result.getSQLXML(columnIndex);
            }
        });
    }

    @Override
    @Suspendable
    public SQLXML getSQLXML(final String columnLabel) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<SQLXML, SQLException>() {
            @Override
            public SQLXML call() throws SQLException {
                return result.getSQLXML(columnLabel);
            }
        });
    }

    @Override
    @Suspendable
    public void updateSQLXML(final int columnIndex, final SQLXML xmlObject) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                result.updateSQLXML(columnIndex, xmlObject);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void updateSQLXML(final String columnLabel, final SQLXML xmlObject) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                result.updateSQLXML(columnLabel, xmlObject);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public String getNString(final int columnIndex) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<String, SQLException>() {
            @Override
            public String call() throws SQLException {
                return result.getNString(columnIndex);
            }
        });
    }

    @Override
    @Suspendable
    public String getNString(final String columnLabel) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<String, SQLException>() {
            @Override
            public String call() throws SQLException {
                return result.getNString(columnLabel);
            }
        });
    }

    @Override
    @Suspendable
    public Reader getNCharacterStream(final int columnIndex) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Reader, SQLException>() {
            @Override
            public Reader call() throws SQLException {
                return result.getNCharacterStream(columnIndex);
            }
        });
    }

    @Override
    @Suspendable
    public Reader getNCharacterStream(final String columnLabel) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Reader, SQLException>() {
            @Override
            public Reader call() throws SQLException {
                return result.getNCharacterStream(columnLabel);
            }
        });
    }

    @Override
    @Suspendable
    public void updateNCharacterStream(final int columnIndex, final Reader x, final long length) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                result.updateNCharacterStream(columnIndex, x, length);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void updateNCharacterStream(final String columnLabel, final Reader reader, final long length) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                result.updateNCharacterStream(columnLabel, reader, length);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void updateAsciiStream(final int columnIndex, final InputStream x, final long length) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                result.updateAsciiStream(columnIndex, x, length);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void updateBinaryStream(final int columnIndex, final InputStream x, final long length) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                result.updateBinaryStream(columnIndex, x, length);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void updateCharacterStream(final int columnIndex, final Reader x, final long length) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                result.updateCharacterStream(columnIndex, x, length);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void updateAsciiStream(final String columnLabel, final InputStream x, final long length) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                result.updateAsciiStream(columnLabel, x, length);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void updateBinaryStream(final String columnLabel, final InputStream x, final long length) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                result.updateBinaryStream(columnLabel, x, length);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void updateCharacterStream(final String columnLabel, final Reader reader, final long length) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                result.updateCharacterStream(columnLabel, reader, length);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void updateBlob(final int columnIndex, final InputStream inputStream, final long length) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                result.updateBlob(columnIndex, inputStream, length);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void updateBlob(final String columnLabel, final InputStream inputStream, final long length) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                result.updateBlob(columnLabel, inputStream, length);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void updateClob(final int columnIndex, final Reader reader, final long length) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                result.updateClob(columnIndex, reader, length);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void updateClob(final String columnLabel, final Reader reader, final long length) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                result.updateClob(columnLabel, reader, length);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void updateNClob(final int columnIndex, final Reader reader, final long length) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                result.updateNClob(columnIndex, reader, length);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void updateNClob(final String columnLabel, final Reader reader, final long length) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                result.updateNClob(columnLabel, reader, length);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void updateNCharacterStream(final int columnIndex, final Reader x) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                result.updateNCharacterStream(columnIndex, x);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void updateNCharacterStream(final String columnLabel, final Reader reader) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                result.updateNCharacterStream(columnLabel, reader);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void updateAsciiStream(final int columnIndex, final InputStream x) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                result.updateAsciiStream(columnIndex, x);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void updateBinaryStream(final int columnIndex, final InputStream x) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                result.updateBinaryStream(columnIndex, x);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void updateCharacterStream(final int columnIndex, final Reader x) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                result.updateCharacterStream(columnIndex, x);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void updateAsciiStream(final String columnLabel, final InputStream x) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                result.updateAsciiStream(columnLabel, x);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void updateBinaryStream(final String columnLabel, final InputStream x) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                result.updateBinaryStream(columnLabel, x);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void updateCharacterStream(final String columnLabel, final Reader reader) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                result.updateCharacterStream(columnLabel, reader);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void updateBlob(final int columnIndex, final InputStream inputStream) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                result.updateBlob(columnIndex, inputStream);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void updateBlob(final String columnLabel, final InputStream inputStream) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                result.updateBlob(columnLabel, inputStream);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void updateClob(final int columnIndex, final Reader reader) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                result.updateClob(columnIndex, reader);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void updateClob(final String columnLabel, final Reader reader) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                result.updateClob(columnLabel, reader);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void updateNClob(final int columnIndex, final Reader reader) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                result.updateNClob(columnIndex, reader);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public void updateNClob(final String columnLabel, final Reader reader) throws SQLException {
        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
            @Override
            public Void call() throws SQLException {
                result.updateNClob(columnLabel, reader);
                return null;
            }
        });
    }

    @Override
    @Suspendable
    public <T> T getObject(final int columnIndex, final Class<T> type) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<T, SQLException>() {
            @Override
            public T call() throws SQLException {
                return result.getObject(columnIndex, type);
            }
        });
    }

    @Override
    @Suspendable
    public <T> T getObject(final String columnLabel, final Class<T> type) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<T, SQLException>() {
            @Override
            public T call() throws SQLException {
                return result.getObject(columnLabel, type);
            }
        });
    }

//    1.8, have default impl.
//  
//    @Override
//    @Suspendable
//    public void updateObject(final int columnIndex, final Object x, final SQLType targetSqlType, final int scaleOrLength) throws SQLException {
//        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
//            @Override
//            public Void call() throws SQLException {
//                result.updateObject(columnIndex, x, targetSqlType, scaleOrLength);
//                return null;
//            }
//        });
//    }
//
//    @Override
//    @Suspendable
//    public void updateObject(final String columnLabel, final Object x, final SQLType targetSqlType, final int scaleOrLength) throws SQLException {
//        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
//            @Override
//            public Void call() throws SQLException {
//                result.updateObject(columnLabel, x, targetSqlType, scaleOrLength);
//                return null;
//            }
//        });
//    }
//
//    @Override
//    @Suspendable
//    public void updateObject(final int columnIndex, final Object x, final SQLType targetSqlType) throws SQLException {
//        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
//            @Override
//            public Void call() throws SQLException {
//                result.updateObject(columnIndex, x, targetSqlType);
//                return null;
//            }
//        });
//    }
//
//    @Override
//    @Suspendable
//    public void updateObject(final String columnLabel, final Object x, final SQLType targetSqlType) throws SQLException {
//        JDBCFiberAsync.exec(executor, new CheckedCallable<Void, SQLException>() {
//            @Override
//            public Void call() throws SQLException {
//                result.updateObject(columnLabel, x, targetSqlType);
//                return null;
//            }
//        });
//    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        return result.unwrap(iface);
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return result.isWrapperFor(iface);
    }
}
