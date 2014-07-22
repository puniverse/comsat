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
import com.allanbank.mongodb.Durability;
import com.allanbank.mongodb.ListenableFuture;
import com.allanbank.mongodb.MongoDatabase;
import com.allanbank.mongodb.MongoDbException;
import com.allanbank.mongodb.MongoIterator;
import com.allanbank.mongodb.ReadPreference;
import com.allanbank.mongodb.bson.Document;
import com.allanbank.mongodb.bson.DocumentAssignable;
import com.allanbank.mongodb.bson.Element;
import com.allanbank.mongodb.builder.Aggregate;
import com.allanbank.mongodb.builder.BatchedWrite;
import com.allanbank.mongodb.builder.Count;
import com.allanbank.mongodb.builder.Distinct;
import com.allanbank.mongodb.builder.Find;
import com.allanbank.mongodb.builder.FindAndModify;
import com.allanbank.mongodb.builder.GroupBy;
import com.allanbank.mongodb.builder.MapReduce;
import com.allanbank.mongodb.builder.ParallelScan;
import com.allanbank.mongodb.builder.Text;
import com.allanbank.mongodb.builder.TextResult;
import com.allanbank.mongodb.client.Client;
import com.allanbank.mongodb.client.SynchronousMongoCollectionImpl;
import java.util.Collection;

/**
 * @author circlespainter
 * 
 * TODO: new fiber-blocking APIs for now async-only operations (when added)
 */
public class FiberMongoCollectionImpl extends SynchronousMongoCollectionImpl {

    public FiberMongoCollectionImpl(Client client, MongoDatabase md, String string) {
        super(client, md, string);
    }
    
    // Fiber-blocking API
    
    @Override
    @Suspendable
    public MongoIterator<Document> aggregate(final Aggregate.Builder command) throws MongoDbException {
        MongoIterator<Document> res = null;
        try {
            res = new FiberMongoCallback<MongoIterator<Document>>() {
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
    public MongoIterator<Document> aggregate(final Aggregate command) throws MongoDbException {
        MongoIterator<Document> res = null;
        try {
            res = new FiberMongoCallback<MongoIterator<Document>>() {
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
    public long count(final Count query) throws MongoDbException {
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
    public long count(final Count.Builder query) throws MongoDbException {
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
    public MongoIterator<Element> distinct(final Distinct.Builder command) throws MongoDbException {
        MongoIterator<Element> res = null;
        try {
            res = new FiberMongoCallback<MongoIterator<Element>>() {
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
    public MongoIterator<Element> distinct(final Distinct command) throws MongoDbException {
        MongoIterator<Element> res = null;
        try {
            res = new FiberMongoCallback<MongoIterator<Element>>() {
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
    public Document explain(final Aggregate.Builder query) throws MongoDbException {
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
    public Document explain(final Aggregate query) throws MongoDbException {
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
    public Document explain(final DocumentAssignable query) throws MongoDbException {
        Document res = null;
        try {
            res = new FiberMongoCallback<Document>() {
                @Override
                protected void requestAsync() {
                    FiberMongoCollectionImpl.super.explainAsync(this, Find.builder().query(query));
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
    public Document findAndModify(final FindAndModify.Builder query) throws MongoDbException {
        Document res = null;
        try {
            res = new FiberMongoCallback<Document>() {
                @Override
                protected void requestAsync() {
                    FiberMongoCollectionImpl.super.findAndModifyAsync(this, query);
                }
            }.run();
        } catch (SuspendExecution | InterruptedException ex) {
            throw new AssertionError("Should never happen", ex);
        }
        return res;
    }

    @Override
    @Suspendable
    public Document findAndModify(final FindAndModify query) throws MongoDbException {
        Document res = null;
        try {
            res = new FiberMongoCallback<Document>() {
                @Override
                protected void requestAsync() {
                    FiberMongoCollectionImpl.super.findAndModifyAsync(this, query);
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
    public MongoIterator<Element> groupBy(final GroupBy.Builder command) throws MongoDbException {
        MongoIterator<Element> res = null;
        try {
            res = new FiberMongoCallback<MongoIterator<Element>>() {
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
    public MongoIterator<Element> groupBy(final GroupBy command) throws MongoDbException {
        MongoIterator<Element> res = null;
        try {
            res = new FiberMongoCallback<MongoIterator<Element>>() {
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
    public int insert(final Durability durability, final DocumentAssignable[] documents) throws MongoDbException {
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
    public MongoIterator<Document> mapReduce(final MapReduce.Builder command) throws MongoDbException {
        MongoIterator<Document> res = null;
        try {
            res = new FiberMongoCallback<MongoIterator<Document>>() {
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
    public MongoIterator<Document> mapReduce(final MapReduce command) throws MongoDbException {
        MongoIterator<Document> res = null;
        try {
            res = new FiberMongoCallback<MongoIterator<Document>>() {
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
    public Collection<MongoIterator<Document>> parallelScan(final ParallelScan.Builder command) throws MongoDbException {
        Collection<MongoIterator<Document>> res = null;
        try {
            res = new FiberMongoCallback<Collection<MongoIterator<Document>>>() {
                @Override
                protected void requestAsync() {
                    FiberMongoCollectionImpl.super.parallelScanAsync(this, command);
                }
            }.run();
        } catch (SuspendExecution | InterruptedException ex) {
            throw new AssertionError("Should never happen", ex);
        }
        return res;
    }

    @Override
    @Suspendable
    public Collection<MongoIterator<Document>> parallelScan(final ParallelScan command) throws MongoDbException {
        Collection<MongoIterator<Document>> res = null;
        try {
            res = new FiberMongoCallback<Collection<MongoIterator<Document>>>() {
                @Override
                protected void requestAsync() {
                    FiberMongoCollectionImpl.super.parallelScanAsync(this, command);
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
    @Deprecated
    public MongoIterator<TextResult> textSearch(final Text.Builder command) throws MongoDbException {
        MongoIterator<TextResult> res = null;
        try {
            res = new FiberMongoCallback<MongoIterator<TextResult>>() {
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
    @Deprecated
    public MongoIterator<TextResult> textSearch(final Text command) throws MongoDbException {
        MongoIterator<TextResult> res = null;
        try {
            res = new FiberMongoCallback<MongoIterator<TextResult>>() {
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

    @Override
    @Suspendable
    public long write(final BatchedWrite query) throws MongoDbException {
        Long res = null;
        try {
            res = new FiberMongoCallback<Long>() {
                @Override
                protected void requestAsync() {
                    FiberMongoCollectionImpl.super.writeAsync(this, query);
                }
            }.run();
        } catch (SuspendExecution | InterruptedException ex) {
            throw new AssertionError("Should never happen", ex);
        }
        return res;
    }

    @Override
    @Suspendable
    public long write(final BatchedWrite.Builder query) throws MongoDbException {
        Long res = null;
        try {
            res = new FiberMongoCallback<Long>() {
                @Override
                protected void requestAsync() {
                    FiberMongoCollectionImpl.super.writeAsync(this, query);
                }
            }.run();
        } catch (SuspendExecution | InterruptedException ex) {
            throw new AssertionError("Should never happen", ex);
        }
        return res;
    }

    
    // Async w/fiber-blocking future API
    
    @Override
    public ListenableFuture<MongoIterator<Document>> aggregateAsync(Aggregate.Builder command) throws MongoDbException {
        final SettableListenableFuture<MongoIterator<Document>> future = new SettableListenableFuture<>();
        aggregateAsync(FiberMongoUtils.callbackSettingFuture(future), command);
        return future;
    }

    @Override
    public ListenableFuture<MongoIterator<Document>> aggregateAsync(Aggregate command) throws MongoDbException {
        final SettableListenableFuture<MongoIterator<Document>> future = new SettableListenableFuture<>();
        aggregateAsync(FiberMongoUtils.callbackSettingFuture(future), command);
        return future;
    }

    @Override
    public ListenableFuture<Long> countAsync() throws MongoDbException {
        final SettableListenableFuture<Long> future = new SettableListenableFuture<>();
        countAsync(FiberMongoUtils.callbackSettingFuture(future));
        return future;
    }

    @Override
    public ListenableFuture<Long> countAsync(Count.Builder builder) throws MongoDbException {
        final SettableListenableFuture<Long> future = new SettableListenableFuture<>();
        countAsync(FiberMongoUtils.callbackSettingFuture(future), builder);
        return future;
    }

    @Override
    public ListenableFuture<Long> countAsync(Count query) throws MongoDbException {
        final SettableListenableFuture<Long> future = new SettableListenableFuture<>();
        countAsync(FiberMongoUtils.callbackSettingFuture(future), query);
        return future;
    }
    
    @Override
    public ListenableFuture<Long> countAsync(DocumentAssignable query) throws MongoDbException {
        final SettableListenableFuture<Long> future = new SettableListenableFuture<>();
        countAsync(FiberMongoUtils.callbackSettingFuture(future), query);
        return future;
    }

    @Override
    public ListenableFuture<Long> countAsync(DocumentAssignable query, ReadPreference readPreference) throws MongoDbException {
        final SettableListenableFuture<Long> future = new SettableListenableFuture<>();
        countAsync(FiberMongoUtils.callbackSettingFuture(future), query, readPreference);
        return future;
    }

    @Override
    public ListenableFuture<Long> countAsync(ReadPreference readPreference) throws MongoDbException {
        final SettableListenableFuture<Long> future = new SettableListenableFuture<>();
        countAsync(FiberMongoUtils.callbackSettingFuture(future), readPreference);
        return future;
    }

    @Override
    public ListenableFuture<Long> deleteAsync(DocumentAssignable query) throws MongoDbException {
        final SettableListenableFuture<Long> future = new SettableListenableFuture<>();
        deleteAsync(FiberMongoUtils.callbackSettingFuture(future), query);
        return future;
    }

    @Override
    public ListenableFuture<Long> deleteAsync(DocumentAssignable query, boolean singleDelete) throws MongoDbException {
        final SettableListenableFuture<Long> future = new SettableListenableFuture<>();
        deleteAsync(FiberMongoUtils.callbackSettingFuture(future), query, singleDelete);
        return future;
    }

    @Override
    public ListenableFuture<Long> deleteAsync(DocumentAssignable query, boolean singleDelete, Durability durability) throws MongoDbException {
        final SettableListenableFuture<Long> future = new SettableListenableFuture<>();
        deleteAsync(FiberMongoUtils.callbackSettingFuture(future), query, singleDelete, durability);
        return future;
    }

    @Override
    public ListenableFuture<Long> deleteAsync(DocumentAssignable query, Durability durability) throws MongoDbException {
        final SettableListenableFuture<Long> future = new SettableListenableFuture<>();
        deleteAsync(FiberMongoUtils.callbackSettingFuture(future), query, durability);
        return future;
    }

    @Override
    public ListenableFuture<MongoIterator<Element>> distinctAsync(Distinct.Builder command) throws MongoDbException {
        final SettableListenableFuture<MongoIterator<Element>> future = new SettableListenableFuture<>();
        distinctAsync(FiberMongoUtils.callbackSettingFuture(future), command);
        return future;
    }

    @Override
    public ListenableFuture<MongoIterator<Element>> distinctAsync(Distinct command) throws MongoDbException {
        final SettableListenableFuture<MongoIterator<Element>> future = new SettableListenableFuture<>();
        distinctAsync(FiberMongoUtils.callbackSettingFuture(future), command);
        return future;
    }

    @Override
    public ListenableFuture<Document> explainAsync(Aggregate.Builder query) throws MongoDbException {
        final SettableListenableFuture<Document> future = new SettableListenableFuture<>();
        explainAsync(FiberMongoUtils.callbackSettingFuture(future), query);
        return future;
    }

    @Override
    public ListenableFuture<Document> explainAsync(Aggregate query) throws MongoDbException {
        final SettableListenableFuture<Document> future = new SettableListenableFuture<>();
        explainAsync(FiberMongoUtils.callbackSettingFuture(future), query);
        return future;
    }
    
    @Override
    public ListenableFuture<Document> explainAsync(Find.Builder query) throws MongoDbException {
        final SettableListenableFuture<Document> future = new SettableListenableFuture<>();
        explainAsync(FiberMongoUtils.callbackSettingFuture(future), query);
        return future;
    }

    @Override
    public ListenableFuture<Document> explainAsync(Find query) throws MongoDbException {
        final SettableListenableFuture<Document> future = new SettableListenableFuture<>();
        explainAsync(FiberMongoUtils.callbackSettingFuture(future), query);
        return future;
    }

    @Override
    public ListenableFuture<MongoIterator<Document>> findAsync(DocumentAssignable query) throws MongoDbException {
        final SettableListenableFuture<MongoIterator<Document>> future = new SettableListenableFuture<>();
        findAsync(FiberMongoUtils.callbackSettingFuture(future), query);
        return future;
    }

    @Override
    public ListenableFuture<MongoIterator<Document>> findAsync(Find.Builder query) throws MongoDbException {
        final SettableListenableFuture<MongoIterator<Document>> future = new SettableListenableFuture<>();
        findAsync(FiberMongoUtils.callbackSettingFuture(future), query);
        return future;
    }

    @Override
    public ListenableFuture<MongoIterator<Document>> findAsync(Find query) throws MongoDbException {
        final SettableListenableFuture<MongoIterator<Document>> future = new SettableListenableFuture<>();
        findAsync(FiberMongoUtils.callbackSettingFuture(future), query);
        return future;
    }

    @Override
    public ListenableFuture<Document> findAndModifyAsync(FindAndModify query) throws MongoDbException {
        final SettableListenableFuture<Document> future = new SettableListenableFuture<>();
        findAndModifyAsync(FiberMongoUtils.callbackSettingFuture(future), query);
        return future;
    }

    @Override
    public ListenableFuture<Document> findAndModifyAsync(FindAndModify.Builder query) throws MongoDbException {
        final SettableListenableFuture<Document> future = new SettableListenableFuture<>();
        findAndModifyAsync(FiberMongoUtils.callbackSettingFuture(future), query);
        return future;
    }
    
    @Override
    public ListenableFuture<Document> findOneAsync(DocumentAssignable query) throws MongoDbException {
        final SettableListenableFuture<Document> future = new SettableListenableFuture<>();
        findOneAsync(FiberMongoUtils.callbackSettingFuture(future), query);
        return future;
    }

    @Override
    public ListenableFuture<Document> findOneAsync(Find.Builder query) throws MongoDbException {
        final SettableListenableFuture<Document> future = new SettableListenableFuture<>();
        findOneAsync(FiberMongoUtils.callbackSettingFuture(future), query);
        return future;
    }

    @Override
    public ListenableFuture<Document> findOneAsync(Find query) throws MongoDbException {
        final SettableListenableFuture<Document> future = new SettableListenableFuture<>();
        findOneAsync(FiberMongoUtils.callbackSettingFuture(future), query);
        return future;
    }

    @Override
    public ListenableFuture<MongoIterator<Element>> groupByAsync(GroupBy.Builder command) throws MongoDbException {
        final SettableListenableFuture<MongoIterator<Element>> future = new SettableListenableFuture<>();
        groupByAsync(FiberMongoUtils.callbackSettingFuture(future), command);
        return future;
    }

    @Override
    public ListenableFuture<MongoIterator<Element>> groupByAsync(GroupBy command) throws MongoDbException {
        final SettableListenableFuture<MongoIterator<Element>> future = new SettableListenableFuture<>();
        groupByAsync(FiberMongoUtils.callbackSettingFuture(future), command);
        return future;
    }

    @Override
    public ListenableFuture<Integer> insertAsync(DocumentAssignable... documents) throws MongoDbException {
        final SettableListenableFuture<Integer> future = new SettableListenableFuture<>();
        insertAsync(FiberMongoUtils.callbackSettingFuture(future), documents);
        return future;
    }

    @Override
    public ListenableFuture<Integer> insertAsync(Durability durability, DocumentAssignable... documents) throws MongoDbException {
        final SettableListenableFuture<Integer> future = new SettableListenableFuture<>();
        insertAsync(FiberMongoUtils.callbackSettingFuture(future), durability, documents);
        return future;
    }

    @Override
    public ListenableFuture<Integer> insertAsync(boolean continueOnError, DocumentAssignable... documents) throws MongoDbException {
        final SettableListenableFuture<Integer> future = new SettableListenableFuture<>();
        insertAsync(FiberMongoUtils.callbackSettingFuture(future), continueOnError, documents);
        return future;
    }

    @Override
    public ListenableFuture<Integer> insertAsync(boolean continueOnError, Durability durability, DocumentAssignable... documents) throws MongoDbException {
        final SettableListenableFuture<Integer> future = new SettableListenableFuture<>();
        insertAsync(FiberMongoUtils.callbackSettingFuture(future), continueOnError, durability, documents);
        return future;
    }

    @Override
    public ListenableFuture<MongoIterator<Document>> mapReduceAsync(MapReduce.Builder command) throws MongoDbException {
        final SettableListenableFuture<MongoIterator<Document>> future = new SettableListenableFuture<>();
        mapReduceAsync(FiberMongoUtils.callbackSettingFuture(future), command);
        return future;
    }

    @Override
    public ListenableFuture<MongoIterator<Document>> mapReduceAsync(MapReduce command) throws MongoDbException {
        final SettableListenableFuture<MongoIterator<Document>> future = new SettableListenableFuture<>();
        mapReduceAsync(FiberMongoUtils.callbackSettingFuture(future), command);
        return future;
    }
    
    @Override
    public ListenableFuture<Collection<MongoIterator<Document>>> parallelScanAsync(ParallelScan.Builder command) throws MongoDbException {
        final SettableListenableFuture<Collection<MongoIterator<Document>>> future = new SettableListenableFuture<>();
        parallelScanAsync(FiberMongoUtils.callbackSettingFuture(future), command);
        return future;
    }

    @Override
    public ListenableFuture<Collection<MongoIterator<Document>>> parallelScanAsync(ParallelScan command) throws MongoDbException {
        final SettableListenableFuture<Collection<MongoIterator<Document>>> future = new SettableListenableFuture<>();
        parallelScanAsync(FiberMongoUtils.callbackSettingFuture(future), command);
        return future;
    }

    @Override
    public ListenableFuture<Integer> saveAsync(DocumentAssignable query) throws MongoDbException {
        final SettableListenableFuture<Integer> future = new SettableListenableFuture<>();
        saveAsync(FiberMongoUtils.callbackSettingFuture(future), query);
        return future;
    }

    @Override
    public ListenableFuture<Integer> saveAsync(DocumentAssignable query, Durability durability) throws MongoDbException {
        final SettableListenableFuture<Integer> future = new SettableListenableFuture<>();
        saveAsync(FiberMongoUtils.callbackSettingFuture(future), query, durability);
        return future;
    }

    @Deprecated
    @Override
    public ListenableFuture<MongoIterator<TextResult>> textSearchAsync(Text.Builder command) throws MongoDbException {
        final SettableListenableFuture<MongoIterator<TextResult>> future = new SettableListenableFuture<>();
        textSearchAsync(FiberMongoUtils.callbackSettingFuture(future), command);
        return future;
    }

    @Deprecated
    @Override
    public ListenableFuture<MongoIterator<TextResult>> textSearchAsync(Text command) throws MongoDbException {
        final SettableListenableFuture<MongoIterator<TextResult>> future = new SettableListenableFuture<>();
        textSearchAsync(FiberMongoUtils.callbackSettingFuture(future), command);
        return future;
    }

    @Override
    public ListenableFuture<Long> updateAsync(DocumentAssignable query, DocumentAssignable update) throws MongoDbException {
        final SettableListenableFuture<Long> future = new SettableListenableFuture<>();
        updateAsync(FiberMongoUtils.callbackSettingFuture(future), query, update);
        return future;
    }

    @Override
    public ListenableFuture<Long> updateAsync(DocumentAssignable query, DocumentAssignable update, boolean multiUpdate, boolean upsert) throws MongoDbException {
        final SettableListenableFuture<Long> future = new SettableListenableFuture<>();
        updateAsync(FiberMongoUtils.callbackSettingFuture(future), query, update, multiUpdate, upsert);
        return future;
    }

    @Override
    public ListenableFuture<Long> updateAsync(DocumentAssignable query, DocumentAssignable update, boolean multiUpdate, boolean upsert, Durability durability) throws MongoDbException {
        final SettableListenableFuture<Long> future = new SettableListenableFuture<>();
        updateAsync(FiberMongoUtils.callbackSettingFuture(future), query, update, multiUpdate, upsert, durability);
        return future;
    }

    @Override
    public ListenableFuture<Long> updateAsync(DocumentAssignable query, DocumentAssignable update, Durability durability) throws MongoDbException {
        final SettableListenableFuture<Long> future = new SettableListenableFuture<>();
        updateAsync(FiberMongoUtils.callbackSettingFuture(future), query, update, durability);
        return future;
    }

    @Override
    public ListenableFuture<Long> writeAsync(BatchedWrite.Builder builder) throws MongoDbException {
        final SettableListenableFuture<Long> future = new SettableListenableFuture<>();
        writeAsync(FiberMongoUtils.callbackSettingFuture(future), builder);
        return future;
    }

    @Override
    public ListenableFuture<Long> writeAsync(BatchedWrite query) throws MongoDbException {
        final SettableListenableFuture<Long> future = new SettableListenableFuture<>();
        writeAsync(FiberMongoUtils.callbackSettingFuture(future), query);
        return future;
    }
}