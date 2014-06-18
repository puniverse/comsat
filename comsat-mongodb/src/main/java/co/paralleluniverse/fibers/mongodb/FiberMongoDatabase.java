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

/**
 * Wrapper adding fiber-blocking and future-based fiber-blocking APIs to 
 * the Asynchronous MongoDB Java Driver http://www.allanbank.com/mongodb-async-driver/index.html
 * 
 * Database-level APIs
 * 
 * @author circlespainter
 */
public interface FiberMongoDatabase {

    /**
     * @return The wrapped MongoDatabase instance
     */
    MongoDatabase getMongoDatabase();

    // Fiber-blocking API
    Document runCommandFiberBlocking(DocumentAssignable command) throws Throwable;
    Document runCommandFiberBlocking(String command) throws Throwable;
    Document runCommandFiberBlocking(String command, DocumentAssignable options) throws Throwable;
    Document runCommandFiberBlocking(String commandName, int commandValue, DocumentAssignable options) throws Throwable;
    Document runCommandFiberBlocking(String commandName, String commandValue, DocumentAssignable options) throws Throwable;

    // Async w/fiber-blocking future API
    Future<Document> runCommandFiberBlockingFuture(DocumentAssignable command);
    Future<Document> runCommandFiberBlockingFuture(String command);
    Future<Document> runCommandFiberBlockingFuture(String command, DocumentAssignable options);
    Future<Document> runCommandFiberBlockingFuture(String commandName, int commandValue, DocumentAssignable options);
    Future<Document> runCommandFiberBlockingFuture(String commandName, String commandValue, DocumentAssignable options);
}
