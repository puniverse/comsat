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
import com.allanbank.mongodb.MongoCollection;
import java.util.concurrent.ExecutionException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author circlespainter
 */
public class FiberMongoCollTest extends AbstractTestFiberMongo {
    private MongoCollection mongoColl;

    @Before
    public void setUpTest() throws Exception {
        super.setUpDbForTest();
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                mongoColl = mongoDb.getCollection("test");
            }
        }).start().join();
    }
    
    @After
    public void tearDownTest() throws Exception {
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

    @Test
    public void testCount() throws SuspendExecution, ExecutionException, InterruptedException {
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                long res = mongoColl.count();
                Assert.assertEquals("OK result", 0, res);
            }
        }).start().join();
    }
}