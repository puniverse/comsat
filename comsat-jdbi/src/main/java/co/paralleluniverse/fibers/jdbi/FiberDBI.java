/*
 * COMSAT
 * Copyright (C) 2014-2015, Parallel Universe Software Co. All rights reserved.
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
package co.paralleluniverse.fibers.jdbi;

import co.paralleluniverse.fibers.Suspendable;
import co.paralleluniverse.fibers.jdbc.FiberDataSource;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.sql.DataSource;

import org.skife.jdbi.v2.*;
import org.skife.jdbi.v2.exceptions.CallbackFailedException;
import org.skife.jdbi.v2.tweak.HandleCallback;
import org.skife.jdbi.v2.tweak.HandleConsumer;

public class FiberDBI implements IDBI {
    private final IDBI jdbi;
    
    /**
     *
     * @param jdbi jdbi based on FiberDataSource
     */
    public FiberDBI(IDBI jdbi) {
        this.jdbi = jdbi;
    }

    /**
     * Constructor for use with a DataSource which will provide
     *
     * @param dataSource may or may not be FiberDataSource
     * @param es
     */
    public FiberDBI(DataSource dataSource, ExecutorService es) {
        this(dataSource instanceof FiberDataSource ? new DBI(dataSource) : new DBI(FiberDataSource.wrap(dataSource, es)));
    }

    /**
     * Constructor for use with a DataSource which will provide using fixed thread pool executor
     *
     * @param dataSource  may or may not be FiberDataSource
     * @param threadCount
     */
    public FiberDBI(DataSource dataSource, int threadCount) {
        this(dataSource, Executors.newFixedThreadPool(threadCount, new ThreadFactoryBuilder().setDaemon(true).build()));
    }

    /**
     * Constructor for use with a DataSource which will provide using 10 threads fixed pool executor
     *
     * @param dataSource may or may not be FiberDataSource
     */
    public FiberDBI(DataSource dataSource) {
        this(dataSource, 10);
    }

    @Override
    @Suspendable
    public Handle open() {
        return jdbi.open();
    }

    @Override
    @Suspendable
    public <ReturnType> ReturnType withHandle(HandleCallback<ReturnType> callback) throws CallbackFailedException {
        return jdbi.withHandle(callback);
    }

    @Override
    @Suspendable
    public void useHandle(HandleConsumer callback) throws CallbackFailedException {
        jdbi.useHandle(callback);
    }

    @Override
    @Suspendable
    public <SqlObjectType> SqlObjectType open(Class<SqlObjectType> sqlObjectType) {
        return jdbi.open(sqlObjectType);
    }

    @Override
    @Suspendable
    public void close(Object sqlObject) {
        jdbi.close(sqlObject);
    }

    @Override
    public <SqlObjectType> SqlObjectType onDemand(Class<SqlObjectType> sqlObjectType) {
        return jdbi.onDemand(sqlObjectType);
    }

    @Override
    @Suspendable
    public <ReturnType> ReturnType inTransaction(TransactionCallback<ReturnType> callback) throws CallbackFailedException {
        return jdbi.inTransaction(callback);
    }

    @Override
    @Suspendable
    public void useTransaction(TransactionConsumer callback) throws CallbackFailedException {
        jdbi.useTransaction(callback);
    }

    @Override
    @Suspendable
    public <ReturnType> ReturnType inTransaction(TransactionIsolationLevel isolation, TransactionCallback<ReturnType> callback) throws CallbackFailedException {
        return jdbi.inTransaction(isolation, callback);
    }

    @Override
    @Suspendable
    public void useTransaction(TransactionIsolationLevel isolation, TransactionConsumer callback) throws CallbackFailedException {
        jdbi.useTransaction(isolation, callback);
    }

    @Override
    public void define(String key, Object value) {
        jdbi.define(key, value);
    }
}
