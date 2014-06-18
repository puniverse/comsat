/*
 * COMSAT
 * Copyright (c) 2013-2014, Parallel Universe Software Co. All rights reserved.
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
package co.paralleluniverse.fibers.mongodb;

import java.util.concurrent.Future;

import com.allanbank.mongodb.MongoDatabase;
import com.allanbank.mongodb.bson.Document;
import com.allanbank.mongodb.bson.DocumentAssignable;

import co.paralleluniverse.strands.SettableFuture;

/**
 * @author circlespainter
 */
public class FiberMongoDatabaseImpl implements FiberMongoDatabase {
    private final MongoDatabase db;
    
    
    /**
     * Wrapping constructor
     * @param db
     */
    public FiberMongoDatabaseImpl(MongoDatabase db) {
        this.db = db;
    }
    
     /**
     * @return the db
     */
    public MongoDatabase getDb() {
        return db;
    }

    
    // Fiber-blocking API
    
    @Override
    public Document runCommandFiberBlocking(final DocumentAssignable command) throws Throwable {
        return new FiberMongoCallback<Document>() {
            @Override
            protected void requestAsync() {
                getDb().runCommandAsync(this, command);
            }
        }.run();
    }

    @Override
    public Document runCommandFiberBlocking(final String command) throws Throwable {
        return new FiberMongoCallback<Document>() {
            @Override
            protected void requestAsync() {
                getDb().runCommandAsync(this, command);
            }
        }.run();
    }

    @Override
    public Document runCommandFiberBlocking(final String command, final DocumentAssignable options) throws Throwable {
        return new FiberMongoCallback<Document>() {
            @Override
            protected void requestAsync() {
                getDb().runCommandAsync(this, command, options);
            }
        }.run();
    }

    @Override
    public Document runCommandFiberBlocking(final String commandName, final int commandValue, final DocumentAssignable options) throws Throwable {
        return new FiberMongoCallback<Document>() {
            @Override
            protected void requestAsync() {
                getDb().runCommandAsync(this, commandName, commandValue, options);
            }
        }.run();
    }

    @Override
    public Document runCommandFiberBlocking(final String commandName, final String commandValue, final DocumentAssignable options) throws Throwable {
        return new FiberMongoCallback<Document>() {
            @Override
            protected void requestAsync() {
                getDb().runCommandAsync(this, commandName, commandValue, options);
            }
        }.run();
    }


    // Async w/fiber-blocking future API
    
    @Override
    public Future<Document> runCommandFiberBlockingFuture(DocumentAssignable command) {
        final SettableFuture<Document> future = new SettableFuture<>();
        getDb().runCommandAsync(FiberMongoUtils.callbackSettingFuture(future), command);
        return future;
    }

    @Override
    public Future<Document> runCommandFiberBlockingFuture(String command) {
        final SettableFuture<Document> future = new SettableFuture<>();
        getDb().runCommandAsync(FiberMongoUtils.callbackSettingFuture(future), command);
        return future;
    }

    @Override
    public Future<Document> runCommandFiberBlockingFuture(String command, DocumentAssignable options) {
        final SettableFuture<Document> future = new SettableFuture<>();
        getDb().runCommandAsync(FiberMongoUtils.callbackSettingFuture(future), command, options);
        return future;
    }

    @Override
    public Future<Document> runCommandFiberBlockingFuture(String commandName, int commandValue, DocumentAssignable options) {
        final SettableFuture<Document> future = new SettableFuture<>();
        getDb().runCommandAsync(FiberMongoUtils.callbackSettingFuture(future), commandName, commandValue, options);
        return future;
    }

    @Override
    public Future<Document> runCommandFiberBlockingFuture(String commandName, String commandValue, DocumentAssignable options) {
        final SettableFuture<Document> future = new SettableFuture<>();
        getDb().runCommandAsync(FiberMongoUtils.callbackSettingFuture(future), commandName, commandValue, options);
        return future;
    }
}