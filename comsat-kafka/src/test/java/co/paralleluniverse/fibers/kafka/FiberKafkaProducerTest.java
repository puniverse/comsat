/*
 * COMSAT
 * Copyright (C) 2013-2015, Parallel Universe Software Co. All rights reserved.
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
package co.paralleluniverse.fibers.kafka;

import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.SuspendableRunnable;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.MockProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class FiberKafkaProducerTest {

    private MockProducer mockProducer;
    private FiberKafkaProducer<byte[], byte[]> fiberProducer;
    private co.paralleluniverse.strands.concurrent.Phaser phaser;

    @Before
    public void setUp() {
        mockProducer = new MockProducer(false);
        fiberProducer = new FiberKafkaProducer<>(mockProducer);
        phaser = new co.paralleluniverse.strands.concurrent.Phaser(2);
    }

    @Test
    public void testSuccessfulSendWithoutCallback() throws InterruptedException, TimeoutException, ExecutionException {
        Fiber<Void> fiber = new Fiber<>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                Future<RecordMetadata> f = fiberProducer.send(new ProducerRecord<>("Topic", "Key".getBytes(), "Value".getBytes()));
                Future<RecordMetadata> f2 = fiberProducer.send(new ProducerRecord<>("Topic", "Key".getBytes(), "Value".getBytes()));
                phaser.arrive();
                try {
                    RecordMetadata recordMetadata = f.get();

                    assertEquals("Topic", recordMetadata.topic());
                    assertEquals(0, recordMetadata.offset());
                    assertEquals(0, recordMetadata.partition());

                    RecordMetadata recordMetadata2 = f2.get();

                    assertEquals("Topic", recordMetadata2.topic());
                    assertEquals(1, recordMetadata2.offset());
                    assertEquals(0, recordMetadata2.partition());
                } catch (ExecutionException e) {
                    fail();
                }
            }
        });
        fiber.start();

        phaser.arriveAndAwaitAdvance();
        mockProducer.completeNext();
        mockProducer.completeNext();

        fiber.join(5000, TimeUnit.MILLISECONDS);
    }

    @Test
    public void testSuccessfulSendWithCallback() throws ExecutionException, InterruptedException, TimeoutException {
        Fiber<Void> fiber = new Fiber<>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {

                final AtomicReference<RecordMetadata> callbackMetadata = new AtomicReference<>(null);
                final AtomicReference<RecordMetadata> callbackMetadata2 = new AtomicReference<>(null);

                Future<RecordMetadata> f = fiberProducer.send(new ProducerRecord<>("Topic", "Key".getBytes(), "Value".getBytes()),
                        new Callback() {
                            @Override
                            public void onCompletion(RecordMetadata metadata, Exception exception) {
                                callbackMetadata.set(metadata);
                            }
                        });
                Future<RecordMetadata> f2 = fiberProducer.send(new ProducerRecord<>("Topic", "Key".getBytes(), "Value".getBytes()),
                        new Callback() {
                            @Override
                            public void onCompletion(RecordMetadata metadata, Exception exception) {
                                callbackMetadata2.set(metadata);
                            }
                        });
                phaser.arriveAndAwaitAdvance();

                try {
                    RecordMetadata recordMetadata = f.get();

                    assertEquals("Topic", recordMetadata.topic());
                    assertEquals(0, recordMetadata.offset());
                    assertEquals(0, recordMetadata.partition());

                    RecordMetadata recordMetadata2 = f2.get();

                    assertEquals("Topic", recordMetadata2.topic());
                    assertEquals(1, recordMetadata2.offset());
                    assertEquals(0, recordMetadata2.partition());
                } catch (ExecutionException e) {
                    fail();
                }

                phaser.arriveAndAwaitAdvance();

                assertEquals("Topic", callbackMetadata.get().topic());
                assertEquals(0, callbackMetadata.get().offset());
                assertEquals(0, callbackMetadata.get().partition());

                assertEquals("Topic", callbackMetadata2.get().topic());
                assertEquals(1, callbackMetadata2.get().offset());
                assertEquals(0, callbackMetadata2.get().partition());
            }
        });
        fiber.start();

        phaser.arriveAndAwaitAdvance();
        mockProducer.completeNext();
        mockProducer.completeNext();
        phaser.arrive();

        fiber.join(5000, TimeUnit.MILLISECONDS);
    }

    @Test
    public void testErrorSendWithoutCallback() throws ExecutionException, InterruptedException, TimeoutException {
        final RuntimeException exception = new RuntimeException("Error");

        Fiber<Void> fiber = new Fiber<>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                Future<RecordMetadata> f = fiberProducer.send(new ProducerRecord<>("Topic", "Key".getBytes(), "Value".getBytes()));
                phaser.arrive();
                try {
                    f.get();
                    fail("Send should have failed.");
                } catch (ExecutionException e) {
                    assertEquals(exception, e.getCause());
                }
            }
        });
        fiber.start();

        phaser.arriveAndAwaitAdvance();
        mockProducer.errorNext(exception);

        fiber.join(5000, TimeUnit.MILLISECONDS);
    }

    @Test
    public void testErrorSendWithCallback() throws ExecutionException, InterruptedException, TimeoutException {
        final RuntimeException exception = new RuntimeException("Error");
        final AtomicReference<Exception> exceptionReference = new AtomicReference<>(null);

        Fiber<Void> fiber = new Fiber<>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                Future<RecordMetadata> f = fiberProducer.send(new ProducerRecord<>("Topic", "Key".getBytes(), "Value".getBytes()),
                        new Callback() {
                            @Override
                            public void onCompletion(RecordMetadata metadata, Exception exception) {
                                exceptionReference.set(exception);
                            }
                        });

                phaser.arriveAndAwaitAdvance();

                try {
                    f.get();
                    fail("Send should have failed.");
                } catch (ExecutionException e) {
                    assertEquals(exception, e.getCause());
                }

                phaser.arriveAndAwaitAdvance();
                assertEquals(exception, exceptionReference.get());
            }
        });
        fiber.start();

        phaser.arriveAndAwaitAdvance();
        mockProducer.errorNext(exception);
        phaser.arrive();

        fiber.join(5000, TimeUnit.MILLISECONDS);
    }
}
