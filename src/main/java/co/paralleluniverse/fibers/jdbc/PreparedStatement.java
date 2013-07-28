/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.fibers.jdbc;

import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.futures.FiberAsyncListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.Date;
import java.sql.NClob;
import java.sql.ParameterMetaData;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Wrapper;
import java.util.Calendar;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

/**
 *
 * @author eitan
 */
public class PreparedStatement implements Wrapper, AutoCloseable {
    private final java.sql.PreparedStatement ps;
    private final ListeningExecutorService exec;

    public ResultSet executeQuery(final String sql) throws SQLException, SuspendExecution {
        try {
            return FiberAsyncListenableFuture.get(exec.submit(new Callable<ResultSet>() {
                @Override
                public ResultSet call() throws Exception {
                    return ps.executeQuery(sql);
                }
            }));
        } catch (InterruptedException | ExecutionException ex) {
            throw new RuntimeException(ex);
        }
    }

    public int executeUpdate(final String sql) throws SQLException, SuspendExecution {
        try {
            return FiberAsyncListenableFuture.get(exec.submit(new Callable<Integer>() {
                @Override
                public Integer call() throws Exception {
                    return ps.executeUpdate(sql);
                }
            }));
        } catch (InterruptedException | ExecutionException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void close() throws SQLException {
        ps.close();
    }

    public int getMaxFieldSize() throws SQLException {
        return ps.getMaxFieldSize();
    }

    public void setMaxFieldSize(int max) throws SQLException {
        ps.setMaxFieldSize(max);
    }

    public int getMaxRows() throws SQLException {
        return ps.getMaxRows();
    }

    public void setMaxRows(int max) throws SQLException {
        ps.setMaxRows(max);
    }

    public void setEscapeProcessing(boolean enable) throws SQLException {
        ps.setEscapeProcessing(enable);
    }

    public int getQueryTimeout() throws SQLException {
        return ps.getQueryTimeout();
    }

    public void setQueryTimeout(int seconds) throws SQLException {
        ps.setQueryTimeout(seconds);
    }

    public void cancel() throws SQLException {
        ps.cancel();
    }

    public SQLWarning getWarnings() throws SQLException {
        return ps.getWarnings();
    }

    public void clearWarnings() throws SQLException {
        ps.clearWarnings();
    }

    public void setCursorName(String name) throws SQLException {
        ps.setCursorName(name);
    }

    public boolean execute(String sql) throws SQLException {
        return ps.execute(sql);
    }

    public ResultSet getResultSet() throws SQLException, SuspendExecution {
        try {
            return FiberAsyncListenableFuture.get(exec.submit(new Callable<ResultSet>() {
                @Override
                public ResultSet call() throws Exception {
                    return ps.getResultSet();
                }
            }));
        } catch (InterruptedException | ExecutionException ex) {
            throw new RuntimeException(ex);
        }
    }

    public int getUpdateCount() throws SQLException {
        return ps.getUpdateCount();
    }

    public boolean getMoreResults() throws SQLException {
        return ps.getMoreResults();
    }

    public void setFetchDirection(int direction) throws SQLException {
        ps.setFetchDirection(direction);
    }

    public int getFetchDirection() throws SQLException {
        return ps.getFetchDirection();
    }

    public void setFetchSize(int rows) throws SQLException {
        ps.setFetchSize(rows);
    }

    public int getFetchSize() throws SQLException {
        return ps.getFetchSize();
    }

    public int getResultSetConcurrency() throws SQLException {
        return ps.getResultSetConcurrency();
    }

    public int getResultSetType() throws SQLException {
        return ps.getResultSetType();
    }

    public void addBatch(String sql) throws SQLException {
        ps.addBatch(sql);
    }

    public void clearBatch() throws SQLException {
        ps.clearBatch();
    }

    public int[] executeBatch() throws SQLException, SuspendExecution {
        try {
            return FiberAsyncListenableFuture.get(exec.submit(new Callable<int[]>() {
                @Override
                public int[] call() throws Exception {
                    return ps.executeBatch();
                }
            }));
        } catch (InterruptedException | ExecutionException ex) {
            throw new RuntimeException(ex);
        }
    }

    public Connection getConnection() throws SQLException {
        return ps.getConnection();
    }

    public boolean getMoreResults(int current) throws SQLException {
        return ps.getMoreResults(current);
    }

    public ResultSet getGeneratedKeys() throws SQLException {
        return ps.getGeneratedKeys();
    }

    public int executeUpdate(final String sql, final int autoGeneratedKeys) throws SQLException, SuspendExecution {
        try {
            return FiberAsyncListenableFuture.get(exec.submit(new Callable<Integer>() {
                @Override
                public Integer call() throws Exception {
                    return ps.executeUpdate(sql, autoGeneratedKeys);
                }
            }));
        } catch (InterruptedException | ExecutionException ex) {
            throw new RuntimeException(ex);
        }
    }

    public int executeUpdate(final String sql, final int[] columnIndexes) throws SQLException, SuspendExecution {
        try {
            return FiberAsyncListenableFuture.get(exec.submit(new Callable<Integer>() {
                @Override
                public Integer call() throws Exception {
                    return ps.executeUpdate(sql, columnIndexes);
                }
            }));
        } catch (InterruptedException | ExecutionException ex) {
            throw new RuntimeException(ex);
        }
    }

    public int executeUpdate(final String sql, final String[] columnNames) throws SQLException, SuspendExecution {
        try {
            return FiberAsyncListenableFuture.get(exec.submit(new Callable<Integer>() {
                @Override
                public Integer call() throws Exception {
                    return ps.executeUpdate(sql, columnNames);
                }
            }));
        } catch (InterruptedException | ExecutionException ex) {
            throw new RuntimeException(ex);
        }
    }

    public boolean execute(final String sql, final int autoGeneratedKeys) throws SQLException, SuspendExecution {
        try {
            return FiberAsyncListenableFuture.get(exec.submit(new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    return ps.execute(sql, autoGeneratedKeys);
                }
            }));
        } catch (InterruptedException | ExecutionException ex) {
            throw new RuntimeException(ex);
        }
    }

    public boolean execute(final String sql, final int[] columnIndexes) throws SQLException, SuspendExecution {
        try {
            return FiberAsyncListenableFuture.get(exec.submit(new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    return ps.execute(sql, columnIndexes);
                }
            }));
        } catch (InterruptedException | ExecutionException ex) {
            throw new RuntimeException(ex);
        }
    }

    public boolean execute(final String sql, final String[] columnNames) throws SQLException, SuspendExecution {
        try {
            return FiberAsyncListenableFuture.get(exec.submit(new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    return ps.execute(sql, columnNames);
                }
            }));
        } catch (InterruptedException | ExecutionException ex) {
            throw new RuntimeException(ex);
        }
    }

    public int getResultSetHoldability() throws SQLException {
        return ps.getResultSetHoldability();
    }

    public boolean isClosed() throws SQLException {
        return ps.isClosed();
    }

    public void setPoolable(boolean poolable) throws SQLException {
        ps.setPoolable(poolable);
    }

    public boolean isPoolable() throws SQLException {
        return ps.isPoolable();
    }

    public void closeOnCompletion() throws SQLException {
        ps.closeOnCompletion();
    }

    public boolean isCloseOnCompletion() throws SQLException {
        return ps.isCloseOnCompletion();
    }

    public PreparedStatement(java.sql.PreparedStatement ps, ListeningExecutorService exec) {
        this.ps = ps;
        this.exec = exec;
    }

    public ResultSet executeQuery() throws SQLException, SuspendExecution {
        try {
            return FiberAsyncListenableFuture.get(exec.submit(new Callable<ResultSet>() {
                @Override
                public ResultSet call() throws Exception {
                    return ps.executeQuery();
                }
            }));
        } catch (InterruptedException | ExecutionException ex) {
            throw new RuntimeException(ex);
        }
    }

    public int executeUpdate() throws SQLException, SuspendExecution {
        try {
            return FiberAsyncListenableFuture.get(exec.submit(new Callable<Integer>() {
                @Override
                public Integer call() throws Exception {
                    return ps.executeUpdate();
                }
            }));
        } catch (InterruptedException | ExecutionException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void setNull(int parameterIndex, int sqlType) throws SQLException {
        ps.setNull(parameterIndex, sqlType);
    }

    public void setBoolean(int parameterIndex, boolean x) throws SQLException {
        ps.setBoolean(parameterIndex, x);
    }

    public void setByte(int parameterIndex, byte x) throws SQLException {
        ps.setByte(parameterIndex, x);
    }

    public void setShort(int parameterIndex, short x) throws SQLException {
        ps.setShort(parameterIndex, x);
    }

    public void setInt(int parameterIndex, int x) throws SQLException {
        ps.setInt(parameterIndex, x);
    }

    public void setLong(int parameterIndex, long x) throws SQLException {
        ps.setLong(parameterIndex, x);
    }

    public void setFloat(int parameterIndex, float x) throws SQLException {
        ps.setFloat(parameterIndex, x);
    }

    public void setDouble(int parameterIndex, double x) throws SQLException {
        ps.setDouble(parameterIndex, x);
    }

    public void setBigDecimal(int parameterIndex, BigDecimal x) throws SQLException {
        ps.setBigDecimal(parameterIndex, x);
    }

    public void setString(int parameterIndex, String x) throws SQLException {
        ps.setString(parameterIndex, x);
    }

    public void setBytes(int parameterIndex, byte[] x) throws SQLException {
        ps.setBytes(parameterIndex, x);
    }

    public void setDate(int parameterIndex, Date x) throws SQLException {
        ps.setDate(parameterIndex, x);
    }

    public void setTime(int parameterIndex, Time x) throws SQLException {
        ps.setTime(parameterIndex, x);
    }

    public void setTimestamp(int parameterIndex, Timestamp x) throws SQLException {
        ps.setTimestamp(parameterIndex, x);
    }

    public void setAsciiStream(int parameterIndex, InputStream x, int length) throws SQLException {
        ps.setAsciiStream(parameterIndex, x, length);
    }

    public void setUnicodeStream(int parameterIndex, InputStream x, int length) throws SQLException {
        ps.setUnicodeStream(parameterIndex, x, length);
    }

    public void setBinaryStream(int parameterIndex, InputStream x, int length) throws SQLException {
        ps.setBinaryStream(parameterIndex, x, length);
    }

    public void clearParameters() throws SQLException {
        ps.clearParameters();
    }

    public void setObject(int parameterIndex, Object x, int targetSqlType) throws SQLException {
        ps.setObject(parameterIndex, x, targetSqlType);
    }

    public void setObject(int parameterIndex, Object x) throws SQLException {
        ps.setObject(parameterIndex, x);
    }

    public boolean execute() throws SQLException, SuspendExecution {
        try {
            return FiberAsyncListenableFuture.get(exec.submit(new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    return ps.execute();
                }
            }));
        } catch (InterruptedException | ExecutionException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void addBatch() throws SQLException {
        ps.addBatch();
    }

    public void setCharacterStream(int parameterIndex, Reader reader, int length) throws SQLException {
        ps.setCharacterStream(parameterIndex, reader, length);
    }

    public void setRef(int parameterIndex, Ref x) throws SQLException {
        ps.setRef(parameterIndex, x);
    }

    public void setBlob(int parameterIndex, Blob x) throws SQLException {
        ps.setBlob(parameterIndex, x);
    }

    public void setClob(int parameterIndex, Clob x) throws SQLException {
        ps.setClob(parameterIndex, x);
    }

    public void setArray(int parameterIndex, Array x) throws SQLException {
        ps.setArray(parameterIndex, x);
    }

    public ResultSetMetaData getMetaData() throws SQLException {
        return ps.getMetaData();
    }

    public void setDate(int parameterIndex, Date x, Calendar cal) throws SQLException {
        ps.setDate(parameterIndex, x, cal);
    }

    public void setTime(int parameterIndex, Time x, Calendar cal) throws SQLException {
        ps.setTime(parameterIndex, x, cal);
    }

    public void setTimestamp(int parameterIndex, Timestamp x, Calendar cal) throws SQLException {
        ps.setTimestamp(parameterIndex, x, cal);
    }

    public void setNull(int parameterIndex, int sqlType, String typeName) throws SQLException {
        ps.setNull(parameterIndex, sqlType, typeName);
    }

    public void setURL(int parameterIndex, URL x) throws SQLException {
        ps.setURL(parameterIndex, x);
    }

    public ParameterMetaData getParameterMetaData() throws SQLException {
        return ps.getParameterMetaData();
    }

    public void setRowId(int parameterIndex, RowId x) throws SQLException {
        ps.setRowId(parameterIndex, x);
    }

    public void setNString(int parameterIndex, String value) throws SQLException {
        ps.setNString(parameterIndex, value);
    }

    public void setNCharacterStream(int parameterIndex, Reader value, long length) throws SQLException {
        ps.setNCharacterStream(parameterIndex, value, length);
    }

    public void setNClob(int parameterIndex, NClob value) throws SQLException {
        ps.setNClob(parameterIndex, value);
    }

    public void setClob(int parameterIndex, Reader reader, long length) throws SQLException {
        ps.setClob(parameterIndex, reader, length);
    }

    public void setBlob(int parameterIndex, InputStream inputStream, long length) throws SQLException {
        ps.setBlob(parameterIndex, inputStream, length);
    }

    public void setNClob(int parameterIndex, Reader reader, long length) throws SQLException {
        ps.setNClob(parameterIndex, reader, length);
    }

    public void setSQLXML(int parameterIndex, SQLXML xmlObject) throws SQLException {
        ps.setSQLXML(parameterIndex, xmlObject);
    }

    public void setObject(int parameterIndex, Object x, int targetSqlType, int scaleOrLength) throws SQLException {
        ps.setObject(parameterIndex, x, targetSqlType, scaleOrLength);
    }

    public void setAsciiStream(int parameterIndex, InputStream x, long length) throws SQLException {
        ps.setAsciiStream(parameterIndex, x, length);
    }

    public void setBinaryStream(int parameterIndex, InputStream x, long length) throws SQLException {
        ps.setBinaryStream(parameterIndex, x, length);
    }

    public void setCharacterStream(int parameterIndex, Reader reader, long length) throws SQLException {
        ps.setCharacterStream(parameterIndex, reader, length);
    }

    public void setAsciiStream(int parameterIndex, InputStream x) throws SQLException {
        ps.setAsciiStream(parameterIndex, x);
    }

    public void setBinaryStream(int parameterIndex, InputStream x) throws SQLException {
        ps.setBinaryStream(parameterIndex, x);
    }

    public void setCharacterStream(int parameterIndex, Reader reader) throws SQLException {
        ps.setCharacterStream(parameterIndex, reader);
    }

    public void setNCharacterStream(int parameterIndex, Reader value) throws SQLException {
        ps.setNCharacterStream(parameterIndex, value);
    }

    public void setClob(int parameterIndex, Reader reader) throws SQLException {
        ps.setClob(parameterIndex, reader);
    }

    public void setBlob(int parameterIndex, InputStream inputStream) throws SQLException {
        ps.setBlob(parameterIndex, inputStream);
    }

    public void setNClob(int parameterIndex, Reader reader) throws SQLException {
        ps.setNClob(parameterIndex, reader);
    }

    public <T> T unwrap(Class<T> iface) throws SQLException {
        return ps.unwrap(iface);
    }

    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return ps.isWrapperFor(iface);
    }

    public int hashCode() {
        return ps.hashCode();
    }

    public boolean equals(Object obj) {
        return ps.equals(obj);
    }

    public String toString() {
        return ps.toString();
    }
}
