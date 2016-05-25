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

import co.paralleluniverse.fibers.Suspendable;
import com.lambdaworks.redis.pubsub.StatefulRedisPubSubConnection;
import com.lambdaworks.redis.pubsub.api.async.RedisPubSubAsyncCommands;
import io.netty.util.internal.EmptyArrays;
import redis.clients.jedis.Client;

import java.util.concurrent.atomic.AtomicLong;

import static co.paralleluniverse.fibers.redis.Utils.checkPubSubConnected;

/**
 * @author circlespainter
 */
@SuppressWarnings("WeakerAccess")
public abstract class JedisPubSub extends redis.clients.jedis.JedisPubSub {
    Jedis jedis;
    StatefulRedisPubSubConnection<String, String> conn;
    RedisPubSubAsyncCommands<String, String> commands;

    AtomicLong subscribedChannels = new AtomicLong();

    @Override
    @Suspendable
    public final void unsubscribe() {
        checkPubSubConnected(jedis, conn, commands);
        jedis.await(() -> commands.unsubscribe());
    }

    @Override
    @Suspendable
    public final void punsubscribe() {
        checkPubSubConnected(jedis, conn, commands);
        jedis.await(() -> commands.punsubscribe());
    }

    @Override
    @Suspendable
    public final void unsubscribe(String... channels) {
        checkPubSubConnected(jedis, conn, commands);
        jedis.await(() -> commands.unsubscribe(channels));
    }

    @Override
    @Suspendable
    public final void punsubscribe(String... patterns) {
        checkPubSubConnected(jedis, conn, commands);
        jedis.await(() -> commands.punsubscribe(patterns));
    }

    @Override
    @Suspendable
    public final void subscribe(String... channels) {
        checkPubSubConnected(jedis, conn, commands);
        jedis.await(() -> commands.subscribe(channels));
    }

    @Override
    @Suspendable
    public final void psubscribe(String... patterns) {
        checkPubSubConnected(jedis, conn, commands);
        jedis.await(() -> commands.psubscribe(patterns));
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

    final void close() {
        commands = null;
        conn.close();
        conn = null;
        jedis = null;
    }
}
