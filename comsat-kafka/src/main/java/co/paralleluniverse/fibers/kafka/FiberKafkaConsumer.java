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

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.OffsetMetadata;
import org.apache.kafka.common.Metric;
import org.apache.kafka.common.MetricName;
import org.apache.kafka.common.TopicPartition;

import java.util.Collection;
import java.util.Map;

public class FiberKafkaConsumer<K, V> implements Consumer<K, V> {

    private final Consumer<K, V> consumer;

    public FiberKafkaConsumer(Consumer<K, V> consumer) {
        this.consumer = consumer;
    }

    @Override
    public void subscribe(String... topics) {
        consumer.subscribe(topics);
    }

    @Override
    public void subscribe(TopicPartition... partitions) {
        consumer.subscribe(partitions);
    }

    @Override
    public void unsubscribe(String... topics) {
        consumer.subscribe(topics);
    }

    @Override
    public void unsubscribe(TopicPartition... partitions) {
        consumer.unsubscribe(partitions);
    }

    @Override
    public Map<String, ConsumerRecords<K, V>> poll(long timeout) {
        return null;
    }

    @Override
    public OffsetMetadata commit(boolean sync) {
        return null;
    }

    @Override
    public OffsetMetadata commit(Map<TopicPartition, Long> offsets, boolean sync) {
        return null;
    }

    @Override
    public void seek(Map<TopicPartition, Long> offsets) {
        consumer.seek(offsets);
    }

    @Override
    public Map<TopicPartition, Long> position(Collection<TopicPartition> partitions) {
        return consumer.position(partitions);
    }

    @Override
    public Map<TopicPartition, Long> committed(Collection<TopicPartition> partitions) {
        return consumer.committed(partitions);
    }

    @Override
    public Map<TopicPartition, Long> offsetsBeforeTime(long timestamp, Collection<TopicPartition> partitions) {
        return consumer.offsetsBeforeTime(timestamp, partitions);
    }

    @Override
    public Map<MetricName, ? extends Metric> metrics() {
        return consumer.metrics();
    }

    @Override
    public void close() {
        consumer.close();

    }
}
