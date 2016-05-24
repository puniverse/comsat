/*
 * COMSAT
 * Copyright (c) 2016, Parallel Universe Software Co. All rights reserved.
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
package co.paralleluniverse.fibers.redis;

import com.lambdaworks.redis.pubsub.RedisPubSubListener;
import redis.clients.jedis.Client;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import static co.paralleluniverse.fibers.redis.Utils.contains;

/**
 * @author circlespainter
 */
@SuppressWarnings("WeakerAccess")
public class BinaryJedisPubSub extends redis.clients.jedis.BinaryJedisPubSub {
    BinaryJedis jedis;

    ConcurrentHashMap<byte[], List<RedisPubSubListener<byte[], byte[]>>> channelListeners = new ConcurrentHashMap<>();
    ConcurrentHashMap<byte[], List<RedisPubSubListener<byte[], byte[]>>> patternListeners = new ConcurrentHashMap<>();

    AtomicLong subscribedChannels = new AtomicLong();

    @Override
    public final void unsubscribe() {
        channelListeners.replaceAll((k, v) -> {
            for (final RedisPubSubListener<byte[], byte[]> l : v)
                jedis.binaryPubSub.removeListener(l);
            return Collections.EMPTY_LIST;
        });
        channelListeners.clear();
    }

    @Override
    public final void unsubscribe(byte[]... channels) {
        channelListeners.replaceAll((k, v) -> {
            if (contains(channels, k)) {
                for (final RedisPubSubListener<byte[], byte[]> l : v)
                    jedis.binaryPubSub.removeListener(l);
                return Collections.EMPTY_LIST;
            } else {
                return v;
            }
        });
        Utils.clearEmpties(channelListeners);
    }

    @Override
    public final void punsubscribe() {
        patternListeners.replaceAll((k, v) -> {
            for(final RedisPubSubListener<byte[], byte[]> l : v)
                jedis.binaryPubSub.removeListener(l);
            return Collections.EMPTY_LIST;
        });
        patternListeners.clear();
    }

    @Override
    public final void punsubscribe(byte[]... patterns) {
        patternListeners.replaceAll((k, v) -> {
            if (contains(patterns, k)) {
                for (final RedisPubSubListener<byte[], byte[]> l : v)
                    jedis.binaryPubSub.removeListener(l);
                return Collections.EMPTY_LIST;
            } else {
                return v;
            }
        });
        Utils.clearEmpties(patternListeners);
    }

    @Override
    public final void subscribe(byte[]... channels) {
        jedis.subscribe(this, channels);
    }

    @Override
    public final void psubscribe(byte[]... patterns) {
        jedis.psubscribe(this, patterns);
    }

    @Override
    public final boolean isSubscribed() {
        return getSubscribedChannels() > 0;
    }

    @Override
    public final int getSubscribedChannels() {
        return Utils.validateInt(subscribedChannels.get());
    }

    @Override
    public final void proceedWithPatterns(Client client, byte[]... patterns) {
        // Nothing to do
    }

    @Override
    public final void proceed(Client client, byte[]... channels) {
        // Nothing to do
    }
}
