package co.paralleluniverse.fibers.jdbi;

import co.paralleluniverse.common.util.CheckedCallable;
import co.paralleluniverse.fibers.FiberAsync;
import co.paralleluniverse.fibers.Suspendable;
import co.paralleluniverse.fibers.jdbc.FiberDataSource;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.ExecutorService;
import javax.sql.DataSource;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.IDBI;
import org.skife.jdbi.v2.TransactionCallback;
import org.skife.jdbi.v2.TransactionIsolationLevel;
import org.skife.jdbi.v2.exceptions.CallbackFailedException;
import org.skife.jdbi.v2.tweak.HandleCallback;

public class FiberDBI implements IDBI {
    private final IDBI jdbi;
    private ExecutorService es;

    /**
     *
     * @param jdbi jdbi based on FiberDataSource
     * @param es
     */
    public FiberDBI(IDBI jdbi, ExecutorService es) {
        this.jdbi = jdbi;
        this.es = es;
    }

    /**
     * Constructor for use with a DataSource which will provide
     *
     * @param dataSource may or may not be FiberDataSource
     * @param es
     */
    public FiberDBI(DataSource dataSource, ExecutorService es) {
        this(dataSource instanceof FiberDataSource ? new DBI(dataSource)
                : new DBI(new FiberDataSource(dataSource, es)), es);
    }

    @Suspendable
    @Override
    public Handle open() {
        return jdbi.open();
    }

    @Override
    public void define(String key, Object value) {
        jdbi.define(key, value);
    }

    @Suspendable
    @Override
    public <ReturnType> ReturnType withHandle(HandleCallback<ReturnType> callback) throws CallbackFailedException {
        return jdbi.withHandle(callback);
    }

    @Override
    public <ReturnType> ReturnType inTransaction(TransactionCallback<ReturnType> callback) throws CallbackFailedException {
        return jdbi.inTransaction(callback);
    }

    @Override
    public <ReturnType> ReturnType inTransaction(TransactionIsolationLevel isolation, TransactionCallback<ReturnType> callback) throws CallbackFailedException {
        return jdbi.inTransaction(isolation, callback);
    }

    @Suspendable
    @Override
    public <SqlObjectType> SqlObjectType open(Class<SqlObjectType> sqlObjectType) {
        return jdbi.open(sqlObjectType);
    }

    @Override
    public <SqlObjectType> SqlObjectType onDemand(Class<SqlObjectType> sqlObjectType) {
        final SqlObjectType onDemand = jdbi.onDemand(sqlObjectType);
        return (SqlObjectType) Proxy.newProxyInstance(ClassLoader.getSystemClassLoader(), new Class[]{sqlObjectType}, new InvocationHandler() {
            @Suspendable
            @Override
            public Object invoke(Object proxy, final Method method, final Object[] args) throws Throwable {
                return FiberAsync.runBlocking(es, new CheckedCallable<Object, Exception>() {
                    @Override
                    public Object call() throws Exception {
                        return method.invoke(onDemand, args);
                    }
                });
            }
        });
    }

    @Override
    public void close(Object sqlObject) {
        jdbi.close(sqlObject);
    }
}
