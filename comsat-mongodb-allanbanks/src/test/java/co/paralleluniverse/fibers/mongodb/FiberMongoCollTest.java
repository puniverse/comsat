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

import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.SuspendableRunnable;
import com.allanbank.mongodb.Durability;
import com.allanbank.mongodb.MongoCollection;
import com.allanbank.mongodb.MongoDbException;
import com.allanbank.mongodb.MongoIterator;
import com.allanbank.mongodb.bson.Document;
import com.allanbank.mongodb.bson.DocumentAssignable;
import com.allanbank.mongodb.bson.Element;
import static com.allanbank.mongodb.bson.builder.BuilderFactory.*;
import com.allanbank.mongodb.builder.Aggregate;
import com.allanbank.mongodb.builder.Count;
import com.allanbank.mongodb.builder.Distinct;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

/**
 * @author circlespainter
 */
public class FiberMongoCollTest extends AbstractTestFiberMongo {
    private static final Integer TEST_SET_SIZE = 2;

    private MongoCollection mongoColl;

    private int insertTestSet() throws SuspendExecution, MongoDbException, IllegalArgumentException {
        return mongoColl.insert (
                Durability.ACK,
                d(e("fieldString", "value1"), e("fieldInt", 1)),
                d(e("fieldString", "value2"), e("fieldInt", TEST_SET_SIZE))
        );
    }

    private long deleteTestSet() throws SuspendExecution, MongoDbException, IllegalArgumentException {
        return mongoColl.delete(d());
    }
    
    @Before
    public void setUpTest() throws ExecutionException, InterruptedException {
        super.setUpDbForTest();
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                mongoColl = mongoDb.getCollection("test");
            }
        }).start().join();
    }
    
    @After
    public void tearDownTest() throws ExecutionException, InterruptedException {
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                if (mongoColl != null) {
                    mongoColl.drop();
                    mongoColl = null;
                }
            }
        }).start().join();
        super.tearDownDbForTest();
    }

    private static String getAggregateSizeAsString(MongoIterator<Document> aggregate) {
        return aggregate.next().get("count").getValueAsString();
    }

    private static Aggregate.Builder getAggregateQuery() throws IllegalArgumentException {
        return Aggregate.builder()
                .match(d(e("fieldInt", d(e("$gt", 0), e("$lt", 3)))))
                .group(d(e("_id", (String) null), e("count", d(e("$sum", 1)))));
    }
        
    @Test
    public void testAggregateQuery() throws ExecutionException, InterruptedException {
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                long inserted = insertTestSet();
                assertEquals((int) TEST_SET_SIZE, inserted);
                MongoIterator<Document> aggregate = mongoColl.aggregate(getAggregateQuery().build());
                assertEquals(TEST_SET_SIZE.toString(), getAggregateSizeAsString(aggregate));
                long deleted = deleteTestSet();
                assertEquals((int) TEST_SET_SIZE, deleted);
            }
        }).start().join();
    }

    @Test
    public void testAggregateBuilder() throws ExecutionException, InterruptedException {
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                long inserted = insertTestSet();
                assertEquals((int) TEST_SET_SIZE, inserted);
                MongoIterator<Document> aggregate = mongoColl.aggregate(getAggregateQuery());
                assertEquals(TEST_SET_SIZE.toString(), getAggregateSizeAsString(aggregate));
                long deleted = deleteTestSet();
                assertEquals((int) TEST_SET_SIZE, deleted);
            }
        }).start().join();
    }

    @Test
    public void testAggregateQueryFuture() throws ExecutionException, InterruptedException {
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                try {
                    long inserted = insertTestSet();
                    assertEquals((int) TEST_SET_SIZE, inserted);
                    MongoIterator<Document> aggregate = mongoColl.aggregateAsync(getAggregateQuery().build()).get();
                    assertEquals(TEST_SET_SIZE.toString(), getAggregateSizeAsString(aggregate));
                    long deleted = deleteTestSet();
                    assertEquals((int) TEST_SET_SIZE, deleted);
                } catch (ExecutionException ex) {
                    fail(ex.getLocalizedMessage());
                }
            }
        }).start().join();
    }

    @Test
    public void testAggregateBuilderFuture() throws ExecutionException, InterruptedException {
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                try {
                    long inserted = insertTestSet();
                    assertEquals((int) TEST_SET_SIZE, inserted);
                    MongoIterator<Document> aggregate = mongoColl.aggregateAsync(getAggregateQuery()).get();
                    assertEquals(TEST_SET_SIZE.toString(), getAggregateSizeAsString(aggregate));
                    long deleted = deleteTestSet();
                    assertEquals((int) TEST_SET_SIZE, deleted);
                } catch (ExecutionException ex) {
                    fail(ex.getLocalizedMessage());
                }
            }
        }).start().join();
    }
    
    @Test
    public void testCount() throws ExecutionException, InterruptedException {
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                long res = mongoColl.count();
                assertEquals(0, res);
            }
        }).start().join();
    }

    @Test
    public void testCountBuilder() throws ExecutionException, InterruptedException {
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                long res = mongoColl.count(Count.builder());
                assertEquals(0, res);
            }
        }).start().join();
    }

    @Test
    public void testCountQuery() throws ExecutionException, InterruptedException {
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                long res = mongoColl.count(Count.builder().build());
                assertEquals(0, res);
            }
        }).start().join();
    }
    
    @Test
    public void testCountQueryDocument() throws ExecutionException, InterruptedException {
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                long res = mongoColl.count();
                assertEquals(0, res);
            }
        }).start().join();
    }

    @Test
    public void testCountAsync() throws ExecutionException, InterruptedException {
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                try {
                    long res = mongoColl.countAsync().get();
                    assertEquals(0, res);
                } catch (ExecutionException ex) {
                    fail(ex.getLocalizedMessage());
                }
            }
        }).start().join();
    }

    @Test
    public void testCountBuilderAsync() throws ExecutionException, InterruptedException {
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                try {
                    long res = mongoColl.countAsync(Count.builder()).get();
                    assertEquals(0, res);
                } catch (ExecutionException ex) {
                    fail(ex.getLocalizedMessage());
                }
            }
        }).start().join();
    }

    @Test
    public void testCountQueryAsync() throws ExecutionException, InterruptedException {
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                try {
                    long res = mongoColl.countAsync(Count.builder().build()).get();
                    assertEquals(0, res);
                } catch (ExecutionException ex) {
                    fail(ex.getLocalizedMessage());
                }
            }
        }).start().join();
    }
    
    @Test
    public void testCountQueryDocumentAsync() throws ExecutionException, InterruptedException {
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                try {
                    long res = mongoColl.countAsync().get();
                    assertEquals(0, res);
                } catch (ExecutionException ex) {
                    fail(ex.getLocalizedMessage());
                }
            }
        }).start().join();
    }

    private static DocumentAssignable getDeleteQuery() throws IllegalArgumentException {
        return d(e("fieldInt", 1));
    }
    
    private static long getDeleteSize() throws IllegalArgumentException { return 1; }
    
    @Test
    public void testDeleteQuery() throws ExecutionException, InterruptedException {
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                long inserted = insertTestSet();
                assertEquals((int) TEST_SET_SIZE, inserted);
                long deleted = mongoColl.delete(getDeleteQuery());
                assertEquals(getDeleteSize(), deleted);
                deleted = deleteTestSet();
                assertEquals((int) TEST_SET_SIZE - getDeleteSize(), deleted);
            }
        }).start().join();
    }

    @Test
    public void testDeleteQuerySingle() throws ExecutionException, InterruptedException {
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                long inserted = insertTestSet();
                assertEquals((int) TEST_SET_SIZE, inserted);
                long deleted = mongoColl.delete(getDeleteQuery(), true);
                assertEquals(getDeleteSize(), deleted);
                deleted = deleteTestSet();
                assertEquals((int) TEST_SET_SIZE - getDeleteSize(), deleted);
            }
        }).start().join();
    }

    @Test
    public void testDeleteQuerySingleDurable() throws ExecutionException, InterruptedException {
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                long inserted = insertTestSet();
                assertEquals((int) TEST_SET_SIZE, inserted);
                long deleted = mongoColl.delete(getDeleteQuery(), true, Durability.ACK);
                assertEquals(getDeleteSize(), deleted);
                deleted = deleteTestSet();
                assertEquals((int) TEST_SET_SIZE - getDeleteSize(), deleted);
            }
        }).start().join();
    }

    @Test
    public void testDeleteQueryDurable() throws ExecutionException, InterruptedException {
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                long inserted = insertTestSet();
                assertEquals((int) TEST_SET_SIZE, inserted);
                long deleted = mongoColl.delete(getDeleteQuery(), Durability.ACK);
                assertEquals(getDeleteSize(), deleted);
                deleted = deleteTestSet();
                assertEquals((int) TEST_SET_SIZE - getDeleteSize(), deleted);
            }
        }).start().join();
    }

    @Test
    public void testDeleteQueryAsync() throws ExecutionException, InterruptedException {
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                try {
                    long inserted = insertTestSet();
                    assertEquals((int) TEST_SET_SIZE, inserted);
                    long deleted = mongoColl.deleteAsync(getDeleteQuery()).get();
                    assertEquals(getDeleteSize(), deleted);
                    deleted = deleteTestSet();
                    assertEquals((int) TEST_SET_SIZE - getDeleteSize(), deleted);
                } catch (ExecutionException ex) {
                    fail(ex.getLocalizedMessage());
                }
            }
        }).start().join();
    }

    @Test
    public void testDeleteQuerySingleAsync() throws ExecutionException, InterruptedException {
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                try {
                    long inserted = insertTestSet();
                    assertEquals((int) TEST_SET_SIZE, inserted);
                    long deleted = mongoColl.deleteAsync(getDeleteQuery(), true).get();
                    assertEquals(getDeleteSize(), deleted);
                    deleted = deleteTestSet();
                    assertEquals((int) TEST_SET_SIZE - getDeleteSize(), deleted);
                } catch (ExecutionException ex) {
                    fail(ex.getLocalizedMessage());
                }
            }
        }).start().join();
    }

    @Test
    public void testDeleteQuerySingleDurableAsync() throws ExecutionException, InterruptedException {
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                try {
                    long inserted = insertTestSet();
                    assertEquals((int) TEST_SET_SIZE, inserted);
                    long deleted = mongoColl.deleteAsync(getDeleteQuery(), true, Durability.ACK).get();
                    assertEquals(getDeleteSize(), deleted);
                    deleted = deleteTestSet();
                    assertEquals((int) TEST_SET_SIZE - getDeleteSize(), deleted);
                } catch (ExecutionException ex) {
                    fail(ex.getLocalizedMessage());
                }
            }
        }).start().join();
    }

    @Test
    public void testDeleteQueryDurableAsync() throws ExecutionException, InterruptedException {
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                try {
                    long inserted = insertTestSet();
                    assertEquals((int) TEST_SET_SIZE, inserted);
                    long deleted = mongoColl.deleteAsync(getDeleteQuery(), Durability.ACK).get();
                    assertEquals(getDeleteSize(), deleted);
                    deleted = deleteTestSet();
                    assertEquals((int) TEST_SET_SIZE - getDeleteSize(), deleted);
                } catch (ExecutionException ex) {
                    fail(ex.getLocalizedMessage());
                }
            }
        }).start().join();
    }

    private static long getDistinctSize(MongoIterator<Element> distinct) throws IllegalArgumentException {
        long res = 0;
        while (distinct.hasNext()) { distinct.next(); res++; }
        return res;
    }

    private static Distinct.Builder getDistinctQuery() throws IllegalArgumentException {
        return Distinct.builder().key("fieldInt");
    }
        
    @Test
    public void testDistinctQuery() throws ExecutionException, InterruptedException {
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                long inserted = insertTestSet();
                assertEquals((int) TEST_SET_SIZE, inserted);
                MongoIterator<Element> distinct = mongoColl.distinct(getDistinctQuery().build());
                assertEquals((long) TEST_SET_SIZE, getDistinctSize(distinct));
                long deleted = deleteTestSet();
                assertEquals((int) TEST_SET_SIZE, deleted);
            }
        }).start().join();
    }

    @Test
    public void testDistinctBuilder() throws ExecutionException, InterruptedException {
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                long inserted = insertTestSet();
                assertEquals((int) TEST_SET_SIZE, inserted);
                MongoIterator<Element> distinct = mongoColl.distinct(getDistinctQuery());
                assertEquals((long) TEST_SET_SIZE, getDistinctSize(distinct));
                long deleted = deleteTestSet();
                assertEquals((int) TEST_SET_SIZE, deleted);
            }
        }).start().join();
    }

    @Test
    public void testDistinctQueryFuture() throws ExecutionException, InterruptedException {
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                try {
                    long inserted = insertTestSet();
                    assertEquals((int) TEST_SET_SIZE, inserted);
                    MongoIterator<Element> distinct = mongoColl.distinctAsync(getDistinctQuery().build()).get();
                    assertEquals((long) TEST_SET_SIZE, getDistinctSize(distinct));
                    long deleted = deleteTestSet();
                    assertEquals((int) TEST_SET_SIZE, deleted);
                } catch (ExecutionException ex) {
                    fail(ex.getLocalizedMessage());
                }
            }
        }).start().join();
    }

    @Test
    public void testDistinctBuilderFuture() throws ExecutionException, InterruptedException {
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                try {
                    long inserted = insertTestSet();
                    assertEquals((int) TEST_SET_SIZE, inserted);
                    MongoIterator<Element> distinct = mongoColl.distinctAsync(getDistinctQuery()).get();
                    assertEquals((long) TEST_SET_SIZE, getDistinctSize(distinct));
                    long deleted = deleteTestSet();
                    assertEquals((int) TEST_SET_SIZE, deleted);
                } catch (ExecutionException ex) {
                    fail(ex.getLocalizedMessage());
                }
            }
        }).start().join();
    }

    @Test
    public void testExplainAggregateQuery() throws ExecutionException, InterruptedException {
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                long inserted = insertTestSet();
                assertEquals((int) TEST_SET_SIZE, inserted);
                Document explain = mongoColl.explain(getAggregateQuery().build());
                assertNotNull(explain);
                long deleted = deleteTestSet();
                assertEquals((int) TEST_SET_SIZE, deleted);
            }
        }).start().join();
    }

    @Test
    public void testExplainAggregateBuilder() throws ExecutionException, InterruptedException {
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                long inserted = insertTestSet();
                assertEquals((int) TEST_SET_SIZE, inserted);
                Document explain = mongoColl.explain(getAggregateQuery());
                assertNotNull(explain);
                long deleted = deleteTestSet();
                assertEquals((int) TEST_SET_SIZE, deleted);
            }
        }).start().join();
    }
}