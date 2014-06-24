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
import com.allanbank.mongodb.builder.Find;
import com.allanbank.mongodb.builder.FindAndModify;
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
                d(e("fieldString", "value2"), e("fieldInt", 2))
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
                insertTestSet();
            }
        }).start().join();
    }
    
    @After
    public void tearDownTest() throws ExecutionException, InterruptedException {
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                deleteTestSet();

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

    private static final Aggregate.Builder AGGREGATE_QUERY =
            Aggregate.builder()
                .match(d(e("fieldInt", d(e("$gt", 0), e("$lt", 3)))))
                .group(d(e("_id", (String) null), e("count", d(e("$sum", 1)))));
        
    @Test
    public void testAggregateQuery() throws ExecutionException, InterruptedException {
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                MongoIterator<Document> aggregate = mongoColl.aggregate(AGGREGATE_QUERY.build());
                assertEquals(TEST_SET_SIZE.toString(), getAggregateSizeAsString(aggregate));
            }
        }).start().join();
    }

    @Test
    public void testAggregateBuilder() throws ExecutionException, InterruptedException {
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                MongoIterator<Document> aggregate = mongoColl.aggregate(AGGREGATE_QUERY);
                assertEquals(TEST_SET_SIZE.toString(), getAggregateSizeAsString(aggregate));
            }
        }).start().join();
    }

    @Test
    public void testAggregateQueryFuture() throws ExecutionException, InterruptedException {
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                try {
                    MongoIterator<Document> aggregate = mongoColl.aggregateAsync(AGGREGATE_QUERY).get();
                    assertEquals(TEST_SET_SIZE.toString(), getAggregateSizeAsString(aggregate));
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
                    MongoIterator<Document> aggregate = mongoColl.aggregateAsync(AGGREGATE_QUERY).get();
                    assertEquals(TEST_SET_SIZE.toString(), getAggregateSizeAsString(aggregate));
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
                assertEquals((int) TEST_SET_SIZE, res);
            }
        }).start().join();
    }

    @Test
    public void testCountBuilder() throws ExecutionException, InterruptedException {
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                long res = mongoColl.count(Count.builder());
                assertEquals((int) TEST_SET_SIZE, res);
            }
        }).start().join();
    }

    @Test
    public void testCountQuery() throws ExecutionException, InterruptedException {
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                long res = mongoColl.count(Count.builder().build());
                assertEquals((int) TEST_SET_SIZE, res);
            }
        }).start().join();
    }
    
    @Test
    public void testCountQueryDocument() throws ExecutionException, InterruptedException {
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                long res = mongoColl.count();
                assertEquals((int) TEST_SET_SIZE, res);
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
                    assertEquals((int) TEST_SET_SIZE, res);
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
                    assertEquals((int) TEST_SET_SIZE, res);
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
                    assertEquals((int) TEST_SET_SIZE, res);
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
                    assertEquals((int) TEST_SET_SIZE, res);
                } catch (ExecutionException ex) {
                    fail(ex.getLocalizedMessage());
                }
            }
        }).start().join();
    }

    private static final DocumentAssignable DELETE_QUERY = d(e("fieldInt", 1));
    private static final int DELETE_SIZE = 1;
    
    @Test
    public void testDeleteQuery() throws ExecutionException, InterruptedException {
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                long deleted = mongoColl.delete(DELETE_QUERY);
                assertEquals(DELETE_SIZE, deleted);
            }
        }).start().join();
    }

    @Test
    public void testDeleteQuerySingle() throws ExecutionException, InterruptedException {
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                long deleted = mongoColl.delete(DELETE_QUERY, true);
                assertEquals(DELETE_SIZE, deleted);
            }
        }).start().join();
    }

    @Test
    public void testDeleteQuerySingleDurable() throws ExecutionException, InterruptedException {
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                long deleted = mongoColl.delete(DELETE_QUERY, true, Durability.ACK);
                assertEquals(DELETE_SIZE, deleted);
            }
        }).start().join();
    }

    @Test
    public void testDeleteQueryDurable() throws ExecutionException, InterruptedException {
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                long deleted = mongoColl.delete(DELETE_QUERY, Durability.ACK);
                assertEquals(DELETE_SIZE, deleted);
            }
        }).start().join();
    }

    @Test
    public void testDeleteQueryAsync() throws ExecutionException, InterruptedException {
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                try {
                    long deleted = mongoColl.deleteAsync(DELETE_QUERY).get();
                    assertEquals(DELETE_SIZE, deleted);
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
                    long deleted = mongoColl.deleteAsync(DELETE_QUERY, true).get();
                    assertEquals(DELETE_SIZE, deleted);
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
                    long deleted = mongoColl.deleteAsync(DELETE_QUERY, true, Durability.ACK).get();
                    assertEquals(DELETE_SIZE, deleted);
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
                    long deleted = mongoColl.deleteAsync(DELETE_QUERY, Durability.ACK).get();
                    assertEquals(DELETE_SIZE, deleted);
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

    private static final Distinct.Builder DISTINCT_QUERY = Distinct.builder().key("fieldInt");
        
    @Test
    public void testDistinctQuery() throws ExecutionException, InterruptedException {
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                MongoIterator<Element> distinct = mongoColl.distinct(DISTINCT_QUERY.build());
                assertEquals((long) TEST_SET_SIZE, getDistinctSize(distinct));
            }
        }).start().join();
    }

    @Test
    public void testDistinctBuilder() throws ExecutionException, InterruptedException {
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                MongoIterator<Element> distinct = mongoColl.distinct(DISTINCT_QUERY);
                assertEquals((long) TEST_SET_SIZE, getDistinctSize(distinct));
            }
        }).start().join();
    }

    @Test
    public void testDistinctQueryFuture() throws ExecutionException, InterruptedException {
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                try {
                    MongoIterator<Element> distinct = mongoColl.distinctAsync(DISTINCT_QUERY.build()).get();
                    assertEquals((long) TEST_SET_SIZE, getDistinctSize(distinct));
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
                    MongoIterator<Element> distinct = mongoColl.distinctAsync(DISTINCT_QUERY).get();
                    assertEquals((long) TEST_SET_SIZE, getDistinctSize(distinct));
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
                Document explain = mongoColl.explain(AGGREGATE_QUERY.build());
                assertNotNull(explain.get("stages"));
            }
        }).start().join();
    }

    @Test
    public void testExplainAggregateBuilder() throws ExecutionException, InterruptedException {
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                Document explain = mongoColl.explain(AGGREGATE_QUERY);
                assertNotNull(explain.get("stages"));
            }
        }).start().join();
    }

    private static final DocumentAssignable FIND_QUERY_DOCUMENT = d(e("fieldInt", 1));
    
    @Test
    public void testExplainFindQueryDocument() throws ExecutionException, InterruptedException {
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                Document explain = mongoColl.explain(FIND_QUERY_DOCUMENT);
                assertNotNull(explain.get("cursor"));
            }
        }).start().join();
    }

    @Test
    public void testExplainFindQuery() throws ExecutionException, InterruptedException {
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                Document explain = mongoColl.explain(Find.builder().query(FIND_QUERY_DOCUMENT).build());
                assertNotNull(explain.get("cursor"));
            }
        }).start().join();
    }

    @Test
    public void testExplainFindBuilder() throws ExecutionException, InterruptedException {
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                Document explain = mongoColl.explain(Find.builder().query(FIND_QUERY_DOCUMENT));
                assertNotNull(explain.get("cursor"));
            }
        }).start().join();
    }
    
    @Test
    public void testExplainAggregateQueryFuture() throws ExecutionException, InterruptedException {
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                try {
                    Document explain = mongoColl.explainAsync(AGGREGATE_QUERY.build()).get();
                    assertNotNull(explain.get("stages"));
                } catch (ExecutionException ex) {
                    fail(ex.getLocalizedMessage());
                }
            }
        }).start().join();
    }

    @Test
    public void testExplainAggregateBuilderFuture() throws ExecutionException, InterruptedException {
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                try {
                    Document explain = mongoColl.explainAsync(AGGREGATE_QUERY).get();
                    assertNotNull(explain.get("stages"));
                } catch (ExecutionException ex) {
                    fail(ex.getLocalizedMessage());
                }
            }
        }).start().join();
    }

    @Test
    public void testExplainFindQueryFuture() throws ExecutionException, InterruptedException {
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                try {
                    Document explain = mongoColl.explainAsync(Find.builder().query(FIND_QUERY_DOCUMENT).build()).get();
                    assertNotNull(explain.get("cursor"));
                } catch (ExecutionException ex) {
                    fail(ex.getLocalizedMessage());
                }
            }
        }).start().join();
    }

    @Test
    public void testExplainFindBuilderFuture() throws ExecutionException, InterruptedException {
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                try {
                    Document explain = mongoColl.explainAsync(Find.builder().query(FIND_QUERY_DOCUMENT)).get();
                    assertNotNull(explain.get("cursor"));
                } catch (ExecutionException ex) {
                    fail(ex.getLocalizedMessage());
                }
            }
        }).start().join();
    }
    
    @Test
    public void testFindQueryDocument() throws ExecutionException, InterruptedException {
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                MongoIterator<Document> find = mongoColl.find(FIND_QUERY_DOCUMENT);
                assertTrue(find.hasNext());
            }
        }).start().join();
    }

    @Test
    public void testFindQuery() throws ExecutionException, InterruptedException {
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                MongoIterator<Document> find = mongoColl.find(Find.builder().query(FIND_QUERY_DOCUMENT).build());
                assertTrue(find.hasNext());
            }
        }).start().join();
    }

    @Test
    public void testFindBuilder() throws ExecutionException, InterruptedException {
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                MongoIterator<Document> find = mongoColl.find(Find.builder().query(FIND_QUERY_DOCUMENT));
                assertTrue(find.hasNext());
            }
        }).start().join();
    }

    @Test
    public void testFindQueryFuture() throws ExecutionException, InterruptedException {
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                try {
                    MongoIterator<Document> find = mongoColl.findAsync(Find.builder().query(FIND_QUERY_DOCUMENT).build()).get();
                    assertTrue(find.hasNext());
                } catch (ExecutionException ex) {
                    fail(ex.getLocalizedMessage());
                }
            }
        }).start().join();
    }

    @Test
    public void testFindQueryDocumentFuture() throws ExecutionException, InterruptedException {
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                try {
                    MongoIterator<Document> find = mongoColl.findAsync(FIND_QUERY_DOCUMENT).get();
                    assertTrue(find.hasNext());
                } catch (ExecutionException ex) {
                    fail(ex.getLocalizedMessage());
                }
            }
        }).start().join();
    }

    @Test
    public void testFindBuilderFuture() throws ExecutionException, InterruptedException {
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                try {
                    MongoIterator<Document> find = mongoColl.findAsync(Find.builder().query(FIND_QUERY_DOCUMENT)).get();
                    assertTrue(find.hasNext());
                } catch (ExecutionException ex) {
                    fail(ex.getLocalizedMessage());
                }
            }
        }).start().join();
    }

    private static final Integer UPDATE_VALUE = 10;
    private static final DocumentAssignable UPDATE_DOCUMENT = d(e("fieldInt", UPDATE_VALUE));
    
    private static final FindAndModify.Builder FIND_AND_MODIFY_BUILDER =
            FindAndModify.builder().query(FIND_QUERY_DOCUMENT).setUpdate(UPDATE_DOCUMENT).setReturnNew(true);
    
    @Test
    public void testFindAndModifyQuery() throws ExecutionException, InterruptedException {
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                Document findAndModify = mongoColl.findAndModify(FIND_AND_MODIFY_BUILDER.build());
                assertEquals(findAndModify.get("fieldInt").getValueAsString(), UPDATE_VALUE.toString());
            }
        }).start().join();
    }

    @Test
    public void testFindAndModifyBuilder() throws ExecutionException, InterruptedException {
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                Document findAndModify = mongoColl.findAndModify(FIND_AND_MODIFY_BUILDER);
                assertEquals(findAndModify.get("fieldInt").getValueAsString(), UPDATE_VALUE.toString());
            }
        }).start().join();
    }
    
    @Test
    public void testFindAndModifyQueryAsync() throws ExecutionException, InterruptedException {
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                try {
                    Document findAndModify = mongoColl.findAndModifyAsync(FIND_AND_MODIFY_BUILDER.build()).get();
                    assertEquals(findAndModify.get("fieldInt").getValueAsString(), UPDATE_VALUE.toString());
                } catch (ExecutionException ex) {
                    fail(ex.getLocalizedMessage());
                }
            }
        }).start().join();
    }

    @Test
    public void testFindAndModifyBuilderAsync() throws ExecutionException, InterruptedException {
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                try {
                    Document findAndModify = mongoColl.findAndModifyAsync(FIND_AND_MODIFY_BUILDER).get();
                    assertEquals(findAndModify.get("fieldInt").getValueAsString(), UPDATE_VALUE.toString());
                } catch (ExecutionException ex) {
                    fail(ex.getLocalizedMessage());
                }
            }
        }).start().join();
    }

    @Test
    public void testFindOneQueryDocument() throws ExecutionException, InterruptedException {
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                Document find = mongoColl.findOne(FIND_QUERY_DOCUMENT);
                assertEquals(find.get("fieldInt").getValueAsString(), "1");
            }
        }).start().join();
    }

    @Test
    public void testFindOneQuery() throws ExecutionException, InterruptedException {
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                Document find = mongoColl.findOne(Find.builder().query(FIND_QUERY_DOCUMENT).build());
                assertEquals(find.get("fieldInt").getValueAsString(), "1");
            }
        }).start().join();
    }

    @Test
    public void testFindOneBuilder() throws ExecutionException, InterruptedException {
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                Document find = mongoColl.findOne(Find.builder().query(FIND_QUERY_DOCUMENT).build());
                assertEquals(find.get("fieldInt").getValueAsString(), "1");
            }
        }).start().join();
    }
    
    @Test
    public void testFindOneQueryDocumentAsync() throws ExecutionException, InterruptedException {
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                try {
                    Document find = mongoColl.findOneAsync(FIND_QUERY_DOCUMENT).get();
                    assertEquals(find.get("fieldInt").getValueAsString(), "1");
                } catch (ExecutionException ex) {
                    fail(ex.getLocalizedMessage());
                }
            }
        }).start().join();
    }

    @Test
    public void testFindOneQueryAsync() throws ExecutionException, InterruptedException {
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                try {
                    Document find = mongoColl.findOneAsync(Find.builder().query(FIND_QUERY_DOCUMENT).build()).get();
                    assertEquals(find.get("fieldInt").getValueAsString(), "1");
                } catch (ExecutionException ex) {
                    fail(ex.getLocalizedMessage());
                }
            }
        }).start().join();
    }

    @Test
    public void testFindOneBuilderAsync() throws ExecutionException, InterruptedException {
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                try {
                    Document find = mongoColl.findOneAsync(Find.builder().query(FIND_QUERY_DOCUMENT)).get();
                    assertEquals(find.get("fieldInt").getValueAsString(), "1");
                } catch (ExecutionException ex) {
                    fail(ex.getLocalizedMessage());
                }
            }
        }).start().join();
    }
}