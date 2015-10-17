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

import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.MockProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;

public class FiberKafkaProducerTest {

    private MockProducer mockProducer;
    private FiberKafkaProducer<byte[], byte[]> fiberProducer;

    @Before
    public void setUp() {
        mockProducer = new MockProducer();
        fiberProducer = new FiberKafkaProducer<>(mockProducer);
    }

    @Test
    public void testSuccessfulSendWithoutCallback() throws ExecutionException, InterruptedException {
        Future<RecordMetadata> f = fiberProducer.send(new ProducerRecord<>("Topic", "Key".getBytes(), "Value".getBytes()));
        RecordMetadata recordMetadata = f.get();

        assertEquals("Topic", recordMetadata.topic());
        assertEquals(0, recordMetadata.offset());
        assertEquals(0, recordMetadata.partition());

        Future<RecordMetadata> f2 = fiberProducer.send(new ProducerRecord<>("Topic", "Key".getBytes(),
                "Value".getBytes()));
        RecordMetadata recordMetadata2 = f2.get();

        assertEquals("Topic", recordMetadata2.topic());
        assertEquals(1, recordMetadata2.offset());
        assertEquals(0, recordMetadata2.partition());
    }

    @Test
    public void testSuccessfulSendWithCallback() throws ExecutionException, InterruptedException {
        final AtomicReference<RecordMetadata> callbackMetadata = new AtomicReference<>(null);
        Future<RecordMetadata> f = fiberProducer.send(new ProducerRecord<>("Topic", "Key".getBytes(), "Value"
                .getBytes()), new Callback() {
            @Override
            public void onCompletion(RecordMetadata metadata, Exception exception) {
                callbackMetadata.set(metadata);

            }
        });
        RecordMetadata recordMetadata = f.get();

        assertEquals("Topic", recordMetadata.topic());
        assertEquals(0, recordMetadata.offset());
        assertEquals(0, recordMetadata.partition());

        assertEquals("Topic", callbackMetadata.get().topic());
        assertEquals(0, callbackMetadata.get().offset());
        assertEquals(0, callbackMetadata.get().partition());

        final AtomicReference<RecordMetadata> callbackMetadata2 = new AtomicReference<>(null);
        Future<RecordMetadata> f2 = fiberProducer.send(new ProducerRecord<>("Topic", "Key".getBytes(),
                "Value".getBytes()), new Callback() {
            @Override
            public void onCompletion(RecordMetadata metadata, Exception exception) {
                callbackMetadata2.set(metadata);

            }
        });
        RecordMetadata recordMetadata2 = f2.get();

        assertEquals("Topic", recordMetadata2.topic());
        assertEquals(1, recordMetadata2.offset());
        assertEquals(0, recordMetadata2.partition());

        assertEquals("Topic", callbackMetadata2.get().topic());
        assertEquals(1, callbackMetadata2.get().offset());
        assertEquals(0, callbackMetadata2.get().partition());
    }
}
