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

import com.allanbank.mongodb.Durability;
import com.allanbank.mongodb.MongoCollection;
import com.allanbank.mongodb.MongoIterator;
import com.allanbank.mongodb.ReadPreference;
import com.allanbank.mongodb.bson.Document;
import com.allanbank.mongodb.bson.DocumentAssignable;
import com.allanbank.mongodb.bson.element.ArrayElement;
import com.allanbank.mongodb.builder.Aggregate;
import com.allanbank.mongodb.builder.Distinct;
import com.allanbank.mongodb.builder.Find;
import com.allanbank.mongodb.builder.GroupBy;
import com.allanbank.mongodb.builder.MapReduce;
import com.allanbank.mongodb.builder.Text;
import com.allanbank.mongodb.builder.TextResult;
import java.util.List;
import java.util.concurrent.Future;

/**
 * Wrapper adding fiber-blocking and future-based fiber-blocking APIs to 
 * the Asynchronous MongoDB Java Driver http://www.allanbank.com/mongodb-async-driver/index.html
 * 
 * Collection-level APIs
 * 
 * @author circlespainter
 */
public interface FiberMongoCollection {
    
    /**
     * @return The wrapped MongoCollection
     */
    MongoCollection getMongoCollection();

    // Fiber-blocking API
    List<Document> aggregateFiberBlocking(Aggregate.Builder command) throws Throwable;
    List<Document> aggregateFiberBlocking(Aggregate command) throws Throwable;
    Long countFiberBlocking() throws Throwable;
    Long countFiberBlocking(DocumentAssignable query) throws Throwable;
    Long countFiberBlocking(DocumentAssignable query, ReadPreference readPreference) throws Throwable;
    Long countFiberBlocking(ReadPreference readPreference) throws Throwable;
    Long deleteFiberBlocking(DocumentAssignable query) throws Throwable;
    Long deleteFiberBlocking(DocumentAssignable query, boolean singleDelete) throws Throwable;
    Long deleteFiberBlocking(DocumentAssignable query, boolean singleDelete, Durability durability) throws Throwable;
    Long deleteFiberBlocking(DocumentAssignable query, Durability durability) throws Throwable;
    ArrayElement distinctFiberBlocking(Distinct.Builder command) throws Throwable;
    ArrayElement distinctFiberBlocking(Distinct command) throws Throwable;
    Document explainFiberBlocking(Find.Builder query) throws Throwable;
    Document explainFiberBlocking(Find query) throws Throwable;
    MongoIterator<Document> findFiberBlocking(DocumentAssignable query) throws Throwable;
    MongoIterator<Document> findFiberBlocking(Find.Builder query) throws Throwable;
    MongoIterator<Document> findFiberBlocking(Find query) throws Throwable;
    Document findOneFiberBlocking(DocumentAssignable query) throws Throwable;
    Document findOneFiberBlocking(Find.Builder query) throws Throwable;
    Document findOneFiberBlocking(Find query) throws Throwable;
    ArrayElement groupByFiberBlocking(GroupBy.Builder command) throws Throwable;
    ArrayElement groupByFiberBlocking(GroupBy command) throws Throwable;
    Integer insertFiberBlocking(DocumentAssignable... documents) throws Throwable;
    Integer insertFiberBlocking(Durability durability, DocumentAssignable... documents) throws Throwable;
    Integer insertFiberBlocking(boolean continueOnError, DocumentAssignable... documents) throws Throwable;
    Integer insertFiberBlocking(boolean continueOnError, Durability durability, DocumentAssignable... documents) throws Throwable;
    List<Document> mapReduceFiberBlocking(MapReduce.Builder command) throws Throwable;
    List<Document> mapReduceFiberBlocking(MapReduce command) throws Throwable;
    Integer saveFiberBlocking(DocumentAssignable query) throws Throwable;
    Integer saveFiberBlocking(DocumentAssignable query, Durability durability) throws Throwable;
    List<TextResult> textSearchFiberBlocking(Text.Builder command) throws Throwable;
    List<TextResult> textSearchFiberBlocking(Text command) throws Throwable;
    Long updateFiberBlocking(DocumentAssignable query, DocumentAssignable update) throws Throwable;
    Long updateFiberBlocking(DocumentAssignable query, DocumentAssignable update, boolean multiUpdate, boolean upsert) throws Throwable;
    Long updateFiberBlocking(DocumentAssignable query, DocumentAssignable update, boolean multiUpdate, boolean upsert, Durability durability) throws Throwable;
    Long updateFiberBlocking(DocumentAssignable query, DocumentAssignable update, Durability durability) throws Throwable;


    // Async w/fiber-blocking future API
    Future<List<Document>> aggregateFiberBlockingFuture(Aggregate.Builder command);
    Future<List<Document>> aggregateFiberBlockingFuture(Aggregate command);
    Future<Long> countFiberBlockingFuture();
    Future<Long> countFiberBlockingFuture(DocumentAssignable query);
    Future<Long> countFiberBlockingFuture(DocumentAssignable query, ReadPreference readPreference);
    Future<Long> countFiberBlockingFuture(ReadPreference readPreference);
    Future<Long> deleteFiberBlockingFuture(DocumentAssignable query);
    Future<Long> deleteFiberBlockingFuture(DocumentAssignable query, boolean singleDelete);
    Future<Long> deleteFiberBlockingFuture(DocumentAssignable query, boolean singleDelete, Durability durability);
    Future<Long> deleteFiberBlockingFuture(DocumentAssignable query, Durability durability);
    Future<ArrayElement> distinctFiberBlockingFuture(Distinct.Builder command);
    Future<ArrayElement> distinctFiberBlockingFuture(Distinct command);
    Future<Document> explainFiberBlockingFuture(Find.Builder query);
    Future<Document> explainFiberBlockingFuture(Find query);
    Future<MongoIterator<Document>> findFiberBlockingFuture(DocumentAssignable query);
    Future<MongoIterator<Document>> findFiberBlockingFuture(Find.Builder query);
    Future<MongoIterator<Document>> findFiberBlockingFuture(Find query);
    Future<Document> findOneFiberBlockingFuture(DocumentAssignable query);
    Future<Document> findOneFiberBlockingFuture(Find.Builder query);
    Future<Document> findOneFiberBlockingFuture(Find query);
    Future<ArrayElement> groupByFiberBlockingFuture(GroupBy.Builder command);
    Future<ArrayElement> groupByFiberBlockingFuture(GroupBy command);
    Future<Integer> insertFiberBlockingFuture(DocumentAssignable... documents);
    Future<Integer> insertFiberBlockingFuture(Durability durability, DocumentAssignable... documents);
    Future<Integer> insertFiberBlockingFuture(boolean continueOnError, DocumentAssignable... documents);
    Future<Integer> insertFiberBlockingFuture(boolean continueOnError, Durability durability, DocumentAssignable... documents);
    Future<List<Document>> mapReduceFiberBlockingFuture(MapReduce.Builder command);
    Future<List<Document>> mapReduceFiberBlockingFuture(MapReduce command);
    Future<Integer> saveFiberBlockingFuture(DocumentAssignable query);
    Future<Integer> saveFiberBlockingFuture(DocumentAssignable query, Durability durability);
    Future<List<TextResult>> textSearchFiberBlockingFuture(Text.Builder command);
    Future<List<TextResult>> textSearchFiberBlockingFuture(Text command);
    Future<Long> updateFiberBlockingFuture(DocumentAssignable query, DocumentAssignable update);
    Future<Long> updateFiberBlockingFuture(DocumentAssignable query, DocumentAssignable update, boolean multiUpdate, boolean upsert);
    Future<Long> updateFiberBlockingFuture(DocumentAssignable query, DocumentAssignable update, boolean multiUpdate, boolean upsert, Durability durability);
    Future<Long> updateFiberBlockingFuture(DocumentAssignable query, DocumentAssignable update, Durability durability);
}