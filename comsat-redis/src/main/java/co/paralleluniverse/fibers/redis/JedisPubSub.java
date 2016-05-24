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
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author circlespainter
 */
@SuppressWarnings("WeakerAccess")
public class JedisPubSub extends redis.clients.jedis.JedisPubSub {
    Jedis jedis;

    ConcurrentHashMap<String, List<RedisPubSubListener<String, String>>> channelListeners = new ConcurrentHashMap<>();
    ConcurrentHashMap<String, List<RedisPubSubListener<String, String>>> patternListeners = new ConcurrentHashMap<>();

    AtomicLong subscribedChannels = new AtomicLong();

    @Override
    public final void unsubscribe() {
        channelListeners.replaceAll((k, v) -> {
            for(final RedisPubSubListener<String, String> l : v)
                jedis.stringPubSub.removeListener(l);
            return Collections.EMPTY_LIST;
        });
        channelListeners.clear();
    }

    @Override
    public final void unsubscribe(String... channels) {
        final List<String> chL = Arrays.asList(channels);
        channelListeners.replaceAll((k, v) -> {
            if (chL.contains(k)) {
                for (final RedisPubSubListener<String, String> l : v)
                    jedis.stringPubSub.removeListener(l);
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
            for(final RedisPubSubListener<String, String> l : v)
                jedis.stringPubSub.removeListener(l);
            return Collections.EMPTY_LIST;
        });
        patternListeners.clear();
    }

    @Override
    public final void punsubscribe(String... patterns) {
        final List<String> chL = Arrays.asList(patterns);
        patternListeners.replaceAll((k, v) -> {
            if (chL.contains(k)) {
                for (final RedisPubSubListener<String, String> l : v)
                    jedis.stringPubSub.removeListener(l);
                return Collections.EMPTY_LIST;
            } else {
                return v;
            }
        });
        Utils.clearEmpties(patternListeners);
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
        return getSubscribedChannels() > 0;
    }

    @Override
    public final int getSubscribedChannels() {
        return Utils.validateInt(subscribedChannels.get());
    }

    @Override
    public final void proceedWithPatterns(Client client, String... patterns) {
        // Nothing to do
    }

    @Override
    public final void proceed(Client client, String... channels) {
        // Nothing to do
    }
}
