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

import co.paralleluniverse.strands.SettableFuture;
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
 * @author circlespainter
 */
public class FiberMongoCollectionImpl implements FiberMongoCollection {
    private final MongoCollection coll;

    /**
     * Wrapping constructor
     * @param coll
     */
    public FiberMongoCollectionImpl(MongoCollection coll) {
        this.coll = coll;
    }
    
    @Override
    public MongoCollection getMongoCollection() {
        return coll;
    }

    
    // Fiber-blocking API
    
    @Override
    public List<Document> aggregateFiberBlocking(final Aggregate.Builder command) throws Throwable {
        return new FiberMongoCallback<List<Document>>() {
            @Override
            protected void requestAsync() {
                coll.aggregateAsync(this, command);
            }
        }.run();
    }

    @Override
    public List<Document> aggregateFiberBlocking(final Aggregate command) throws Throwable {
        return new FiberMongoCallback<List<Document>>() {
            @Override
            protected void requestAsync() {
                coll.aggregateAsync(this, command);
            }
        }.run();
    }

    @Override
    public Long countFiberBlocking() throws Throwable {
        return new FiberMongoCallback<Long>() {
            @Override
            protected void requestAsync() {
                coll.countAsync(this);
            }
        }.run();
    }

    @Override
    public Long countFiberBlocking(final DocumentAssignable query) throws Throwable {
        return new FiberMongoCallback<Long>() {
            @Override
            protected void requestAsync() {
                coll.countAsync(this, query);
            }
        }.run();
    }

    @Override
    public Long countFiberBlocking(final DocumentAssignable query, final ReadPreference readPreference) throws Throwable {
        return new FiberMongoCallback<Long>() {
            @Override
            protected void requestAsync() {
                coll.countAsync(this, query, readPreference);
            }
        }.run();
    }

    @Override
    public Long countFiberBlocking(final ReadPreference readPreference) throws Throwable {
        return new FiberMongoCallback<Long>() {
            @Override
            protected void requestAsync() {
                coll.countAsync(this, readPreference);
            }
        }.run();
    }

    @Override
    public Long deleteFiberBlocking(final DocumentAssignable query) throws Throwable {
        return new FiberMongoCallback<Long>() {
            @Override
            protected void requestAsync() {
                coll.deleteAsync(this, query);
            }
        }.run();
    }

    @Override
    public Long deleteFiberBlocking(final DocumentAssignable query, final boolean singleDelete) throws Throwable {
        return new FiberMongoCallback<Long>() {
            @Override
            protected void requestAsync() {
                coll.deleteAsync(this, query, singleDelete);
            }
        }.run();
    }

    @Override
    public Long deleteFiberBlocking(final DocumentAssignable query, final boolean singleDelete, final Durability durability) throws Throwable {
        return new FiberMongoCallback<Long>() {
            @Override
            protected void requestAsync() {
                coll.deleteAsync(this, query, singleDelete, durability);
            }
        }.run();
    }

    @Override
    public Long deleteFiberBlocking(final DocumentAssignable query, final Durability durability) throws Throwable {
        return new FiberMongoCallback<Long>() {
            @Override
            protected void requestAsync() {
                coll.deleteAsync(this, query, durability);
            }
        }.run();
    }

    @Override
    public ArrayElement distinctFiberBlocking(final Distinct.Builder command) throws Throwable {
        return new FiberMongoCallback<ArrayElement>() {
            @Override
            protected void requestAsync() {
                coll.distinctAsync(this, command);
            }
        }.run();
    }

    @Override
    public ArrayElement distinctFiberBlocking(final Distinct command) throws Throwable {
        return new FiberMongoCallback<ArrayElement>() {
            @Override
            protected void requestAsync() {
                coll.distinctAsync(this, command);
            }
        }.run();
    }

    @Override
    public Document explainFiberBlocking(final Find.Builder query) throws Throwable {
        return new FiberMongoCallback<Document>() {
            @Override
            protected void requestAsync() {
                coll.explainAsync(this, query);
            }
        }.run();
    }

    @Override
    public Document explainFiberBlocking(final Find query) throws Throwable {
        return new FiberMongoCallback<Document>() {
            @Override
            protected void requestAsync() {
                coll.explainAsync(this, query);
            }
        }.run();
    }

    @Override
    public MongoIterator<Document> findFiberBlocking(final DocumentAssignable query) throws Throwable {
        return new FiberMongoCallback<MongoIterator<Document>>() {
            @Override
            protected void requestAsync() {
                coll.findAsync(this, query);
            }
        }.run();
    }

    @Override
    public MongoIterator<Document> findFiberBlocking(final Find.Builder query) throws Throwable {
        return new FiberMongoCallback<MongoIterator<Document>>() {
            @Override
            protected void requestAsync() {
                coll.findAsync(this, query);
            }
        }.run();
    }

    @Override
    public MongoIterator<Document> findFiberBlocking(final Find query) throws Throwable {
        return new FiberMongoCallback<MongoIterator<Document>>() {
            @Override
            protected void requestAsync() {
                coll.findAsync(this, query);
            }
        }.run();
    }

    @Override
    public Document findOneFiberBlocking(final DocumentAssignable query) throws Throwable {
        return new FiberMongoCallback<Document>() {
            @Override
            protected void requestAsync() {
                coll.findOneAsync(this, query);
            }
        }.run();
    }

    @Override
    public Document findOneFiberBlocking(final Find.Builder query) throws Throwable {
        return new FiberMongoCallback<Document>() {
            @Override
            protected void requestAsync() {
                coll.findOneAsync(this, query);
            }
        }.run();
    }

    @Override
    public Document findOneFiberBlocking(final Find query) throws Throwable {
        return new FiberMongoCallback<Document>() {
            @Override
            protected void requestAsync() {
                coll.findOneAsync(this, query);
            }
        }.run();
    }

    @Override
    public ArrayElement groupByFiberBlocking(final GroupBy.Builder command) throws Throwable {
        return new FiberMongoCallback<ArrayElement>() {
            @Override
            protected void requestAsync() {
                coll.groupByAsync(this, command);
            }
        }.run();
    }

    @Override
    public ArrayElement groupByFiberBlocking(final GroupBy command) throws Throwable {
        return new FiberMongoCallback<ArrayElement>() {
            @Override
            protected void requestAsync() {
                coll.groupByAsync(this, command);
            }
        }.run();
    }

    @Override
    public Integer insertFiberBlocking(final DocumentAssignable... documents) throws Throwable {
        return new FiberMongoCallback<Integer>() {
            @Override
            protected void requestAsync() {
                coll.insertAsync(this, documents);
            }
        }.run();
    }

    @Override
    public Integer insertFiberBlocking(final Durability durability, final DocumentAssignable... documents) throws Throwable {
        return new FiberMongoCallback<Integer>() {
            @Override
            protected void requestAsync() {
                coll.insertAsync(this, durability, documents);
            }
        }.run();    }

    @Override
    public Integer insertFiberBlocking(final boolean continueOnError, final DocumentAssignable... documents) throws Throwable {
        return new FiberMongoCallback<Integer>() {
            @Override
            protected void requestAsync() {
                coll.insertAsync(this, continueOnError, documents);
            }
        }.run();
    }

    @Override
    public Integer insertFiberBlocking(final boolean continueOnError, final Durability durability, final DocumentAssignable... documents) throws Throwable {
        return new FiberMongoCallback<Integer>() {
            @Override
            protected void requestAsync() {
                coll.insertAsync(this, continueOnError, durability, documents);
            }
        }.run();
    }

    @Override
    public List<Document> mapReduceFiberBlocking(final MapReduce.Builder command) throws Throwable {
        return new FiberMongoCallback<List<Document>>() {
            @Override
            protected void requestAsync() {
                coll.mapReduceAsync(this, command);
            }
        }.run();
    }

    @Override
    public List<Document> mapReduceFiberBlocking(final MapReduce command) throws Throwable {
        return new FiberMongoCallback<List<Document>>() {
            @Override
            protected void requestAsync() {
                coll.mapReduceAsync(this, command);
            }
        }.run();
    }

    @Override
    public Integer saveFiberBlocking(final DocumentAssignable query) throws Throwable {
        return new FiberMongoCallback<Integer>() {
            @Override
            protected void requestAsync() {
                coll.saveAsync(this, query);
            }
        }.run();
    }

    @Override
    public Integer saveFiberBlocking(final DocumentAssignable query, final Durability durability) throws Throwable {
        return new FiberMongoCallback<Integer>() {
            @Override
            protected void requestAsync() {
                coll.saveAsync(this, query, durability);
            }
        }.run();
    }

    @Override
    public List<TextResult> textSearchFiberBlocking(final Text.Builder command) throws Throwable {
        return new FiberMongoCallback<List<TextResult>>() {
            @Override
            protected void requestAsync() {
                coll.textSearchAsync(this, command);
            }
        }.run();
    }

    @Override
    public List<TextResult> textSearchFiberBlocking(final Text command) throws Throwable {
        return new FiberMongoCallback<List<TextResult>>() {
            @Override
            protected void requestAsync() {
                coll.textSearchAsync(this, command);
            }
        }.run();
    }

    @Override
    public Long updateFiberBlocking(final DocumentAssignable query, final DocumentAssignable update) throws Throwable {
        return new FiberMongoCallback<Long>() {
            @Override
            protected void requestAsync() {
                coll.updateAsync(this, query, update);
            }
        }.run();
    }

    @Override
    public Long updateFiberBlocking(final DocumentAssignable query, final DocumentAssignable update, final boolean multiUpdate, final boolean upsert) throws Throwable {
        return new FiberMongoCallback<Long>() {
            @Override
            protected void requestAsync() {
                coll.updateAsync(this, query, update, multiUpdate, upsert);
            }
        }.run();
    }

    @Override
    public Long updateFiberBlocking(final DocumentAssignable query, final DocumentAssignable update, final boolean multiUpdate, final boolean upsert, final Durability durability) throws Throwable {
        return new FiberMongoCallback<Long>() {
            @Override
            protected void requestAsync() {
                coll.updateAsync(this, query, update, multiUpdate, upsert, durability);
            }
        }.run();
    }

    @Override
    public Long updateFiberBlocking(final DocumentAssignable query, final DocumentAssignable update, final Durability durability) throws Throwable {
        return new FiberMongoCallback<Long>() {
            @Override
            protected void requestAsync() {
                coll.updateAsync(this, query, update, durability);
            }
        }.run();
    }

    
    // Async w/fiber-blocking future API
    
    @Override
    public Future<List<Document>> aggregateFiberBlockingFuture(Aggregate.Builder command) {
        final SettableFuture<List<Document>> future = new SettableFuture<>();
        coll.aggregateAsync(FiberMongoUtils.callbackSettingFuture(future), command);
        return future;
    }

    @Override
    public Future<List<Document>> aggregateFiberBlockingFuture(Aggregate command) {
        final SettableFuture<List<Document>> future = new SettableFuture<>();
        coll.aggregateAsync(FiberMongoUtils.callbackSettingFuture(future), command);
        return future;
    }

    @Override
    public Future<Long> countFiberBlockingFuture() {
        final SettableFuture<Long> future = new SettableFuture<>();
        coll.countAsync(FiberMongoUtils.callbackSettingFuture(future));
        return future;
    }

    @Override
    public Future<Long> countFiberBlockingFuture(DocumentAssignable query) {
        final SettableFuture<Long> future = new SettableFuture<>();
        coll.countAsync(FiberMongoUtils.callbackSettingFuture(future), query);
        return future;
    }

    @Override
    public Future<Long> countFiberBlockingFuture(DocumentAssignable query, ReadPreference readPreference) {
        final SettableFuture<Long> future = new SettableFuture<>();
        coll.countAsync(FiberMongoUtils.callbackSettingFuture(future), query, readPreference);
        return future;
    }

    @Override
    public Future<Long> countFiberBlockingFuture(ReadPreference readPreference) {
        final SettableFuture<Long> future = new SettableFuture<>();
        coll.countAsync(FiberMongoUtils.callbackSettingFuture(future), readPreference);
        return future;
    }

    @Override
    public Future<Long> deleteFiberBlockingFuture(DocumentAssignable query) {
        final SettableFuture<Long> future = new SettableFuture<>();
        coll.deleteAsync(FiberMongoUtils.callbackSettingFuture(future), query);
        return future;
    }

    @Override
    public Future<Long> deleteFiberBlockingFuture(DocumentAssignable query, boolean singleDelete) {
        final SettableFuture<Long> future = new SettableFuture<>();
        coll.deleteAsync(FiberMongoUtils.callbackSettingFuture(future), query, singleDelete);
        return future;    }

    @Override
    public Future<Long> deleteFiberBlockingFuture(DocumentAssignable query, boolean singleDelete, Durability durability) {
        final SettableFuture<Long> future = new SettableFuture<>();
        coll.deleteAsync(FiberMongoUtils.callbackSettingFuture(future), query, singleDelete, durability);
        return future;
    }

    @Override
    public Future<Long> deleteFiberBlockingFuture(DocumentAssignable query, Durability durability) {
        final SettableFuture<Long> future = new SettableFuture<>();
        coll.deleteAsync(FiberMongoUtils.callbackSettingFuture(future), query, durability);
        return future;
    }

    @Override
    public Future<ArrayElement> distinctFiberBlockingFuture(Distinct.Builder command) {
        final SettableFuture<ArrayElement> future = new SettableFuture<>();
        coll.distinctAsync(FiberMongoUtils.callbackSettingFuture(future), command);
        return future;
    }

    @Override
    public Future<ArrayElement> distinctFiberBlockingFuture(Distinct command) {
        final SettableFuture<ArrayElement> future = new SettableFuture<>();
        coll.distinctAsync(FiberMongoUtils.callbackSettingFuture(future), command);
        return future;
    }

    @Override
    public Future<Document> explainFiberBlockingFuture(Find.Builder query) {
        final SettableFuture<Document> future = new SettableFuture<>();
        coll.explainAsync(FiberMongoUtils.callbackSettingFuture(future), query);
        return future;
    }

    @Override
    public Future<Document> explainFiberBlockingFuture(Find query) {
        final SettableFuture<Document> future = new SettableFuture<>();
        coll.explainAsync(FiberMongoUtils.callbackSettingFuture(future), query);
        return future;
    }

    @Override
    public Future<MongoIterator<Document>> findFiberBlockingFuture(DocumentAssignable query) {
        final SettableFuture<MongoIterator<Document>> future = new SettableFuture<>();
        coll.findAsync(FiberMongoUtils.callbackSettingFuture(future), query);
        return future;
    }

    @Override
    public Future<MongoIterator<Document>> findFiberBlockingFuture(Find.Builder query) {
        final SettableFuture<MongoIterator<Document>> future = new SettableFuture<>();
        coll.findAsync(FiberMongoUtils.callbackSettingFuture(future), query);
        return future;
    }

    @Override
    public Future<MongoIterator<Document>> findFiberBlockingFuture(Find query) {
        final SettableFuture<MongoIterator<Document>> future = new SettableFuture<>();
        coll.findAsync(FiberMongoUtils.callbackSettingFuture(future), query);
        return future;
    }

    @Override
    public Future<Document> findOneFiberBlockingFuture(DocumentAssignable query) {
        final SettableFuture<Document> future = new SettableFuture<>();
        coll.findOneAsync(FiberMongoUtils.callbackSettingFuture(future), query);
        return future;
    }

    @Override
    public Future<Document> findOneFiberBlockingFuture(Find.Builder query) {
        final SettableFuture<Document> future = new SettableFuture<>();
        coll.findOneAsync(FiberMongoUtils.callbackSettingFuture(future), query);
        return future;
    }

    @Override
    public Future<Document> findOneFiberBlockingFuture(Find query) {
        final SettableFuture<Document> future = new SettableFuture<>();
        coll.findOneAsync(FiberMongoUtils.callbackSettingFuture(future), query);
        return future;
    }

    @Override
    public Future<ArrayElement> groupByFiberBlockingFuture(GroupBy.Builder command) {
        final SettableFuture<ArrayElement> future = new SettableFuture<>();
        coll.groupByAsync(FiberMongoUtils.callbackSettingFuture(future), command);
        return future;
    }

    @Override
    public Future<ArrayElement> groupByFiberBlockingFuture(GroupBy command) {
        final SettableFuture<ArrayElement> future = new SettableFuture<>();
        coll.groupByAsync(FiberMongoUtils.callbackSettingFuture(future), command);
        return future;
    }

    @Override
    public Future<Integer> insertFiberBlockingFuture(DocumentAssignable... documents) {
        final SettableFuture<Integer> future = new SettableFuture<>();
        coll.insertAsync(FiberMongoUtils.callbackSettingFuture(future), documents);
        return future;
    }

    @Override
    public Future<Integer> insertFiberBlockingFuture(Durability durability, DocumentAssignable... documents) {
        final SettableFuture<Integer> future = new SettableFuture<>();
        coll.insertAsync(FiberMongoUtils.callbackSettingFuture(future), durability, documents);
        return future;
    }

    @Override
    public Future<Integer> insertFiberBlockingFuture(boolean continueOnError, DocumentAssignable... documents) {
        final SettableFuture<Integer> future = new SettableFuture<>();
        coll.insertAsync(FiberMongoUtils.callbackSettingFuture(future), continueOnError, documents);
        return future;
    }

    @Override
    public Future<Integer> insertFiberBlockingFuture(boolean continueOnError, Durability durability, DocumentAssignable... documents) {
        final SettableFuture<Integer> future = new SettableFuture<>();
        coll.insertAsync(FiberMongoUtils.callbackSettingFuture(future), continueOnError, durability, documents);
        return future;
    }

    @Override
    public Future<List<Document>> mapReduceFiberBlockingFuture(MapReduce.Builder command) {
        final SettableFuture<List<Document>> future = new SettableFuture<>();
        coll.mapReduceAsync(FiberMongoUtils.callbackSettingFuture(future), command);
        return future;
    }

    @Override
    public Future<List<Document>> mapReduceFiberBlockingFuture(MapReduce command) {
        final SettableFuture<List<Document>> future = new SettableFuture<>();
        coll.mapReduceAsync(FiberMongoUtils.callbackSettingFuture(future), command);
        return future;
    }

    @Override
    public Future<Integer> saveFiberBlockingFuture(DocumentAssignable query) {
        final SettableFuture<Integer> future = new SettableFuture<>();
        coll.saveAsync(FiberMongoUtils.callbackSettingFuture(future), query);
        return future;
    }

    @Override
    public Future<Integer> saveFiberBlockingFuture(DocumentAssignable query, Durability durability) {
        final SettableFuture<Integer> future = new SettableFuture<>();
        coll.saveAsync(FiberMongoUtils.callbackSettingFuture(future), query, durability);
        return future;
    }

    @Override
    public Future<List<TextResult>> textSearchFiberBlockingFuture(Text.Builder command) {
        final SettableFuture<List<TextResult>> future = new SettableFuture<>();
        coll.textSearchAsync(FiberMongoUtils.callbackSettingFuture(future), command);
        return future;
    }

    @Override
    public Future<List<TextResult>> textSearchFiberBlockingFuture(Text command) {
        final SettableFuture<List<TextResult>> future = new SettableFuture<>();
        coll.textSearchAsync(FiberMongoUtils.callbackSettingFuture(future), command);
        return future;
    }

    @Override
    public Future<Long> updateFiberBlockingFuture(DocumentAssignable query, DocumentAssignable update) {
        final SettableFuture<Long> future = new SettableFuture<>();
        coll.updateAsync(FiberMongoUtils.callbackSettingFuture(future), query, update);
        return future;
    }

    @Override
    public Future<Long> updateFiberBlockingFuture(DocumentAssignable query, DocumentAssignable update, boolean multiUpdate, boolean upsert) {
        final SettableFuture<Long> future = new SettableFuture<>();
        coll.updateAsync(FiberMongoUtils.callbackSettingFuture(future), query, update, multiUpdate, upsert);
        return future;
    }

    @Override
    public Future<Long> updateFiberBlockingFuture(DocumentAssignable query, DocumentAssignable update, boolean multiUpdate, boolean upsert, Durability durability) {
        final SettableFuture<Long> future = new SettableFuture<>();
        coll.updateAsync(FiberMongoUtils.callbackSettingFuture(future), query, update, multiUpdate, upsert, durability);
        return future;
    }

    @Override
    public Future<Long> updateFiberBlockingFuture(DocumentAssignable query, DocumentAssignable update, Durability durability) {
        final SettableFuture<Long> future = new SettableFuture<>();
        coll.updateAsync(FiberMongoUtils.callbackSettingFuture(future), query, update, durability);
        return future;
    }
}