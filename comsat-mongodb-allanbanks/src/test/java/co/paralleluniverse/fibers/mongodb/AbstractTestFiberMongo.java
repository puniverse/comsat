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
import co.paralleluniverse.strands.channels.Channel;
import co.paralleluniverse.strands.channels.Channels;
import com.allanbank.mongodb.ListenableFuture;
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
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.AfterClass;
import static org.junit.Assert.assertTrue;
import org.junit.BeforeClass;

/**
 * @author circlespainter
 */
public abstract class AbstractTestFiberMongo {
    private static MongodExecutable mongodExecutable;
    private static MongoClient mongoClient;

    protected static Executor executor;

    protected AtomicBoolean listenerCalledFlag;
    protected Runnable listenerCalledSetter;
    protected Channel<AtomicBoolean> listenerCalledGoChannel;

    protected MongoDatabase mongoDb;

    protected <V> ListenableFuture<V> addListenerCalledFlagSetter(ListenableFuture<V> l) {
        l.addListener(listenerCalledSetter, executor);
        return l;
    }
    
    protected void setUpTestBase() throws ExecutionException, InterruptedException {
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                listenerCalledFlag = new AtomicBoolean(false);
                listenerCalledGoChannel = Channels.newChannel(0);
                listenerCalledSetter = new Runnable() { @Override public void run() {
                    try {
                        listenerCalledFlag.set(true);
                        listenerCalledGoChannel.send(listenerCalledFlag);
                    } catch (SuspendExecution | InterruptedException ex) {
                        throw new AssertionError("This should never happen as we're using channels in threads");
                    }
                }};
                
                mongoDb = mongoClient.getDatabase("test");
            }
        }).start().join();
    }
    
    protected void tearDownTestBase() throws ExecutionException, InterruptedException {
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                listenerCalledSetter = null;
                listenerCalledGoChannel = null;
                listenerCalledFlag= null;

                if (mongoDb != null) {
                    mongoDb.drop();
                    mongoDb = null;
                }
            }
        }).start().join();
    }

    protected void assertListenerCalled() throws SuspendExecution, InterruptedException {
        assertTrue("Listener called", listenerCalledGoChannel.receive(30, TimeUnit.SECONDS).get());
    }
    
    @BeforeClass
    public static void setUpClass() throws IOException {
        executor = Executors.newSingleThreadExecutor();

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
}