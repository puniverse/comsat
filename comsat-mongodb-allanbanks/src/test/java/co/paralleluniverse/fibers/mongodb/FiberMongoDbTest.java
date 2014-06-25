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
import com.allanbank.mongodb.bson.Document;
import com.allanbank.mongodb.bson.builder.BuilderFactory;
import java.util.concurrent.ExecutionException;
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
 * 3) New fiber-blocking APIs for now async-only operations (when added, @see co.paralleluniverse.fibers.mongodb.FiberMongoDatabaseImpl)
 */
public class FiberMongoDbTest extends AbstractTestFiberMongo {
    @Before
    public void setUpTest() throws ExecutionException, InterruptedException {
        super.setUpDbForTest();
    }
    
    @After
    public void tearDownTest() throws ExecutionException, InterruptedException {
        super.tearDownDbForTest();
    }
    
    @Test
    public void testRunCommandDocument() throws ExecutionException, InterruptedException {
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                Document res = mongoDb.runCommand (
                    BuilderFactory.start()
                        .add("ping", 1)
                );
                assertEquals("1.0", res.get("ok").getValueAsString());
            }
        }).start().join();
    }
    
    @Test
    public void testRunCommandString() throws ExecutionException, InterruptedException {
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                Document res = mongoDb.runCommand("ping");
                assertEquals("1.0", res.get("ok").getValueAsString());
            }
        }).start().join();
    }

    @Test
    public void testRunCommandOptions() throws ExecutionException, InterruptedException {
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                Document res = mongoDb.runCommand("ping", null);
                assertEquals("1.0", res.get("ok").getValueAsString());
            }
        }).start().join();
    }

    @Test
    public void testRunCommandNameIntValueOptions() throws ExecutionException, InterruptedException {
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                Document res = mongoDb.runCommand("ping", 1, null);
                assertEquals("1.0", res.get("ok").getValueAsString());
            }
        }).start().join();
    }

    @Test
    public void testRunCommandNameStringValueOptions() throws ExecutionException, InterruptedException {
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                Document res = mongoDb.runCommand("ping", "1", null);
                assertEquals("1.0", res.get("ok").getValueAsString());
            }
        }).start().join();
    }
    
    @Test
    public void testRunCommandDocumentFuture() throws ExecutionException, InterruptedException {
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                try {
                    Document res = mongoDb.runCommandAsync (
                            BuilderFactory.start()
                                    .add("ping", 1)
                    ).get();
                    assertEquals("1.0", res.get("ok").getValueAsString());
                } catch (ExecutionException ex) {
                    fail(ex.getLocalizedMessage());
                }
            }
        }).start().join();
    }
    
    @Test
    public void testRunCommandStringFuture() throws ExecutionException, InterruptedException {
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                try {
                    Document res = mongoDb.runCommandAsync("ping").get();
                    assertEquals("1.0", res.get("ok").getValueAsString());
                } catch (ExecutionException ex) {
                    fail(ex.getLocalizedMessage());
                }
            }
        }).start().join();
    }

    @Test
    public void testRunCommandOptionsFuture() throws ExecutionException, InterruptedException {
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                try {
                    Document res = mongoDb.runCommandAsync("ping", null).get();
                    assertEquals("1.0", res.get("ok").getValueAsString());
                } catch (ExecutionException ex) {
                    fail(ex.getLocalizedMessage());
                }
            }
        }).start().join();
    }

    @Test
    public void testRunCommandNameIntValueOptionsFuture() throws ExecutionException, InterruptedException {
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                try {
                    Document res = mongoDb.runCommandAsync("ping", 1, null).get();
                    assertEquals("1.0", res.get("ok").getValueAsString());
                } catch (ExecutionException ex) {
                    fail(ex.getLocalizedMessage());
                }
            }
        }).start().join();
    }

    @Test
    public void testRunCommandNameStringValueOptionsFuture() throws ExecutionException, InterruptedException {
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                try {
                    Document res = mongoDb.runCommandAsync("ping", "1", null).get();
                    assertEquals("1.0", res.get("ok").getValueAsString());
                } catch (ExecutionException ex) {
                    fail(ex.getLocalizedMessage());
                }
            }
        }).start().join();
    }
}