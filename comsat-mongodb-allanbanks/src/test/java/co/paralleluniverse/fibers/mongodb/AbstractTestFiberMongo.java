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
import com.allanbank.mongodb.MongoClient;
import com.allanbank.mongodb.MongoDatabase;
import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodProcess;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.IMongodConfig;
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.runtime.Network;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import org.junit.AfterClass;
import org.junit.BeforeClass;

/**
 * @author circlespainter
 */
public abstract class AbstractTestFiberMongo {
    private static MongodExecutable mongodExecutable;
    private static MongoClient mongoClient;

    protected MongoDatabase mongoDb;

    @BeforeClass
    public static void setUpClass() throws IOException {
        MongodStarter starter = MongodStarter.getDefaultInstance();

        int port = 12345;
        IMongodConfig mongodConfig = new MongodConfigBuilder()
            .version(Version.Main.PRODUCTION)
            .net(new Net(port, Network.localhostIsIPv6()))
            .build();

        try {
            mongodExecutable = starter.prepare(mongodConfig);
            MongodProcess mongod = mongodExecutable.start();
            
            mongoClient = FiberMongoFactory.createClient( "mongodb://localhost:" + port + "/test?maxConnectionCount=10" ).asSerializedClient();
        } catch (IOException ioe) {
            tearDownClass();
        }
    }

    @AfterClass
    public static void tearDownClass() throws IOException {
        if (mongoClient != null) {
            mongoClient.close();
            mongoClient = null;
        }
        if (mongodExecutable != null) {
            mongodExecutable.stop();
            mongodExecutable = null;
        }
    }

    protected void setUpDbForTest() throws ExecutionException, InterruptedException {
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                mongoDb = mongoClient.getDatabase("test");
            }
        }).start().join();
    }
    
    protected void tearDownDbForTest() throws ExecutionException, InterruptedException {
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                if (mongoDb != null) {
                    mongoDb.drop();
                    mongoDb = null;
                }

            }
        }).start().join();
    }
}
