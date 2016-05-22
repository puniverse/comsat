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

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.IntBinaryOperator;
import java.util.function.LongBinaryOperator;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;

/**
 * @author circlespainter
 */
@SuppressWarnings("WeakerAccess")
public class JedisPubSub extends redis.clients.jedis.JedisPubSub {
    private Jedis jedis;

    ConcurrentHashMap<Set<String>, Optional<RedisPubSubListener<String, String>>> channelListeners = new ConcurrentHashMap<>();
    ConcurrentHashMap<Set<String>, Optional<RedisPubSubListener<String, String>>> patternListeners = new ConcurrentHashMap<>();

    public JedisPubSub(Jedis jedis) {
        this.jedis = jedis;
    }

    @Override
    public final void unsubscribe() {
        channelListeners.replaceAll((k, v) -> {
            if (v.isPresent())
                jedis.pubSub.removeListener(v.get());
            return Optional.empty();
        });
        channelListeners.clear();
    }

    @Override
    public final void unsubscribe(String... channels) {
        channelListeners.computeIfPresent(new HashSet<>(Arrays.asList(channels)), (k, v) -> {
            if (v.isPresent())
                jedis.pubSub.removeListener(v.get());
            return Optional.empty();
        });
        Jedis.clearEmpties(channelListeners);
    }

    @Override
    public final void punsubscribe() {
        patternListeners.replaceAll((k, v) -> {
            if (v.isPresent())
                jedis.pubSub.removeListener(v.get());
            return Optional.empty();
        });
        patternListeners.clear();
    }

    @Override
    public final void punsubscribe(String... patterns) {
        patternListeners.computeIfPresent(new HashSet<>(Arrays.asList(patterns)), (k, v) -> {
            if (v.isPresent())
                jedis.pubSub.removeListener(v.get());
            return Optional.empty();
        });
        Jedis.clearEmpties(patternListeners);
    }

    @Override
    public final void subscribe(String... channels) {
        jedis.subscribe(this, channels);
    }

    @Override
    public final void psubscribe(String... patterns) {
        jedis.psubscribe(this, patterns);
    }

    @Override
    public final boolean isSubscribed() {
        final ToLongFunction<Map.Entry<Set<String>, Optional<RedisPubSubListener<String, String>>>> tlf =
            e -> e.getValue().map(v -> 1L).orElse(0L);
        final LongBinaryOperator lbo = (l1, l2) -> l1 + l2;
        return
            channelListeners.reduceEntriesToLong(Runtime.getRuntime().availableProcessors(), tlf, 0, lbo) > 0 ||
            patternListeners.reduceEntriesToLong(Runtime.getRuntime().availableProcessors(), tlf, 0, lbo) > 0;
    }

    @Override
    public final int getSubscribedChannels() {
        final ToIntFunction<Map.Entry<Set<String>, Optional<RedisPubSubListener<String, String>>>> tlf =
            e -> e.getValue().map(v -> jedis.pubsubNumSub(e.getKey().toArray(new String[e.getKey().size()])).size()).orElse(0);
        final IntBinaryOperator lbo = (l1, l2) -> l1 + l2;
        return
            channelListeners.reduceEntriesToInt(Runtime.getRuntime().availableProcessors(), tlf, 0, lbo) +
            patternListeners.reduceEntriesToInt(Runtime.getRuntime().availableProcessors(), tlf, 0, lbo);
    }

    /////////////////////////// MEANINGLESS
    @Override
    public final void proceedWithPatterns(Client client, String... patterns) {
        throw new UnsupportedOperationException();
    }

    @Override
    public final void proceed(Client client, String... channels) {
        throw new UnsupportedOperationException();
    }
}
