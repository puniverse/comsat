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

import co.paralleluniverse.strands.SettableFuture;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.Metric;
import org.apache.kafka.common.MetricName;
import org.apache.kafka.common.PartitionInfo;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

public class FiberKafkaProducer<K, V> implements Producer<K, V> {

    private final Producer<K, V> producer;

    public FiberKafkaProducer(Producer<K, V> producer) {
        this.producer = producer;
    }

    @Override
    public Future<RecordMetadata> send(ProducerRecord<K, V> record) {
        return send(record, null);
    }

    @Override
    public Future<RecordMetadata> send(ProducerRecord<K, V> record, Callback callback) {
        SettableFuture<RecordMetadata> future = new SettableFuture<>();
        producer.send(record, new CallbackWrapper(future, callback));
        return future;
    }

    @Override
    public List<PartitionInfo> partitionsFor(String topic) {
        return producer.partitionsFor(topic);
    }

    @Override
    public Map<MetricName, ? extends Metric> metrics() {
        return producer.metrics();
    }

    @Override
    public void close() {
        producer.close();
    }

    private static class CallbackWrapper implements Callback {

        private final SettableFuture<RecordMetadata> future;
        private final Callback callback;

        public CallbackWrapper(SettableFuture<RecordMetadata> future, Callback callback) {
            this.future = future;
            this.callback = callback;
        }

        @Override
        public void onCompletion(RecordMetadata metadata, Exception exception) {
            if (metadata != null) {
                future.set(metadata);
            } else {
                future.setException(exception);
            }
            if (callback != null) {
                callback.onCompletion(metadata, exception);
            }
        }
    }
}
