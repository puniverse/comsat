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

import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.Suspendable;
import co.paralleluniverse.fibers.futures.AsyncCompletionStage;
import com.lambdaworks.redis.*;
import com.lambdaworks.redis.api.StatefulRedisConnection;
import com.lambdaworks.redis.api.async.RedisAsyncCommands;
import com.lambdaworks.redis.pubsub.RedisPubSubListener;
import com.lambdaworks.redis.pubsub.StatefulRedisPubSubConnection;
import com.lambdaworks.redis.pubsub.api.async.RedisPubSubAsyncCommands;
import redis.clients.jedis.*;
import redis.clients.jedis.params.geo.GeoRadiusParam;
import redis.clients.jedis.params.sortedset.ZAddParams;
import redis.clients.jedis.params.sortedset.ZIncrByParams;
import redis.clients.util.Pool;
import redis.clients.util.Slowlog;

import java.net.URI;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.IntBinaryOperator;
import java.util.function.LongBinaryOperator;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;
import java.util.stream.Collectors;

import static java.util.Collections.EMPTY_MAP;

/**
 * @author circlespainter
 */
public final class FiberJedis extends Jedis {
    private final RedisClient redisClient;
    private final StatefulRedisPubSubConnection<String, String> pubSubConn;
    private final StatefulRedisConnection<String, String> commandsConn;

    final RedisPubSubAsyncCommands<String, String> pubSub;
    final RedisAsyncCommands<String, String> commands;

    public FiberJedis(RedisClient client) {
        redisClient = client;
        pubSubConn = redisClient.connectPubSub();
        commandsConn = redisClient.connect();
        commands = commandsConn.async();
        pubSub = pubSubConn.async();
    }

    public FiberJedis() {
        this(RedisClient.create());
    }

    public FiberJedis(String host) {
        this(RedisClient.create(RedisURI.create("redis://" + host)));
    }

    public FiberJedis(String host, int port) {
        this(RedisClient.create(RedisURI.create(host, port)));
    }

    public FiberJedis(String host, int port, int timeout) {
        this(RedisClient.create(RedisURI.Builder.redis(host, port).withTimeout(timeout, TimeUnit.MILLISECONDS).build()));
    }

    public FiberJedis(URI uri) {
        this(RedisClient.create(RedisURI.Builder.redis(uri.getHost(), uri.getPort()).build()));
    }

    public FiberJedis(URI uri, int timeout) {
        this(RedisClient.create(RedisURI.Builder.redis(uri.getHost(), uri.getPort()).withTimeout(timeout, TimeUnit.MILLISECONDS).build()));
    }

    @Override
    public final void close() {
        commands.close();
    }

    @Suspendable
    private static <T> T e(Callable<T> c) {
        try {
            return c.call();
        } catch (final SuspendExecution e) {
            throw new AssertionError(e);
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static SetArgs toSetArgs(String nxxx) {
        final SetArgs b = new SetArgs();

        if ("XX".equalsIgnoreCase(nxxx))
            b.xx();
        else if ("NX".equalsIgnoreCase(nxxx))
            b.nx();

        return b;
    }

    private static SetArgs toSetArgs(String nxxx, String expx, long time) {
        final SetArgs b = new SetArgs();

        if ("XX".equalsIgnoreCase(nxxx))
            b.xx();
        else if ("NX".equalsIgnoreCase(nxxx))
            b.nx();

        if ("EX".equalsIgnoreCase(expx))
            b.ex(time);
        else if ("PX".equalsIgnoreCase(expx))
            b.px(time);

        return b;
    }

    @Override
    @Suspendable
    public final String set(String key, String value) {
        return e(() -> AsyncCompletionStage.get(commands.set(key, value)));
    }

    @Override
    @Suspendable
    public final String set(String key, String value, String nxxx, String expx, long time) {
        return e(() -> AsyncCompletionStage.get(commands.set(key, value, toSetArgs(nxxx, expx, time))));
    }

    @Override
    @Suspendable
    public final String get(String key) {
        return e(() -> AsyncCompletionStage.get(commands.get(key)));
    }

    @Override
    @Suspendable
    public final Long exists(String... keys) {
        return e(() -> AsyncCompletionStage.get(commands.exists(keys)));
    }

    @Override
    @Deprecated
    @Suspendable
    public final Boolean exists(String key) {
        //noinspection deprecation
        return e(() -> AsyncCompletionStage.get(commands.exists(key)));
    }

    @Override
    @Suspendable
    public final Long del(String... keys) {
        return e(() -> AsyncCompletionStage.get(commands.del(keys)));
    }

    @Override
    @Suspendable
    public final Long del(String key) {
        return e(() -> AsyncCompletionStage.get(commands.del(key)));
    }

    @Override
    @Suspendable
    public final String type(String key) {
        return e(() -> AsyncCompletionStage.get(commands.type(key)));
    }

    @Override
    @Suspendable
    public final Set<String> keys(String pattern) {
        return new HashSet<>(e(() -> AsyncCompletionStage.get(commands.keys(pattern))));
    }

    @Override
    @Suspendable
    public final String randomKey() {
        return e(() -> AsyncCompletionStage.get(commands.randomkey()));
    }

    @Override
    @Suspendable
    public final String rename(String oldKey, String newKey) {
        return e(() -> AsyncCompletionStage.get(commands.rename(oldKey, newKey)));
    }

    @Override
    @Suspendable
    public final Long renamenx(String oldKey, String newKey) {
        return e(() -> AsyncCompletionStage.get(commands.renamenx(oldKey, newKey))) ? 1L : 0L;
    }

    @Override
    @Suspendable
    public final Long expire(String key, int seconds) {
        return e(() -> AsyncCompletionStage.get(commands.expire(key, seconds))) ? 1L : 0L;
    }

    @Override
    @Suspendable
    public final Long expireAt(String key, long unixTime) {
        return e(() -> AsyncCompletionStage.get(commands.expireat(key, unixTime))) ? 1L : 0L;
    }

    @Override
    @Suspendable
    public final Long ttl(String key) {
        return e(() -> AsyncCompletionStage.get(commands.ttl(key)));
    }

    @Override
    @Suspendable
    public final Long move(String key, int dbIndex) {
        return e(() -> AsyncCompletionStage.get(commands.move(key, dbIndex))) ? 1L : 0L;
    }

    @Override
    @Suspendable
    public final String getSet(String key, String value) {
        return e(() -> AsyncCompletionStage.get(commands.getset(key, value)));
    }

    @Override
    @Suspendable
    public final List<String> mget(String... keys) {
        return e(() -> AsyncCompletionStage.get(commands.mget(keys)));
    }

    @Override
    @Suspendable
    public final Long setnx(String key, String value) {
        return e(() -> AsyncCompletionStage.get(commands.setnx(key, value))) ? 1L : 0L;
    }

    @Override
    @Suspendable
    public final String setex(String key, int seconds, String value) {
        return e(() -> AsyncCompletionStage.get(commands.setex(key, seconds, value)));
    }

    private static Map<String, String> kvArrayToMap(String... keysValues) {
        if (keysValues == null)
            //noinspection unchecked
            return EMPTY_MAP;

        if (keysValues.length % 2 != 0)
            throw new IllegalArgumentException("Even elements count required");

        final Map<String, String> res = new HashMap<>();
        boolean odd = true;
        String key = null, value = null;
        for (final String e : keysValues) {
            if (odd)
                key = e;
            else
                value = e;
            odd = !odd;
            if (key != null && value != null)
                res.put(key, value);
        }
        return res;
    }

    @Override
    @Suspendable
    public final String mset(String... keysValues) {
        return e(() -> AsyncCompletionStage.get(commands.mset(kvArrayToMap(keysValues))));
    }

    @Override
    @Suspendable
    public final Long msetnx(String... keysValues) {
        return e(() -> AsyncCompletionStage.get(commands.msetnx(kvArrayToMap(keysValues)))) ? 1L : 0L;
    }

    @Override
    @Suspendable
    public final Long decrBy(String key, long integer) {
        return e(() -> AsyncCompletionStage.get(commands.decrby(key, integer)));
    }

    @Override
    @Suspendable
    public final Long decr(String key) {
        return e(() -> AsyncCompletionStage.get(commands.decr(key)));
    }

    @Override
    @Suspendable
    public final Long incrBy(String key, long integer) {
        return e(() -> AsyncCompletionStage.get(commands.incrby(key, integer)));
    }

    @Override
    @Suspendable
    public final Double incrByFloat(String key, double value) {
        return e(() -> AsyncCompletionStage.get(commands.incrbyfloat(key, value)));
    }

    @Override
    @Suspendable
    public final Long incr(String key) {
        return e(() -> AsyncCompletionStage.get(commands.incr(key)));
    }

    @Override
    @Suspendable
    public final Long append(String key, String value) {
        return e(() -> AsyncCompletionStage.get(commands.append(key, value)));
    }

    @Override
    @Suspendable
    public final String substr(String key, int start, int end) {
        return e(() -> AsyncCompletionStage.get(commands.getrange(key, start, end)));
    }

    @Override
    @Suspendable
    public final Long hset(String key, String field, String value) {
        return e(() -> AsyncCompletionStage.get(commands.hset(key, field, value))) ? 1L : 0L;
    }

    @Override
    @Suspendable
    public final String hget(String key, String field) {
        return e(() -> AsyncCompletionStage.get(commands.hget(key, field)));
    }

    @Override
    @Suspendable
    public final Long hsetnx(String key, String field, String value) {
        return e(() -> AsyncCompletionStage.get(commands.hsetnx(key, field, value))) ? 1L : 0L;
    }

    @Override
    @Suspendable
    public final String hmset(String key, Map<String, String> hash) {
        return e(() -> AsyncCompletionStage.get(commands.hmset(key, hash)));
    }

    @Override
    @Suspendable
    public final List<String> hmget(String key, String... fields) {
        return e(() -> AsyncCompletionStage.get(commands.hmget(key, fields)));
    }

    @Override
    @Suspendable
    public final Long hincrBy(String key, String field, long value) {
        return e(() -> AsyncCompletionStage.get(commands.hincrby(key, field, value)));
    }

    @Override
    @Suspendable
    public final Double hincrByFloat(String key, String field, double value) {
        return e(() -> AsyncCompletionStage.get(commands.hincrbyfloat(key, field, value)));
    }

    @Override
    @Suspendable
    public final Boolean hexists(String key, String field) {
        return e(() -> AsyncCompletionStage.get(commands.hexists(key, field)));
    }

    @Override
    @Suspendable
    public final Long hdel(String key, String... fields) {
        return e(() -> AsyncCompletionStage.get(commands.hdel(key, fields)));
    }

    @Override
    @Suspendable
    public final Long hlen(String key) {
        return e(() -> AsyncCompletionStage.get(commands.hlen(key)));
    }

    @Override
    @Suspendable
    public final Set<String> hkeys(String key) {
        return new HashSet<>(e(() -> AsyncCompletionStage.get(commands.hkeys(key))));
    }

    @Override
    @Suspendable
    public final List<String> hvals(String key) {
        return e(() -> AsyncCompletionStage.get(commands.hvals(key)));
    }

    @Override
    @Suspendable
    public final Map<String, String> hgetAll(String key) {
        return e(() -> AsyncCompletionStage.get(commands.hgetall(key)));
    }

    @Override
    @Suspendable
    public final Long rpush(String key, String... strings) {
        return e(() -> AsyncCompletionStage.get(commands.rpush(key, strings)));
    }

    @Override
    @Suspendable
    public final Long lpush(String key, String... strings) {
        return e(() -> AsyncCompletionStage.get(commands.lpush(key, strings)));
    }

    @Override
    @Suspendable
    public final Long llen(String key) {
        return e(() -> AsyncCompletionStage.get(commands.llen(key)));
    }

    @Override
    @Suspendable
    public final List<String> lrange(String key, long start, long end) {
        return e(() -> AsyncCompletionStage.get(commands.lrange(key, start, end)));
    }

    @Override
    @Suspendable
    public final String ltrim(String key, long start, long end) {
        return e(() -> AsyncCompletionStage.get(commands.ltrim(key, start, end)));
    }

    @Override
    @Suspendable
    public final String lindex(String key, long index) {
        return e(() -> AsyncCompletionStage.get(commands.lindex(key, index)));
    }

    @Override
    @Suspendable
    public final String lset(String key, long index, String value) {
        return e(() -> AsyncCompletionStage.get(commands.lset(key, index, value)));
    }

    @Override
    @Suspendable
    public final Long lrem(String key, long count, String value) {
        return e(() -> AsyncCompletionStage.get(commands.lrem(key, count, value)));
    }

    @Override
    @Suspendable
    public final String lpop(String key) {
        return e(() -> AsyncCompletionStage.get(commands.lpop(key)));
    }

    @Override
    @Suspendable
    public final String rpop(String key) {
        return e(() -> AsyncCompletionStage.get(commands.rpop(key)));
    }

    @Override
    @Suspendable
    public final String rpoplpush(String srcKey, String dstKey) {
        return e(() -> AsyncCompletionStage.get(commands.rpoplpush(srcKey, dstKey)));
    }

    @Override
    @Suspendable
    public final Long sadd(String key, String... members) {
        return e(() -> AsyncCompletionStage.get(commands.sadd(key, members)));
    }

    @Override
    @Suspendable
    public final Set<String> smembers(String key) {
        return e(() -> AsyncCompletionStage.get(commands.smembers(key)));
    }

    @Override
    @Suspendable
    public final Long srem(String key, String... members) {
        return e(() -> AsyncCompletionStage.get(commands.srem(key, members)));
    }

    @Override
    @Suspendable
    public final String spop(String key) {
        return e(() -> AsyncCompletionStage.get(commands.spop(key)));
    }

    @Override
    @Suspendable
    public final Long smove(String srcKey, String dstKey, String member) {
        return e(() -> AsyncCompletionStage.get(commands.smove(srcKey, dstKey, member))) ? 1L : 0L;
    }

    @Override
    @Suspendable
    public final Long scard(String key) {
        return e(() -> AsyncCompletionStage.get(commands.scard(key)));
    }

    @Override
    @Suspendable
    public final Boolean sismember(String key, String member) {
        return e(() -> AsyncCompletionStage.get(commands.sismember(key, member)));
    }

    @Override
    @Suspendable
    public final Set<String> sinter(String... keys) {
        return e(() -> AsyncCompletionStage.get(commands.sinter(keys)));
    }

    @Override
    @Suspendable
    public final Long sinterstore(String dstKey, String... keys) {
        return e(() -> AsyncCompletionStage.get(commands.sinterstore(dstKey, keys)));
    }

    @Override
    @Suspendable
    public final Set<String> sunion(String... keys) {
        return e(() -> AsyncCompletionStage.get(commands.sunion(keys)));
    }

    @Override
    @Suspendable
    public final Long sunionstore(String dstKey, String... keys) {
        return e(() -> AsyncCompletionStage.get(commands.sunionstore(dstKey, keys)));
    }

    @Override
    @Suspendable
    public final Set<String> sdiff(String... keys) {
        return e(() -> AsyncCompletionStage.get(commands.sdiff(keys)));
    }

    @Override
    @Suspendable
    public final Long sdiffstore(String dstKey, String... keys) {
        return e(() -> AsyncCompletionStage.get(commands.sdiffstore(dstKey, keys)));
    }

    @Override
    @Suspendable
    public final String srandmember(String key) {
        return e(() -> AsyncCompletionStage.get(commands.srandmember(key)));
    }

    @Override
    @Suspendable
    public final List<String> srandmember(String key, int count) {
        return new ArrayList<>(e(() -> AsyncCompletionStage.get(commands.srandmember(key, count))));
    }

    @Override
    @Suspendable
    public final Long zadd(String key, double score, String member) {
        return e(() -> AsyncCompletionStage.get(commands.zadd(key, score, member)));
    }

    private static ZAddArgs toZAddArgs(ZAddParams params) {
        if (params.contains("xx"))
            return ZAddArgs.Builder.xx();

        if (params.contains("nx"))
            return ZAddArgs.Builder.nx();

        if (params.contains("ch"))
            return ZAddArgs.Builder.ch();

        return null;
    }

    @Override
    @Suspendable
    public final Long zadd(String key, double score, String member, ZAddParams params) {
        return e(() -> AsyncCompletionStage.get(commands.zadd(key, toZAddArgs(params), score, member)));
    }

    private static Object[] toObjectScoreValueArray(Map<String, Double> scoreMembers) {
        if (scoreMembers == null)
            return null;

        final Object[] ret = new Object[scoreMembers.size() * 2];
        int i = 0;
        for (final Map.Entry<String, Double> k : scoreMembers.entrySet()) {
            ret[i++] = k.getKey();
            ret[i++] = k.getValue();
        }
        return ret;
    }

    @Override
    @Suspendable
    public final Long zadd(String key, Map<String, Double> scoreMembers) {
        return e(() -> AsyncCompletionStage.get(commands.zadd(key, toObjectScoreValueArray(scoreMembers))));
    }

    @Override
    @Suspendable
    public final Long zadd(String key, Map<String, Double> scoreMembers, ZAddParams params) {
        return e(() -> AsyncCompletionStage.get(commands.zadd(key, toZAddArgs(params), toObjectScoreValueArray(scoreMembers))));
    }

    @Override
    @Suspendable
    public final Set<String> zrange(String key, long start, long end) {
        return new HashSet<>(e(() -> AsyncCompletionStage.get(commands.zrange(key, start, end))));
    }

    @Override
    @Suspendable
    public final Long zrem(String key, String... members) {
        return e(() -> AsyncCompletionStage.get(commands.zrem(key, members)));
    }

    @Override
    @Suspendable
    public final Double zincrby(String key, double score, String member) {
        return e(() -> AsyncCompletionStage.get(commands.zincrby(key, score, member)));
    }

    @Override
    @Suspendable
    public final Long zrank(String key, String member) {
        return e(() -> AsyncCompletionStage.get(commands.zrank(key, member)));
    }

    @Override
    @Suspendable
    public final Long zrevrank(String key, String member) {
        return e(() -> AsyncCompletionStage.get(commands.zrevrank(key, member)));
    }

    @Override
    @Suspendable
    public final Set<String> zrevrange(String key, long start, long end) {
        return new HashSet<>(e(() -> AsyncCompletionStage.get(commands.zrevrange(key, start, end))));
    }

    private static Set<Tuple> toTupleSet(List<ScoredValue<String>> l) {
        return l.stream().map(e -> new Tuple(e.value, e.score)).collect(Collectors.toCollection(HashSet::new));
    }

    @Override
    @Suspendable
    public final Set<Tuple> zrangeWithScores(String key, long start, long end) {
        return toTupleSet(e(() -> AsyncCompletionStage.get(commands.zrangeWithScores(key, start, end))));
    }

    @Override
    @Suspendable
    public final Set<Tuple> zrevrangeWithScores(String key, long start, long end) {
        return toTupleSet(e(() -> AsyncCompletionStage.get(commands.zrevrangeWithScores(key, start, end))));
    }

    @Override
    @Suspendable
    public final Long zcard(String key) {
        return e(() -> AsyncCompletionStage.get(commands.zcard(key)));
    }

    @Override
    @Suspendable
    public final Double zscore(String key, String member) {
        return e(() -> AsyncCompletionStage.get(commands.zscore(key, member)));
    }

    @Override
    @Suspendable
    public final String watch(String... keys) {
        return e(() -> AsyncCompletionStage.get(commands.watch(keys)));
    }

    @Override
    @Suspendable
    public final List<String> sort(String key) {
        return e(() -> AsyncCompletionStage.get(commands.sort(key)));
    }

    private static SortArgs toSortArgs(SortingParams sp) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    @Override
    @Suspendable
    public final List<String> sort(String key, SortingParams sortingParameters) {
        return e(() -> AsyncCompletionStage.get(commands.sort(key, toSortArgs(sortingParameters))));
    }

    private static List<String> toList(KeyValue<String, String> kv) {
        final List<String> ret = new ArrayList<>(2);
        ret.add(kv.key);
        ret.add(kv.value);
        return ret;
    }

    @Override
    @Suspendable
    public final List<String> blpop(int timeout, String... keys) {
        return toList(e(() -> AsyncCompletionStage.get(commands.blpop(timeout, keys))));
    }

    @Override
    @Suspendable
    public final List<String> blpop(String... args) {
        return toList(e(() -> AsyncCompletionStage.get(commands.blpop(0 /* Indefinitely */, args))));
    }

    @Override
    @Suspendable
    public final List<String> brpop(String... args) {
        return toList(e(() -> AsyncCompletionStage.get(commands.brpop(0 /* Indefinitely */, args))));
    }

    @Override
    @Suspendable
    public final Long sort(String key, SortingParams sortingParameters, String dstKey) {
        return e(() -> AsyncCompletionStage.get(commands.sortStore(key, toSortArgs(sortingParameters), dstKey)));
    }

    @Override
    @Suspendable
    public final Long sort(String key, String dstKey) {
        return e(() -> AsyncCompletionStage.get(commands.sortStore(key, new SortArgs(), dstKey)));
    }

    @Override
    @Suspendable
    public final List<String> brpop(int timeout, String... keys) {
        return toList(e(() -> AsyncCompletionStage.get(commands.brpop(timeout, keys))));
    }

    @Override
    @Suspendable
    public final Long zcount(String key, double min, double max) {
        return e(() -> AsyncCompletionStage.get(commands.zcount(key, min, max)));
    }

    @Override
    @Suspendable
    public final Long zcount(String key, String min, String max) {
        return e(() -> AsyncCompletionStage.get(commands.zcount(key, min, max)));
    }

    @Override
    @Suspendable
    public final Set<String> zrangeByScore(String key, double min, double max) {
        return new HashSet<>(e(() -> AsyncCompletionStage.get(commands.zrangebyscore(key, min, max))));
    }

    @Override
    @Suspendable
    public final Set<String> zrangeByScore(String key, String min, String max) {
        return new HashSet<>(e(() -> AsyncCompletionStage.get(commands.zrangebyscore(key, min, max))));
    }

    @Override
    @Suspendable
    public final Set<String> zrangeByScore(String key, double min, double max, int offset, int count) {
        return new HashSet<>(e(() -> AsyncCompletionStage.get(commands.zrangebyscore(key, min, max, offset, count))));
    }

    @Override
    @Suspendable
    public final Set<String> zrangeByScore(String key, String min, String max, int offset, int count) {
        return new HashSet<>(e(() -> AsyncCompletionStage.get(commands.zrangebyscore(key, min, max, offset, count))));
    }

    @Override
    @Suspendable
    public final Set<Tuple> zrangeByScoreWithScores(String key, double min, double max) {
        return toTupleSet(e(() -> AsyncCompletionStage.get(commands.zrangebyscoreWithScores(key, min, max))));
    }

    @Override
    @Suspendable
    public final Set<Tuple> zrangeByScoreWithScores(String key, String min, String max) {
        return toTupleSet(e(() -> AsyncCompletionStage.get(commands.zrangebyscoreWithScores(key, min, max))));
    }

    @Override
    @Suspendable
    public final Set<Tuple> zrangeByScoreWithScores(String key, double min, double max, int offset, int count) {
        return toTupleSet(e(() -> AsyncCompletionStage.get(commands.zrangebyscoreWithScores(key, min, max, offset, count))));
    }

    @Override
    @Suspendable
    public final Set<Tuple> zrangeByScoreWithScores(String key, String min, String max, int offset, int count) {
        return toTupleSet(e(() -> AsyncCompletionStage.get(commands.zrangebyscoreWithScores(key, min, max, offset, count))));
    }

    @Override
    @Suspendable
    public final Set<String> zrevrangeByScore(String key, double max, double min) {
        return new HashSet<>(e(() -> AsyncCompletionStage.get(commands.zrevrangebyscore(key, min, max))));
    }

    @Override
    @Suspendable
    public final Set<String> zrevrangeByScore(String key, String max, String min) {
        return new HashSet<>(e(() -> AsyncCompletionStage.get(commands.zrevrangebyscore(key, min, max))));
    }

    @Override
    @Suspendable
    public final Set<String> zrevrangeByScore(String key, double max, double min, int offset, int count) {
        return new HashSet<>(e(() -> AsyncCompletionStage.get(commands.zrevrangebyscore(key, min, max))));
    }

    @Override
    @Suspendable
    public final Set<Tuple> zrevrangeByScoreWithScores(String key, double max, double min) {
        return toTupleSet(e(() -> AsyncCompletionStage.get(commands.zrevrangebyscoreWithScores(key, min, max))));
    }

    @Override
    @Suspendable
    public final Set<Tuple> zrevrangeByScoreWithScores(String key, double max, double min, int offset, int count) {
        return toTupleSet(e(() -> AsyncCompletionStage.get(commands.zrevrangebyscoreWithScores(key, min, max, offset, count))));
    }

    @Override
    @Suspendable
    public final Set<Tuple> zrevrangeByScoreWithScores(String key, String max, String min, int offset, int count) {
        return toTupleSet(e(() -> AsyncCompletionStage.get(commands.zrevrangebyscoreWithScores(key, min, max, offset, count))));
    }

    @Override
    @Suspendable
    public final Set<String> zrevrangeByScore(String key, String max, String min, int offset, int count) {
        return new HashSet<>(e(() -> AsyncCompletionStage.get(commands.zrevrangebyscore(key, min, max, offset, count))));
    }

    @Override
    @Suspendable
    public final Set<Tuple> zrevrangeByScoreWithScores(String key, String max, String min) {
        return toTupleSet(e(() -> AsyncCompletionStage.get(commands.zrevrangebyscoreWithScores(key, min, max))));
    }

    @Override
    @Suspendable
    public final Long zremrangeByRank(String key, long start, long end) {
        return e(() -> AsyncCompletionStage.get(commands.zremrangebyrank(key, start, end)));
    }

    @Override
    @Suspendable
    public final Long zremrangeByScore(String key, double start, double end) {
        return e(() -> AsyncCompletionStage.get(commands.zremrangebyscore(key, start, end)));
    }

    @Override
    @Suspendable
    public final Long zremrangeByScore(String key, String start, String end) {
        return e(() -> AsyncCompletionStage.get(commands.zremrangebyscore(key, start, end)));
    }

    @Override
    @Suspendable
    public final Long zunionstore(String dstKey, String... sets) {
        return e(() -> AsyncCompletionStage.get(commands.zunionstore(dstKey, sets)));
    }

    private static ZStoreArgs toZStoreArgs(ZParams ps) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    @Override
    @Suspendable
    public final Long zunionstore(String dstKey, ZParams params, String... sets) {
        return e(() -> AsyncCompletionStage.get(commands.zunionstore(dstKey, toZStoreArgs(params), sets)));
    }

    @Override
    @Suspendable
    public final Long zinterstore(String dstKey, String... sets) {
        return e(() -> AsyncCompletionStage.get(commands.zinterstore(dstKey, sets)));
    }

    @Override
    @Suspendable
    public final Long zinterstore(String dstKey, ZParams params, String... sets) {
        return e(() -> AsyncCompletionStage.get(commands.zinterstore(dstKey, toZStoreArgs(params), sets)));
    }

    @Override
    @Suspendable
    public final Long zlexcount(String key, String min, String max) {
        return e(() -> AsyncCompletionStage.get(commands.zlexcount(key, min, max)));
    }

    @Override
    @Suspendable
    public final Set<String> zrangeByLex(String key, String min, String max) {
        return new HashSet<>(e(() -> AsyncCompletionStage.get(commands.zrangebylex(key, min, max))));
    }

    @Override
    @Suspendable
    public final Set<String> zrangeByLex(String key, String min, String max, int offset, int count) {
        return new HashSet<>(e(() -> AsyncCompletionStage.get(commands.zrangebylex(key, min, max, offset, count))));
    }

    @Override
    @Suspendable
    public final Set<String> zrevrangeByLex(String key, String max, String min) {
        return new HashSet<>(e(() -> AsyncCompletionStage.get(commands.zrangebylex(key, min, max))));
    }

    @Override
    @Suspendable
    public final Set<String> zrevrangeByLex(String key, String max, String min, int offset, int count) {
        return new HashSet<>(e(() -> AsyncCompletionStage.get(commands.zrangebylex(key, min, max, offset, count))));
    }

    @Override
    @Suspendable
    public final Long zremrangeByLex(String key, String min, String max) {
        return e(() -> AsyncCompletionStage.get(commands.zremrangebylex(key, min, max)));
    }

    @Override
    @Suspendable
    public final Long strlen(String key) {
        return e(() -> AsyncCompletionStage.get(commands.strlen(key)));
    }

    @Override
    @Suspendable
    public final Long persist(String key) {
        return e(() -> AsyncCompletionStage.get(commands.persist(key))) ? 1L : 0L;
    }

    @Override
    @Suspendable
    public final String echo(String string) {
        return e(() -> AsyncCompletionStage.get(commands.echo(string)));
    }

    @Override
    @Suspendable
    public final Long linsert(String key, BinaryClient.LIST_POSITION where, String pivot, String value) {
        return e(() -> AsyncCompletionStage.get(commands.linsert(key, BinaryClient.LIST_POSITION.BEFORE.equals(where), pivot, value)));
    }

    @Override
    @Suspendable
    public final String brpoplpush(String source, String destination, int timeout) {
        return e(() -> AsyncCompletionStage.get(commands.brpoplpush(timeout, source, destination)));
    }

    @Override
    @Suspendable
    public final Boolean setbit(String key, long offset, boolean value) {
        return e(() -> AsyncCompletionStage.get(commands.setbit(key, offset, value ? 1 : 0))) > 0;
    }

    @Override
    @Suspendable
    public final Boolean setbit(String key, long offset, String value) {
        return e(() -> AsyncCompletionStage.get(commands.setbit(key, offset, Boolean.parseBoolean(value) ? 1 : 0))) > 0;
    }

    @Override
    @Suspendable
    public final Boolean getbit(String key, long offset) {
        return e(() -> AsyncCompletionStage.get(commands.getbit(key, offset))) > 0;
    }

    @Override
    @Suspendable
    public final Long setrange(String key, long offset, String value) {
        return e(() -> AsyncCompletionStage.get(commands.setrange(key, offset, value)));
    }

    @Override
    @Suspendable
    public final String getrange(String key, long startOffset, long endOffset) {
        return e(() -> AsyncCompletionStage.get(commands.getrange(key, startOffset, endOffset)));
    }

    @Override
    @Suspendable
    public final Long bitpos(String key, boolean value) {
        return e(() -> AsyncCompletionStage.get(commands.bitpos(key, value)));
    }

    private static long getStart(BitPosParams params) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    private static long getEnd(BitPosParams params) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    @Override
    @Suspendable
    public final Long bitpos(String key, boolean value, BitPosParams params) {
        return e(() -> AsyncCompletionStage.get(commands.bitpos(key, value, getStart(params), getEnd(params))));
    }

    @Override
    @Suspendable
    public final List<String> configGet(String pattern) {
        return e(() -> AsyncCompletionStage.get(commands.configGet(pattern)));
    }

    @Override
    @Suspendable
    public final String configSet(String parameter, String value) {
        return e(() -> AsyncCompletionStage.get(commands.configSet(parameter, value)));
    }

    @Override
    @Suspendable
    public final List<Slowlog> slowlogGet() {
        return e(() -> Slowlog.from(AsyncCompletionStage.get(commands.slowlogGet())));
    }

    @Override
    @Suspendable
    public final List<Slowlog> slowlogGet(long entries) {
        validateInt(entries);
        return e(() -> Slowlog.from(AsyncCompletionStage.get(commands.slowlogGet((int) entries))));
    }

    @SuppressWarnings("WeakerAccess")
    static void validateInt(long entries) {
        if (entries > Integer.MAX_VALUE)
            throw new IllegalArgumentException("entries can't exceed int size");
    }

    @Override
    @Suspendable
    public final Long objectRefcount(String string) {
        return e(() -> AsyncCompletionStage.get(commands.objectRefcount(string)));
    }

    @Override
    @Suspendable
    public final String objectEncoding(String string) {
        return e(() -> AsyncCompletionStage.get(commands.objectEncoding(string)));
    }

    @Override
    @Suspendable
    public final Long objectIdletime(String string) {
        return e(() -> AsyncCompletionStage.get(commands.objectIdletime(string)));
    }

    @Override
    @Suspendable
    public final Long bitcount(String key) {
        return e(() -> AsyncCompletionStage.get(commands.bitcount(key)));
    }

    @Override
    @Suspendable
    public final Long bitcount(String key, long start, long end) {
        return e(() -> AsyncCompletionStage.get(commands.bitcount(key, start, end)));
    }

    @Override
    @Suspendable
    public final Long bitop(BitOP op, String destKey, String... srcKeys) {
        return e(() -> {
            final Long res;
            switch (op) {
                case AND:
                    res = AsyncCompletionStage.get(commands.bitopAnd(destKey, srcKeys));
                    break;
                case OR:
                    res = AsyncCompletionStage.get(commands.bitopOr(destKey, srcKeys));
                    break;
                case XOR:
                    res = AsyncCompletionStage.get(commands.bitopXor(destKey, srcKeys));
                    break;
                case NOT:
                    if (srcKeys == null || srcKeys.length != 1)
                        throw new IllegalArgumentException("'not' requires exactly one argument");
                    res = AsyncCompletionStage.get(commands.bitopNot(destKey, srcKeys[0]));
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported operation: " + op);
            }
            return res;
        });
    }

    @Override
    @Suspendable
    public final byte[] dump(String key) {
        return e(() -> AsyncCompletionStage.get(commands.dump(key)));
    }

    @Override
    @Suspendable
    public final String restore(String key, int ttl, byte[] serializedValue) {
        return e(() -> AsyncCompletionStage.get(commands.restore(key, ttl, serializedValue)));
    }

    @Override
    @Deprecated
    @Suspendable
    @SuppressWarnings("deprecation")
    public final Long pexpire(String key, int milliseconds) {
        return pexpire(key, (long) milliseconds);
    }

    @Override
    @Suspendable
    public final Long pexpire(String key, long milliseconds) {
        return e(() -> AsyncCompletionStage.get(commands.pexpire(key, milliseconds))) ? 1L : 0;
    }

    @Override
    @Suspendable
    public final Long pexpireAt(String key, long millisecondsTimestamp) {
        return e(() -> AsyncCompletionStage.get(commands.pexpireat(key, millisecondsTimestamp))) ? 1L : 0;
    }

    @Override
    @Suspendable
    public final Long pttl(String key) {
        return e(() -> AsyncCompletionStage.get(commands.pttl(key)));
    }

    @Override
    @Deprecated
    @Suspendable
    @SuppressWarnings("deprecation")
    public final String psetex(String key, int milliseconds, String value) {
        return psetex(key, (long) milliseconds, value);
    }

    @Override
    @Suspendable
    public final String psetex(String key, long milliseconds, String value) {
        return e(() -> AsyncCompletionStage.get(commands.psetex(key, milliseconds, value)));
    }

    @Override
    @Suspendable
    public final String set(String key, String value, String nxxx) {
        return e(() -> AsyncCompletionStage.get(commands.set(key, value, toSetArgs(nxxx))));
    }

    @Override
    @Suspendable
    public final String set(String key, String value, String nxxx, String expx, int time) {
        return e(() -> AsyncCompletionStage.get(commands.set(key, value, toSetArgs(nxxx, expx, time))));
    }

    @Override
    @Suspendable
    public final String clientKill(String client) {
        return e(() -> AsyncCompletionStage.get(commands.clientKill(client)));
    }

    @Override
    @Suspendable
    public final String clientSetname(String name) {
        return e(() -> AsyncCompletionStage.get(commands.clientSetname(name)));
    }

    @Override
    @Suspendable
    public final String migrate(String host, int port, String key, int destinationDb, int timeout) {
        return e(() -> AsyncCompletionStage.get(commands.migrate(host, port, key, destinationDb, timeout)));
    }

    @Override
    @Suspendable
    public final Long pfadd(String key, String... elements) {
        return e(() -> AsyncCompletionStage.get(commands.pfadd(key, elements)));
    }

    @Override
    @Suspendable
    public final long pfcount(String key) {
        return e(() -> AsyncCompletionStage.get(commands.pfcount(new String[] { key })));
    }

    @Override
    @Suspendable
    public final long pfcount(String... keys) {
        return e(() -> AsyncCompletionStage.get(commands.pfcount(keys)));
    }

    @Override
    @Suspendable
    public final String pfmerge(String destKey, String... sourceKeys) {
        return e(() -> AsyncCompletionStage.get(commands.pfmerge(destKey, sourceKeys)));
    }

    @Override
    @Suspendable
    public final List<String> blpop(int timeout, String key) {
        return toList(e(() -> AsyncCompletionStage.get(commands.blpop(timeout, key))));
    }

    @Override
    @Suspendable
    public final List<String> brpop(int timeout, String key) {
        return toList(e(() -> AsyncCompletionStage.get(commands.brpop(timeout, key))));
    }

    @Override
    @Suspendable
    public final Long geoadd(String key, double longitude, double latitude, String member) {
        return e(() -> AsyncCompletionStage.get(commands.geoadd(key, longitude, latitude, member)));
    }

    private static Object[] toTripletArray(Map<String, GeoCoordinate> memberCoordinateMap) {
        if (memberCoordinateMap == null)
            return null;

        final Object[] ret = new Object[memberCoordinateMap.size() * 3];
        int i = 0;
        for (final Map.Entry<String, GeoCoordinate> e : memberCoordinateMap.entrySet()) {
            final GeoCoordinate v = e.getValue();
            ret[i++] = v.getLongitude();
            ret[i++] = v.getLatitude();
            ret[i++] = e.getKey();
        }
        return ret;
    }

    @Override
    @Suspendable
    public final Long geoadd(String key, Map<String, GeoCoordinate> memberCoordinateMap) {
        return e(() -> AsyncCompletionStage.get(commands.geoadd(key, toTripletArray(memberCoordinateMap))));
    }

    @Override
    @Suspendable
    public final Double geodist(String key, String member1, String member2) {
        return e(() -> AsyncCompletionStage.get(commands.geodist(key, member1, member2, null)));
    }

    private static GeoArgs.Unit toGeoArgsUnit(GeoUnit unit) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    @Override
    @Suspendable
    public final Double geodist(String key, String member1, String member2, GeoUnit unit) {
        return e(() -> AsyncCompletionStage.get(commands.geodist(key, member1, member2, toGeoArgsUnit(unit))));
    }

    private static GeoCoordinate toGeoCoordinate(GeoCoordinates c) {
        return new GeoCoordinate(c.x.doubleValue(), c.y.doubleValue());
    }

    @Override
    @Suspendable
    public final List<GeoCoordinate> geopos(String key, String... members) {
        return e(() -> AsyncCompletionStage.get(commands.geopos(key, members)).stream().map(FiberJedis::toGeoCoordinate).collect(Collectors.toList()));
    }

    private static GeoArgs.Unit toUnit(GeoUnit unit) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    private static GeoRadiusResponse toGeoRadius(String s) {
        return new GeoRadiusResponse(s.getBytes());
    }

    @Override
    @Suspendable
    public final List<GeoRadiusResponse> georadius(String key, double longitude, double latitude, double radius, GeoUnit unit) {
        return e(() -> AsyncCompletionStage.get(commands.georadius(key, longitude, latitude, radius, toUnit(unit))).stream().map(FiberJedis::toGeoRadius).collect(Collectors.toList()));
    }

    private static GeoRadiusResponse toGeoRadius(GeoWithin<String> s) {
        final GeoRadiusResponse r = new GeoRadiusResponse(s.member.getBytes());
        r.setDistance(s.distance);
        r.setCoordinate(toGeoCoordinate(s.coordinates));
        return r;
    }

    @Override
    @Suspendable
    public final List<GeoRadiusResponse> georadius(String key, double longitude, double latitude, double radius, GeoUnit unit, GeoRadiusParam param) {
        return e(() -> AsyncCompletionStage.get(commands.georadius(key, longitude, latitude, radius, toUnit(unit), toGeoArgs(param))).stream().map(FiberJedis::toGeoRadius).collect(Collectors.toList()));
    }

    private static GeoArgs toGeoArgs(GeoRadiusParam param) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    @Override
    @Suspendable
    public final List<GeoRadiusResponse> georadiusByMember(String key, String member, double radius, GeoUnit unit) {
        return e(() -> AsyncCompletionStage.get(commands.georadiusbymember(key, member, radius, toUnit(unit))).stream().map(FiberJedis::toGeoRadius).collect(Collectors.toList()));
    }

    @Override
    @Suspendable
    public final List<GeoRadiusResponse> georadiusByMember(String key, String member, double radius, GeoUnit unit, GeoRadiusParam param) {
        return e(() -> AsyncCompletionStage.get(commands.georadiusbymember(key, member, radius, toUnit(unit), toGeoArgs(param))).stream().map(FiberJedis::toGeoRadius).collect(Collectors.toList()));
    }


    @Override
    @Suspendable
    public final String readonly() {
        return e(() -> AsyncCompletionStage.get(commands.readOnly()));
    }

    @Override
    @Suspendable
    public final String asking() {
        return e(() -> AsyncCompletionStage.get(commands.asking()));
    }

    private static <T> ScanResult<T> toScanResult(KeyScanCursor<T> c) {
        return new ScanResult<>(c.getCursor(), c.getKeys());
    }

    @Override
    @Suspendable
    public final ScanResult<String> scan(String cursor) {
        return toScanResult(e(() -> AsyncCompletionStage.get(commands.scan(ScanCursor.of(cursor)))));
    }

    private static ScanArgs toScanArgs(ScanParams params) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    @Override
    @Suspendable
    public final ScanResult<String> scan(String cursor, ScanParams params) {
        return toScanResult(e(() -> AsyncCompletionStage.get(commands.scan(ScanCursor.of(cursor), toScanArgs(params)))));
    }

    private static <T1, T2> ScanResult<Map.Entry<T1, T2>> toScanResult(MapScanCursor<T1, T2> c) {
        return new ScanResult<>(c.getCursor(), new ArrayList<>(c.getMap().entrySet()));
    }

    @Override
    @Suspendable
    public final ScanResult<Map.Entry<String, String>> hscan(String key, String cursor) {
        return toScanResult(e(() -> AsyncCompletionStage.get(commands.hscan(key, ScanCursor.of(cursor)))));
    }

    @Override
    @Suspendable
    public final ScanResult<Map.Entry<String, String>> hscan(String key, String cursor, ScanParams params) {
        return toScanResult(e(() -> AsyncCompletionStage.get(commands.hscan(key, ScanCursor.of(cursor), toScanArgs(params)))));
    }

    private static <T> ScanResult<T> toScanResult(ValueScanCursor<T> c) {
        return new ScanResult<>(c.getCursor(), c.getValues());
    }

    @Override
    @Suspendable
    public final ScanResult<String> sscan(String key, String cursor) {
        return toScanResult(e(() -> AsyncCompletionStage.get(commands.sscan(key, ScanCursor.of(cursor)))));
    }

    @Override
    @Suspendable
    public final ScanResult<String> sscan(String key, String cursor, ScanParams params) {
        return toScanResult(e(() -> AsyncCompletionStage.get(commands.sscan(key, ScanCursor.of(cursor), toScanArgs(params)))));
    }

    private static ScanResult<Tuple> toScanResult(ScoredValueScanCursor<String> c) {
        return new ScanResult<>(c.getCursor(), c.getValues().stream().map((sv) -> new Tuple(sv.value, sv.score)).collect(Collectors.toList()));
    }

    @Override
    @Suspendable
    public final ScanResult<Tuple> zscan(String key, String cursor) {
        return toScanResult(e(() -> AsyncCompletionStage.get(commands.zscan(key, ScanCursor.of(cursor)))));
    }

    @Override
    @Suspendable
    public final ScanResult<Tuple> zscan(String key, String cursor, ScanParams params) {
        return toScanResult(e(() -> AsyncCompletionStage.get(commands.zscan(key, ScanCursor.of(cursor), toScanArgs(params)))));
    }

    /////////////////////////// PUBSUB

    static void clearEmpties(ConcurrentHashMap<Set<String>, Optional<RedisPubSubListener<String, String>>> m) {
        final List<Set<String>> toBeRemoved = new ArrayList<>(4);
        toBeRemoved.addAll(m.entrySet().stream().filter(e -> !e.getValue().isPresent()).map(Map.Entry::getKey).collect(Collectors.toList()));
        toBeRemoved.forEach(m::remove);
    }

    @SuppressWarnings("WeakerAccess")
    public class PubSub extends JedisPubSub {
        private ConcurrentHashMap<Set<String>, Optional<RedisPubSubListener<String, String>>> channelListeners = new ConcurrentHashMap<>();

        private ConcurrentHashMap<Set<String>, Optional<RedisPubSubListener<String, String>>> patternListeners = new ConcurrentHashMap<>();

        @Override
        public final void unsubscribe() {
            channelListeners.replaceAll((k, v) -> {
                if (v.isPresent())
                    FiberJedis.this.pubSub.removeListener(v.get());
                return Optional.empty();
            });
            channelListeners.clear();
        }

        @Override
        public final void unsubscribe(String... channels) {
            channelListeners.computeIfPresent(new HashSet<>(Arrays.asList(channels)), (k, v) -> {
                if (v.isPresent())
                    FiberJedis.this.pubSub.removeListener(v.get());
                return Optional.empty();
            });
            clearEmpties(channelListeners);
        }

        @Override
        public final void punsubscribe() {
            patternListeners.replaceAll((k, v) -> {
                if (v.isPresent())
                    FiberJedis.this.pubSub.removeListener(v.get());
                return Optional.empty();
            });
            patternListeners.clear();
        }

        @Override
        public final void punsubscribe(String... patterns) {
            patternListeners.computeIfPresent(new HashSet<>(Arrays.asList(patterns)), (k, v) -> {
                if (v.isPresent())
                    FiberJedis.this.pubSub.removeListener(v.get());
                return Optional.empty();
            });
            clearEmpties(patternListeners);
        }

        @Override
        public final void subscribe(String... channels) {
            FiberJedis.this.subscribe(this, channels);
        }

        @Override
        public final void psubscribe(String... patterns) {
            FiberJedis.this.psubscribe(this, patterns);
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
                e -> e.getValue().map(v -> FiberJedis.this.pubsubNumSub(e.getKey().toArray(new String[e.getKey().size()])).size()).orElse(0);
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

    private static final class FiberRedisPubSubListener implements RedisPubSubListener<String, String> {
        private final PubSub pubSub;

        FiberRedisPubSubListener(PubSub pubSub, boolean pattern, String... args) {
            this.pubSub = pubSub;
            final HashSet<String> k = new HashSet<>(Arrays.asList(args));
            final Optional<RedisPubSubListener<String, String>> v = Optional.of(this);
            if (pattern)
                pubSub.patternListeners.put(k, v);
            else
                pubSub.patternListeners.put(k, v);
        }

        @Override
        public final void message(String channel, String message) {
            pubSub.onMessage(channel, message);
        }

        @Override
        public final void message(String pattern, String channel, String message) {
            pubSub.onPMessage(pattern, channel, message);
        }

        @Override
        public final void subscribed(String channel, long count) {
            validateInt(count);
            pubSub.onSubscribe(channel, (int) count);
        }

        @Override
        public final void psubscribed(String pattern, long count) {
            validateInt(count);
            pubSub.onPSubscribe(pattern, (int) count);
        }

        @Override
        public final void unsubscribed(String channel, long count) {
            validateInt(count);
            pubSub.onUnsubscribe(channel, (int) count);
        }

        @Override
        public final void punsubscribed(String pattern, long count) {
            validateInt(count);
            pubSub.onPUnsubscribe(pattern, (int) count);
        }
    }

    @Override
    @Suspendable
    public final void subscribe(JedisPubSub jedisPubSub, String... channels) {
        pubSub.addListener(new FiberRedisPubSubListener((PubSub) jedisPubSub, false, channels));
    }

    @Override
    @Suspendable
    public final Long publish(String channel, String message) {
        return e(() -> AsyncCompletionStage.get(commands.publish(channel, message)));
    }

    @Override
    @Suspendable
    public final void psubscribe(JedisPubSub jedisPubSub, String... patterns) {
        pubSubConn.addListener(new FiberRedisPubSubListener((PubSub) jedisPubSub, true, patterns));
    }

    @Override
    @Suspendable
    public final List<String> pubsubChannels(String pattern) {
        return e(() -> AsyncCompletionStage.get(commands.pubsubChannels(pattern)));
    }

    @Override
    @Suspendable
    public final Long pubsubNumPat() {
        return e(() -> AsyncCompletionStage.get(commands.pubsubNumpat()));
    }

    private static <K, V> Map<String, String> toStringMap(Map<K, V> m) {
        final Map<String, String> ret = new HashMap<>();
        for (final Map.Entry<K, V> e : m.entrySet())
            ret.put(e.getKey().toString(), e.getValue().toString());
        return ret;
    }

    @Override
    @Suspendable
    public final Map<String, String> pubsubNumSub(String... channels) {
        return toStringMap(e(() -> AsyncCompletionStage.get(commands.pubsubNumsub(channels))));
    }

    /////////////////////////// SCRIPTING
    @Override
    @Suspendable
    public final Object eval(String script, int keyCount, String... params) {
        throw new UnsupportedOperationException("Not (yet) supported");
    }

    @Override
    @Suspendable
    public final Object eval(String script, List<String> keys, List<String> args) {
        throw new UnsupportedOperationException("Not (yet) supported");
    }

    @Override
    @Suspendable
    public final Object eval(String script) {
        throw new UnsupportedOperationException("Not (yet) supported");
    }

    @Override
    @Suspendable
    public final Object evalsha(String script) {
        throw new UnsupportedOperationException("Not (yet) supported");
    }

    @Override
    @Suspendable
    public final Object evalsha(String sha1, List<String> keys, List<String> args) {
        throw new UnsupportedOperationException("Not (yet) supported");
    }

    @Override
    @Suspendable
    public final Object evalsha(String sha1, int keyCount, String... params) {
        throw new UnsupportedOperationException("Not (yet) supported");
    }

    @Override
    @Suspendable
    public final Boolean scriptExists(String sha1) {
        throw new UnsupportedOperationException("Not (yet) supported");
    }

    @Override
    @Suspendable
    public final List<Boolean> scriptExists(String... sha1) {
        throw new UnsupportedOperationException("Not (yet) supported");
    }

    @Override
    @Suspendable
    public final String scriptLoad(String script) {
        throw new UnsupportedOperationException("Not (yet) supported");
    }

    @Override
    @Suspendable
    public final List<Map<String, String>> sentinelMasters() {
        throw new UnsupportedOperationException("Not (yet) supported");
    }

    @Override
    @Suspendable
    public final List<String> sentinelGetMasterAddrByName(String masterName) {
        throw new UnsupportedOperationException("Not (yet) supported");
    }

    /////////////////////////// SENTINEL
    @Override
    @Suspendable
    public final Long sentinelReset(String pattern) {
        throw new UnsupportedOperationException("Not (yet) supported");
    }

    @Override
    @Suspendable
    public final List<Map<String, String>> sentinelSlaves(String masterName) {
        throw new UnsupportedOperationException("Not (yet) supported");
    }

    @Override
    @Suspendable
    public final String sentinelFailover(String masterName) {
        throw new UnsupportedOperationException("Not (yet) supported");
    }

    @Override
    @Suspendable
    public final String sentinelMonitor(String masterName, String ip, int port, int quorum) {
        throw new UnsupportedOperationException("Not (yet) supported");
    }

    @Override
    @Suspendable
    public final String sentinelRemove(String masterName) {
        throw new UnsupportedOperationException("Not (yet) supported");
    }

    @Override
    @Suspendable
    public final String sentinelSet(String masterName, Map<String, String> parameterMap) {
        throw new UnsupportedOperationException("Not (yet) supported");
    }

    /////////////////////////// CLUSTER
    @Override
    @Suspendable
    public final String clusterNodes() {
        throw new UnsupportedOperationException("Not (yet) supported");
    }

    @Override
    @Suspendable
    public final String clusterMeet(String ip, int port) {
        throw new UnsupportedOperationException("Not (yet) supported");
    }

    @Override
    @Suspendable
    public final String clusterReset(JedisCluster.Reset resetType) {
        throw new UnsupportedOperationException("Not (yet) supported");
    }

    @Override
    @Suspendable
    public final String clusterAddSlots(int... slots) {
        throw new UnsupportedOperationException("Not (yet) supported");
    }

    @Override
    @Suspendable
    public final String clusterDelSlots(int... slots) {
        throw new UnsupportedOperationException("Not (yet) supported");
    }

    @Override
    @Suspendable
    public final String clusterInfo() {
        throw new UnsupportedOperationException("Not (yet) supported");
    }

    @Override
    @Suspendable
    public final List<String> clusterGetKeysInSlot(int slot, int count) {
        throw new UnsupportedOperationException("Not (yet) supported");
    }

    @Override
    @Suspendable
    public final String clusterSetSlotNode(int slot, String nodeId) {
        throw new UnsupportedOperationException("Not (yet) supported");
    }

    @Override
    @Suspendable
    public final String clusterSetSlotMigrating(int slot, String nodeId) {
        throw new UnsupportedOperationException("Not (yet) supported");
    }

    @Override
    @Suspendable
    public final String clusterSetSlotImporting(int slot, String nodeId) {
        throw new UnsupportedOperationException("Not (yet) supported");
    }

    @Override
    @Suspendable
    public final String clusterSetSlotStable(int slot) {
        throw new UnsupportedOperationException("Not (yet) supported");
    }

    @Override
    @Suspendable
    public final String clusterForget(String nodeId) {
        throw new UnsupportedOperationException("Not (yet) supported");
    }

    @Override
    @Suspendable
    public final String clusterFlushSlots() {
        throw new UnsupportedOperationException("Not (yet) supported");
    }

    @Override
    @Suspendable
    public final Long clusterKeySlot(String key) {
        throw new UnsupportedOperationException("Not (yet) supported");
    }

    @Override
    @Suspendable
    public final Long clusterCountKeysInSlot(int slot) {
        throw new UnsupportedOperationException("Not (yet) supported");
    }

    @Override
    @Suspendable
    public final String clusterSaveConfig() {
        throw new UnsupportedOperationException("Not (yet) supported");
    }

    @Override
    @Suspendable
    public final String clusterReplicate(String nodeId) {
        throw new UnsupportedOperationException("Not (yet) supported");
    }

    @Override
    @Suspendable
    public final List<String> clusterSlaves(String nodeId) {
        throw new UnsupportedOperationException("Not (yet) supported");
    }

    @Override
    @Suspendable
    public final String clusterFailover() {
        throw new UnsupportedOperationException("Not (yet) supported");
    }

    @Override
    @Suspendable
    public final List<Object> clusterSlots() {
        throw new UnsupportedOperationException("Not (yet) supported");
    }

    /////////////////////////// WILL BE REMOVED BY JEDIS
    @Override
    @Deprecated
    @Suspendable
    @SuppressWarnings("deprecation")
    public final ScanResult<String> sscan(String key, int cursor) {
        throw new UnsupportedOperationException("Unsupported, will be removed by Jedis, see https://github.com/xetorthio/jedis/issues/531");
    }

    @Override
    @Deprecated
    @Suspendable
    @SuppressWarnings("deprecation")
    public final ScanResult<String> sscan(String key, int cursor, ScanParams params) {
        throw new UnsupportedOperationException("Unsupported, will be removed by Jedis, see https://github.com/xetorthio/jedis/issues/531");
    }

    @Override
    @Deprecated
    @Suspendable
    @SuppressWarnings("deprecation")
    public final ScanResult<Tuple> zscan(String key, int cursor) {
        throw new UnsupportedOperationException("Unsupported, will be removed by Jedis, see https://github.com/xetorthio/jedis/issues/531");
    }

    @Override
    @Deprecated
    @Suspendable
    @SuppressWarnings("deprecation")
    public final ScanResult<Tuple> zscan(String key, int cursor, ScanParams params) {
        throw new UnsupportedOperationException("Unsupported, will be removed by Jedis, see https://github.com/xetorthio/jedis/issues/531");
    }

    @Override
    @Deprecated
    @Suspendable
    @SuppressWarnings("deprecation")
    public final ScanResult<String> scan(int cursor) {
        throw new UnsupportedOperationException("Unsupported, will be removed by Jedis, see https://github.com/xetorthio/jedis/issues/531");
    }

    @Override
    @Deprecated
    @Suspendable
    @SuppressWarnings("deprecation")
    public final ScanResult<String> scan(int cursor, ScanParams params) {
        throw new UnsupportedOperationException("Unsupported, will be removed by Jedis, see https://github.com/xetorthio/jedis/issues/531");
    }

    @Override
    @Deprecated
    @Suspendable
    @SuppressWarnings("deprecation")
    public final ScanResult<Map.Entry<String, String>> hscan(String key, int cursor) {
        throw new UnsupportedOperationException("Unsupported, will be removed by Jedis, see https://github.com/xetorthio/jedis/issues/531");
    }

    @Override
    @Deprecated
    @Suspendable
    @SuppressWarnings("deprecation")
    public final ScanResult<Map.Entry<String, String>> hscan(String key, int cursor, ScanParams params) {
        throw new UnsupportedOperationException("Unsupported, will be removed by Jedis, see https://github.com/xetorthio/jedis/issues/531");
    }

    /////////////////////////// UNSUPPORTED BY REDIS http://redis.io/commands
    @Override
    @Suspendable
    public final Double zincrby(String key, double score, String member, ZIncrByParams params) {
        throw new UnsupportedOperationException("Not (yet) supported by Redis");
    }

    @Override
    @Suspendable
    public final Set<String> spop(String key, long count) {
        throw new UnsupportedOperationException("Not (yet) supported by Redis");
    }

    /////////////////////////// UNSUPPORTED BY LETTUCE
    @Override
    @Suspendable
    public final Long lpushx(String key, String... args) {
        throw new UnsupportedOperationException("Not (yet) supported");
    }

    @Override
    @Suspendable
    public final Long rpushx(String key, String... string) {
        throw new UnsupportedOperationException("Not (yet) supported");
    }

    @Override
    @Suspendable
    public final List<String> geohash(String key, String... members) {
        throw new UnsupportedOperationException("Not (yet) supported");
    }

    /////////////////////////// MEANINGLESS
    @Override
    public void setDataSource(Pool<Jedis> jedisPool) {
        throw new UnsupportedOperationException();
    }
}
