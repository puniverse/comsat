/*
 * COMSAT
 * Copyright (C) 2013, Parallel Universe Software Co. All rights reserved.
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

import com.google.common.util.concurrent.ListeningExecutorService;
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
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLXML;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Map;

/**
 *
 * @author eitan
 */
public class CallableStatement extends PreparedStatement {
    public CallableStatement(java.sql.CallableStatement cs, ListeningExecutorService exec) {
        super(cs, exec);
    }

    @Override
    java.sql.CallableStatement stmt() {
        return (java.sql.CallableStatement) super.stmt();
    }

    //Delegations
    public void registerOutParameter(int parameterIndex, int sqlType) throws SQLException {
        stmt().registerOutParameter(parameterIndex, sqlType);
    }

    public void registerOutParameter(int parameterIndex, int sqlType, int scale) throws SQLException {
        stmt().registerOutParameter(parameterIndex, sqlType, scale);
    }

    public boolean wasNull() throws SQLException {
        return stmt().wasNull();
    }

    public String getString(int parameterIndex) throws SQLException {
        return stmt().getString(parameterIndex);
    }

    public boolean getBoolean(int parameterIndex) throws SQLException {
        return stmt().getBoolean(parameterIndex);
    }

    public byte getByte(int parameterIndex) throws SQLException {
        return stmt().getByte(parameterIndex);
    }

    public short getShort(int parameterIndex) throws SQLException {
        return stmt().getShort(parameterIndex);
    }

    public int getInt(int parameterIndex) throws SQLException {
        return stmt().getInt(parameterIndex);
    }

    public long getLong(int parameterIndex) throws SQLException {
        return stmt().getLong(parameterIndex);
    }

    public float getFloat(int parameterIndex) throws SQLException {
        return stmt().getFloat(parameterIndex);
    }

    public double getDouble(int parameterIndex) throws SQLException {
        return stmt().getDouble(parameterIndex);
    }

    public BigDecimal getBigDecimal(int parameterIndex, int scale) throws SQLException {
        return stmt().getBigDecimal(parameterIndex, scale);
    }

    public byte[] getBytes(int parameterIndex) throws SQLException {
        return stmt().getBytes(parameterIndex);
    }

    public Date getDate(int parameterIndex) throws SQLException {
        return stmt().getDate(parameterIndex);
    }

    public Time getTime(int parameterIndex) throws SQLException {
        return stmt().getTime(parameterIndex);
    }

    public Timestamp getTimestamp(int parameterIndex) throws SQLException {
        return stmt().getTimestamp(parameterIndex);
    }

    public Object getObject(int parameterIndex) throws SQLException {
        return stmt().getObject(parameterIndex);
    }

    public BigDecimal getBigDecimal(int parameterIndex) throws SQLException {
        return stmt().getBigDecimal(parameterIndex);
    }

    public Object getObject(int parameterIndex, Map<String, Class<?>> map) throws SQLException {
        return stmt().getObject(parameterIndex, map);
    }

    public Ref getRef(int parameterIndex) throws SQLException {
        return stmt().getRef(parameterIndex);
    }

    public Blob getBlob(int parameterIndex) throws SQLException {
        return stmt().getBlob(parameterIndex);
    }

    public Clob getClob(int parameterIndex) throws SQLException {
        return stmt().getClob(parameterIndex);
    }

    public Array getArray(int parameterIndex) throws SQLException {
        return stmt().getArray(parameterIndex);
    }

    public Date getDate(int parameterIndex, Calendar cal) throws SQLException {
        return stmt().getDate(parameterIndex, cal);
    }

    public Time getTime(int parameterIndex, Calendar cal) throws SQLException {
        return stmt().getTime(parameterIndex, cal);
    }

    public Timestamp getTimestamp(int parameterIndex, Calendar cal) throws SQLException {
        return stmt().getTimestamp(parameterIndex, cal);
    }

    public void registerOutParameter(int parameterIndex, int sqlType, String typeName) throws SQLException {
        stmt().registerOutParameter(parameterIndex, sqlType, typeName);
    }

    public void registerOutParameter(String parameterName, int sqlType) throws SQLException {
        stmt().registerOutParameter(parameterName, sqlType);
    }

    public void registerOutParameter(String parameterName, int sqlType, int scale) throws SQLException {
        stmt().registerOutParameter(parameterName, sqlType, scale);
    }

    public void registerOutParameter(String parameterName, int sqlType, String typeName) throws SQLException {
        stmt().registerOutParameter(parameterName, sqlType, typeName);
    }

    public URL getURL(int parameterIndex) throws SQLException {
        return stmt().getURL(parameterIndex);
    }

    public void setURL(String parameterName, URL val) throws SQLException {
        stmt().setURL(parameterName, val);
    }

    public void setNull(String parameterName, int sqlType) throws SQLException {
        stmt().setNull(parameterName, sqlType);
    }

    public void setBoolean(String parameterName, boolean x) throws SQLException {
        stmt().setBoolean(parameterName, x);
    }

    public void setByte(String parameterName, byte x) throws SQLException {
        stmt().setByte(parameterName, x);
    }

    public void setShort(String parameterName, short x) throws SQLException {
        stmt().setShort(parameterName, x);
    }

    public void setInt(String parameterName, int x) throws SQLException {
        stmt().setInt(parameterName, x);
    }

    public void setLong(String parameterName, long x) throws SQLException {
        stmt().setLong(parameterName, x);
    }

    public void setFloat(String parameterName, float x) throws SQLException {
        stmt().setFloat(parameterName, x);
    }

    public void setDouble(String parameterName, double x) throws SQLException {
        stmt().setDouble(parameterName, x);
    }

    public void setBigDecimal(String parameterName, BigDecimal x) throws SQLException {
        stmt().setBigDecimal(parameterName, x);
    }

    public void setString(String parameterName, String x) throws SQLException {
        stmt().setString(parameterName, x);
    }

    public void setBytes(String parameterName, byte[] x) throws SQLException {
        stmt().setBytes(parameterName, x);
    }

    public void setDate(String parameterName, Date x) throws SQLException {
        stmt().setDate(parameterName, x);
    }

    public void setTime(String parameterName, Time x) throws SQLException {
        stmt().setTime(parameterName, x);
    }

    public void setTimestamp(String parameterName, Timestamp x) throws SQLException {
        stmt().setTimestamp(parameterName, x);
    }

    public void setAsciiStream(String parameterName, InputStream x, int length) throws SQLException {
        stmt().setAsciiStream(parameterName, x, length);
    }

    public void setBinaryStream(String parameterName, InputStream x, int length) throws SQLException {
        stmt().setBinaryStream(parameterName, x, length);
    }

    public void setObject(String parameterName, Object x, int targetSqlType, int scale) throws SQLException {
        stmt().setObject(parameterName, x, targetSqlType, scale);
    }

    public void setObject(String parameterName, Object x, int targetSqlType) throws SQLException {
        stmt().setObject(parameterName, x, targetSqlType);
    }

    public void setObject(String parameterName, Object x) throws SQLException {
        stmt().setObject(parameterName, x);
    }

    public void setCharacterStream(String parameterName, Reader reader, int length) throws SQLException {
        stmt().setCharacterStream(parameterName, reader, length);
    }

    public void setDate(String parameterName, Date x, Calendar cal) throws SQLException {
        stmt().setDate(parameterName, x, cal);
    }

    public void setTime(String parameterName, Time x, Calendar cal) throws SQLException {
        stmt().setTime(parameterName, x, cal);
    }

    public void setTimestamp(String parameterName, Timestamp x, Calendar cal) throws SQLException {
        stmt().setTimestamp(parameterName, x, cal);
    }

    public void setNull(String parameterName, int sqlType, String typeName) throws SQLException {
        stmt().setNull(parameterName, sqlType, typeName);
    }

    public String getString(String parameterName) throws SQLException {
        return stmt().getString(parameterName);
    }

    public boolean getBoolean(String parameterName) throws SQLException {
        return stmt().getBoolean(parameterName);
    }

    public byte getByte(String parameterName) throws SQLException {
        return stmt().getByte(parameterName);
    }

    public short getShort(String parameterName) throws SQLException {
        return stmt().getShort(parameterName);
    }

    public int getInt(String parameterName) throws SQLException {
        return stmt().getInt(parameterName);
    }

    public long getLong(String parameterName) throws SQLException {
        return stmt().getLong(parameterName);
    }

    public float getFloat(String parameterName) throws SQLException {
        return stmt().getFloat(parameterName);
    }

    public double getDouble(String parameterName) throws SQLException {
        return stmt().getDouble(parameterName);
    }

    public byte[] getBytes(String parameterName) throws SQLException {
        return stmt().getBytes(parameterName);
    }

    public Date getDate(String parameterName) throws SQLException {
        return stmt().getDate(parameterName);
    }

    public Time getTime(String parameterName) throws SQLException {
        return stmt().getTime(parameterName);
    }

    public Timestamp getTimestamp(String parameterName) throws SQLException {
        return stmt().getTimestamp(parameterName);
    }

    public Object getObject(String parameterName) throws SQLException {
        return stmt().getObject(parameterName);
    }

    public BigDecimal getBigDecimal(String parameterName) throws SQLException {
        return stmt().getBigDecimal(parameterName);
    }

    public Object getObject(String parameterName, Map<String, Class<?>> map) throws SQLException {
        return stmt().getObject(parameterName, map);
    }

    public Ref getRef(String parameterName) throws SQLException {
        return stmt().getRef(parameterName);
    }

    public Blob getBlob(String parameterName) throws SQLException {
        return stmt().getBlob(parameterName);
    }

    public Clob getClob(String parameterName) throws SQLException {
        return stmt().getClob(parameterName);
    }

    public Array getArray(String parameterName) throws SQLException {
        return stmt().getArray(parameterName);
    }

    public Date getDate(String parameterName, Calendar cal) throws SQLException {
        return stmt().getDate(parameterName, cal);
    }

    public Time getTime(String parameterName, Calendar cal) throws SQLException {
        return stmt().getTime(parameterName, cal);
    }

    public Timestamp getTimestamp(String parameterName, Calendar cal) throws SQLException {
        return stmt().getTimestamp(parameterName, cal);
    }

    public URL getURL(String parameterName) throws SQLException {
        return stmt().getURL(parameterName);
    }

    public RowId getRowId(int parameterIndex) throws SQLException {
        return stmt().getRowId(parameterIndex);
    }

    public RowId getRowId(String parameterName) throws SQLException {
        return stmt().getRowId(parameterName);
    }

    public void setRowId(String parameterName, RowId x) throws SQLException {
        stmt().setRowId(parameterName, x);
    }

    public void setNString(String parameterName, String value) throws SQLException {
        stmt().setNString(parameterName, value);
    }

    public void setNCharacterStream(String parameterName, Reader value, long length) throws SQLException {
        stmt().setNCharacterStream(parameterName, value, length);
    }

    public void setNClob(String parameterName, NClob value) throws SQLException {
        stmt().setNClob(parameterName, value);
    }

    public void setClob(String parameterName, Reader reader, long length) throws SQLException {
        stmt().setClob(parameterName, reader, length);
    }

    public void setBlob(String parameterName, InputStream inputStream, long length) throws SQLException {
        stmt().setBlob(parameterName, inputStream, length);
    }

    public void setNClob(String parameterName, Reader reader, long length) throws SQLException {
        stmt().setNClob(parameterName, reader, length);
    }

    public NClob getNClob(int parameterIndex) throws SQLException {
        return stmt().getNClob(parameterIndex);
    }

    public NClob getNClob(String parameterName) throws SQLException {
        return stmt().getNClob(parameterName);
    }

    public void setSQLXML(String parameterName, SQLXML xmlObject) throws SQLException {
        stmt().setSQLXML(parameterName, xmlObject);
    }

    public SQLXML getSQLXML(int parameterIndex) throws SQLException {
        return stmt().getSQLXML(parameterIndex);
    }

    public SQLXML getSQLXML(String parameterName) throws SQLException {
        return stmt().getSQLXML(parameterName);
    }

    public String getNString(int parameterIndex) throws SQLException {
        return stmt().getNString(parameterIndex);
    }

    public String getNString(String parameterName) throws SQLException {
        return stmt().getNString(parameterName);
    }

    public Reader getNCharacterStream(int parameterIndex) throws SQLException {
        return stmt().getNCharacterStream(parameterIndex);
    }

    public Reader getNCharacterStream(String parameterName) throws SQLException {
        return stmt().getNCharacterStream(parameterName);
    }

    public Reader getCharacterStream(int parameterIndex) throws SQLException {
        return stmt().getCharacterStream(parameterIndex);
    }

    public Reader getCharacterStream(String parameterName) throws SQLException {
        return stmt().getCharacterStream(parameterName);
    }

    public void setBlob(String parameterName, Blob x) throws SQLException {
        stmt().setBlob(parameterName, x);
    }

    public void setClob(String parameterName, Clob x) throws SQLException {
        stmt().setClob(parameterName, x);
    }

    public void setAsciiStream(String parameterName, InputStream x, long length) throws SQLException {
        stmt().setAsciiStream(parameterName, x, length);
    }

    public void setBinaryStream(String parameterName, InputStream x, long length) throws SQLException {
        stmt().setBinaryStream(parameterName, x, length);
    }

    public void setCharacterStream(String parameterName, Reader reader, long length) throws SQLException {
        stmt().setCharacterStream(parameterName, reader, length);
    }

    public void setAsciiStream(String parameterName, InputStream x) throws SQLException {
        stmt().setAsciiStream(parameterName, x);
    }

    public void setBinaryStream(String parameterName, InputStream x) throws SQLException {
        stmt().setBinaryStream(parameterName, x);
    }

    public void setCharacterStream(String parameterName, Reader reader) throws SQLException {
        stmt().setCharacterStream(parameterName, reader);
    }

    public void setNCharacterStream(String parameterName, Reader value) throws SQLException {
        stmt().setNCharacterStream(parameterName, value);
    }

    public void setClob(String parameterName, Reader reader) throws SQLException {
        stmt().setClob(parameterName, reader);
    }

    public void setBlob(String parameterName, InputStream inputStream) throws SQLException {
        stmt().setBlob(parameterName, inputStream);
    }

    public void setNClob(String parameterName, Reader reader) throws SQLException {
        stmt().setNClob(parameterName, reader);
    }

    public <T> T getObject(int parameterIndex, Class<T> type) throws SQLException {
        return stmt().getObject(parameterIndex, type);
    }

    public <T> T getObject(String parameterName, Class<T> type) throws SQLException {
        return stmt().getObject(parameterName, type);
    }
}
