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

import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.Metric;
import org.apache.kafka.common.MetricName;
import org.apache.kafka.common.PartitionInfo;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

public class FiberKafkaProducer<K, V> implements Producer<K, V> {

    private final KafkaProducer<K, V> producer;

    public FiberKafkaProducer(KafkaProducer<K, V> producer) {
        this.producer = producer;
    }

    @Override
    public Future<RecordMetadata> send(ProducerRecord<K, V> record) {
        return null;
    }

    @Override
    public Future<RecordMetadata> send(ProducerRecord<K, V> record, Callback callback) {
        return null;
    }

    @Override
    public List<PartitionInfo> partitionsFor(String topic) {
        return null;
    }

    @Override
    public Map<MetricName, ? extends Metric> metrics() {
        return null;
    }

    @Override
    public void close() {
        producer.close();
    }
}
