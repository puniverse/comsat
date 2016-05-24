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
import co.paralleluniverse.strands.concurrent.ReentrantLock;
import com.google.common.base.Charsets;
import com.lambdaworks.redis.*;
import com.lambdaworks.redis.api.StatefulRedisConnection;
import com.lambdaworks.redis.api.async.RedisAsyncCommands;
import com.lambdaworks.redis.codec.ByteArrayCodec;
import com.lambdaworks.redis.pubsub.RedisPubSubListener;
import com.lambdaworks.redis.pubsub.StatefulRedisPubSubConnection;
import com.lambdaworks.redis.pubsub.api.async.RedisPubSubAsyncCommands;
import redis.clients.jedis.*;
import redis.clients.util.Pool;
import redis.clients.jedis.params.geo.GeoRadiusParam;
import redis.clients.jedis.params.sortedset.ZAddParams;
import redis.clients.jedis.params.sortedset.ZIncrByParams;
import redis.clients.util.Slowlog;

import java.net.URI;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * @author circlespainter
 */
public final class Jedis extends redis.clients.jedis.Jedis {
    private final Callable<RedisClient> redisClientProvider;

    private RedisClient redisClient;

    private StatefulRedisConnection<String, String> stringCommandsConn;
    private RedisAsyncCommands<String, String> stringCommands;
    private StatefulRedisConnection<byte[], byte[]> binaryCommandsConn;
    private RedisAsyncCommands<byte[], byte[]> binaryCommands;

    private StatefulRedisPubSubConnection<String, String> stringPubSubConn;
    RedisPubSubAsyncCommands<String, String> stringPubSub;
    private StatefulRedisPubSubConnection<byte[], byte[]> binaryPubSubConn;
    RedisPubSubAsyncCommands<byte[], byte[]> binaryPubSub;

    public Jedis(Callable<RedisClient> cp) {
        redisClientProvider = cp;
    }

    public Jedis() {
        this(RedisClient::create);
    }

    public Jedis(String host) {
        this(() -> RedisClient.create(RedisURI.create("redis://" + host)));
    }

    public Jedis(String host, int port) {
        this(() -> RedisClient.create(RedisURI.create(host, port)));
    }

    public Jedis(String host, int port, int timeout) {
        redisClientProvider = () -> RedisClient.create(RedisURI.Builder.redis(host, port).withTimeout(timeout, TimeUnit.MILLISECONDS).build());
    }

    public Jedis(URI uri) {
        this(() -> RedisClient.create(RedisURI.Builder.redis(uri.getHost(), uri.getPort()).build()));
    }

    public Jedis(URI uri, int timeout) {
        this(() -> RedisClient.create(RedisURI.Builder.redis(uri.getHost(), uri.getPort()).withTimeout(timeout, TimeUnit.MILLISECONDS).build()));
    }

    @Override
    public final void setDataSource(Pool<redis.clients.jedis.Jedis> jedisPool) {
        // Nothing to do
    }


    @Override
    public final void connect() {
        try {
            redisClient = redisClientProvider.call();
        } catch (Exception e) {
            throw new AssertionError(e);
        }
        stringCommandsConn = redisClient.connect();
        stringCommands = stringCommandsConn.async();
        binaryCommandsConn = redisClient.connect(new ByteArrayCodec());
        binaryCommands = binaryCommandsConn.async();
        stringPubSubConn = redisClient.connectPubSub();
        stringPubSub = stringPubSubConn.async();
        binaryPubSubConn = redisClient.connectPubSub(new ByteArrayCodec());
        binaryPubSub = binaryPubSubConn.async();
    }

    @Override
    public final String auth(String password) {
        if (!isConnected())
            connect();
        final String a1 = stringCommands.auth(password);
        final String a2 = binaryCommands.auth(password);
        final String a3 = stringPubSub.auth(password);
        final String a4 = binaryPubSub.auth(password);
        if (a1.equals(a2) && a2.equals(a3) && a3.equals(a4) && a4.equals(a1))
            return a1;
        throw new IllegalStateException("Authentications returned different result codes");
    }

    @Override
    public final void close() {
        disconnect();
    }

    @Override
    public final void disconnect() {
        if (!isConnected())
            return;
        binaryPubSub.close();
        binaryPubSub = null;
        stringPubSub.close();
        stringPubSub = null;
        binaryPubSubConn.close();
        binaryPubSubConn = null;
        stringPubSubConn.close();
        stringPubSubConn = null;
        binaryCommands.close();
        binaryCommands = null;
        stringCommands.close();
        stringCommands = null;
        binaryCommandsConn.close();
        binaryCommandsConn = null;
        stringCommandsConn.close();
        stringCommandsConn = null;
        redisClient.shutdown();
        redisClient = null;
    }

    @Override
    @Suspendable
    public final String set(String key, String value) {
        return await(stringCommands.set(key, value));
    }

    @Override
    @Suspendable
    public final String set(String key, String value, String nxxx, String expx, long time) {
        return await(stringCommands.set(key, value, toSetArgs(nxxx, expx, time)));
    }

    @Override
    @Suspendable
    public final String get(String key) {
        return await(stringCommands.get(key));
    }

    @Override
    @Suspendable
    public final Long exists(String... keys) {
        return await(stringCommands.exists(keys));
    }

    @Override
    @Deprecated
    @Suspendable
    public final Boolean exists(String key) {
        //noinspection deprecation
        return await(stringCommands.exists(key));
    }

    @Override
    @Suspendable
    public final Long del(String... keys) {
        return await(stringCommands.del(keys));
    }

    @Override
    @Suspendable
    public final Long del(String key) {
        return await(stringCommands.del(key));
    }

    @Override
    @Suspendable
    public final String type(String key) {
        return await(stringCommands.type(key));
    }

    @Override
    @Suspendable
    public final Set<String> keys(String pattern) {
        return new HashSet<>(await(stringCommands.keys(pattern)));
    }

    @Override
    @Suspendable
    public final String randomKey() {
        return await(stringCommands.randomkey());
    }

    @Override
    @Suspendable
    public final String rename(String oldKey, String newKey) {
        return await(stringCommands.rename(oldKey, newKey));
    }

    @Override
    @Suspendable
    public final Long renamenx(String oldKey, String newKey) {
        return await(stringCommands.renamenx(oldKey, newKey)) ? 1L : 0L;
    }

    @Override
    @Suspendable
    public final Long expire(String key, int seconds) {
        return await(stringCommands.expire(key, seconds)) ? 1L : 0L;
    }

    @Override
    @Suspendable
    public final Long expireAt(String key, long unixTime) {
        return await(stringCommands.expireat(key, unixTime)) ? 1L : 0L;
    }

    @Override
    @Suspendable
    public final Long ttl(String key) {
        return await(stringCommands.ttl(key));
    }

    @Override
    @Suspendable
    public final Long move(String key, int dbIndex) {
        return await(stringCommands.move(key, dbIndex)) ? 1L : 0L;
    }

    @Override
    @Suspendable
    public final String getSet(String key, String value) {
        return await(stringCommands.getset(key, value));
    }

    @Override
    @Suspendable
    public final List<String> mget(String... keys) {
        return await(stringCommands.mget(keys));
    }

    @Override
    @Suspendable
    public final Long setnx(String key, String value) {
        return await(stringCommands.setnx(key, value)) ? 1L : 0L;
    }

    @Override
    @Suspendable
    public final String setex(String key, int seconds, String value) {
        return await(stringCommands.setex(key, seconds, value));
    }

    @Override
    @Suspendable
    public final String mset(String... keysValues) {
        return await(stringCommands.mset(kvArrayToMap(keysValues)));
    }

    @Override
    @Suspendable
    public final Long msetnx(String... keysValues) {
        return await(stringCommands.msetnx(kvArrayToMap(keysValues))) ? 1L : 0L;
    }

    @Override
    @Suspendable
    public final Long decrBy(String key, long integer) {
        return await(stringCommands.decrby(key, integer));
    }

    @Override
    @Suspendable
    public final Long decr(String key) {
        return await(stringCommands.decr(key));
    }

    @Override
    @Suspendable
    public final Long incrBy(String key, long integer) {
        return await(stringCommands.incrby(key, integer));
    }

    @Override
    @Suspendable
    public final Double incrByFloat(String key, double value) {
        return await(stringCommands.incrbyfloat(key, value));
    }

    @Override
    @Suspendable
    public final Long incr(String key) {
        return await(stringCommands.incr(key));
    }

    @Override
    @Suspendable
    public final Long append(String key, String value) {
        return await(stringCommands.append(key, value));
    }

    @Override
    @Suspendable
    public final String substr(String key, int start, int end) {
        return await(stringCommands.getrange(key, start, end));
    }

    @Override
    @Suspendable
    public final Long hset(String key, String field, String value) {
        return await(stringCommands.hset(key, field, value)) ? 1L : 0L;
    }

    @Override
    @Suspendable
    public final String hget(String key, String field) {
        return await(stringCommands.hget(key, field));
    }

    @Override
    @Suspendable
    public final Long hsetnx(String key, String field, String value) {
        return await(stringCommands.hsetnx(key, field, value)) ? 1L : 0L;
    }

    @Override
    @Suspendable
    public final String hmset(String key, Map<String, String> hash) {
        return await(stringCommands.hmset(key, hash));
    }

    @Override
    @Suspendable
    public final List<String> hmget(String key, String... fields) {
        return await(stringCommands.hmget(key, fields));
    }

    @Override
    @Suspendable
    public final Long hincrBy(String key, String field, long value) {
        return await(stringCommands.hincrby(key, field, value));
    }

    @Override
    @Suspendable
    public final Double hincrByFloat(String key, String field, double value) {
        return await(stringCommands.hincrbyfloat(key, field, value));
    }

    @Override
    @Suspendable
    public final Boolean hexists(String key, String field) {
        return await(stringCommands.hexists(key, field));
    }

    @Override
    @Suspendable
    public final Long hdel(String key, String... fields) {
        return await(stringCommands.hdel(key, fields));
    }

    @Override
    @Suspendable
    public final Long hlen(String key) {
        return await(stringCommands.hlen(key));
    }

    @Override
    @Suspendable
    public final Set<String> hkeys(String key) {
        return new HashSet<>(await(stringCommands.hkeys(key)));
    }

    @Override
    @Suspendable
    public final List<String> hvals(String key) {
        return await(stringCommands.hvals(key));
    }

    @Override
    @Suspendable
    public final Map<String, String> hgetAll(String key) {
        return await(stringCommands.hgetall(key));
    }

    @Override
    @Suspendable
    public final Long rpush(String key, String... strings) {
        return await(stringCommands.rpush(key, strings));
    }

    @Override
    @Suspendable
    public final Long lpush(String key, String... strings) {
        return await(stringCommands.lpush(key, strings));
    }

    @Override
    @Suspendable
    public final Long llen(String key) {
        return await(stringCommands.llen(key));
    }

    @Override
    @Suspendable
    public final List<String> lrange(String key, long start, long end) {
        return await(stringCommands.lrange(key, start, end));
    }

    @Override
    @Suspendable
    public final String ltrim(String key, long start, long end) {
        return await(stringCommands.ltrim(key, start, end));
    }

    @Override
    @Suspendable
    public final String lindex(String key, long index) {
        return await(stringCommands.lindex(key, index));
    }

    @Override
    @Suspendable
    public final String lset(String key, long index, String value) {
        return await(stringCommands.lset(key, index, value));
    }

    @Override
    @Suspendable
    public final Long lrem(String key, long count, String value) {
        return await(stringCommands.lrem(key, count, value));
    }

    @Override
    @Suspendable
    public final String lpop(String key) {
        return await(stringCommands.lpop(key));
    }

    @Override
    @Suspendable
    public final String rpop(String key) {
        return await(stringCommands.rpop(key));
    }

    @Override
    @Suspendable
    public final String rpoplpush(String srcKey, String dstKey) {
        return await(stringCommands.rpoplpush(srcKey, dstKey));
    }

    @Override
    @Suspendable
    public final Long sadd(String key, String... members) {
        return await(stringCommands.sadd(key, members));
    }

    @Override
    @Suspendable
    public final Set<String> smembers(String key) {
        return await(stringCommands.smembers(key));
    }

    @Override
    @Suspendable
    public final Long srem(String key, String... members) {
        return await(stringCommands.srem(key, members));
    }

    @Override
    @Suspendable
    public final String spop(String key) {
        return await(stringCommands.spop(key));
    }

    @Override
    @Suspendable
    public final Long smove(String srcKey, String dstKey, String member) {
        return await(stringCommands.smove(srcKey, dstKey, member)) ? 1L : 0L;
    }

    @Override
    @Suspendable
    public final Long scard(String key) {
        return await(stringCommands.scard(key));
    }

    @Override
    @Suspendable
    public final Boolean sismember(String key, String member) {
        return await(stringCommands.sismember(key, member));
    }

    @Override
    @Suspendable
    public final Set<String> sinter(String... keys) {
        return await(stringCommands.sinter(keys));
    }

    @Override
    @Suspendable
    public final Long sinterstore(String dstKey, String... keys) {
        return await(stringCommands.sinterstore(dstKey, keys));
    }

    @Override
    @Suspendable
    public final Set<String> sunion(String... keys) {
        return await(stringCommands.sunion(keys));
    }

    @Override
    @Suspendable
    public final Long sunionstore(String dstKey, String... keys) {
        return await(stringCommands.sunionstore(dstKey, keys));
    }

    @Override
    @Suspendable
    public final Set<String> sdiff(String... keys) {
        return await(stringCommands.sdiff(keys));
    }

    @Override
    @Suspendable
    public final Long sdiffstore(String dstKey, String... keys) {
        return await(stringCommands.sdiffstore(dstKey, keys));
    }

    @Override
    @Suspendable
    public final String srandmember(String key) {
        return await(stringCommands.srandmember(key));
    }

    @Override
    @Suspendable
    public final List<String> srandmember(String key, int count) {
        return new ArrayList<>(await(stringCommands.srandmember(key, count)));
    }

    @Override
    @Suspendable
    public final Long zadd(String key, double score, String member) {
        return await(stringCommands.zadd(key, score, member));
    }

    @Override
    @Suspendable
    public final Long zadd(String key, double score, String member, ZAddParams params) {
        return await(stringCommands.zadd(key, toZAddArgs(params), score, member));
    }

    @Override
    @Suspendable
    public final Long zadd(String key, Map<String, Double> scoreMembers) {
        return await(stringCommands.zadd(key, toObjectScoreValueArray(scoreMembers)));
    }

    @Override
    @Suspendable
    public final Long zadd(String key, Map<String, Double> scoreMembers, ZAddParams params) {
        return await(stringCommands.zadd(key, toZAddArgs(params), toObjectScoreValueArray(scoreMembers)));
    }

    @Override
    @Suspendable
    public final Set<String> zrange(String key, long start, long end) {
        return new HashSet<>(await(stringCommands.zrange(key, start, end)));
    }

    @Override
    @Suspendable
    public final Long zrem(String key, String... members) {
        return await(stringCommands.zrem(key, members));
    }

    @Override
    @Suspendable
    public final Double zincrby(String key, double score, String member) {
        return await(stringCommands.zincrby(key, score, member));
    }

    @Override
    @Suspendable
    public final Long zrank(String key, String member) {
        return await(stringCommands.zrank(key, member));
    }

    @Override
    @Suspendable
    public final Long zrevrank(String key, String member) {
        return await(stringCommands.zrevrank(key, member));
    }

    @Override
    @Suspendable
    public final Set<String> zrevrange(String key, long start, long end) {
        return new HashSet<>(await(stringCommands.zrevrange(key, start, end)));
    }

    @Override
    @Suspendable
    public final Set<Tuple> zrangeWithScores(String key, long start, long end) {
        return toTupleSet((List) await(stringCommands.zrangeWithScores(key, start, end)));
    }

    @Override
    @Suspendable
    public final Set<Tuple> zrevrangeWithScores(String key, long start, long end) {
        return toTupleSet((List) await(stringCommands.zrevrangeWithScores(key, start, end)));
    }

    @Override
    @Suspendable
    public final Long zcard(String key) {
        return await(stringCommands.zcard(key));
    }

    @Override
    @Suspendable
    public final Double zscore(String key, String member) {
        return await(stringCommands.zscore(key, member));
    }

    @Override
    @Suspendable
    public final String watch(String... keys) {
        return await(stringCommands.watch(keys));
    }

    @Override
    @Suspendable
    public final List<String> sort(String key) {
        return await(stringCommands.sort(key));
    }

    @Override
    @Suspendable
    public final List<String> sort(String key, SortingParams sortingParameters) {
        return await(stringCommands.sort(key, toSortArgs(sortingParameters)));
    }

    @Override
    @Suspendable
    public final List<String> blpop(int timeout, String... keys) {
        return toList(await(stringCommands.blpop(timeout, keys)));
    }

    @Override
    @Suspendable
    public final List<String> blpop(String... args) {
        return toList(await(stringCommands.blpop(0 /* Indefinitely */, args)));
    }

    @Override
    @Suspendable
    public final List<String> brpop(String... args) {
        return toList(await(stringCommands.brpop(0 /* Indefinitely */, args)));
    }

    @Override
    @Suspendable
    public final Long sort(String key, SortingParams sortingParameters, String dstKey) {
        return await(stringCommands.sortStore(key, toSortArgs(sortingParameters), dstKey));
    }

    @Override
    @Suspendable
    public final Long sort(String key, String dstKey) {
        return await(stringCommands.sortStore(key, new SortArgs(), dstKey));
    }

    @Override
    @Suspendable
    public final List<String> brpop(int timeout, String... keys) {
        return toList(await(stringCommands.brpop(timeout, keys)));
    }

    @Override
    @Suspendable
    public final Long zcount(String key, double min, double max) {
        return await(stringCommands.zcount(key, min, max));
    }

    @Override
    @Suspendable
    public final Long zcount(String key, String min, String max) {
        return await(stringCommands.zcount(key, min, max));
    }

    @Override
    @Suspendable
    public final Set<String> zrangeByScore(String key, double min, double max) {
        return new HashSet<>(await(stringCommands.zrangebyscore(key, min, max)));
    }

    @Override
    @Suspendable
    public final Set<String> zrangeByScore(String key, String min, String max) {
        return new HashSet<>(await(stringCommands.zrangebyscore(key, min, max)));
    }

    @Override
    @Suspendable
    public final Set<String> zrangeByScore(String key, double min, double max, int offset, int count) {
        return new HashSet<>(await(stringCommands.zrangebyscore(key, min, max, offset, count)));
    }

    @Override
    @Suspendable
    public final Set<String> zrangeByScore(String key, String min, String max, int offset, int count) {
        return new HashSet<>(await(stringCommands.zrangebyscore(key, min, max, offset, count)));
    }

    @Override
    @Suspendable
    public final Set<Tuple> zrangeByScoreWithScores(String key, double min, double max) {
        return toTupleSet((List) await(stringCommands.zrangebyscoreWithScores(key, min, max)));
    }

    @Override
    @Suspendable
    public final Set<Tuple> zrangeByScoreWithScores(String key, String min, String max) {
        return toTupleSet((List) await(stringCommands.zrangebyscoreWithScores(key, min, max)));
    }

    @Override
    @Suspendable
    public final Set<Tuple> zrangeByScoreWithScores(String key, double min, double max, int offset, int count) {
        return toTupleSet((List) await(stringCommands.zrangebyscoreWithScores(key, min, max, offset, count)));
    }

    @Override
    @Suspendable
    public final Set<Tuple> zrangeByScoreWithScores(String key, String min, String max, int offset, int count) {
        return toTupleSet((List) await(stringCommands.zrangebyscoreWithScores(key, min, max, offset, count)));
    }

    @Override
    @Suspendable
    public final Set<String> zrevrangeByScore(String key, double max, double min) {
        return new HashSet<>(await(stringCommands.zrevrangebyscore(key, min, max)));
    }

    @Override
    @Suspendable
    public final Set<String> zrevrangeByScore(String key, String max, String min) {
        return new HashSet<>(await(stringCommands.zrevrangebyscore(key, min, max)));
    }

    @Override
    @Suspendable
    public final Set<String> zrevrangeByScore(String key, double max, double min, int offset, int count) {
        return new HashSet<>(await(stringCommands.zrevrangebyscore(key, min, max)));
    }

    @Override
    @Suspendable
    public final Set<Tuple> zrevrangeByScoreWithScores(String key, double max, double min) {
        return toTupleSet((List) await(stringCommands.zrevrangebyscoreWithScores(key, min, max)));
    }

    @Override
    @Suspendable
    public final Set<Tuple> zrevrangeByScoreWithScores(String key, double max, double min, int offset, int count) {
        return toTupleSet((List) await(stringCommands.zrevrangebyscoreWithScores(key, min, max, offset, count)));
    }

    @Override
    @Suspendable
    public final Set<Tuple> zrevrangeByScoreWithScores(String key, String max, String min, int offset, int count) {
        return toTupleSet((List) await(stringCommands.zrevrangebyscoreWithScores(key, min, max, offset, count)));
    }

    @Override
    @Suspendable
    public final Set<String> zrevrangeByScore(String key, String max, String min, int offset, int count) {
        return new HashSet<>(await(stringCommands.zrevrangebyscore(key, min, max, offset, count)));
    }

    @Override
    @Suspendable
    public final Set<Tuple> zrevrangeByScoreWithScores(String key, String max, String min) {
        return toTupleSet((List) await(stringCommands.zrevrangebyscoreWithScores(key, min, max)));
    }

    @Override
    @Suspendable
    public final Long zremrangeByRank(String key, long start, long end) {
        return await(stringCommands.zremrangebyrank(key, start, end));
    }

    @Override
    @Suspendable
    public final Long zremrangeByScore(String key, double start, double end) {
        return await(stringCommands.zremrangebyscore(key, start, end));
    }

    @Override
    @Suspendable
    public final Long zremrangeByScore(String key, String start, String end) {
        return await(stringCommands.zremrangebyscore(key, start, end));
    }

    @Override
    @Suspendable
    public final Long zunionstore(String dstKey, String... sets) {
        return await(stringCommands.zunionstore(dstKey, sets));
    }

    @Override
    @Suspendable
    public final Long zunionstore(String dstKey, ZParams params, String... sets) {
        return await(stringCommands.zunionstore(dstKey, toZStoreArgs(params), sets));
    }

    @Override
    @Suspendable
    public final Long zinterstore(String dstKey, String... sets) {
        return await(stringCommands.zinterstore(dstKey, sets));
    }

    @Override
    @Suspendable
    public final Long zinterstore(String dstKey, ZParams params, String... sets) {
        return await(stringCommands.zinterstore(dstKey, toZStoreArgs(params), sets));
    }

    @Override
    @Suspendable
    public final Long zlexcount(String key, String min, String max) {
        return await(stringCommands.zlexcount(key, min, max));
    }

    @Override
    @Suspendable
    public final Set<String> zrangeByLex(String key, String min, String max) {
        return new HashSet<>(await(stringCommands.zrangebylex(key, min, max)));
    }

    @Override
    @Suspendable
    public final Set<String> zrangeByLex(String key, String min, String max, int offset, int count) {
        return new HashSet<>(await(stringCommands.zrangebylex(key, min, max, offset, count)));
    }

    @Override
    @Suspendable
    public final Set<String> zrevrangeByLex(String key, String max, String min) {
        return new HashSet<>(await(stringCommands.zrangebylex(key, min, max)));
    }

    @Override
    @Suspendable
    public final Set<String> zrevrangeByLex(String key, String max, String min, int offset, int count) {
        return new HashSet<>(await(stringCommands.zrangebylex(key, min, max, offset, count)));
    }

    @Override
    @Suspendable
    public final Long zremrangeByLex(String key, String min, String max) {
        return await(stringCommands.zremrangebylex(key, min, max));
    }

    @Override
    @Suspendable
    public final Long strlen(String key) {
        return await(stringCommands.strlen(key));
    }

    @Override
    @Suspendable
    public final Long persist(String key) {
        return await(stringCommands.persist(key)) ? 1L : 0L;
    }

    @Override
    @Suspendable
    public final String echo(String string) {
        return await(stringCommands.echo(string));
    }

    @Override
    @Suspendable
    public final Long linsert(String key, BinaryClient.LIST_POSITION where, String pivot, String value) {
        return await(stringCommands.linsert(key, BinaryClient.LIST_POSITION.BEFORE.equals(where), pivot, value));
    }

    @Override
    @Suspendable
    public final String brpoplpush(String source, String destination, int timeout) {
        return await(stringCommands.brpoplpush(timeout, source, destination));
    }

    @Override
    @Suspendable
    public final Boolean setbit(String key, long offset, boolean value) {
        return await(stringCommands.setbit(key, offset, value ? 1 : 0)) > 0;
    }

    @Override
    @Suspendable
    public final Boolean setbit(String key, long offset, String value) {
        return await(stringCommands.setbit(key, offset, Boolean.parseBoolean(value) ? 1 : 0)) > 0;
    }

    @Override
    @Suspendable
    public final Boolean getbit(String key, long offset) {
        return await(stringCommands.getbit(key, offset)) > 0;
    }

    @Override
    @Suspendable
    public final Long setrange(String key, long offset, String value) {
        return await(stringCommands.setrange(key, offset, value));
    }

    @Override
    @Suspendable
    public final String getrange(String key, long startOffset, long endOffset) {
        return await(stringCommands.getrange(key, startOffset, endOffset));
    }

    @Override
    @Suspendable
    public final Long bitpos(String key, boolean value) {
        return await(stringCommands.bitpos(key, value));
    }

    @Override
    @Suspendable
    public final Long bitpos(String key, boolean value, BitPosParams params) {
        return await(stringCommands.bitpos(key, value, getStart(params), getEnd(params)));
    }

    @Override
    @Suspendable
    public final List<String> configGet(String pattern) {
        return await(stringCommands.configGet(pattern));
    }

    @Override
    @Suspendable
    public final String configSet(String parameter, String value) {
        return await(stringCommands.configSet(parameter, value));
    }

    @Override
    @Suspendable
    public final List<Slowlog> slowlogGet() {
        return Slowlog.from(await(stringCommands.slowlogGet()));
    }

    @Override
    @Suspendable
    public final List<Slowlog> slowlogGet(long entries) {
        return Slowlog.from(await(stringCommands.slowlogGet(validateInt(entries))));
    }

    @Override
    @Suspendable
    public final Long objectRefcount(String string) {
        return await(stringCommands.objectRefcount(string));
    }

    @Override
    @Suspendable
    public final String objectEncoding(String string) {
        return await(stringCommands.objectEncoding(string));
    }

    @Override
    @Suspendable
    public final Long objectIdletime(String string) {
        return await(stringCommands.objectIdletime(string));
    }

    @Override
    @Suspendable
    public final Long bitcount(String key) {
        return await(stringCommands.bitcount(key));
    }

    @Override
    @Suspendable
    public final Long bitcount(String key, long start, long end) {
        return await(stringCommands.bitcount(key, start, end));
    }

    @Override
    @Suspendable
    public final Long bitop(BitOP op, String destKey, String... srcKeys) {
        final Long res;
        switch (op) {
            case AND:
                res = await(stringCommands.bitopAnd(destKey, srcKeys));
                break;
            case OR:
                res = await(stringCommands.bitopOr(destKey, srcKeys));
                break;
            case XOR:
                res = await(stringCommands.bitopXor(destKey, srcKeys));
                break;
            case NOT:
                if (srcKeys == null || srcKeys.length != 1)
                    throw new IllegalArgumentException("'not' requires exactly one argument");
                res = await(stringCommands.bitopNot(destKey, srcKeys[0]));
                break;
            default:
                throw new IllegalArgumentException("Unsupported operation: " + op);
        }
        return res;
    }

    @Override
    @Suspendable
    public final byte[] dump(String key) {
        return await(stringCommands.dump(key));
    }

    @Override
    @Suspendable
    public final String restore(String key, int ttl, byte[] serializedValue) {
        return await(stringCommands.restore(key, ttl, serializedValue));
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
        return await(stringCommands.pexpire(key, milliseconds)) ? 1L : 0;
    }

    @Override
    @Suspendable
    public final Long pexpireAt(String key, long millisecondsTimestamp) {
        return await(stringCommands.pexpireat(key, millisecondsTimestamp)) ? 1L : 0;
    }

    @Override
    @Suspendable
    public final Long pttl(String key) {
        return await(stringCommands.pttl(key));
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
        return await(stringCommands.psetex(key, milliseconds, value));
    }

    @Override
    @Suspendable
    public final String set(String key, String value, String nxxx) {
        return await(stringCommands.set(key, value, toSetArgs(nxxx)));
    }

    @Override
    @Suspendable
    public final String set(String key, String value, String nxxx, String expx, int time) {
        return await(stringCommands.set(key, value, toSetArgs(nxxx, expx, time)));
    }

    @Override
    @Suspendable
    public final String clientKill(String client) {
        return await(stringCommands.clientKill(client));
    }

    @Override
    @Suspendable
    public final String clientSetname(String name) {
        return await(stringCommands.clientSetname(name));
    }

    @Override
    @Suspendable
    public final String migrate(String host, int port, String key, int destinationDb, int timeout) {
        return await(stringCommands.migrate(host, port, key, destinationDb, timeout));
    }

    @Override
    @Suspendable
    public final Long pfadd(String key, String... elements) {
        return await(stringCommands.pfadd(key, elements));
    }

    @Override
    @Suspendable
    public final long pfcount(String key) {
        return await(stringCommands.pfcount(new String[] { key }));
    }

    @Override
    @Suspendable
    public final long pfcount(String... keys) {
        return await(stringCommands.pfcount(keys));
    }

    @Override
    @Suspendable
    public final String pfmerge(String destKey, String... sourceKeys) {
        return await(stringCommands.pfmerge(destKey, sourceKeys));
    }

    @Override
    @Suspendable
    public final List<String> blpop(int timeout, String key) {
        return toList(await(stringCommands.blpop(timeout, key)));
    }

    @Override
    @Suspendable
    public final List<String> brpop(int timeout, String key) {
        return toList(await(stringCommands.brpop(timeout, key)));
    }

    @Override
    @Suspendable
    public final Long geoadd(String key, double longitude, double latitude, String member) {
        return await(stringCommands.geoadd(key, longitude, latitude, member));
    }

    @Override
    @Suspendable
    public final Long geoadd(String key, Map<String, GeoCoordinate> memberCoordinateMap) {
        return await(stringCommands.geoadd(key, toTripletArray(memberCoordinateMap)));
    }

    @Override
    @Suspendable
    public final Double geodist(String key, String member1, String member2) {
        return await(stringCommands.geodist(key, member1, member2, null));
    }

    @Override
    @Suspendable
    public final Double geodist(String key, String member1, String member2, GeoUnit unit) {
        return await(stringCommands.geodist(key, member1, member2, toGeoArgsUnit(unit)));
    }

    @Override
    @Suspendable
    public final List<GeoCoordinate> geopos(String key, String... members) {
        return await(stringCommands.geopos(key, members)).stream().map(Jedis::toGeoCoordinate).collect(Collectors.toList());
    }

    @Override
    @Suspendable
    public final List<GeoRadiusResponse> georadius(String key, double longitude, double latitude, double radius, GeoUnit unit) {
        return await(stringCommands.georadius(key, longitude, latitude, radius, toUnit(unit))).stream().map(Jedis::toGeoRadius).collect(Collectors.toList());
    }

    @Override
    @Suspendable
    public final List<GeoRadiusResponse> georadius(String key, double longitude, double latitude, double radius, GeoUnit unit, GeoRadiusParam param) {
        return await(stringCommands.georadius(key, longitude, latitude, radius, toUnit(unit), toGeoArgs(param))).stream().map(Jedis::toGeoRadius).collect(Collectors.toList());
    }

    @Override
    @Suspendable
    public final List<GeoRadiusResponse> georadiusByMember(String key, String member, double radius, GeoUnit unit) {
        return await(stringCommands.georadiusbymember(key, member, radius, toUnit(unit))).stream().map(Jedis::toGeoRadius).collect(Collectors.toList());
    }

    @Override
    @Suspendable
    public final List<GeoRadiusResponse> georadiusByMember(String key, String member, double radius, GeoUnit unit, GeoRadiusParam param) {
        return await(stringCommands.georadiusbymember(key, member, radius, toUnit(unit), toGeoArgs(param))).stream().map(Jedis::toGeoRadius).collect(Collectors.toList());
    }


    @Override
    @Suspendable
    public final String readonly() {
        return await(stringCommands.readOnly());
    }

    @Override
    @Suspendable
    public final String asking() {
        return await(stringCommands.asking());
    }

    @Override
    @Suspendable
    public final ScanResult<String> scan(String cursor) {
        return toScanResult(await(stringCommands.scan(ScanCursor.of(cursor))));
    }

    @Override
    @Suspendable
    public final ScanResult<String> scan(String cursor, ScanParams params) {
        return toScanResult(await(stringCommands.scan(ScanCursor.of(cursor), toScanArgs(params))));
    }

    @Override
    @Suspendable
    public final ScanResult<Map.Entry<String, String>> hscan(String key, String cursor) {
        return toScanResult(await(stringCommands.hscan(key, ScanCursor.of(cursor))));
    }

    @Override
    @Suspendable
    public final ScanResult<Map.Entry<String, String>> hscan(String key, String cursor, ScanParams params) {
        return toScanResult(await(stringCommands.hscan(key, ScanCursor.of(cursor), toScanArgs(params))));
    }

    @Override
    @Suspendable
    public final ScanResult<String> sscan(String key, String cursor) {
        return toScanResult(await(stringCommands.sscan(key, ScanCursor.of(cursor))));
    }

    @Override
    @Suspendable
    public final ScanResult<String> sscan(String key, String cursor, ScanParams params) {
        return toScanResult(await(stringCommands.sscan(key, ScanCursor.of(cursor), toScanArgs(params))));
    }

    @Override
    @Suspendable
    public final ScanResult<Tuple> zscan(String key, String cursor) {
        return toScanResult(await(stringCommands.zscan(key, ScanCursor.of(cursor))));
    }

    @Override
    @Suspendable
    public final ScanResult<Tuple> zscan(String key, String cursor, ScanParams params) {
        return toScanResult(await(stringCommands.zscan(key, ScanCursor.of(cursor), toScanArgs(params))));
    }

    @Override
    @Suspendable
    public final String flushAll() {
        return await(stringCommands.flushall());
    }

    @Override
    @Suspendable
    public final String flushDB() {
        return await(stringCommands.flushdb());
    }

    @Override
    @Suspendable
    public final String ping() {
        return await(stringCommands.ping());
    }

    /**
     * Return the number of keys in the currently selected database.
     *
     * @return Integer reply
     */
    @Override
    @Suspendable
    public final Long dbSize() {
        return await(stringCommands.dbsize());
    }

    @Override
    public final void resetState() {
        // Nothing to do
    }

    @Override
    @Suspendable
    public final String save() {
        return await(stringCommands.save());
    }


    @Override
    @Suspendable
    public final String bgsave() {
        return await(stringCommands.bgsave());
    }

    @Override
    @Suspendable
    public final String bgrewriteaof() {
        return await(stringCommands.bgrewriteaof());
    }

    @Override
    @Suspendable
    public final Long lastsave() {
        return await(stringCommands.lastsave()).getTime();
    }

    @Override
    public final String shutdown() {
        stringCommands.shutdown(true);
        return null;
    }

    @Override
    @Suspendable
    public final String info() {
        return await(stringCommands.info());
    }

    @Override
    @Suspendable
    public final String info(String section) {
        return await(stringCommands.info(section));
    }

    @Override
    public final boolean isConnected() {
        return
            stringCommands != null && stringCommands.isOpen() &&
            binaryCommands != null && binaryCommands.isOpen() &&
            stringPubSub != null && stringPubSub.isOpen() &&
            binaryPubSub != null && binaryPubSub.isOpen();
    }

    @Override
    @Suspendable
    public final void sync() {
        await(stringCommands.sync());
    }

    @Override
    @Suspendable
    public final String slowlogReset() {
        return await(stringCommands.slowlogReset());
    }

    @Override
    @Suspendable
    public final Long slowlogLen() {
        return await(stringCommands.slowlogLen());
    }

    @Override
    @Suspendable
    public final String unwatch() {
        return await(stringCommands.unwatch());
    }

    @Override
    @Suspendable
    public final String set(byte[] key, byte[] value) {
        return await(binaryCommands.set(key, value));
    }

    @Override
    @Suspendable
    public final String set(byte[] key, byte[] value, byte[] nxxx, byte[] expx, long time) {
        return await(binaryCommands.set(key, value, toSetArgs(nxxx, expx, time)));
    }

    @Override
    @Suspendable
    public final byte[] get(byte[] key) {
        return await(binaryCommands.get(key));
    }

    @Override
    @Suspendable
    public final String quit() {
        return await(binaryCommands.quit());
    }

    @Override
    @Suspendable
    public final Long exists(byte[]... keys) {
        return await(binaryCommands.exists(keys));
    }

    @Override
    @Suspendable
    public final Boolean exists(byte[] key) {
        //noinspection deprecation
        return await(binaryCommands.exists(key));
    }

    @Override
    @Suspendable
    public final Long del(byte[]... keys) {
        return await(binaryCommands.del(keys));
    }

    @Override
    @Suspendable
    public final Long del(byte[] key) {
        return await(binaryCommands.del(key));
    }

    @Override
    @Suspendable
    public final String type(byte[] key) {
        return await(binaryCommands.type(key));
    }


    @Override
    @Suspendable
    public final Set<byte[]> keys(byte[] pattern) {
        return new HashSet<>(await(binaryCommands.keys(pattern)));
    }

    @Override
    @Suspendable
    public final byte[] randomBinaryKey() {
        return await(binaryCommands.randomkey());
    }

    @Override
    @Suspendable
    public final String rename(byte[] oldKey, byte[] newKey) {
        return await(binaryCommands.rename(oldKey, newKey));
    }

    @Override
    @Suspendable
    public final Long renamenx(byte[] oldKey, byte[] newKey) {
        return await(binaryCommands.renamenx(oldKey, newKey)) ? 1L : 0L;
    }

    @Override
    @Suspendable
    public final Long expire(byte[] key, int seconds) {
        return await(binaryCommands.expire(key, seconds)) ? 1L : 0L;
    }

    @Override
    @Suspendable
    public final Long expireAt(byte[] key, long unixTime) {
        return await(binaryCommands.expireat(key, unixTime)) ? 1L : 0L;
    }

    @Override
    @Suspendable
    public final Long ttl(byte[] key) {
        return await(binaryCommands.ttl(key));
    }

    @Override
    public final String select(int index) {
        return binaryCommands.select(index);
    }


    @Override
    @Suspendable
    public final Long move(byte[] key, int dbIndex) {
        return await(binaryCommands.move(key, dbIndex)) ? 1L : 0L;
    }

    @Override
    @Suspendable
    public final byte[] getSet(byte[] key, byte[] value) {
        return await(binaryCommands.getset(key, value));
    }

    @Override
    @Suspendable
    public final List<byte[]> mget(byte[]... keys) {
        return await(binaryCommands.mget(keys));
    }

    @Override
    @Suspendable
    public final Long setnx(byte[] key, byte[] value) {
        return await(binaryCommands.setnx(key, value)) ? 1L : 0L;
    }

    @Override
    @Suspendable
    public final String setex(byte[] key, int seconds, byte[] value) {
        return await(binaryCommands.setex(key, seconds, value));
    }

    @Override
    @Suspendable
    public final String mset(byte[]... keysValues) {
        return await(binaryCommands.mset(kvArrayToMap(keysValues)));
    }

    @Override
    @Suspendable
    public final Long msetnx(byte[]... keysValues) {
        return await(binaryCommands.msetnx(kvArrayToMap(keysValues))) ? 1L : 0L;
    }

    @Override
    @Suspendable
    public final Long decrBy(byte[] key, long amount) {
        return await(binaryCommands.decrby(key, amount));
    }

    @Override
    @Suspendable
    public final Long decr(byte[] key) {
        return await(binaryCommands.decr(key));
    }

    @Override
    @Suspendable
    public final Long incrBy(byte[] key, long amount) {
        return await(binaryCommands.incrby(key, amount));
    }

    @Override
    @Suspendable
    public final Double incrByFloat(byte[] key, double amount) {
        return await(binaryCommands.incrbyfloat(key, amount));
    }

    @Override
    @Suspendable
    public final Long incr(byte[] key) {
        return await(binaryCommands.incr(key));
    }

    @Override
    @Suspendable
    public final Long append(byte[] key, byte[] value) {
        return await(binaryCommands.append(key, value));
    }

    @Override
    @Suspendable
    public final byte[] substr(byte[] key, int start, int end) {
        return await(binaryCommands.getrange(key, start, end));
    }

    @Override
    @Suspendable
    public final Long hset(byte[] key, byte[] field, byte[] value) {
        return await(binaryCommands.hset(key, field, value)) ? 1L : 0L;
    }

    @Override
    @Suspendable
    public final byte[] hget(byte[] key, byte[] field) {
        return await(binaryCommands.hget(key, field));
    }

    @Override
    @Suspendable
    public final Long hsetnx(byte[] key, byte[] field, byte[] value) {
        return await(binaryCommands.hsetnx(key, field, value)) ? 1L : 0L;
    }

    @Override
    @Suspendable
    public final String hmset(byte[] key, Map<byte[], byte[]> hash) {
        return await(binaryCommands.hmset(key, hash));
    }

    @Override
    @Suspendable
    public final List<byte[]> hmget(byte[] key, byte[]... fields) {
        return await(binaryCommands.hmget(key, fields));
    }

    @Override
    @Suspendable
    public final Long hincrBy(byte[] key, byte[] field, long value) {
        return await(binaryCommands.hincrby(key, field, value));
    }

    @Override
    @Suspendable
    public final Double hincrByFloat(byte[] key, byte[] field, double value) {
        return await(binaryCommands.hincrbyfloat(key, field, value));
    }

    @Override
    @Suspendable
    public final Boolean hexists(byte[] key, byte[] field) {
        return await(binaryCommands.hexists(key, field));
    }

    @Override
    @Suspendable
    public final Long hdel(byte[] key, byte[]... fields) {
        return await(binaryCommands.hdel(key, fields));
    }

    @Override
    @Suspendable
    public final Long hlen(byte[] key) {
        return await(binaryCommands.hlen(key));
    }

    @Override
    @Suspendable
    public final Set<byte[]> hkeys(byte[] key) {
        return new HashSet<>(await(binaryCommands.hkeys(key)));
    }

    @Override
    @Suspendable
    public final List<byte[]> hvals(byte[] key) {
        return await(binaryCommands.hvals(key));
    }
    @Override
    @Suspendable
    public final Map<byte[], byte[]> hgetAll(byte[] key) {
        return await(binaryCommands.hgetall(key));
    }

    @Override
    @Suspendable
    public final Long rpush(byte[] key, byte[]... strings) {
        return await(binaryCommands.rpush(key, strings));
    }

    @Override
    @Suspendable
    public final Long lpush(byte[] key, byte[]... strings) {
        return await(binaryCommands.lpush(key, strings));
    }

    @Override
    @Suspendable
    public final Long llen(byte[] key) {
        return await(binaryCommands.llen(key));
    }

    @Override
    @Suspendable
    public final List<byte[]> lrange(byte[] key, long start, long end) {
        return await(binaryCommands.lrange(key, start, end));
    }

    @Override
    @Suspendable
    public final String ltrim(byte[] key, long start, long end) {
        return await(binaryCommands.ltrim(key, start, end));
    }

    @Override
    @Suspendable
    public final byte[] lindex(byte[] key, long index) {
        return await(binaryCommands.lindex(key, index));
    }

    @Override
    @Suspendable
    public final String lset(byte[] key, long index, byte[] value) {
        return await(binaryCommands.lset(key, index, value));
    }

    @Override
    @Suspendable
    public final Long lrem(byte[] key, long count, byte[] value) {
        return await(binaryCommands.lrem(key, count, value));
    }

    @Override
    @Suspendable
    public final byte[] lpop(byte[] key) {
        return await(binaryCommands.lpop(key));
    }

    @Override
    @Suspendable
    public final byte[] rpop(byte[] key) {
        return await(binaryCommands.rpop(key));
    }

    @Override
    @Suspendable
    public final byte[] rpoplpush(byte[] srcKey, byte[] dstKey) {
        return await(binaryCommands.rpoplpush(srcKey, dstKey));
    }

    @Override
    @Suspendable
    public final Long sadd(byte[] key, byte[]... members) {
        return await(binaryCommands.sadd(key, members));
    }

    @Override
    @Suspendable
    public final Set<byte[]> smembers(byte[] key) {
        return await(binaryCommands.smembers(key));
    }
    @Override
    @Suspendable
    public final Long srem(byte[] key, byte[]... members) {
        return await(binaryCommands.srem(key, members));
    }

    @Override
    @Suspendable
    public final byte[] spop(byte[] key) {
        return await(binaryCommands.spop(key));
    }

    @Override
    @Suspendable
    public final Long smove(byte[] srcKey, byte[] dstKey, byte[] member) {
        return await(binaryCommands.smove(srcKey, dstKey, member)) ? 1L : 0L;
    }

    @Override
    @Suspendable
    public final Long scard(byte[] key) {
        return await(binaryCommands.scard(key));
    }

    @Override
    @Suspendable
    public final Boolean sismember(byte[] key, byte[] member) {
        return await(binaryCommands.sismember(key, member));
    }

    @Override
    @Suspendable
    public final Set<byte[]> sinter(byte[]... keys) {
        return await(binaryCommands.sinter(keys));
    }

    @Override
    @Suspendable
    public final Long sinterstore(byte[] dstKey, byte[]... keys) {
        return await(binaryCommands.sinterstore(dstKey, keys));
    }

    @Override
    @Suspendable
    public final Set<byte[]> sunion(byte[]... keys) {
        return await(binaryCommands.sunion(keys));
    }

    @Override
    @Suspendable
    public final Long sunionstore(byte[] dstKey, byte[]... keys) {
        return await(binaryCommands.sunionstore(dstKey, keys));
    }

    @Override
    @Suspendable
    public final Set<byte[]> sdiff(byte[]... keys) {
        return await(binaryCommands.sdiff(keys));
    }

    @Override
    @Suspendable
    public final Long sdiffstore(byte[] dstKey, byte[]... keys) {
        return await(binaryCommands.sdiffstore(dstKey, keys));
    }

    @Override
    @Suspendable
    public final byte[] srandmember(byte[] key) {
        return await(binaryCommands.srandmember(key));
    }

    @Override
    @Suspendable
    public final List<byte[]> srandmember(byte[] key, int count) {
        return new ArrayList<>(await(binaryCommands.srandmember(key, count)));
    }

    @Override
    @Suspendable
    public final Long zadd(byte[] key, double score, byte[] member) {
        return await(binaryCommands.zadd(key, score, member));
    }

    @Override
    @Suspendable
    public final Long zadd(byte[] key, double score, byte[] member, ZAddParams params) {
        return await(binaryCommands.zadd(key, toZAddArgs(params), score, member));
    }

    @Override
    @Suspendable
    public final Long zadd(byte[] key, Map<byte[], Double> scoreMembers) {
        return await(binaryCommands.zadd(key, scoreMembers));
    }

    @Override
    @Suspendable
    public final Long zadd(byte[] key, Map<byte[], Double> scoreMembers, ZAddParams params) {
        return await(binaryCommands.zadd(key, toZAddArgs(params), scoreMembers));
    }

    @Override
    @Suspendable
    public final Set<byte[]> zrange(byte[] key, long start, long end) {
        return new HashSet<>(await(binaryCommands.zrange(key, start, end)));
    }

    @Override
    @Suspendable
    public final Long zrem(byte[] key, byte[]... members) {
        return await(binaryCommands.zrem(key, members));
    }

    @Override
    @Suspendable
    public final Double zincrby(byte[] key, double score, byte[] member) {
        return await(binaryCommands.zincrby(key, score, member));
    }

    @Override
    @Suspendable
    public final Long zrank(byte[] key, byte[] member) {
        return await(binaryCommands.zrank(key, member));
    }

    @Override
    @Suspendable
    public final Long zrevrank(byte[] key, byte[] member) {
        return await(binaryCommands.zrevrank(key, member));
    }

    @Override
    @Suspendable
    public final Set<byte[]> zrevrange(byte[] key, long start, long end) {
        return new HashSet<>(await(binaryCommands.zrevrange(key, start, end)));
    }

    @Override
    @Suspendable
    public final Set<Tuple> zrangeWithScores(byte[] key, long start, long end) {
        return toTupleSet((List) await(binaryCommands.zrangeWithScores(key, start, end)));
    }

    @Override
    @Suspendable
    public final Set<Tuple> zrevrangeWithScores(byte[] key, long start, long end) {
        return toTupleSet((List) await(binaryCommands.zrevrangeWithScores(key, start, end)));
    }

    @Override
    @Suspendable
    public final Long zcard(byte[] key) {
        return await(binaryCommands.zcard(key));
    }

    @Override
    @Suspendable
    public final Double zscore(byte[] key, byte[] member) {
        return await(binaryCommands.zscore(key, member));
    }

    @Override
    @Suspendable
    public final String watch(byte[]... keys) {
        return await(binaryCommands.watch(keys));
    }

    @Override
    @Suspendable
    public final List<byte[]> sort(byte[] key) {
        return await(binaryCommands.sort(key));
    }

    @Override
    @Suspendable
    public final List<byte[]> sort(byte[] key, SortingParams sp) {
        return await(binaryCommands.sort(key, toSortArgs(sp)));
    }

    @Override
    @Suspendable
    public final List<byte[]> blpop(int timeout, byte[]... keys) {
        return toList(await(binaryCommands.blpop(timeout, keys)));
    }

    @Override
    @Suspendable
    public final Long sort(byte[] key, SortingParams sp, byte[] dstKey) {
        return await(binaryCommands.sortStore(key, toSortArgs(sp), dstKey));
    }

    @Override
    @Suspendable
    public final Long sort(byte[] key, byte[] dstKey) {
        return await(binaryCommands.sortStore(key, new SortArgs(), dstKey));
    }

    @Override
    @Suspendable
    public final List<byte[]> brpop(int timeout, byte[]... keys) {
        return toList(await(binaryCommands.brpop(timeout, keys)));
    }

    @Override
    public List<byte[]> blpop(byte[]... args) {
        return toList(await(binaryCommands.blpop(0 /* Indefinitely */, args)));
    }

    @Override
    @Suspendable
    public final List<byte[]> brpop(byte[]... args) {
        return toList(await(binaryCommands.brpop(0 /* Indefinitely */, args)));
    }

    @Override
    @Suspendable
    public final Long zcount(byte[] key, double min, double max) {
        return await(binaryCommands.zcount(key, min, max));
    }

    @Override
    @Suspendable
    public final Long zcount(byte[] key, byte[] min, byte[] max) {
        return await(binaryCommands.zcount(key, new String(min, Charsets.UTF_8), new String(max, Charsets.UTF_8)));
    }

    @Override
    @Suspendable
    public final Set<byte[]> zrangeByScore(byte[] key, double min, double max) {
        return new HashSet<>(await(binaryCommands.zrangebyscore(key, min, max)));
    }

    @Override
    @Suspendable
    public final Set<byte[]> zrangeByScore(byte[] key, byte[] min, byte[] max) {
        return new HashSet<>(await(binaryCommands.zrangebyscore(key, new String(min, Charsets.UTF_8), new String(max, Charsets.UTF_8))));
    }

    @Override
    @Suspendable
    public final Set<byte[]> zrangeByScore(byte[] key, double min, double max, int offset, int count) {
        return new HashSet<>(await(binaryCommands.zrangebyscore(key, min, max, offset, count)));
    }

    @Override
    @Suspendable
    public final Set<byte[]> zrangeByScore(byte[] key, byte[] min, byte[] max, int offset, int count) {
        return new HashSet<>(await(binaryCommands.zrangebyscore(key, new String(min, Charsets.UTF_8), new String(max, Charsets.UTF_8), offset, count)));
    }

    @Override
    @Suspendable
    public final Set<Tuple> zrangeByScoreWithScores(byte[] key, double min, double max) {
        return toTupleSet((List) await(binaryCommands.zrangebyscoreWithScores(key, min, max)));
    }

    @Override
    @Suspendable
    public final Set<Tuple> zrangeByScoreWithScores(byte[] key, byte[] min, byte[] max) {
        return toTupleSet((List) await(binaryCommands.zrangebyscoreWithScores(key, new String(min, Charsets.UTF_8), new String(max, Charsets.UTF_8))));
    }

    @Override
    @Suspendable
    public final Set<Tuple> zrangeByScoreWithScores(byte[] key, double min, double max, int offset, int count) {
        return toTupleSet((List) await(binaryCommands.zrangebyscoreWithScores(key, min, max, offset, count)));
    }

    @Override
    @Suspendable
    public final Set<Tuple> zrangeByScoreWithScores(byte[] key, byte[] min, byte[] max, int offset, int count) {
        return toTupleSet((List) await(binaryCommands.zrangebyscoreWithScores(key, new String(min, Charsets.UTF_8), new String(max, Charsets.UTF_8), offset, count)));
    }

    @Override
    @Suspendable
    public final Set<byte[]> zrevrangeByScore(byte[] key, double max, double min) {
        return new HashSet<>(await(binaryCommands.zrevrangebyscore(key, min, max)));
    }

    @Override
    @Suspendable
    public final Set<byte[]> zrevrangeByScore(byte[] key, byte[] max, byte[] min) {
        return new HashSet<>(await(binaryCommands.zrevrangebyscore(key, new String(min, Charsets.UTF_8), new String(max, Charsets.UTF_8))));
    }

    @Override
    @Suspendable
    public final Set<byte[]> zrevrangeByScore(byte[] key, double max, double min, int offset, int count) {
        return new HashSet<>(await(binaryCommands.zrevrangebyscore(key, min, max, offset, count)));
    }

    @Override
    @Suspendable
    public final Set<byte[]> zrevrangeByScore(byte[] key, byte[] max, byte[] min, int offset, int count) {
        return new HashSet<>(await(binaryCommands.zrevrangebyscore(key, new String(min, Charsets.UTF_8), new String(max, Charsets.UTF_8), offset, count)));
    }

    @Override
    @Suspendable
    public final Set<Tuple> zrevrangeByScoreWithScores(byte[] key, double max, double min) {
        return toTupleSet((List) await(binaryCommands.zrevrangebyscoreWithScores(key, min, max)));
    }

    @Override
    @Suspendable
    public final Set<Tuple> zrevrangeByScoreWithScores(byte[] key, double max, double min, int offset, int count) {
        return toTupleSet((List) await(binaryCommands.zrevrangebyscoreWithScores(key, min, max, offset, count)));
    }

    @Override
    @Suspendable
    public final Set<Tuple> zrevrangeByScoreWithScores(byte[] key, byte[] max, byte[] min) {
        return toTupleSet((List) await(binaryCommands.zrevrangebyscoreWithScores(key, new String(min, Charsets.UTF_8), new String(max, Charsets.UTF_8))));
    }

    @Override
    @Suspendable
    public final Set<Tuple> zrevrangeByScoreWithScores(byte[] key, byte[] max, byte[] min, int offset, int count) {
        return toTupleSet((List) await(binaryCommands.zrevrangebyscoreWithScores(key, new String(min, Charsets.UTF_8), new String(max, Charsets.UTF_8), offset, count)));
    }

    @Override
    @Suspendable
    public final Long zremrangeByRank(byte[] key, long start, long end) {
        return await(binaryCommands.zremrangebyrank(key, start, end));
    }

    @Override
    @Suspendable
    public final Long zremrangeByScore(byte[] key, double start, double end) {
        return await(binaryCommands.zremrangebyscore(key, start, end));
    }

    @Override
    @Suspendable
    public final Long zremrangeByScore(byte[] key, byte[] start, byte[] end) {
        return await(binaryCommands.zremrangebyscore(key, new String(start, Charsets.UTF_8), new String(end, Charsets.UTF_8)));
    }

    @Override
    @Suspendable
    public final Long zunionstore(byte[] dstKey, byte[]... sets) {
        return await(binaryCommands.zunionstore(dstKey, sets));
    }

    @Override
    @Suspendable
    public final Long zunionstore(byte[] dstKey, ZParams params, byte[]... sets) {
        return await(binaryCommands.zunionstore(dstKey, toZStoreArgs(params), sets));
    }

    @Override
    @Suspendable
    public final Long zinterstore(byte[] dstKey, byte[]... sets) {
        return await(binaryCommands.zinterstore(dstKey, sets));
    }

    @Override
    @Suspendable
    public final Long zinterstore(byte[] dstKey, ZParams params, byte[]... sets) {
        return await(binaryCommands.zinterstore(dstKey, toZStoreArgs(params), sets));
    }

    @Override
    @Suspendable
    public final Long zlexcount(byte[] key, byte[] min, byte[] max) {
        return await(binaryCommands.zlexcount(key, new String(min, Charsets.UTF_8), new String(max, Charsets.UTF_8)));
    }

    @Override
    @Suspendable
    public final Set<byte[]> zrangeByLex(byte[] key, byte[] min, byte[] max) {
        return new HashSet<>(await(binaryCommands.zrangebylex(key, new String(min, Charsets.UTF_8), new String(max, Charsets.UTF_8))));
    }

    @Override
    @Suspendable
    public final Set<byte[]> zrangeByLex(byte[] key, byte[] min, byte[] max, int offset, int count) {
        return new HashSet<>(await(binaryCommands.zrangebylex(key, new String(min, Charsets.UTF_8), new String(max, Charsets.UTF_8), offset, count)));
    }

    @Override
    @Suspendable
    public final Long zremrangeByLex(byte[] key, byte[] min, byte[] max) {
        return await(binaryCommands.zremrangebylex(key, new String(min, Charsets.UTF_8), new String(max, Charsets.UTF_8)));
    }

    @Override
    @Suspendable
    public final String slaveof(String host, int port) {
        return await(binaryCommands.slaveof(host, port));
    }

    @Override
    @Suspendable
    public final String slaveofNoOne() {
        return await(binaryCommands.slaveofNoOne());
    }

    @Override
    @Suspendable
    public final List<byte[]> configGet(byte[] pattern) {
        return await(binaryCommands.configGet(new String(pattern, Charsets.UTF_8))).stream().map(s -> s.getBytes(Charsets.UTF_8)).collect(Collectors.toList());
    }

    @Override
    @Suspendable
    public final String configResetStat() {
        return await(binaryCommands.configResetstat());
    }

    @Override
    @Suspendable
    public final byte[] configSet(byte[] parameter, byte[] value) {
        return await(binaryCommands.configSet(new String(parameter, Charsets.UTF_8), new String(value, Charsets.UTF_8))).getBytes(Charsets.UTF_8);
    }

    @Override
    @Suspendable
    public final Long strlen(byte[] key) {
        return await(binaryCommands.strlen(key));
    }

    @Override
    @Suspendable
    public final Long persist(byte[] key) {
        return await(binaryCommands.persist(key)) ? 1L : 0L;
    }

    @Override
    @Suspendable
    public byte[] echo(byte[] string) {
        return await(binaryCommands.echo(string));
    }

    @Override
    @Suspendable
    public final Long linsert(byte[] key, BinaryClient.LIST_POSITION where, byte[] pivot, byte[] value) {
        return await(binaryCommands.linsert(key, BinaryClient.LIST_POSITION.BEFORE.equals(where), pivot, value));
    }

    @Override
    @Suspendable
    public final String debug(DebugParams params) {
        if ("SEGFAULT".equalsIgnoreCase(params.getCommand()[0]))
            binaryCommands.debugSegfault();
        else if ("RELOAD".equalsIgnoreCase(params.getCommand()[0]))
            return await(binaryCommands.debugReload());
        else if ("OBJECT".equalsIgnoreCase(params.getCommand()[0]))
            return await(binaryCommands.debugObject(params.getCommand()[1].getBytes(Charsets.UTF_8)));
        return null;
    }


    @Override
    @Suspendable
    public final byte[] brpoplpush(byte[] source, byte[] destination, int timeout) {
        return await(binaryCommands.brpoplpush(timeout, source, destination));
    }

    @Override
    @Suspendable
    public final Boolean setbit(byte[] key, long offset, boolean value) {
        return await(binaryCommands.setbit(key, offset, value ? 1 : 0)) > 0;
    }

    @Override
    @Suspendable
    public final Boolean setbit(byte[] key, long offset, byte[] value) {
        return await(binaryCommands.setbit(key, offset, Integer.parseInt(new String(value, Charsets.UTF_8)))) > 0;
    }

    @Override
    @Suspendable
    public final Boolean getbit(byte[] key, long offset) {
        return await(binaryCommands.getbit(key, offset)) > 0;
    }

    @Override
    @Suspendable
    public final Long bitpos(byte[] key, boolean value) {
        return await(binaryCommands.bitpos(key, value));
    }

    @Override
    @Suspendable
    public final Long bitpos(byte[] key, boolean value, BitPosParams params) {
        return await(binaryCommands.bitpos(key, value, getStart(params), getEnd(params)));
    }

    @Override
    @Suspendable
    public final Long setrange(byte[] key, long offset, byte[] value) {
        return await(binaryCommands.setrange(key, offset, value));
    }

    @Override
    @Suspendable
    public final byte[] getrange(byte[] key, long startOffset, long endOffset) {
        return await(binaryCommands.getrange(key, startOffset, endOffset));
    }

    @Override
    @Suspendable
    public final List<byte[]> slowlogGetBinary() {
        return toByteArrayList(await(binaryCommands.slowlogGet()));
    }

    @Override
    @Suspendable
    public final List<byte[]> slowlogGetBinary(long entries) {
        return toByteArrayList(await(binaryCommands.slowlogGet(validateInt(entries))));
    }

    @Override
    @Suspendable
    public final Long objectRefcount(byte[] key) {
        return await(binaryCommands.objectRefcount(key));
    }

    @Override
    @Suspendable
    public final byte[] objectEncoding(byte[] key) {
        return await(binaryCommands.objectEncoding(key)).getBytes(Charsets.UTF_8);
    }

    @Override
    @Suspendable
    public final Long objectIdletime(byte[] key) {
        return await(binaryCommands.objectIdletime(key));
    }

    @Override
    @Suspendable
    public final Long bitcount(byte[] key) {
        return await(binaryCommands.bitcount(key));
    }

    @Override
    @Suspendable
    public final Long bitcount(byte[] key, long start, long end) {
        return await(binaryCommands.bitcount(key, start, end));
    }

    @Override
    @Suspendable
    public final Long bitop(BitOP op, byte[] destKey, byte[]... srcKeys) {
        final Long res;
        switch (op) {
            case AND:
                res = await(binaryCommands.bitopAnd(destKey, srcKeys));
                break;
            case OR:
                res = await(binaryCommands.bitopOr(destKey, srcKeys));
                break;
            case XOR:
                res = await(binaryCommands.bitopXor(destKey, srcKeys));
                break;
            case NOT:
                if (srcKeys == null || srcKeys.length != 1)
                    throw new IllegalArgumentException("'not' requires exactly one argument");
                res = await(binaryCommands.bitopNot(destKey, srcKeys[0]));
                break;
            default:
                throw new IllegalArgumentException("Unsupported operation: " + op);
        }
        return res;
    }

    @Override
    @Suspendable
    public final byte[] dump(byte[] key) {
        return await(binaryCommands.dump(key));
    }

    @Override
    @Suspendable
    public final String restore(byte[] key, int ttl, byte[] serializedValue) {
        return await(binaryCommands.restore(key, ttl, serializedValue));
    }

    @Override
    @Deprecated
    @Suspendable
    @SuppressWarnings("deprecation")
    public final Long pexpire(byte[] key, int milliseconds) {
        return pexpire(key, (long) milliseconds);
    }

    @Override
    @Suspendable
    public final Long pexpire(byte[] key, long milliseconds) {
        return await(binaryCommands.pexpire(key, milliseconds)) ? 1L : 0L;
    }

    @Override
    @Suspendable
    public final Long pexpireAt(byte[] key, long millisecondsTimestamp) {
        return await(binaryCommands.pexpireat(key, millisecondsTimestamp)) ? 1L : 0L;
    }

    @Override
    @Suspendable
    public final Long pttl(byte[] key) {
        return await(binaryCommands.pttl(key));
    }

    @Override
    @Deprecated
    @Suspendable
    @SuppressWarnings("deprecation")
    public final String psetex(byte[] key, int milliseconds, byte[] value) {
        return psetex(key, milliseconds, value);
    }

    @Override
    @Suspendable
    public final String psetex(byte[] key, long milliseconds, byte[] value) {
        return await(binaryCommands.psetex(key, milliseconds, value));
    }

    @Override
    @Suspendable
    public final String set(byte[] key, byte[] value, byte[] nxxx) {
        return await(binaryCommands.set(key, value, toSetArgs(new String(nxxx, Charsets.UTF_8))));
    }

    @Override
    @Suspendable
    public final String set(byte[] key, byte[] value, byte[] nxxx, byte[] expx, int time) {
        return await(binaryCommands.set(key, value, toSetArgs(new String(nxxx, Charsets.UTF_8), new String(expx, Charsets.UTF_8), time)));
    }

    @Override
    @Suspendable
    public final String clientKill(byte[] client) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    @Override
    @Suspendable
    public final String clientGetname() {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    @Override
    @Suspendable
    public final String clientList() {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    @Override
    @Suspendable
    public final String clientSetname(byte[] name) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    @Override
    @Suspendable
    public final List<String> time() {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    @Override
    @Suspendable
    public final String migrate(byte[] host, int port, byte[] key, int destinationDb, int timeout) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    @Override
    @Suspendable
    public final Long pfadd(byte[] key, byte[]... elements) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    @Override
    @Suspendable
    public final long pfcount(byte[] key) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    @Override
    @Suspendable
    public final String pfmerge(byte[] destkey, byte[]... sourcekeys) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    @Override
    @Suspendable
    public final Long pfcount(byte[]... keys) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    @Override
    @Suspendable
    public final ScanResult<byte[]> scan(byte[] cursor) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    @Override
    @Suspendable
    public final ScanResult<byte[]> scan(byte[] cursor, ScanParams params) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    @Override
    @Suspendable
    public final ScanResult<Map.Entry<byte[], byte[]>> hscan(byte[] key, byte[] cursor) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    @Override
    @Suspendable
    public final ScanResult<Map.Entry<byte[], byte[]>> hscan(byte[] key, byte[] cursor, ScanParams params) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    @Override
    @Suspendable
    public final ScanResult<byte[]> sscan(byte[] key, byte[] cursor) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    @Override
    public ScanResult<byte[]> sscan(byte[] key, byte[] cursor, ScanParams params) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    @Override
    @Suspendable
    public final ScanResult<Tuple> zscan(byte[] key, byte[] cursor) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    @Override
    @Suspendable
    public final ScanResult<Tuple> zscan(byte[] key, byte[] cursor, ScanParams params) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    @Override
    @Suspendable
    public final Long geoadd(byte[] key, double longitude, double latitude, byte[] member) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    @Override
    @Suspendable
    public final Long geoadd(byte[] key, Map<byte[], GeoCoordinate> memberCoordinateMap) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    @Override
    @Suspendable
    public final Double geodist(byte[] key, byte[] member1, byte[] member2) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    @Override
    @Suspendable
    public final Double geodist(byte[] key, byte[] member1, byte[] member2, GeoUnit unit) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    @Override
    @Suspendable
    public final List<byte[]> geohash(byte[] key, byte[]... members) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    @Override
    @Suspendable
    public final List<GeoCoordinate> geopos(byte[] key, byte[]... members) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    @Override
    @Suspendable
    public final List<GeoRadiusResponse> georadius(byte[] key, double longitude, double latitude, double radius, GeoUnit unit) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    @Override
    @Suspendable
    public final List<GeoRadiusResponse> georadius(byte[] key, double longitude, double latitude, double radius, GeoUnit unit, GeoRadiusParam param) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    @Override
    @Suspendable
    public final List<GeoRadiusResponse> georadiusByMember(byte[] key, byte[] member, double radius, GeoUnit unit) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    @Override
    @Suspendable
    public final List<GeoRadiusResponse> georadiusByMember(byte[] key, byte[] member, double radius, GeoUnit unit, GeoRadiusParam param) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    /////////////////////////// PUBSUB

    private static final class FiberRedisStringPubSubListener implements RedisPubSubListener<String, String> {
        private final JedisPubSub pubSub;

        FiberRedisStringPubSubListener(Jedis jedis, JedisPubSub pubSub, boolean pattern, String... args) {
            this.pubSub = pubSub;
            pubSub.jedis = jedis;
            Map<String, List<RedisPubSubListener<String, String>>> m =
                pattern ? pubSub.patternListeners : pubSub.channelListeners;
            final ReentrantLock l = new ReentrantLock();
            l.lock();
            try {
                for (final String a : args)
                    m.compute(a, (k, v) -> {
                        if (v == null)
                            return Collections.singletonList(this);
                        v.add(this);
                        return v;
                    });
            } finally {
                l.unlock();
            }
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
            int c = validateInt(count);
            pubSub.subscribedChannels.addAndGet(c);
            pubSub.onSubscribe(channel, c);
        }

        @Override
        public final void psubscribed(String pattern, long count) {
            int c = validateInt(count);
            pubSub.subscribedChannels.addAndGet(c);
            pubSub.onPSubscribe(pattern, c);
        }

        @Override
        public final void unsubscribed(String channel, long count) {
            int c = validateInt(count);
            pubSub.subscribedChannels.addAndGet(-c);
            pubSub.onUnsubscribe(channel, c);
        }

        @Override
        public final void punsubscribed(String pattern, long count) {
            int c = validateInt(count);
            pubSub.subscribedChannels.addAndGet(-c);
            pubSub.onPUnsubscribe(pattern, (int) count);
        }
    }

    private static final class FiberRedisBinaryPubSubListener implements RedisPubSubListener<byte[], byte[]> {
        private final BinaryJedisPubSub pubSub;

        FiberRedisBinaryPubSubListener(Jedis jedis, BinaryJedisPubSub pubSub, boolean pattern, byte[]... args) {
            this.pubSub = pubSub;
            pubSub.jedis = jedis;
            final Map<byte[], List<RedisPubSubListener<byte[], byte[]>>> m =
                pattern ? pubSub.patternListeners : pubSub.channelListeners;
            final ReentrantLock l = new ReentrantLock();
            l.lock();
            try {
                for (final byte[] a : args)
                    m.compute(a, (k, v) -> {
                        if (v == null)
                            return Collections.singletonList(this);
                        v.add(this);
                        return v;
                    });
            } finally {
                l.unlock();
            }
        }

        @Override
        public final void message(byte[] channel, byte[] message) {
            pubSub.onMessage(channel, message);
        }

        @Override
        public final void message(byte[] pattern, byte[] channel, byte[] message) {
            pubSub.onPMessage(pattern, channel, message);
        }

        @Override
        public final void subscribed(byte[] channel, long count) {
            int c = validateInt(count);
            pubSub.subscribedChannels.addAndGet(c);
            pubSub.onSubscribe(channel, c);
        }

        @Override
        public final void psubscribed(byte[] pattern, long count) {
            int c = validateInt(count);
            pubSub.subscribedChannels.addAndGet(c);
            pubSub.onPSubscribe(pattern, c);
        }

        @Override
        public final void unsubscribed(byte[] channel, long count) {
            int c = validateInt(count);
            pubSub.subscribedChannels.addAndGet(-c);
            pubSub.onUnsubscribe(channel, c);
        }

        @Override
        public final void punsubscribed(byte[] pattern, long count) {
            int c = validateInt(count);
            pubSub.subscribedChannels.addAndGet(-c);
            pubSub.onPUnsubscribe(pattern, (int) count);
        }
    }

    @Override
    @Suspendable
    public final void subscribe(redis.clients.jedis.JedisPubSub jedisPubSub, String... channels) {
        validateFiberPubSub(jedisPubSub);
        stringPubSub.addListener(new FiberRedisStringPubSubListener(this, (JedisPubSub) jedisPubSub, false, channels));
    }

    @Override
    @Suspendable
    public final Long publish(String channel, String message) {
        return await(stringCommands.publish(channel, message));
    }

    @Override
    @Suspendable
    public final void psubscribe(redis.clients.jedis.JedisPubSub jedisPubSub, String... patterns) {
        validateFiberPubSub(jedisPubSub);
        stringPubSubConn.addListener(new FiberRedisStringPubSubListener(this, (JedisPubSub) jedisPubSub, true, patterns));
    }

    @Override
    @Suspendable
    public final List<String> pubsubChannels(String pattern) {
        return await(stringCommands.pubsubChannels(pattern));
    }

    @Override
    @Suspendable
    public final Long pubsubNumPat() {
        return await(stringCommands.pubsubNumpat());
    }

    @Override
    @Suspendable
    public final Map<String, String> pubsubNumSub(String... channels) {
        return toStringMap(await(stringCommands.pubsubNumsub(channels)));
    }

    @Override
    @Suspendable
    public final Long publish(byte[] channel, byte[] message) {
        return await(binaryCommands.publish(channel, message));
    }

    @Override
    @Suspendable
    public final void subscribe(redis.clients.jedis.BinaryJedisPubSub jedisPubSub, byte[]... channels) {
        validateFiberPubSub(jedisPubSub);
        binaryPubSub.addListener(new FiberRedisBinaryPubSubListener(this, (BinaryJedisPubSub) jedisPubSub, false, channels));
    }

    @Override
    @Suspendable
    public final void psubscribe(redis.clients.jedis.BinaryJedisPubSub jedisPubSub, byte[]... patterns) {
        validateFiberPubSub(jedisPubSub);
        binaryPubSub.addListener(new FiberRedisBinaryPubSubListener(this, (BinaryJedisPubSub) jedisPubSub, true, patterns));
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

    /**
     * Evaluates scripts using the Lua interpreter built into Redis starting from version 2.6.0.
     * <p>
     *
     * @param script
     * @param keys
     * @param args
     * @return Script result
     */
    @Override
    @Suspendable
    public final Object eval(byte[] script, List<byte[]> keys, List<byte[]> args) {
        throw new UnsupportedOperationException("Not (yet) supported");
    }

    @Override
    @Suspendable
    public final Object eval(byte[] script, byte[] keyCount, byte[]... params) {
        throw new UnsupportedOperationException("Not (yet) supported");
    }

    @Override
    @Suspendable
    public final Object eval(byte[] script, int keyCount, byte[]... params) {
        throw new UnsupportedOperationException("Not (yet) supported");
    }

    @Override
    @Suspendable
    public final Object eval(byte[] script) {
        throw new UnsupportedOperationException("Not (yet) supported");
    }

    @Override
    @Suspendable
    public final Object evalsha(byte[] sha1) {
        throw new UnsupportedOperationException("Not (yet) supported");
    }

    @Override
    @Suspendable
    public final Object evalsha(byte[] sha1, List<byte[]> keys, List<byte[]> args) {
        throw new UnsupportedOperationException("Not (yet) supported");
    }

    @Override
    @Suspendable
    public final Object evalsha(byte[] sha1, int keyCount, byte[]... params) {
        throw new UnsupportedOperationException("Not (yet) supported");
    }

    @Override
    @Suspendable
    public final String scriptFlush() {
        throw new UnsupportedOperationException("Not (yet) supported");
    }

    @Override
    @Suspendable
    public final Long scriptExists(byte[] sha1) {
        throw new UnsupportedOperationException("Not (yet) supported");
    }

    @Override
    @Suspendable
    public final List<Long> scriptExists(byte[]... sha1) {
        throw new UnsupportedOperationException("Not (yet) supported");
    }

    @Override
    @Suspendable
    public final byte[] scriptLoad(byte[] script) {
        throw new UnsupportedOperationException("Not (yet) supported");
    }

    @Override
    @Suspendable
    public final String scriptKill() {
        throw new UnsupportedOperationException("Not (yet) supported");
    }

    /////////////////////////// SENTINEL
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

    /////////////////////////// CLUSTER / SHARDING / REPLICA
    @Override
    public final Long getDB() {
        return 0L; // TODO Support shards
    }

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

    @Override
    @Suspendable
    public final Long waitReplicas(int replicas, long timeout) {
        throw new UnsupportedOperationException("Not (yet) supported");
    }

    /////////////////////////// WILL BE REMOVED BY JEDIS
    @Override
    @Deprecated
    @SuppressWarnings("deprecation")
    public final ScanResult<String> sscan(String key, int cursor) {
        throw new UnsupportedOperationException("Unsupported, will be removed by Jedis, see https://github.com/xetorthio/jedis/issues/531");
    }

    @Override
    @Deprecated
    @SuppressWarnings("deprecation")
    public final ScanResult<String> sscan(String key, int cursor, ScanParams params) {
        throw new UnsupportedOperationException("Unsupported, will be removed by Jedis, see https://github.com/xetorthio/jedis/issues/531");
    }

    @Override
    @Deprecated
    @SuppressWarnings("deprecation")
    public final ScanResult<Tuple> zscan(String key, int cursor) {
        throw new UnsupportedOperationException("Unsupported, will be removed by Jedis, see https://github.com/xetorthio/jedis/issues/531");
    }

    @Override
    @Deprecated
    @SuppressWarnings("deprecation")
    public final ScanResult<Tuple> zscan(String key, int cursor, ScanParams params) {
        throw new UnsupportedOperationException("Unsupported, will be removed by Jedis, see https://github.com/xetorthio/jedis/issues/531");
    }

    @Override
    @Deprecated
    @SuppressWarnings("deprecation")
    public final ScanResult<String> scan(int cursor) {
        throw new UnsupportedOperationException("Unsupported, will be removed by Jedis, see https://github.com/xetorthio/jedis/issues/531");
    }

    @Override
    @Deprecated
    @SuppressWarnings("deprecation")
    public final ScanResult<String> scan(int cursor, ScanParams params) {
        throw new UnsupportedOperationException("Unsupported, will be removed by Jedis, see https://github.com/xetorthio/jedis/issues/531");
    }

    @Override
    @Deprecated
    @SuppressWarnings("deprecation")
    public final ScanResult<Map.Entry<String, String>> hscan(String key, int cursor) {
        throw new UnsupportedOperationException("Unsupported, will be removed by Jedis, see https://github.com/xetorthio/jedis/issues/531");
    }

    @Override
    @Deprecated
    @SuppressWarnings("deprecation")
    public final ScanResult<Map.Entry<String, String>> hscan(String key, int cursor, ScanParams params) {
        throw new UnsupportedOperationException("Unsupported, will be removed by Jedis, see https://github.com/xetorthio/jedis/issues/531");
    }

    @Override
    @Deprecated
    @SuppressWarnings("deprecation")
    public final List<String> blpop(String arg) {
        throw new UnsupportedOperationException("Unusable, will be removed by Jedis");
    }

    @Override
    @Deprecated
    @SuppressWarnings("deprecation")
    public final List<String> brpop(String arg) {
        throw new UnsupportedOperationException("Unusable, will be removed by Jedis");
    }

    @Override
    @Deprecated
    @SuppressWarnings("deprecation")
    public final List<byte[]> blpop(byte[] arg) {
        throw new UnsupportedOperationException("Unusable, will be removed by Jedis");
    }

    @Override
    @Deprecated
    @SuppressWarnings("deprecation")
    public final List<byte[]> brpop(byte[] arg) {
        throw new UnsupportedOperationException("Unusable, will be removed by Jedis");
    }

    /////////////////////////// UNSUPPORTED BY REDIS http://redis.io/commands
    @Override
    @Suspendable
    public final Double zincrby(String key, double score, String member, ZIncrByParams params) {
        throw new UnsupportedOperationException("Not (yet) supported by Redis");
    }

    @Override
    @Suspendable
    public final Double zincrby(byte[] key, double score, byte[] member, ZIncrByParams params) {
        throw new UnsupportedOperationException("Not (yet) supported by Redis");
    }

    @Override
    @Suspendable
    public final Set<String> spop(String key, long count) {
        throw new UnsupportedOperationException("Not (yet) supported by Redis");
    }

    @Override
    @Suspendable
    public final Set<byte[]> spop(byte[] key, long count) {
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
    public final Long lpushx(byte[] key, byte[]... string) {
        throw new UnsupportedOperationException("Not (yet) supported");
    }

    @Override
    @Suspendable
    public final Long rpushx(String key, String... string) {
        throw new UnsupportedOperationException("Not (yet) supported");
    }

    @Override
    @Suspendable
    public final Long rpushx(byte[] key, byte[]... string) {
        throw new UnsupportedOperationException("Not (yet) supported");
    }

    @Override
    @Suspendable
    public final List<String> geohash(String key, String... members) {
        throw new UnsupportedOperationException("Not (yet) supported");
    }

    @Override
    @Suspendable
    public final Set<byte[]> zrevrangeByLex(byte[] key, byte[] max, byte[] min) {
        throw new UnsupportedOperationException("Not (yet) supported");
    }

    @Override
    @Suspendable
    public final Set<byte[]> zrevrangeByLex(byte[] key, byte[] max, byte[] min, int offset, int count) {
        throw new UnsupportedOperationException("Not (yet) supported");
    }

    /////////////////////////// MONITOR
    @Override
    @Suspendable
    public final void monitor(JedisMonitor jedisMonitor) {
        throw new UnsupportedOperationException("Not (yet) supported");
    }

    /////////////////////////// TRANSACTION (AKA MULTI) AND PIPELINE
    @Override
    @Suspendable
    protected final void checkIsInMultiOrPipeline() {
        throw new UnsupportedOperationException("Not (yet) supported");
    }

    @Override
    @Suspendable
    public final Transaction multi() {
        throw new UnsupportedOperationException("Not (yet) supported");
    }

    @Override
    @Deprecated
    @Suspendable
    @SuppressWarnings("deprecation")
    public final List<Object> multi(TransactionBlock jedisTransaction) {
        throw new UnsupportedOperationException("Not (yet) supported");
    }

    @Override
    @Suspendable
    public final List<Object> pipelined(PipelineBlock jedisPipeline) {
        throw new UnsupportedOperationException("Not (yet) supported");
    }

    @Override
    @Suspendable
    public final Pipeline pipelined() {
        throw new UnsupportedOperationException("Not (yet) supported");
    }

    /////////////////////////// NOT SUPPORTED
    @Override
    @Suspendable
    public final Client getClient() {
        throw new UnsupportedOperationException("Not supported"); // TODO
    }

    /////////////////////////// UTILS
    @SuppressWarnings("WeakerAccess")
    static int validateInt(long v) {
        if (v > Integer.MAX_VALUE)
            throw new ArithmeticException("exceeds int");
        return (int) v;
    }

    static <T> void clearEmpties(ConcurrentHashMap<T, List<RedisPubSubListener<T, T>>> m) {
        if (m == null)
            return;

        final List<T> toBeRemoved = new ArrayList<>(4);
        toBeRemoved.addAll(m.entrySet().stream().filter(e -> e.getValue() == null || e.getValue().size() > 0).map(Map.Entry::getKey).collect(Collectors.toList()));
        toBeRemoved.forEach(m::remove);
    }

    static boolean contains(byte[][] channels, byte[] c) {
        if (channels != null && c != null)
            for (final byte[] ba : channels) {
                if (Arrays.equals(c, ba))
                    return true;
            }
        return false;
    }

    @Suspendable
    private static <T> T await(RedisFuture<T> f) {
        // TODO Convert exceptions
        try {
            return AsyncCompletionStage.get(f);
        } catch (final ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        } catch (final SuspendExecution e) {
            throw new AssertionError(e);
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

    private static SetArgs toSetArgs(byte[] nxxx, byte[] expx, long time) {
        return toSetArgs(new String(nxxx, Charsets.UTF_8), new String(expx, Charsets.UTF_8), time);
    }

    private static <T> Map<T, T> kvArrayToMap(T... keysValues) {
        if (keysValues == null)
            return null;

        if (keysValues.length % 2 != 0)
            throw new IllegalArgumentException("Even elements count required");

        final Map<T, T> res = new HashMap<>();
        boolean odd = true;
        T key = null, value = null;
        for (final T e : keysValues) {
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

    private static ZAddArgs toZAddArgs(ZAddParams params) {
        if (params == null)
            return null;

        if (params.contains("xx"))
            return ZAddArgs.Builder.xx();

        if (params.contains("nx"))
            return ZAddArgs.Builder.nx();

        if (params.contains("ch"))
            return ZAddArgs.Builder.ch();

        return null;
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

    private static Set<Tuple> toTupleSet(List<ScoredValue> l) {
        if (l == null)
            return null;

        return l.stream().map(e -> {
            if (e.value instanceof String)
                return new Tuple((String) e.value, e.score);
            return new Tuple((byte[]) e.value, e.score);
        }).collect(Collectors.toCollection(HashSet::new));
    }

    private static SortArgs toSortArgs(SortingParams sp) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    private static <T> List<T> toList(KeyValue<T, T> kv) {
        if (kv == null)
            return null;

        final List<T> ret = new ArrayList<>(2);
        ret.add(kv.key);
        ret.add(kv.value);
        return ret;
    }

    private static ZStoreArgs toZStoreArgs(ZParams ps) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    private static long getStart(BitPosParams params) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    private static long getEnd(BitPosParams params) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
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

    private static GeoArgs.Unit toGeoArgsUnit(GeoUnit unit) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    private static GeoCoordinate toGeoCoordinate(GeoCoordinates c) {
        if (c == null)
            return null;

        return new GeoCoordinate(c.x.doubleValue(), c.y.doubleValue());
    }

    private static GeoArgs.Unit toUnit(GeoUnit unit) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    private static GeoRadiusResponse toGeoRadius(String s) {
        if (s == null)
            return null;

        return new GeoRadiusResponse(s.getBytes());
    }

    private static GeoRadiusResponse toGeoRadius(GeoWithin<String> s) {
        if (s == null)
            return null;

        final GeoRadiusResponse r = new GeoRadiusResponse(s.member.getBytes());
        r.setDistance(s.distance);
        r.setCoordinate(toGeoCoordinate(s.coordinates));
        return r;
    }

    private static GeoArgs toGeoArgs(GeoRadiusParam param) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    private static <T> ScanResult<T> toScanResult(KeyScanCursor<T> c) {
        return new ScanResult<>(c.getCursor(), c.getKeys());
    }

    private static ScanArgs toScanArgs(ScanParams params) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    private static <T1, T2> ScanResult<Map.Entry<T1, T2>> toScanResult(MapScanCursor<T1, T2> c) {
        if (c == null)
            return null;

        return new ScanResult<>(c.getCursor(), new ArrayList<>(c.getMap().entrySet()));
    }

    private static <T> ScanResult<T> toScanResult(ValueScanCursor<T> c) {
        return new ScanResult<>(c.getCursor(), c.getValues());
    }

    private static ScanResult<Tuple> toScanResult(ScoredValueScanCursor<String> c) {
        if (c == null)
            return null;

        return new ScanResult<>(c.getCursor(), c.getValues().stream().map((sv) -> new Tuple(sv.value, sv.score)).collect(Collectors.toList()));
    }

    private static void validateFiberPubSub(redis.clients.jedis.JedisPubSub jedisPubSub) {
        if (!(jedisPubSub instanceof JedisPubSub))
            throw new IllegalArgumentException("Only subclasses of '" + JedisPubSub.class.getName() + "' can be used with '" + Jedis.class.getName() + "'");
    }

    private static void validateFiberPubSub(redis.clients.jedis.BinaryJedisPubSub jedisPubSub) {
        if (!(jedisPubSub instanceof BinaryJedisPubSub))
            throw new IllegalArgumentException("Only subclasses of '" + BinaryJedisPubSub.class.getName() + "' can be used with '" + Jedis.class.getName() + "'");
    }

    private static <K, V> Map<String, String> toStringMap(Map<K, V> m) {
        if (m == null)
            return null;

        final Map<String, String> ret = new HashMap<>();
        for (final Map.Entry<K, V> e : m.entrySet())
            ret.put(e.getKey().toString(), e.getValue().toString());
        return ret;
    }

    private static List<byte[]> toByteArrayList(List<Object> l) {
        return (List<byte[]>) ((List) l);
    }
}
