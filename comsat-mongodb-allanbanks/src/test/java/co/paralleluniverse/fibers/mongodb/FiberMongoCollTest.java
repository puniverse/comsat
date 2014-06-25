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
import com.allanbank.mongodb.builder.BatchedWrite;
import com.allanbank.mongodb.builder.Count;
import com.allanbank.mongodb.builder.Distinct;
import com.allanbank.mongodb.builder.Find;
import com.allanbank.mongodb.builder.FindAndModify;
import com.allanbank.mongodb.builder.GroupBy;
import com.allanbank.mongodb.builder.Index;
import com.allanbank.mongodb.builder.MapReduce;
import com.allanbank.mongodb.builder.ParallelScan;
import com.allanbank.mongodb.builder.Text;
import com.allanbank.mongodb.builder.TextResult;
import java.util.Collection;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

/**
 * @author circlespainter
 * 
 * TODO Missing tests:
 * 
 * 1) Non-async suspendable methods (already declared in "suspendables"); anyway some are already used as part of test setup
 * 2) ListenableFuture functionality
 * 3) New fiber-blocking APIs for now async-only operations (when added, @see co.paralleluniverse.fibers.mongodb.FiberMongoCollectionImpl)
 */
public class FiberMongoCollTest extends AbstractTestFiberMongo {
    private static final String COLL_NAME = "test";
    
    private static final Integer TEST_SET_SIZE = 2;

    private static final String FIELD_INT_NAME = "fieldInt";
    private static final String FIELD_TY_NAME = "ty";
    private static final Integer FIELD_INT_FIRST_VALUE = 1;
    private static final Integer TEST_SET_DOCS_COUNT_WITH_FIELD_INT_FIRST_VALUE = 1;

    private static final Element ELEM_TY = e(FIELD_TY_NAME, "test");
    private static final String TEXT_SEARCH_TERM = "value";
    private static final String FIELD_STRING_NAME = "fieldString";
    private static final Element ELEM_FIELD_STRING_1 = e(FIELD_STRING_NAME, TEXT_SEARCH_TERM + " 1");
    private static final DocumentAssignable FIRST_TEST_ELEM = d(ELEM_TY, ELEM_FIELD_STRING_1, e(FIELD_INT_NAME, FIELD_INT_FIRST_VALUE));
    private static final DocumentAssignable SECOND_TEST_ELEM = d(ELEM_TY, e(FIELD_STRING_NAME, TEXT_SEARCH_TERM + " 2"), e(FIELD_INT_NAME, 2));
    
    private MongoCollection mongoColl;
    
    private int insertTestSet() throws SuspendExecution, MongoDbException, IllegalArgumentException {
        return mongoColl.insert(Durability.ACK, FIRST_TEST_ELEM, SECOND_TEST_ELEM);
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
                mongoColl = mongoDb.getCollection(COLL_NAME);
                mongoColl.createIndex(Index.text(FIELD_STRING_NAME));
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

    private static final String FIELD_COUNT_NAME = "count";
    
    private static String getAggregateSizeAsString(MongoIterator<Document> aggregate) {
        return aggregate.next().get(FIELD_COUNT_NAME).getValueAsString();
    }

    private static final Aggregate.Builder AGGREGATE_QUERY =
            Aggregate.builder()
                .match(d(e(FIELD_INT_NAME, d(e("$gt", 0), e("$lt", 3)))))
                .group(d(e("_id", (String) null), e(FIELD_COUNT_NAME, d(e("$sum", 1)))));
        
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
    public void testCountFuture() throws ExecutionException, InterruptedException {
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
    public void testCountBuilderFuture() throws ExecutionException, InterruptedException {
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
    public void testCountQueryFuture() throws ExecutionException, InterruptedException {
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
    public void testCountQueryDocumentFuture() throws ExecutionException, InterruptedException {
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

    private static final DocumentAssignable DELETE_DOCUMENT = d(e(FIELD_INT_NAME, FIELD_INT_FIRST_VALUE));
    private static final int DELETE_SIZE = TEST_SET_DOCS_COUNT_WITH_FIELD_INT_FIRST_VALUE;
    
    @Test
    public void testDeleteQuery() throws ExecutionException, InterruptedException {
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                long deleted = mongoColl.delete(DELETE_DOCUMENT);
                assertEquals(DELETE_SIZE, deleted);
            }
        }).start().join();
    }

    @Test
    public void testDeleteQuerySingle() throws ExecutionException, InterruptedException {
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                long deleted = mongoColl.delete(DELETE_DOCUMENT, true);
                assertEquals(DELETE_SIZE, deleted);
            }
        }).start().join();
    }

    @Test
    public void testDeleteQuerySingleDurable() throws ExecutionException, InterruptedException {
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                long deleted = mongoColl.delete(DELETE_DOCUMENT, true, Durability.ACK);
                assertEquals(DELETE_SIZE, deleted);
            }
        }).start().join();
    }

    @Test
    public void testDeleteQueryDurable() throws ExecutionException, InterruptedException {
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                long deleted = mongoColl.delete(DELETE_DOCUMENT, Durability.ACK);
                assertEquals(DELETE_SIZE, deleted);
            }
        }).start().join();
    }

    @Test
    public void testDeleteQueryFuture() throws ExecutionException, InterruptedException {
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                try {
                    long deleted = mongoColl.deleteAsync(DELETE_DOCUMENT).get();
                    assertEquals(DELETE_SIZE, deleted);
                } catch (ExecutionException ex) {
                    fail(ex.getLocalizedMessage());
                }
            }
        }).start().join();
    }

    @Test
    public void testDeleteQuerySingleFuture() throws ExecutionException, InterruptedException {
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                try {
                    long deleted = mongoColl.deleteAsync(DELETE_DOCUMENT, true).get();
                    assertEquals(DELETE_SIZE, deleted);
                } catch (ExecutionException ex) {
                    fail(ex.getLocalizedMessage());
                }
            }
        }).start().join();
    }

    @Test
    public void testDeleteQuerySingleDurableFuture() throws ExecutionException, InterruptedException {
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                try {
                    long deleted = mongoColl.deleteAsync(DELETE_DOCUMENT, true, Durability.ACK).get();
                    assertEquals(DELETE_SIZE, deleted);
                } catch (ExecutionException ex) {
                    fail(ex.getLocalizedMessage());
                }
            }
        }).start().join();
    }

    @Test
    public void testDeleteQueryDurableFuture() throws ExecutionException, InterruptedException {
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                try {
                    long deleted = mongoColl.deleteAsync(DELETE_DOCUMENT, Durability.ACK).get();
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

    private static final Distinct.Builder DISTINCT_QUERY = Distinct.builder().key(FIELD_INT_NAME);
        
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

    private static final String FIELD_AGGREGATE_RESULT_STAGES_NAME = "stages";
    private static final String FIELD_AGGREGATE_RESULT_CURSOR_NAME = "cursor";

    @Test
    public void testExplainAggregateQuery() throws ExecutionException, InterruptedException {
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                Document explain = mongoColl.explain(AGGREGATE_QUERY.build());
                assertNotNull(explain.get(FIELD_AGGREGATE_RESULT_STAGES_NAME));
            }
        }).start().join();
    }

    @Test
    public void testExplainAggregateBuilder() throws ExecutionException, InterruptedException {
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                Document explain = mongoColl.explain(AGGREGATE_QUERY);
                assertNotNull(explain.get(FIELD_AGGREGATE_RESULT_STAGES_NAME));
            }
        }).start().join();
    }

    private static final DocumentAssignable FIND_DOCUMENT = d(e(FIELD_INT_NAME, FIELD_INT_FIRST_VALUE));
    
    @Test
    public void testExplainFindQueryDocument() throws ExecutionException, InterruptedException {
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                Document explain = mongoColl.explain(FIND_DOCUMENT);
                assertNotNull(explain.get(FIELD_AGGREGATE_RESULT_CURSOR_NAME));
            }
        }).start().join();
    }

    @Test
    public void testExplainFindQuery() throws ExecutionException, InterruptedException {
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                Document explain = mongoColl.explain(Find.builder().query(FIND_DOCUMENT).build());
                assertNotNull(explain.get(FIELD_AGGREGATE_RESULT_CURSOR_NAME));
            }
        }).start().join();
    }

    @Test
    public void testExplainFindBuilder() throws ExecutionException, InterruptedException {
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                Document explain = mongoColl.explain(Find.builder().query(FIND_DOCUMENT));
                assertNotNull(explain.get(FIELD_AGGREGATE_RESULT_CURSOR_NAME));
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
                    assertNotNull(explain.get(FIELD_AGGREGATE_RESULT_STAGES_NAME));
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
                    assertNotNull(explain.get(FIELD_AGGREGATE_RESULT_STAGES_NAME));
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
                    Document explain = mongoColl.explainAsync(Find.builder().query(FIND_DOCUMENT).build()).get();
                    assertNotNull(explain.get(FIELD_AGGREGATE_RESULT_CURSOR_NAME));
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
                    Document explain = mongoColl.explainAsync(Find.builder().query(FIND_DOCUMENT)).get();
                    assertNotNull(explain.get(FIELD_AGGREGATE_RESULT_CURSOR_NAME));
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
                MongoIterator<Document> find = mongoColl.find(FIND_DOCUMENT);
                assertTrue(find.hasNext());
            }
        }).start().join();
    }

    @Test
    public void testFindQuery() throws ExecutionException, InterruptedException {
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                MongoIterator<Document> find = mongoColl.find(Find.builder().query(FIND_DOCUMENT).build());
                assertTrue(find.hasNext());
            }
        }).start().join();
    }

    @Test
    public void testFindBuilder() throws ExecutionException, InterruptedException {
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                MongoIterator<Document> find = mongoColl.find(Find.builder().query(FIND_DOCUMENT));
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
                    MongoIterator<Document> find = mongoColl.findAsync(Find.builder().query(FIND_DOCUMENT).build()).get();
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
                    MongoIterator<Document> find = mongoColl.findAsync(FIND_DOCUMENT).get();
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
                    MongoIterator<Document> find = mongoColl.findAsync(Find.builder().query(FIND_DOCUMENT)).get();
                    assertTrue(find.hasNext());
                } catch (ExecutionException ex) {
                    fail(ex.getLocalizedMessage());
                }
            }
        }).start().join();
    }

    private static final Integer UPDATE_VALUE = 10;
    private static final Element UPDATE_ELEMENT = e(FIELD_INT_NAME, UPDATE_VALUE);
    private static final DocumentAssignable UPDATE_DOCUMENT = d(UPDATE_ELEMENT);
    
    private static final FindAndModify.Builder FIND_AND_MODIFY_BUILDER =
            FindAndModify.builder().query(FIND_DOCUMENT).setUpdate(UPDATE_DOCUMENT).setReturnNew(true);
    
    @Test
    public void testFindAndModifyQuery() throws ExecutionException, InterruptedException {
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                Document findAndModify = mongoColl.findAndModify(FIND_AND_MODIFY_BUILDER.build());
                assertEquals(UPDATE_VALUE.toString(), findAndModify.get(FIELD_INT_NAME).getValueAsString());
            }
        }).start().join();
    }

    @Test
    public void testFindAndModifyBuilder() throws ExecutionException, InterruptedException {
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                Document findAndModify = mongoColl.findAndModify(FIND_AND_MODIFY_BUILDER);
                assertEquals(UPDATE_VALUE.toString(), findAndModify.get(FIELD_INT_NAME).getValueAsString());
            }
        }).start().join();
    }
    
    @Test
    public void testFindAndModifyQueryFuture() throws ExecutionException, InterruptedException {
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                try {
                    Document findAndModify = mongoColl.findAndModifyAsync(FIND_AND_MODIFY_BUILDER.build()).get();
                    assertEquals(UPDATE_VALUE.toString(), findAndModify.get(FIELD_INT_NAME).getValueAsString());
                } catch (ExecutionException ex) {
                    fail(ex.getLocalizedMessage());
                }
            }
        }).start().join();
    }

    @Test
    public void testFindAndModifyBuilderFuture() throws ExecutionException, InterruptedException {
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                try {
                    Document findAndModify = mongoColl.findAndModifyAsync(FIND_AND_MODIFY_BUILDER).get();
                    assertEquals(UPDATE_VALUE.toString(), findAndModify.get(FIELD_INT_NAME).getValueAsString());
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
                Document find = mongoColl.findOne(FIND_DOCUMENT);
                assertEquals(FIELD_INT_FIRST_VALUE.toString(), find.get(FIELD_INT_NAME).getValueAsString());
            }
        }).start().join();
    }

    @Test
    public void testFindOneQuery() throws ExecutionException, InterruptedException {
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                Document find = mongoColl.findOne(Find.builder().query(FIND_DOCUMENT).build());
                assertEquals(FIELD_INT_FIRST_VALUE.toString(), find.get(FIELD_INT_NAME).getValueAsString());
            }
        }).start().join();
    }

    @Test
    public void testFindOneBuilder() throws ExecutionException, InterruptedException {
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                Document find = mongoColl.findOne(Find.builder().query(FIND_DOCUMENT).build());
                assertEquals(FIELD_INT_FIRST_VALUE.toString(), find.get(FIELD_INT_NAME).getValueAsString());
            }
        }).start().join();
    }
    
    @Test
    public void testFindOneQueryDocumentFuture() throws ExecutionException, InterruptedException {
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                try {
                    Document find = mongoColl.findOneAsync(FIND_DOCUMENT).get();
                    assertEquals(FIELD_INT_FIRST_VALUE.toString(), find.get(FIELD_INT_NAME).getValueAsString());
                } catch (ExecutionException ex) {
                    fail(ex.getLocalizedMessage());
                }
            }
        }).start().join();
    }

    @Test
    public void testFindOneQueryFuture() throws ExecutionException, InterruptedException {
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                try {
                    Document find = mongoColl.findOneAsync(Find.builder().query(FIND_DOCUMENT).build()).get();
                    assertEquals(FIELD_INT_FIRST_VALUE.toString(), find.get(FIELD_INT_NAME).getValueAsString());
                } catch (ExecutionException ex) {
                    fail(ex.getLocalizedMessage());
                }
            }
        }).start().join();
    }

    @Test
    public void testFindOneBuilderFuture() throws ExecutionException, InterruptedException {
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                try {
                    Document find = mongoColl.findOneAsync(Find.builder().query(FIND_DOCUMENT)).get();
                    assertEquals(FIELD_INT_FIRST_VALUE.toString(), find.get(FIELD_INT_NAME).getValueAsString());
                } catch (ExecutionException ex) {
                    fail(ex.getLocalizedMessage());
                }
            }
        }).start().join();
    }

    private static final String FIELD_INT_SUM_NAME = "fieldIntSum";

    private static final GroupBy.Builder GROUP_BY_BUILDER =
            GroupBy.builder()
                    .setInitialValue(d(e(FIELD_INT_SUM_NAME, 0)))
                    .setKeyFunction("function(doc) { return { " + FIELD_TY_NAME + " : doc." + FIELD_TY_NAME + " }; }")
                    .setReduceFunction("function(curr, result) { result." + FIELD_INT_SUM_NAME + " += curr." + FIELD_INT_NAME + "; }");

    private static final String FIELD_INT_SUM = "3.0";
    
    private static String getGroupByResultAsString(Element groupBy) {
        return ((Document) groupBy.getValueAsObject()).get(FIELD_INT_SUM_NAME).getValueAsString();
    }
    
    @Test
    public void testGroupByQuery() throws ExecutionException, InterruptedException {
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                MongoIterator<Element> groupBy = mongoColl.groupBy(GROUP_BY_BUILDER.build());
                assertEquals(FIELD_INT_SUM, getGroupByResultAsString(groupBy.next()));
            }
        }).start().join();
    }

    @Test
    public void testGroupByBuilder() throws ExecutionException, InterruptedException {
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                MongoIterator<Element> groupBy = mongoColl.groupBy(GROUP_BY_BUILDER);
                assertEquals(FIELD_INT_SUM, getGroupByResultAsString(groupBy.next()));
            }
        }).start().join();
    }

    @Test
    public void testGroupByQueryFuture() throws ExecutionException, InterruptedException {
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                try {
                    MongoIterator<Element> groupBy = mongoColl.groupByAsync(GROUP_BY_BUILDER.build()).get();
                    assertEquals(FIELD_INT_SUM, getGroupByResultAsString(groupBy.next()));
                } catch (ExecutionException ex) {
                    fail(ex.getLocalizedMessage());
                }
            }
        }).start().join();
    }

    @Test
    public void testGroupByBuilderFuture() throws ExecutionException, InterruptedException {
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                try {
                    MongoIterator<Element> groupBy = mongoColl.groupByAsync(GROUP_BY_BUILDER).get();
                    assertEquals(FIELD_INT_SUM, getGroupByResultAsString(groupBy.next()));
                } catch (ExecutionException ex) {
                    fail(ex.getLocalizedMessage());
                }
            }
        }).start().join();
    }

    @Test
    public void testInsert() throws ExecutionException, InterruptedException {
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                deleteTestSet();
                int inserted = mongoColl.insert(FIRST_TEST_ELEM, SECOND_TEST_ELEM);
                assertEquals((int) TEST_SET_SIZE, inserted);
            }
        }).start().join();
    }

    @Test
    public void testInsertContinueOnError() throws ExecutionException, InterruptedException {
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                deleteTestSet();
                int inserted = mongoColl.insert(true, FIRST_TEST_ELEM, SECOND_TEST_ELEM);
                assertEquals((int) TEST_SET_SIZE, inserted);
            }
        }).start().join();
    }

    @Test
    public void testInsertContinueOnErrorDurable() throws ExecutionException, InterruptedException {
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                deleteTestSet();
                int inserted = mongoColl.insert(true, Durability.ACK, FIRST_TEST_ELEM, SECOND_TEST_ELEM);
                assertEquals((int) TEST_SET_SIZE, inserted);
            }
        }).start().join();
    }

    @Test
    public void testInsertDurable() throws ExecutionException, InterruptedException {
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                deleteTestSet();
                int inserted = mongoColl.insert(Durability.ACK, FIRST_TEST_ELEM, SECOND_TEST_ELEM);
                assertEquals((int) TEST_SET_SIZE, inserted);
            }
        }).start().join();
    }

    @Test
    public void testInsertFuture() throws ExecutionException, InterruptedException {
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                try {
                    deleteTestSet();
                    int inserted = mongoColl.insertAsync(FIRST_TEST_ELEM, SECOND_TEST_ELEM).get();
                    assertEquals((int) TEST_SET_SIZE, inserted);
                } catch (ExecutionException ex) {
                    fail(ex.getLocalizedMessage());
                }
            }
        }).start().join();
    }

    @Test
    public void testInsertContinueOnErrorFuture() throws ExecutionException, InterruptedException {
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                try {
                    deleteTestSet();
                    int inserted = mongoColl.insertAsync(true, FIRST_TEST_ELEM, SECOND_TEST_ELEM).get();
                    assertEquals((int) TEST_SET_SIZE, inserted);
                } catch (ExecutionException ex) {
                    fail(ex.getLocalizedMessage());
                }
            }
        }).start().join();
    }

    @Test
    public void testInsertContinueOnErrorDurableFuture() throws ExecutionException, InterruptedException {
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                try {
                    deleteTestSet();
                    int inserted = mongoColl.insertAsync(true, Durability.ACK, FIRST_TEST_ELEM, SECOND_TEST_ELEM).get();
                    assertEquals((int) TEST_SET_SIZE, inserted);
                } catch (ExecutionException ex) {
                    fail(ex.getLocalizedMessage());
                }
            }
        }).start().join();
    }

    @Test
    public void testInsertDurableFuture() throws ExecutionException, InterruptedException {
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                try {
                deleteTestSet();
                int inserted = mongoColl.insertAsync(Durability.ACK, FIRST_TEST_ELEM, SECOND_TEST_ELEM).get();
                assertEquals((int) TEST_SET_SIZE, inserted);
                } catch (ExecutionException ex) {
                    fail(ex.getLocalizedMessage());
                }
            }
        }).start().join();
    }

    private static String getMapReduceSizeAsString(MongoIterator<Document> mapReduce) {
        Document res = (Document) mapReduce.next().get("value").getValueAsObject();
        return res.get(FIELD_INT_SUM_NAME).getValueAsString();
    }

    private static final MapReduce.Builder MAP_REDUCE_QUERY =
        MapReduce.builder()
            .map("function() { emit(\"" + FIELD_INT_SUM_NAME + "\", { " + FIELD_INT_SUM_NAME + " : this." + FIELD_INT_NAME + " }); }")
            .reduce ("function (key, values) {\n" +
                     "    var result = { " + FIELD_INT_SUM_NAME + " : 0 };\n" +
                     "    values.forEach(function (value) { result." + FIELD_INT_SUM_NAME + " += value." + FIELD_INT_SUM_NAME + "; });\n" +
                     "    return result;\n" +
                     "}"
            );
        
    @Test
    public void testMapReduceQuery() throws ExecutionException, InterruptedException {
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                MongoIterator<Document> mapReduce = mongoColl.mapReduce(MAP_REDUCE_QUERY.build());
                assertEquals(FIELD_INT_SUM, getMapReduceSizeAsString(mapReduce));
            }
        }).start().join();
    }

    @Test
    public void testMapReduceBuilder() throws ExecutionException, InterruptedException {
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                MongoIterator<Document> mapReduce = mongoColl.mapReduce(MAP_REDUCE_QUERY);
                assertEquals(FIELD_INT_SUM, getMapReduceSizeAsString(mapReduce));
            }
        }).start().join();
    }

    @Test
    public void testMapReduceQueryFuture() throws ExecutionException, InterruptedException {
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                try {
                    MongoIterator<Document> mapReduce = mongoColl.mapReduceAsync(MAP_REDUCE_QUERY).get();
                    assertEquals(FIELD_INT_SUM, getMapReduceSizeAsString(mapReduce));
                } catch (ExecutionException ex) {
                    fail(ex.getLocalizedMessage());
                }
            }
        }).start().join();
    }

    @Test
    public void testMapReduceBuilderFuture() throws ExecutionException, InterruptedException {
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                try {
                    MongoIterator<Document> mapReduce = mongoColl.mapReduceAsync(MAP_REDUCE_QUERY).get();
                    assertEquals(FIELD_INT_SUM, getMapReduceSizeAsString(mapReduce));
                } catch (ExecutionException ex) {
                    fail(ex.getLocalizedMessage());
                }
            }
        }).start().join();
    }

    private static final int PARALLEL_SCAN_CONCURRENT_ITERATORS = 2;
    private static final ParallelScan.Builder PARALLEL_SCAN_QUERY = ParallelScan.builder().setRequestedIteratorCount(PARALLEL_SCAN_CONCURRENT_ITERATORS);
        
    @Test
    public void testParallelScanQuery() throws ExecutionException, InterruptedException {
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                Collection<MongoIterator<Document>> parallelScan = mongoColl.parallelScan(PARALLEL_SCAN_QUERY.build());
                assertEquals(1, parallelScan.size());
            }
        }).start().join();
    }

    @Test
    public void testParallelScanBuilder() throws ExecutionException, InterruptedException {
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                Collection<MongoIterator<Document>> parallelScan = mongoColl.parallelScan(PARALLEL_SCAN_QUERY);
                assertEquals(1, parallelScan.size());
            }
        }).start().join();
    }

    @Test
    public void testParallelScanQueryFuture() throws ExecutionException, InterruptedException {
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                try {
                    Collection<MongoIterator<Document>> parallelScan = mongoColl.parallelScanAsync(PARALLEL_SCAN_QUERY.build()).get();
                    assertEquals(1, parallelScan.size());
                } catch (ExecutionException ex) {
                    fail(ex.getLocalizedMessage());
                }
            }
        }).start().join();
    }

    @Test
    public void testParallelScanBuilderFuture() throws ExecutionException, InterruptedException {
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                try {
                    Collection<MongoIterator<Document>> parallelScan = mongoColl.parallelScanAsync(PARALLEL_SCAN_QUERY).get();
                    assertEquals(1, parallelScan.size());
                } catch (ExecutionException ex) {
                    fail(ex.getLocalizedMessage());
                }
            }
        }).start().join();
    }

    
    private static Document updatedDocument(Document docLoaded) {
        return d(e("_id", docLoaded.get("_id")), ELEM_TY, ELEM_FIELD_STRING_1, UPDATE_ELEMENT).build();
    }

    
    @Test
    public void testSaveQuery() throws ExecutionException, InterruptedException {
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                Document docLoaded = mongoColl.findOne(FIND_DOCUMENT);
                Document docChanged = updatedDocument(docLoaded);
                int saved = mongoColl.save(docChanged);
                assertEquals(1, saved); // TODO return value seems incompatible with API description, to be better understood
            }
        }).start().join();
    }

    @Test
    public void testSaveQueryDurable() throws ExecutionException, InterruptedException {
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                Document docLoaded = mongoColl.findOne(FIND_DOCUMENT);
                Document docChanged = updatedDocument(docLoaded);
                int saved = mongoColl.save(docChanged, Durability.ACK);
                assertEquals(1, saved); // TODO return value seems incompatible with API description, to be better understood
            }
        }).start().join();
    }  

    @Test
    public void testSaveQueryFuture() throws ExecutionException, InterruptedException {
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                try {
                    Document docLoaded = mongoColl.findOne(FIND_DOCUMENT);
                    Document docChanged = updatedDocument(docLoaded);
                    int saved = mongoColl.saveAsync(docChanged).get();
                    assertEquals(1, saved); // No listenable future
                } catch (ExecutionException ex) {
                    fail(ex.getLocalizedMessage());
                }
            }
        }).start().join();
    }

    @Test
    public void testSaveQueryDurableFuture() throws ExecutionException, InterruptedException {
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                try {
                    Document docLoaded = mongoColl.findOne(FIND_DOCUMENT);
                    Document docChanged = updatedDocument(docLoaded);
                    int saved = mongoColl.saveAsync(docChanged, Durability.ACK).get();
                    assertEquals(1, saved); // TODO return value seems incompatible with API description, to be better understood
                } catch (ExecutionException ex) {
                    fail(ex.getLocalizedMessage());
                }
            }
        }).start().join();
    }

    private static final Text.Builder TEXT_SEARCH_QUERY = Text.builder().searchTerm(TEXT_SEARCH_TERM);

    @Test
    @Deprecated
    public void testTextSearchQuery() throws ExecutionException, InterruptedException {
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                MongoIterator<TextResult> textResult = mongoColl.textSearch(TEXT_SEARCH_QUERY.build());
                assertNotNull(textResult.hasNext());
                textResult.next();
                assertNotNull(textResult.hasNext()); // 2 results
            }
        }).start().join();
    }

    @Test
    @Deprecated
    public void testTextSearchBuilder() throws ExecutionException, InterruptedException {
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                MongoIterator<TextResult> textResult = mongoColl.textSearch(TEXT_SEARCH_QUERY);
                assertNotNull(textResult.hasNext());
                textResult.next();
                assertNotNull(textResult.hasNext()); // 2 results
            }
        }).start().join();
    }

    @Test
    @Deprecated
    public void testTextSearchQueryFuture() throws ExecutionException, InterruptedException {
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                try {
                    MongoIterator<TextResult> textResult = mongoColl.textSearchAsync(TEXT_SEARCH_QUERY.build()).get();
                    assertNotNull(textResult.hasNext());
                    textResult.next();
                    assertNotNull(textResult.hasNext()); // 2 results
                } catch (ExecutionException ex) {
                    fail(ex.getLocalizedMessage());
                }
            }
        }).start().join();
    }

    @Test
    @Deprecated
    public void testTextSearchBuilderFuture() throws ExecutionException, InterruptedException {
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                try {
                    MongoIterator<TextResult> textResult = mongoColl.textSearchAsync(TEXT_SEARCH_QUERY).get();
                    assertNotNull(textResult.hasNext());
                    textResult.next();
                    assertNotNull(textResult.hasNext()); // 2 results
                } catch (ExecutionException ex) {
                    fail(ex.getLocalizedMessage());
                }
            }
        }).start().join();
    }

    private static final int UPDATE_SIZE = TEST_SET_DOCS_COUNT_WITH_FIELD_INT_FIRST_VALUE;
    
    @Test
    public void testUpate() throws ExecutionException, InterruptedException {
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                long updated = mongoColl.update(FIND_DOCUMENT, UPDATE_DOCUMENT);
                assertEquals(UPDATE_SIZE, updated);
            }
        }).start().join();
    }

    @Test
    public void testUpdateMultiUpsert() throws ExecutionException, InterruptedException {
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                long updated = mongoColl.update(FIND_DOCUMENT, UPDATE_DOCUMENT, false, true);
                assertEquals(UPDATE_SIZE, updated);
            }
        }).start().join();
    }

    @Test
    public void testUpdateMultiUpsertDurability() throws ExecutionException, InterruptedException {
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                long updated = mongoColl.update(FIND_DOCUMENT, UPDATE_DOCUMENT, false, true, Durability.ACK);
                assertEquals(UPDATE_SIZE, updated);
            }
        }).start().join();
    }

    @Test
    public void testUpdateDurability() throws ExecutionException, InterruptedException {
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                long updated = mongoColl.update(FIND_DOCUMENT, UPDATE_DOCUMENT, Durability.ACK);
                assertEquals(UPDATE_SIZE, updated);
            }
        }).start().join();
    }

    @Test
    public void testUpdateAsync() throws ExecutionException, InterruptedException {
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                try {
                    long updated = mongoColl.updateAsync(FIND_DOCUMENT, UPDATE_DOCUMENT).get();
                    assertEquals(UPDATE_SIZE, updated);
                } catch (ExecutionException ex) {
                    fail(ex.getLocalizedMessage());
                }
            }
        }).start().join();
    }

    @Test
    public void testUpdateMultiUpsertAsync() throws ExecutionException, InterruptedException {
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                try {
                    long updated = mongoColl.updateAsync(FIND_DOCUMENT, UPDATE_DOCUMENT, false, true).get();
                    assertEquals(UPDATE_SIZE, updated);
                } catch (ExecutionException ex) {
                    fail(ex.getLocalizedMessage());
                }
            }
        }).start().join();
    }

    @Test
    public void testUpdateMultiUpsertDurabilityAsync() throws ExecutionException, InterruptedException {
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                try {
                    long updated = mongoColl.updateAsync(FIND_DOCUMENT, UPDATE_DOCUMENT, false, true, Durability.ACK).get();
                    assertEquals(UPDATE_SIZE, updated);
                } catch (ExecutionException ex) {
                    fail(ex.getLocalizedMessage());
                }
            }
        }).start().join();
    }

    @Test
    public void testUpdateDurabilityAsync() throws ExecutionException, InterruptedException {
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                try {
                    long updated = mongoColl.updateAsync(FIND_DOCUMENT, UPDATE_DOCUMENT, Durability.ACK).get();
                    assertEquals(UPDATE_SIZE, updated);
                } catch (ExecutionException ex) {
                    fail(ex.getLocalizedMessage());
                }
            }
        }).start().join();
    }

    private static final BatchedWrite.Builder BATCHED_WRITE_BUILDER = BatchedWrite.builder().update(FIND_DOCUMENT, UPDATE_DOCUMENT);
    
    @Test
    public void testWriteQuery() throws ExecutionException, InterruptedException {
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                long written = mongoColl.write(BATCHED_WRITE_BUILDER.build());
                assertEquals(UPDATE_SIZE, written);
            }
        }).start().join();
    }

    @Test
    public void testWriteBuilder() throws ExecutionException, InterruptedException {
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                long written = mongoColl.write(BATCHED_WRITE_BUILDER);
                assertEquals(UPDATE_SIZE, written);
            }
        }).start().join();
    }

    @Test
    public void testWriteQueryAsync() throws ExecutionException, InterruptedException {
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                try {
                    long written = mongoColl.writeAsync(BATCHED_WRITE_BUILDER.build()).get();
                    assertEquals(UPDATE_SIZE, written);
                } catch (ExecutionException ex) {
                    fail(ex.getLocalizedMessage());
                }
            }
        }).start().join();
    }

    @Test
    public void testWriteBuilderAsync() throws ExecutionException, InterruptedException {
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                try {
                    long written = mongoColl.writeAsync(BATCHED_WRITE_BUILDER).get();
                    assertEquals(UPDATE_SIZE, written);
                } catch (ExecutionException ex) {
                    fail(ex.getLocalizedMessage());
                }
            }
        }).start().join();
    } 
}