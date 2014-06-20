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
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author circlespainter
 */
public class FiberMongoDbTest extends AbstractTestFiberMongo {
    @Before
    public void setUpTest() throws Exception {
        super.setUpDbForTest();
    }
    
    @After
    public void tearDownTest() throws Exception {
        super.tearDownDbForTest();
    }
    
    @Test
    public void testRunCommandDocument() throws SuspendExecution, ExecutionException, InterruptedException {
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                Document res = mongoDb.runCommand (
                    BuilderFactory.start()
                        .add("ping", 1)
                );
                Assert.assertEquals("OK result", "1.0", res.get("ok").getValueAsString());
            }
        }).start().join();
    }
}
