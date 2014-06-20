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

import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.Suspendable;
import co.paralleluniverse.strands.SettableFuture;
import com.allanbank.mongodb.Durability;
import com.allanbank.mongodb.MongoDatabase;
import com.allanbank.mongodb.MongoDbException;
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
import com.allanbank.mongodb.client.Client;
import com.allanbank.mongodb.client.MongoCollectionImpl;
import java.util.List;
import java.util.concurrent.Future;

/**
 * @author circlespainter
 */
public class FiberMongoCollectionImpl extends MongoCollectionImpl {

    public FiberMongoCollectionImpl(Client client, MongoDatabase md, String string) {
        super(client, md, string);
    }
    
    // Fiber-blocking API
    
    @Override
    @Suspendable
    public List<Document> aggregate(final Aggregate.Builder command) throws MongoDbException {
        List<Document> res = null;
        try {
            res = new FiberMongoCallback<List<Document>>() {
                @Override
                protected void requestAsync() {
                    FiberMongoCollectionImpl.super.aggregateAsync(this, command);
                }
            }.run();
        } catch (SuspendExecution | InterruptedException ex) {
            throw new AssertionError("Should never happen", ex);
        }
        return res;
    }

    @Override
    @Suspendable
    public List<Document> aggregate(final Aggregate command) throws MongoDbException {
        List<Document> res = null;
        try {
            res = new FiberMongoCallback<List<Document>>() {
                @Override
                protected void requestAsync() {
                    FiberMongoCollectionImpl.super.aggregateAsync(this, command);
                }
            }.run();
        } catch (SuspendExecution | InterruptedException ex) {
            throw new AssertionError("Should never happen", ex);
        }
        return res;
    }

    @Override
    @Suspendable
    public long count() throws MongoDbException {
        Long res = null;
        try {
            res = new FiberMongoCallback<Long>() {
                @Override
                protected void requestAsync() {
                    FiberMongoCollectionImpl.super.countAsync(this);
                }
            }.run();
        } catch (SuspendExecution | InterruptedException ex) {
            throw new AssertionError("Should never happen", ex);
        }
        return res;
    }

    @Override
    @Suspendable
    public long count(final DocumentAssignable query) throws MongoDbException {
        Long res = null;
        try {
            res = new FiberMongoCallback<Long>() {
                @Override
                protected void requestAsync() {
                    FiberMongoCollectionImpl.super.countAsync(this, query);
                }
            }.run();
        } catch (SuspendExecution | InterruptedException ex) {
            throw new AssertionError("Should never happen", ex);
        }
        return res;
    }

    @Override
    @Suspendable
    public long count(final DocumentAssignable query, final ReadPreference readPreference) throws MongoDbException {
        Long res = null;
        try {
            res = new FiberMongoCallback<Long>() {
                @Override
                protected void requestAsync() {
                    FiberMongoCollectionImpl.super.countAsync(this, query, readPreference);
                }
            }.run();
        } catch (SuspendExecution | InterruptedException ex) {
            throw new AssertionError("Should never happen", ex);
        }
        return res;
    }

    @Override
    @Suspendable
    public long count(final ReadPreference readPreference) throws MongoDbException {
        Long res = null;
        try {
            res = new FiberMongoCallback<Long>() {
                @Override
                protected void requestAsync() {
                    FiberMongoCollectionImpl.super.countAsync(this, readPreference);
                }
            }.run();
        } catch (SuspendExecution | InterruptedException ex) {
            throw new AssertionError("Should never happen", ex);
        }
        return res;
    }

    @Override
    @Suspendable
    public long delete(final DocumentAssignable query) throws MongoDbException {
        Long res = null;
        try {
            res = new FiberMongoCallback<Long>() {
                @Override
                protected void requestAsync() {
                    FiberMongoCollectionImpl.super.deleteAsync(this, query);
                }
            }.run();
        } catch (SuspendExecution | InterruptedException ex) {
            throw new AssertionError("Should never happen", ex);
        }
        return res;
    }

    @Override
    @Suspendable
    public long delete(final DocumentAssignable query, final boolean singleDelete) throws MongoDbException {
        Long res = null;
        try {
            res = new FiberMongoCallback<Long>() {
                @Override
                protected void requestAsync() {
                    FiberMongoCollectionImpl.super.deleteAsync(this, query, singleDelete);
                }
            }.run();
        } catch (SuspendExecution | InterruptedException ex) {
            throw new AssertionError("Should never happen", ex);
        }
        return res;
    }

    @Override
    @Suspendable
    public long delete(final DocumentAssignable query, final boolean singleDelete, final Durability durability) throws MongoDbException {
        Long res = null;
        try {
            res = new FiberMongoCallback<Long>() {
                @Override
                protected void requestAsync() {
                    FiberMongoCollectionImpl.super.deleteAsync(this, query, singleDelete, durability);
                }
            }.run();
        } catch (SuspendExecution | InterruptedException ex) {
            throw new AssertionError("Should never happen", ex);
        }
        return res;
    }

    @Override
    @Suspendable
    public long delete(final DocumentAssignable query, final Durability durability) throws MongoDbException {
        Long res = null;
        try {
            res = new FiberMongoCallback<Long>() {
                @Override
                protected void requestAsync() {
                    FiberMongoCollectionImpl.super.deleteAsync(this, query, durability);
                }
            }.run();
        } catch (SuspendExecution | InterruptedException ex) {
            throw new AssertionError("Should never happen", ex);
        }
        return res;
    }

    @Override
    @Suspendable
    public ArrayElement distinct(final Distinct.Builder command) throws MongoDbException {
        ArrayElement res = null;
        try {
            res = new FiberMongoCallback<ArrayElement>() {
                @Override
                protected void requestAsync() {
                    FiberMongoCollectionImpl.super.distinctAsync(this, command);
                }
            }.run();
        } catch (SuspendExecution | InterruptedException ex) {
            throw new AssertionError("Should never happen", ex);
        }
        return res;
    }

    @Override
    @Suspendable
    public ArrayElement distinct(final Distinct command) throws MongoDbException {
        ArrayElement res = null;
        try {
            res = new FiberMongoCallback<ArrayElement>() {
                @Override
                protected void requestAsync() {
                    FiberMongoCollectionImpl.super.distinctAsync(this, command);
                }
            }.run();
        } catch (SuspendExecution | InterruptedException ex) {
            throw new AssertionError("Should never happen", ex);
        }
        return res;
    }

    @Override
    @Suspendable
    public Document explain(final Find.Builder query) throws MongoDbException {
        Document res = null;
        try {
            res = new FiberMongoCallback<Document>() {
                @Override
                protected void requestAsync() {
                    FiberMongoCollectionImpl.super.explainAsync(this, query);
                }
            }.run();
        } catch (SuspendExecution | InterruptedException ex) {
            throw new AssertionError("Should never happen", ex);
        }
        return res;
    }

    @Override
    @Suspendable
    public Document explain(final Find query) throws MongoDbException {
        Document res = null;
        try {
            res = new FiberMongoCallback<Document>() {
                @Override
                protected void requestAsync() {
                    FiberMongoCollectionImpl.super.explainAsync(this, query);
                }
            }.run();
        } catch (SuspendExecution | InterruptedException ex) {
            throw new AssertionError("Should never happen", ex);
        }
        return res;
    }

    @Override
    @Suspendable
    public MongoIterator<Document> find(final DocumentAssignable query) throws MongoDbException {
        MongoIterator<Document> res = null;
        try {
            res = new FiberMongoCallback<MongoIterator<Document>>() {
                @Override
                protected void requestAsync() {
                    FiberMongoCollectionImpl.super.findAsync(this, query);
                }
            }.run();
        } catch (SuspendExecution | InterruptedException ex) {
            throw new AssertionError("Should never happen", ex);
        }
        return res;
    }

    @Override
    @Suspendable
    public MongoIterator<Document> find(final Find.Builder query) throws MongoDbException {
        MongoIterator<Document> res = null;
        try {
            res = new FiberMongoCallback<MongoIterator<Document>>() {
                @Override
                protected void requestAsync() {
                    FiberMongoCollectionImpl.super.findAsync(this, query);
                }
            }.run();
        } catch (SuspendExecution | InterruptedException ex) {
            throw new AssertionError("Should never happen", ex);
        }
        return res;
    }

    @Override
    @Suspendable
    public MongoIterator<Document> find(final Find query) throws MongoDbException {
        MongoIterator<Document> res = null;
        try {
            res = new FiberMongoCallback<MongoIterator<Document>>() {
                @Override
                protected void requestAsync() {
                    FiberMongoCollectionImpl.super.findAsync(this, query);
                }
            }.run();
        } catch (SuspendExecution | InterruptedException ex) {
            throw new AssertionError("Should never happen", ex);
        }
        return res;
    }

    @Override
    @Suspendable
    public Document findOne(final DocumentAssignable query) throws MongoDbException {
        Document res = null;
        try {
            res = new FiberMongoCallback<Document>() {
                @Override
                protected void requestAsync() {
                    FiberMongoCollectionImpl.super.findOneAsync(this, query);
                }
            }.run();
        } catch (SuspendExecution | InterruptedException ex) {
            throw new AssertionError("Should never happen", ex);
        }
        return res;
    }

    @Override
    @Suspendable
    public Document findOne(final Find.Builder query) throws MongoDbException {
        Document res = null;
        try {
            res = new FiberMongoCallback<Document>() {
                @Override
                protected void requestAsync() {
                    FiberMongoCollectionImpl.super.findOneAsync(this, query);
                }
            }.run();
        } catch (SuspendExecution | InterruptedException ex) {
            throw new AssertionError("Should never happen", ex);
        }
        return res;
    }

    @Override
    @Suspendable
    public Document findOne(final Find query) throws MongoDbException {
        Document res = null;
        try {
            res = new FiberMongoCallback<Document>() {
                @Override
                protected void requestAsync() {
                    FiberMongoCollectionImpl.super.findOneAsync(this, query);
                }
            }.run();
        } catch (SuspendExecution | InterruptedException ex) {
            throw new AssertionError("Should never happen", ex);
        }
        return res;
    }

    @Override
    @Suspendable
    public ArrayElement groupBy(final GroupBy.Builder command) throws MongoDbException {
        ArrayElement res = null;
        try {
            res = new FiberMongoCallback<ArrayElement>() {
                @Override
                protected void requestAsync() {
                    FiberMongoCollectionImpl.super.groupByAsync(this, command);
                }
            }.run();
        } catch (SuspendExecution | InterruptedException ex) {
            throw new AssertionError("Should never happen", ex);
        }
        return res;
    }

    @Override
    @Suspendable
    public ArrayElement groupBy(final GroupBy command) throws MongoDbException {
        ArrayElement res = null;
        try {
            res = new FiberMongoCallback<ArrayElement>() {
                @Override
                protected void requestAsync() {
                    FiberMongoCollectionImpl.super.groupByAsync(this, command);
                }
            }.run();
        } catch (SuspendExecution | InterruptedException ex) {
            throw new AssertionError("Should never happen", ex);
        }
        return res;
    }

    @Override
    @Suspendable
    public int insert(final DocumentAssignable... documents) throws MongoDbException {
        Integer res = null;
        try {
            res = new FiberMongoCallback<Integer>() {
                @Override
                protected void requestAsync() {
                    FiberMongoCollectionImpl.super.insertAsync(this, documents);
                }
            }.run();
        } catch (SuspendExecution | InterruptedException ex) {
            throw new AssertionError("Should never happen", ex);
        }
        return res;
    }

    @Override
    @Suspendable
    public int insert(final Durability durability, final DocumentAssignable... documents) throws MongoDbException {
        Integer res = null;
        try {
            res = new FiberMongoCallback<Integer>() {
                @Override
                protected void requestAsync() {
                    FiberMongoCollectionImpl.super.insertAsync(this, durability, documents);
                }
            }.run();
        } catch (SuspendExecution | InterruptedException ex) {
            throw new AssertionError("Should never happen", ex);
        }
        return res;
    }

    @Override
    @Suspendable
    public int insert(final boolean continueOnError, final DocumentAssignable... documents) throws MongoDbException {
        Integer res = null;
        try {
            res = new FiberMongoCallback<Integer>() {
                @Override
                protected void requestAsync() {
                    FiberMongoCollectionImpl.super.insertAsync(this, continueOnError, documents);
                }
            }.run();
        } catch (SuspendExecution | InterruptedException ex) {
            throw new AssertionError("Should never happen", ex);
        }
        return res;
    }

    @Override
    @Suspendable
    public int insert(final boolean continueOnError, final Durability durability, final DocumentAssignable... documents) throws MongoDbException {
        Integer res = null;
        try {
            res = new FiberMongoCallback<Integer>() {
                @Override
                protected void requestAsync() {
                    FiberMongoCollectionImpl.super.insertAsync(this, continueOnError, durability, documents);
                }
            }.run();
        } catch (SuspendExecution | InterruptedException ex) {
            throw new AssertionError("Should never happen", ex);
        }
        return res;
    }

    @Override
    @Suspendable
    public List<Document> mapReduce(final MapReduce.Builder command) throws MongoDbException {
        List<Document> res = null;
        try {
            res = new FiberMongoCallback<List<Document>>() {
                @Override
                protected void requestAsync() {
                    FiberMongoCollectionImpl.super.mapReduceAsync(this, command);
                }
            }.run();
        } catch (SuspendExecution | InterruptedException ex) {
            throw new AssertionError("Should never happen", ex);
        }
        return res;
    }

    @Override
    @Suspendable
    public List<Document> mapReduce(final MapReduce command) throws MongoDbException {
        List<Document> res = null;
        try {
            res = new FiberMongoCallback<List<Document>>() {
                @Override
                protected void requestAsync() {
                    FiberMongoCollectionImpl.super.mapReduceAsync(this, command);
                }
            }.run();
        } catch (SuspendExecution | InterruptedException ex) {
            throw new AssertionError("Should never happen", ex);
        }
        return res;
    }

    @Override
    @Suspendable
    public int save(final DocumentAssignable query) throws MongoDbException {
        Integer res = null;
        try {
            res = new FiberMongoCallback<Integer>() {
                @Override
                protected void requestAsync() {
                    FiberMongoCollectionImpl.super.saveAsync(this, query);
                }
            }.run();
        } catch (SuspendExecution | InterruptedException ex) {
            throw new AssertionError("Should never happen", ex);
        }
        return res;
    }

    @Override
    @Suspendable
    public int save(final DocumentAssignable query, final Durability durability) throws MongoDbException {
        Integer res = null;
        try {
            res = new FiberMongoCallback<Integer>() {
                @Override
                protected void requestAsync() {
                    FiberMongoCollectionImpl.super.saveAsync(this, query, durability);
                }
            }.run();
        } catch (SuspendExecution | InterruptedException ex) {
            throw new AssertionError("Should never happen", ex);
        }
        return res;
    }

    @Override
    @Suspendable
    public List<TextResult> textSearch(final Text.Builder command) throws MongoDbException {
        List<TextResult> res = null;
        try {
            res = new FiberMongoCallback<List<TextResult>>() {
                @Override
                protected void requestAsync() {
                    FiberMongoCollectionImpl.super.textSearchAsync(this, command);
                }
            }.run();
        } catch (SuspendExecution | InterruptedException ex) {
            throw new AssertionError("Should never happen", ex);
        }
        return res;
    }

    @Override
    @Suspendable
    public List<TextResult> textSearch(final Text command) throws MongoDbException {
        List<TextResult> res = null;
        try {
            res = new FiberMongoCallback<List<TextResult>>() {
                @Override
                protected void requestAsync() {
                    FiberMongoCollectionImpl.super.textSearchAsync(this, command);
                }
            }.run();
        } catch (SuspendExecution | InterruptedException ex) {
            throw new AssertionError("Should never happen", ex);
        }
        return res;
    }

    @Override
    @Suspendable
    public long update(final DocumentAssignable query, final DocumentAssignable update) throws MongoDbException {
        Long res = null;
        try {
            res = new FiberMongoCallback<Long>() {
                @Override
                protected void requestAsync() {
                    FiberMongoCollectionImpl.super.updateAsync(this, query, update);
                }
            }.run();
        } catch (SuspendExecution | InterruptedException ex) {
            throw new AssertionError("Should never happen", ex);
        }
        return res;
    }

    @Override
    @Suspendable
    public long update(final DocumentAssignable query, final DocumentAssignable update, final boolean multiUpdate, final boolean upsert) throws MongoDbException {
        Long res = null;
        try {
            res = new FiberMongoCallback<Long>() {
                @Override
                protected void requestAsync() {
                    FiberMongoCollectionImpl.super.updateAsync(this, query, update, multiUpdate, upsert);
                }
            }.run();
        } catch (SuspendExecution | InterruptedException ex) {
            throw new AssertionError("Should never happen", ex);
        }
        return res;
    }

    @Override
    @Suspendable
    public long update(final DocumentAssignable query, final DocumentAssignable update, final boolean multiUpdate, final boolean upsert, final Durability durability) throws MongoDbException {
        Long res = null;
        try {
            res = new FiberMongoCallback<Long>() {
                @Override
                protected void requestAsync() {
                    FiberMongoCollectionImpl.super.updateAsync(this, query, update, multiUpdate, upsert, durability);
                }
            }.run();
        } catch (SuspendExecution | InterruptedException ex) {
            throw new AssertionError("Should never happen", ex);
        }
        return res;
    }

    @Override
    @Suspendable
    public long update(final DocumentAssignable query, final DocumentAssignable update, final Durability durability) throws MongoDbException {
        Long res = null;
        try {
            res = new FiberMongoCallback<Long>() {
                @Override
                protected void requestAsync() {
                    FiberMongoCollectionImpl.super.updateAsync(this, query, update, durability);
                }
            }.run();
        } catch (SuspendExecution | InterruptedException ex) {
            throw new AssertionError("Should never happen", ex);
        }
        return res;
    }

    
    // Async w/fiber-blocking future API
    
    @Override
    public Future<List<Document>> aggregateAsync(Aggregate.Builder command) throws MongoDbException {
        final SettableFuture<List<Document>> future = new SettableFuture<>();
        aggregateAsync(FiberMongoUtils.callbackSettingFuture(future), command);
        return future;
    }

    @Override
    public Future<List<Document>> aggregateAsync(Aggregate command) throws MongoDbException {
        final SettableFuture<List<Document>> future = new SettableFuture<>();
        aggregateAsync(FiberMongoUtils.callbackSettingFuture(future), command);
        return future;
    }

    @Override
    public Future<Long> countAsync() throws MongoDbException {
        final SettableFuture<Long> future = new SettableFuture<>();
        countAsync(FiberMongoUtils.callbackSettingFuture(future));
        return future;
    }

    @Override
    public Future<Long> countAsync(DocumentAssignable query) throws MongoDbException {
        final SettableFuture<Long> future = new SettableFuture<>();
        countAsync(FiberMongoUtils.callbackSettingFuture(future), query);
        return future;
    }

    @Override
    public Future<Long> countAsync(DocumentAssignable query, ReadPreference readPreference) throws MongoDbException {
        final SettableFuture<Long> future = new SettableFuture<>();
        countAsync(FiberMongoUtils.callbackSettingFuture(future), query, readPreference);
        return future;
    }

    @Override
    public Future<Long> countAsync(ReadPreference readPreference) throws MongoDbException {
        final SettableFuture<Long> future = new SettableFuture<>();
        countAsync(FiberMongoUtils.callbackSettingFuture(future), readPreference);
        return future;
    }

    @Override
    public Future<Long> deleteAsync(DocumentAssignable query) throws MongoDbException {
        final SettableFuture<Long> future = new SettableFuture<>();
        deleteAsync(FiberMongoUtils.callbackSettingFuture(future), query);
        return future;
    }

    @Override
    public Future<Long> deleteAsync(DocumentAssignable query, boolean singleDelete) throws MongoDbException {
        final SettableFuture<Long> future = new SettableFuture<>();
        deleteAsync(FiberMongoUtils.callbackSettingFuture(future), query, singleDelete);
        return future;
    }

    @Override
    public Future<Long> deleteAsync(DocumentAssignable query, boolean singleDelete, Durability durability) throws MongoDbException {
        final SettableFuture<Long> future = new SettableFuture<>();
        deleteAsync(FiberMongoUtils.callbackSettingFuture(future), query, singleDelete, durability);
        return future;
    }

    @Override
    public Future<Long> deleteAsync(DocumentAssignable query, Durability durability) throws MongoDbException {
        final SettableFuture<Long> future = new SettableFuture<>();
        deleteAsync(FiberMongoUtils.callbackSettingFuture(future), query, durability);
        return future;
    }

    @Override
    public Future<ArrayElement> distinctAsync(Distinct.Builder command) throws MongoDbException {
        final SettableFuture<ArrayElement> future = new SettableFuture<>();
        distinctAsync(FiberMongoUtils.callbackSettingFuture(future), command);
        return future;
    }

    @Override
    public Future<ArrayElement> distinctAsync(Distinct command) throws MongoDbException {
        final SettableFuture<ArrayElement> future = new SettableFuture<>();
        distinctAsync(FiberMongoUtils.callbackSettingFuture(future), command);
        return future;
    }

    @Override
    public Future<Document> explainAsync(Find.Builder query) throws MongoDbException {
        final SettableFuture<Document> future = new SettableFuture<>();
        explainAsync(FiberMongoUtils.callbackSettingFuture(future), query);
        return future;
    }

    @Override
    public Future<Document> explainAsync(Find query) throws MongoDbException {
        final SettableFuture<Document> future = new SettableFuture<>();
        explainAsync(FiberMongoUtils.callbackSettingFuture(future), query);
        return future;
    }

    @Override
    public Future<MongoIterator<Document>> findAsync(DocumentAssignable query) throws MongoDbException {
        final SettableFuture<MongoIterator<Document>> future = new SettableFuture<>();
        findAsync(FiberMongoUtils.callbackSettingFuture(future), query);
        return future;
    }

    @Override
    public Future<MongoIterator<Document>> findAsync(Find.Builder query) throws MongoDbException {
        final SettableFuture<MongoIterator<Document>> future = new SettableFuture<>();
        findAsync(FiberMongoUtils.callbackSettingFuture(future), query);
        return future;
    }

    @Override
    public Future<MongoIterator<Document>> findAsync(Find query) throws MongoDbException {
        final SettableFuture<MongoIterator<Document>> future = new SettableFuture<>();
        findAsync(FiberMongoUtils.callbackSettingFuture(future), query);
        return future;
    }

    @Override
    public Future<Document> findOneAsync(DocumentAssignable query) throws MongoDbException {
        final SettableFuture<Document> future = new SettableFuture<>();
        findOneAsync(FiberMongoUtils.callbackSettingFuture(future), query);
        return future;
    }

    @Override
    public Future<Document> findOneAsync(Find.Builder query) throws MongoDbException {
        final SettableFuture<Document> future = new SettableFuture<>();
        findOneAsync(FiberMongoUtils.callbackSettingFuture(future), query);
        return future;
    }

    @Override
    public Future<Document> findOneAsync(Find query) throws MongoDbException {
        final SettableFuture<Document> future = new SettableFuture<>();
        findOneAsync(FiberMongoUtils.callbackSettingFuture(future), query);
        return future;
    }

    @Override
    public Future<ArrayElement> groupByAsync(GroupBy.Builder command) throws MongoDbException {
        final SettableFuture<ArrayElement> future = new SettableFuture<>();
        groupByAsync(FiberMongoUtils.callbackSettingFuture(future), command);
        return future;
    }

    @Override
    public Future<ArrayElement> groupByAsync(GroupBy command) throws MongoDbException {
        final SettableFuture<ArrayElement> future = new SettableFuture<>();
        groupByAsync(FiberMongoUtils.callbackSettingFuture(future), command);
        return future;
    }

    @Override
    public Future<Integer> insertAsync(DocumentAssignable... documents) throws MongoDbException {
        final SettableFuture<Integer> future = new SettableFuture<>();
        insertAsync(FiberMongoUtils.callbackSettingFuture(future), documents);
        return future;
    }

    @Override
    public Future<Integer> insertAsync(Durability durability, DocumentAssignable... documents) throws MongoDbException {
        final SettableFuture<Integer> future = new SettableFuture<>();
        insertAsync(FiberMongoUtils.callbackSettingFuture(future), durability, documents);
        return future;
    }

    @Override
    public Future<Integer> insertAsync(boolean continueOnError, DocumentAssignable... documents) throws MongoDbException {
        final SettableFuture<Integer> future = new SettableFuture<>();
        insertAsync(FiberMongoUtils.callbackSettingFuture(future), continueOnError, documents);
        return future;
    }

    @Override
    public Future<Integer> insertAsync(boolean continueOnError, Durability durability, DocumentAssignable... documents) throws MongoDbException {
        final SettableFuture<Integer> future = new SettableFuture<>();
        insertAsync(FiberMongoUtils.callbackSettingFuture(future), continueOnError, durability, documents);
        return future;
    }

    @Override
    public Future<List<Document>> mapReduceAsync(MapReduce.Builder command) throws MongoDbException {
        final SettableFuture<List<Document>> future = new SettableFuture<>();
        mapReduceAsync(FiberMongoUtils.callbackSettingFuture(future), command);
        return future;
    }

    @Override
    public Future<List<Document>> mapReduceAsync(MapReduce command) throws MongoDbException {
        final SettableFuture<List<Document>> future = new SettableFuture<>();
        mapReduceAsync(FiberMongoUtils.callbackSettingFuture(future), command);
        return future;
    }

    @Override
    public Future<Integer> saveAsync(DocumentAssignable query) throws MongoDbException {
        final SettableFuture<Integer> future = new SettableFuture<>();
        saveAsync(FiberMongoUtils.callbackSettingFuture(future), query);
        return future;
    }

    @Override
    public Future<Integer> saveAsync(DocumentAssignable query, Durability durability) throws MongoDbException {
        final SettableFuture<Integer> future = new SettableFuture<>();
        saveAsync(FiberMongoUtils.callbackSettingFuture(future), query, durability);
        return future;
    }

    @Override
    public Future<List<TextResult>> textSearchAsync(Text.Builder command) throws MongoDbException {
        final SettableFuture<List<TextResult>> future = new SettableFuture<>();
        textSearchAsync(FiberMongoUtils.callbackSettingFuture(future), command);
        return future;
    }

    @Override
    public Future<List<TextResult>> textSearchAsync(Text command) throws MongoDbException {
        final SettableFuture<List<TextResult>> future = new SettableFuture<>();
        textSearchAsync(FiberMongoUtils.callbackSettingFuture(future), command);
        return future;
    }

    @Override
    public Future<Long> updateAsync(DocumentAssignable query, DocumentAssignable update) throws MongoDbException {
        final SettableFuture<Long> future = new SettableFuture<>();
        updateAsync(FiberMongoUtils.callbackSettingFuture(future), query, update);
        return future;
    }

    @Override
    public Future<Long> updateAsync(DocumentAssignable query, DocumentAssignable update, boolean multiUpdate, boolean upsert) throws MongoDbException {
        final SettableFuture<Long> future = new SettableFuture<>();
        updateAsync(FiberMongoUtils.callbackSettingFuture(future), query, update, multiUpdate, upsert);
        return future;
    }

    @Override
    public Future<Long> updateAsync(DocumentAssignable query, DocumentAssignable update, boolean multiUpdate, boolean upsert, Durability durability) throws MongoDbException {
        final SettableFuture<Long> future = new SettableFuture<>();
        updateAsync(FiberMongoUtils.callbackSettingFuture(future), query, update, multiUpdate, upsert, durability);
        return future;
    }

    @Override
    public Future<Long> updateAsync(DocumentAssignable query, DocumentAssignable update, Durability durability) throws MongoDbException {
        final SettableFuture<Long> future = new SettableFuture<>();
        updateAsync(FiberMongoUtils.callbackSettingFuture(future), query, update, durability);
        return future;
    }
}