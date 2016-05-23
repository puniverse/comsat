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

import static java.util.Collections.EMPTY_MAP;

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
        return toTupleSet(await(stringCommands.zrangeWithScores(key, start, end)));
    }

    @Override
    @Suspendable
    public final Set<Tuple> zrevrangeWithScores(String key, long start, long end) {
        return toTupleSet(await(stringCommands.zrevrangeWithScores(key, start, end)));
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
        return toTupleSet(await(stringCommands.zrangebyscoreWithScores(key, min, max)));
    }

    @Override
    @Suspendable
    public final Set<Tuple> zrangeByScoreWithScores(String key, String min, String max) {
        return toTupleSet(await(stringCommands.zrangebyscoreWithScores(key, min, max)));
    }

    @Override
    @Suspendable
    public final Set<Tuple> zrangeByScoreWithScores(String key, double min, double max, int offset, int count) {
        return toTupleSet(await(stringCommands.zrangebyscoreWithScores(key, min, max, offset, count)));
    }

    @Override
    @Suspendable
    public final Set<Tuple> zrangeByScoreWithScores(String key, String min, String max, int offset, int count) {
        return toTupleSet(await(stringCommands.zrangebyscoreWithScores(key, min, max, offset, count)));
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
        return toTupleSet(await(stringCommands.zrevrangebyscoreWithScores(key, min, max)));
    }

    @Override
    @Suspendable
    public final Set<Tuple> zrevrangeByScoreWithScores(String key, double max, double min, int offset, int count) {
        return toTupleSet(await(stringCommands.zrevrangebyscoreWithScores(key, min, max, offset, count)));
    }

    @Override
    @Suspendable
    public final Set<Tuple> zrevrangeByScoreWithScores(String key, String max, String min, int offset, int count) {
        return toTupleSet(await(stringCommands.zrevrangebyscoreWithScores(key, min, max, offset, count)));
    }

    @Override
    @Suspendable
    public final Set<String> zrevrangeByScore(String key, String max, String min, int offset, int count) {
        return new HashSet<>(await(stringCommands.zrevrangebyscore(key, min, max, offset, count)));
    }

    @Override
    @Suspendable
    public final Set<Tuple> zrevrangeByScoreWithScores(String key, String max, String min) {
        return toTupleSet(await(stringCommands.zrevrangebyscoreWithScores(key, min, max)));
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
        // TODO Nothing to do
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
    public final Long getDB() {
        return 0L; // TODO SUpport shards
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

    /**
     * Set the string value as value of the key. The string can't be longer than 1073741824 bytes (1
     * GB).
     * <p>
     * Time complexity: O(1)
     *
     * @param key
     * @param value
     * @return Status code reply
     */
    @Override
    @Suspendable
    public final String set(byte[] key, byte[] value) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    /**
     * Set the string value as value of the key. The string can't be longer than 1073741824 bytes (1
     * GB).
     *
     * @param key
     * @param value
     * @param nxxx  NX|XX, NX -- Only set the key if it does not already exist. XX -- Only set the key
     *              if it already exist.
     * @param expx  EX|PX, expire time units: EX = seconds; PX = milliseconds
     * @param time  expire time in the units of <code>expx</code>
     * @return Status code reply
     */
    @Override
    @Suspendable
    public final String set(byte[] key, byte[] value, byte[] nxxx, byte[] expx, long time) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    /**
     * Get the value of the specified key. If the key does not exist the special value 'nil' is
     * returned. If the value stored at key is not a string an error is returned because GET can only
     * handle string values.
     * <p>
     * Time complexity: O(1)
     *
     * @param key
     * @return Bulk reply
     */
    @Override
    @Suspendable
    public final byte[] get(byte[] key) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    /**
     * Ask the server to silently close the connection.
     */
    @Override
    @Suspendable
    public final String quit() {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    /**
     * Test if the specified keys exist. The command returns the number of keys existed Time
     * complexity: O(N)
     *
     * @param keys
     * @return Integer reply, specifically: an integer greater than 0 if one or more keys existed 0 if
     * none of the specified keys existed
     */
    @Override
    @Suspendable
    public final Long exists(byte[]... keys) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    /**
     * Test if the specified key exists. The command returns "1" if the key exists, otherwise "0" is
     * returned. Note that even keys set with an empty string as value will return "1". Time
     * complexity: O(1)
     *
     * @param key
     * @return Boolean reply, true if the key exists, otherwise false
     */
    @Override
    @Suspendable
    public final Boolean exists(byte[] key) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    /**
     * Remove the specified keys. If a given key does not exist no operation is performed for this
     * key. The command returns the number of keys removed. Time complexity: O(1)
     *
     * @param keys
     * @return Integer reply, specifically: an integer greater than 0 if one or more keys were removed
     * 0 if none of the specified key existed
     */
    @Override
    @Suspendable
    public final Long del(byte[]... keys) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    @Override
    @Suspendable
    public final Long del(byte[] key) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    /**
     * Return the type of the value stored at key in form of a string. The type can be one of "none",
     * "string", "list", "set". "none" is returned if the key does not exist. Time complexity: O(1)
     *
     * @param key
     * @return Status code reply, specifically: "none" if the key does not exist "string" if the key
     * contains a String value "list" if the key contains a List value "set" if the key
     * contains a Set value "zset" if the key contains a Sorted Set value "hash" if the key
     * contains a Hash value
     */
    @Override
    @Suspendable
    public final String type(byte[] key) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    /**
     * Returns all the keys matching the glob-style pattern as space separated strings. For example if
     * you have in the database the keys "foo" and "foobar" the command "KEYS foo*" will return
     * "foo foobar".
     * <p>
     * Note that while the time complexity for this operation is O(n) the constant times are pretty
     * low. For example Redis running on an entry level laptop can scan a 1 million keys database in
     * 40 milliseconds. <b>Still it's better to consider this one of the slow stringCommands that may ruin
     * the DB performance if not used with care.</b>
     * <p>
     * In other words this command is intended only for debugging and special operations like creating
     * a script to change the DB schema. Don't use it in your normal code. Use Redis Sets in order to
     * group together a subset of objects.
     * <p>
     * Glob style patterns examples:
     * <ul>
     * <li>h?llo will match hello hallo hhllo
     * <li>h*llo will match hllo heeeello
     * <li>h[ae]llo will match hello and hallo, but not hillo
     * </ul>
     * <p>
     * Use \ to escape special chars if you want to match them verbatim.
     * <p>
     * Time complexity: O(n) (with n being the number of keys in the DB, and assuming keys and pattern
     * of limited length)
     *
     * @param pattern
     * @return Multi bulk reply
     */
    @Override
    @Suspendable
    public final Set<byte[]> keys(byte[] pattern) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    /**
     * Return a randomly selected key from the currently selected DB.
     * <p>
     * Time complexity: O(1)
     *
     * @return Singe line reply, specifically the randomly selected key or an empty string is the
     * database is empty
     */
    @Override
    @Suspendable
    public final byte[] randomBinaryKey() {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    /**
     * Atomically renames the key oldkey to newkey. If the source and destination name are the same an
     * error is returned. If newkey already exists it is overwritten.
     * <p>
     * Time complexity: O(1)
     *
     * @param oldkey
     * @param newkey
     * @return Status code repy
     */
    @Override
    @Suspendable
    public final String rename(byte[] oldkey, byte[] newkey) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    /**
     * Rename oldkey into newkey but fails if the destination key newkey already exists.
     * <p>
     * Time complexity: O(1)
     *
     * @param oldkey
     * @param newkey
     * @return Integer reply, specifically: 1 if the key was renamed 0 if the target key already exist
     */
    @Override
    @Suspendable
    public final Long renamenx(byte[] oldkey, byte[] newkey) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    /**
     * Set a timeout on the specified key. After the timeout the key will be automatically deleted by
     * the server. A key with an associated timeout is said to be volatile in Redis terminology.
     * <p>
     * Voltile keys are stored on disk like the other keys, the timeout is persistent too like all the
     * other aspects of the dataset. Saving a dataset containing expires and stopping the server does
     * not stop the flow of time as Redis stores on disk the time when the key will no longer be
     * available as Unix time, and not the remaining seconds.
     * <p>
     * Since Redis 2.1.3 you can update the value of the timeout of a key already having an expire
     * set. It is also possible to undo the expire at all turning the key into a normal key using the
     * {@link #persist(byte[]) PERSIST} command.
     * <p>
     * Time complexity: O(1)
     *
     * @param key
     * @param seconds
     * @return Integer reply, specifically: 1: the timeout was set. 0: the timeout was not set since
     * the key already has an associated timeout (this may happen only in Redis versions &lt;
     * 2.1.3, Redis &gt;= 2.1.3 will happily update the timeout), or the key does not exist.
     * @see <a href="http://code.google.com/p/redis/wiki/ExpireCommand">ExpireCommand</a>
     */
    @Override
    @Suspendable
    public final Long expire(byte[] key, int seconds) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    /**
     * EXPIREAT works exctly like {@link #expire(byte[], int) EXPIRE} but instead to get the number of
     * seconds representing the Time To Live of the key as a second argument (that is a relative way
     * of specifing the TTL), it takes an absolute one in the form of a UNIX timestamp (Number of
     * seconds elapsed since 1 Gen 1970).
     * <p>
     * EXPIREAT was introduced in order to implement the Append Only File persistence mode so that
     * EXPIRE stringCommands are automatically translated into EXPIREAT stringCommands for the append only file.
     * Of course EXPIREAT can also used by programmers that need a way to simply specify that a given
     * key should expire at a given time in the future.
     * <p>
     * Since Redis 2.1.3 you can update the value of the timeout of a key already having an expire
     * set. It is also possible to undo the expire at all turning the key into a normal key using the
     * {@link #persist(byte[]) PERSIST} command.
     * <p>
     * Time complexity: O(1)
     *
     * @param key
     * @param unixTime
     * @return Integer reply, specifically: 1: the timeout was set. 0: the timeout was not set since
     * the key already has an associated timeout (this may happen only in Redis versions &lt;
     * 2.1.3, Redis &gt;= 2.1.3 will happily update the timeout), or the key does not exist.
     * @see <a href="http://code.google.com/p/redis/wiki/ExpireCommand">ExpireCommand</a>
     */
    @Override
    @Suspendable
    public final Long expireAt(byte[] key, long unixTime) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    /**
     * The TTL command returns the remaining time to live in seconds of a key that has an
     * {@link #expire(byte[], int) EXPIRE} set. This introspection capability allows a Redis client to
     * check how many seconds a given key will continue to be part of the dataset.
     *
     * @param key
     * @return Integer reply, returns the remaining time to live in seconds of a key that has an
     * EXPIRE. If the Key does not exists or does not have an associated expire, -1 is
     * returned.
     */
    @Override
    @Suspendable
    public final Long ttl(byte[] key) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    /**
     * Select the DB with having the specified zero-based numeric index. For default every new client
     * connection is automatically selected to DB 0.
     *
     * @param index
     * @return Status code reply
     */
    @Override
    @Suspendable
    public final String select(int index) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    /**
     * Move the specified key from the currently selected DB to the specified destination DB. Note
     * that this command returns 1 only if the key was successfully moved, and 0 if the target key was
     * already there or if the source key was not found at all, so it is possible to use MOVE as a
     * locking primitive.
     *
     * @param key
     * @param dbIndex
     * @return Integer reply, specifically: 1 if the key was moved 0 if the key was not moved because
     * already present on the target DB or was not found in the current DB.
     */
    @Override
    @Suspendable
    public final Long move(byte[] key, int dbIndex) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    /**
     * GETSET is an atomic set this value and return the old value command. Set key to the string
     * value and return the old value stored at key. The string can't be longer than 1073741824 bytes
     * (1 GB).
     * <p>
     * Time complexity: O(1)
     *
     * @param key
     * @param value
     * @return Bulk reply
     */
    @Override
    @Suspendable
    public final byte[] getSet(byte[] key, byte[] value) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    /**
     * Get the values of all the specified keys. If one or more keys dont exist or is not of type
     * String, a 'nil' value is returned instead of the value of the specified key, but the operation
     * never fails.
     * <p>
     * Time complexity: O(1) for every key
     *
     * @param keys
     * @return Multi bulk reply
     */
    @Override
    @Suspendable
    public final List<byte[]> mget(byte[]... keys) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    /**
     * SETNX works exactly like {@link #set(byte[], byte[]) SET} with the only difference that if the
     * key already exists no operation is performed. SETNX actually means "SET if Not eXists".
     * <p>
     * Time complexity: O(1)
     *
     * @param key
     * @param value
     * @return Integer reply, specifically: 1 if the key was set 0 if the key was not set
     */
    @Override
    @Suspendable
    public final Long setnx(byte[] key, byte[] value) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    /**
     * The command is exactly equivalent to the following group of stringCommands:
     * {@link #set(byte[], byte[]) SET} + {@link #expire(byte[], int) EXPIRE}. The operation is
     * atomic.
     * <p>
     * Time complexity: O(1)
     *
     * @param key
     * @param seconds
     * @param value
     * @return Status code reply
     */
    @Override
    @Suspendable
    public final String setex(byte[] key, int seconds, byte[] value) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    /**
     * Set the the respective keys to the respective values. MSET will replace old values with new
     * values, while {@link #msetnx(byte[]...) MSETNX} will not perform any operation at all even if
     * just a single key already exists.
     * <p>
     * Because of this semantic MSETNX can be used in order to set different keys representing
     * different fields of an unique logic object in a way that ensures that either all the fields or
     * none at all are set.
     * <p>
     * Both MSET and MSETNX are atomic operations. This means that for instance if the keys A and B
     * are modified, another client talking to Redis can either see the changes to both A and B at
     * once, or no modification at all.
     *
     * @param keysvalues
     * @return Status code reply Basically +OK as MSET can't fail
     * @see #msetnx(byte[]...)
     */
    @Override
    @Suspendable
    public final String mset(byte[]... keysvalues) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    /**
     * Set the the respective keys to the respective values. {@link #mset(byte[]...) MSET} will
     * replace old values with new values, while MSETNX will not perform any operation at all even if
     * just a single key already exists.
     * <p>
     * Because of this semantic MSETNX can be used in order to set different keys representing
     * different fields of an unique logic object in a way that ensures that either all the fields or
     * none at all are set.
     * <p>
     * Both MSET and MSETNX are atomic operations. This means that for instance if the keys A and B
     * are modified, another client talking to Redis can either see the changes to both A and B at
     * once, or no modification at all.
     *
     * @param keysvalues
     * @return Integer reply, specifically: 1 if the all the keys were set 0 if no key was set (at
     * least one key already existed)
     * @see #mset(byte[]...)
     */
    @Override
    @Suspendable
    public final Long msetnx(byte[]... keysvalues) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    /**
     * DECRBY work just like {@link #decr(byte[]) INCR} but instead to decrement by 1 the decrement is
     * integer.
     * <p>
     * INCR stringCommands are limited to 64 bit signed integers.
     * <p>
     * Note: this is actually a string operation, that is, in Redis there are not "integer" types.
     * Simply the string stored at the key is parsed as a base 10 64 bit signed integer, incremented,
     * and then converted back as a string.
     * <p>
     * Time complexity: O(1)
     *
     * @param key
     * @param integer
     * @return Integer reply, this stringCommands will reply with the new value of key after the increment.
     * @see #incr(byte[])
     * @see #decr(byte[])
     * @see #incrBy(byte[], long)
     */
    @Override
    @Suspendable
    public final Long decrBy(byte[] key, long integer) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    /**
     * Decrement the number stored at key by one. If the key does not exist or contains a value of a
     * wrong type, set the key to the value of "0" before to perform the decrement operation.
     * <p>
     * INCR stringCommands are limited to 64 bit signed integers.
     * <p>
     * Note: this is actually a string operation, that is, in Redis there are not "integer" types.
     * Simply the string stored at the key is parsed as a base 10 64 bit signed integer, incremented,
     * and then converted back as a string.
     * <p>
     * Time complexity: O(1)
     *
     * @param key
     * @return Integer reply, this stringCommands will reply with the new value of key after the increment.
     * @see #incr(byte[])
     * @see #incrBy(byte[], long)
     * @see #decrBy(byte[], long)
     */
    @Override
    @Suspendable
    public final Long decr(byte[] key) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    /**
     * INCRBY work just like {@link #incr(byte[]) INCR} but instead to increment by 1 the increment is
     * integer.
     * <p>
     * INCR stringCommands are limited to 64 bit signed integers.
     * <p>
     * Note: this is actually a string operation, that is, in Redis there are not "integer" types.
     * Simply the string stored at the key is parsed as a base 10 64 bit signed integer, incremented,
     * and then converted back as a string.
     * <p>
     * Time complexity: O(1)
     *
     * @param key
     * @param integer
     * @return Integer reply, this stringCommands will reply with the new value of key after the increment.
     * @see #incr(byte[])
     * @see #decr(byte[])
     * @see #decrBy(byte[], long)
     */
    @Override
    @Suspendable
    public final Long incrBy(byte[] key, long integer) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    /**
     * INCRBYFLOAT work just like {@link #incrBy(byte[], long)} INCRBY} but increments by floats
     * instead of integers.
     * <p>
     * INCRBYFLOAT stringCommands are limited to double precision floating point values.
     * <p>
     * Note: this is actually a string operation, that is, in Redis there are not "double" types.
     * Simply the string stored at the key is parsed as a base double precision floating point value,
     * incremented, and then converted back as a string. There is no DECRYBYFLOAT but providing a
     * negative value will work as expected.
     * <p>
     * Time complexity: O(1)
     *
     * @param key     the key to increment
     * @param integer the value to increment by
     * @return Integer reply, this stringCommands will reply with the new value of key after the increment.
     * @see #incr(byte[])
     * @see #decr(byte[])
     * @see #decrBy(byte[], long)
     */
    @Override
    @Suspendable
    public final Double incrByFloat(byte[] key, double integer) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    /**
     * Increment the number stored at key by one. If the key does not exist or contains a value of a
     * wrong type, set the key to the value of "0" before to perform the increment operation.
     * <p>
     * INCR stringCommands are limited to 64 bit signed integers.
     * <p>
     * Note: this is actually a string operation, that is, in Redis there are not "integer" types.
     * Simply the string stored at the key is parsed as a base 10 64 bit signed integer, incremented,
     * and then converted back as a string.
     * <p>
     * Time complexity: O(1)
     *
     * @param key
     * @return Integer reply, this stringCommands will reply with the new value of key after the increment.
     * @see #incrBy(byte[], long)
     * @see #decr(byte[])
     * @see #decrBy(byte[], long)
     */
    @Override
    @Suspendable
    public final Long incr(byte[] key) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    /**
     * If the key already exists and is a string, this command appends the provided value at the end
     * of the string. If the key does not exist it is created and set as an empty string, so APPEND
     * will be very similar to SET in this special case.
     * <p>
     * Time complexity: O(1). The amortized time complexity is O(1) assuming the appended value is
     * small and the already present value is of any size, since the dynamic string library used by
     * Redis will double the free space available on every reallocation.
     *
     * @param key
     * @param value
     * @return Integer reply, specifically the total length of the string after the append operation.
     */
    @Override
    @Suspendable
    public final Long append(byte[] key, byte[] value) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    /**
     * Return a subset of the string from offset start to offset end (both offsets are inclusive).
     * Negative offsets can be used in order to provide an offset starting from the end of the string.
     * So -1 means the last char, -2 the penultimate and so forth.
     * <p>
     * The function handles out of range requests without raising an error, but just limiting the
     * resulting range to the actual length of the string.
     * <p>
     * Time complexity: O(start+n) (with start being the start index and n the total length of the
     * requested range). Note that the lookup part of this command is O(1) so for small strings this
     * is actually an O(1) command.
     *
     * @param key
     * @param start
     * @param end
     * @return Bulk reply
     */
    @Override
    @Suspendable
    public final byte[] substr(byte[] key, int start, int end) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    /**
     * Set the specified hash field to the specified value.
     * <p>
     * If key does not exist, a new key holding a hash is created.
     * <p>
     * <b>Time complexity:</b> O(1)
     *
     * @param key
     * @param field
     * @param value
     * @return If the field already exists, and the HSET just produced an update of the value, 0 is
     * returned, otherwise if a new field is created 1 is returned.
     */
    @Override
    @Suspendable
    public final Long hset(byte[] key, byte[] field, byte[] value) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    /**
     * If key holds a hash, retrieve the value associated to the specified field.
     * <p>
     * If the field is not found or the key does not exist, a special 'nil' value is returned.
     * <p>
     * <b>Time complexity:</b> O(1)
     *
     * @param key
     * @param field
     * @return Bulk reply
     */
    @Override
    @Suspendable
    public final byte[] hget(byte[] key, byte[] field) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    /**
     * Set the specified hash field to the specified value if the field not exists. <b>Time
     * complexity:</b> O(1)
     *
     * @param key
     * @param field
     * @param value
     * @return If the field already exists, 0 is returned, otherwise if a new field is created 1 is
     * returned.
     */
    @Override
    @Suspendable
    public final Long hsetnx(byte[] key, byte[] field, byte[] value) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    /**
     * Set the respective fields to the respective values. HMSET replaces old values with new values.
     * <p>
     * If key does not exist, a new key holding a hash is created.
     * <p>
     * <b>Time complexity:</b> O(N) (with N being the number of fields)
     *
     * @param key
     * @param hash
     * @return Always OK because HMSET can't fail
     */
    @Override
    @Suspendable
    public final String hmset(byte[] key, Map<byte[], byte[]> hash) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    /**
     * Retrieve the values associated to the specified fields.
     * <p>
     * If some of the specified fields do not exist, nil values are returned. Non existing keys are
     * considered like empty hashes.
     * <p>
     * <b>Time complexity:</b> O(N) (with N being the number of fields)
     *
     * @param key
     * @param fields
     * @return Multi Bulk Reply specifically a list of all the values associated with the specified
     * fields, in the same order of the request.
     */
    @Override
    @Suspendable
    public final List<byte[]> hmget(byte[] key, byte[]... fields) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    /**
     * Increment the number stored at field in the hash at key by value. If key does not exist, a new
     * key holding a hash is created. If field does not exist or holds a string, the value is set to 0
     * before applying the operation. Since the value argument is signed you can use this command to
     * perform both increments and decrements.
     * <p>
     * The range of values supported by HINCRBY is limited to 64 bit signed integers.
     * <p>
     * <b>Time complexity:</b> O(1)
     *
     * @param key
     * @param field
     * @param value
     * @return Integer reply The new value at field after the increment operation.
     */
    @Override
    @Suspendable
    public final Long hincrBy(byte[] key, byte[] field, long value) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    /**
     * Increment the number stored at field in the hash at key by a double precision floating point
     * value. If key does not exist, a new key holding a hash is created. If field does not exist or
     * holds a string, the value is set to 0 before applying the operation. Since the value argument
     * is signed you can use this command to perform both increments and decrements.
     * <p>
     * The range of values supported by HINCRBYFLOAT is limited to double precision floating point
     * values.
     * <p>
     * <b>Time complexity:</b> O(1)
     *
     * @param key
     * @param field
     * @param value
     * @return Double precision floating point reply The new value at field after the increment
     * operation.
     */
    @Override
    @Suspendable
    public final Double hincrByFloat(byte[] key, byte[] field, double value) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    /**
     * Test for existence of a specified field in a hash. <b>Time complexity:</b> O(1)
     *
     * @param key
     * @param field
     * @return Return 1 if the hash stored at key contains the specified field. Return 0 if the key is
     * not found or the field is not present.
     */
    @Override
    @Suspendable
    public final Boolean hexists(byte[] key, byte[] field) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    /**
     * Remove the specified field from an hash stored at key.
     * <p>
     * <b>Time complexity:</b> O(1)
     *
     * @param key
     * @param fields
     * @return If the field was present in the hash it is deleted and 1 is returned, otherwise 0 is
     * returned and no operation is performed.
     */
    @Override
    @Suspendable
    public final Long hdel(byte[] key, byte[]... fields) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    /**
     * Return the number of items in a hash.
     * <p>
     * <b>Time complexity:</b> O(1)
     *
     * @param key
     * @return The number of entries (fields) contained in the hash stored at key. If the specified
     * key does not exist, 0 is returned assuming an empty hash.
     */
    @Override
    @Suspendable
    public final Long hlen(byte[] key) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    /**
     * Return all the fields in a hash.
     * <p>
     * <b>Time complexity:</b> O(N), where N is the total number of entries
     *
     * @param key
     * @return All the fields names contained into a hash.
     */
    @Override
    @Suspendable
    public final Set<byte[]> hkeys(byte[] key) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    /**
     * Return all the values in a hash.
     * <p>
     * <b>Time complexity:</b> O(N), where N is the total number of entries
     *
     * @param key
     * @return All the fields values contained into a hash.
     */
    @Override
    @Suspendable
    public final List<byte[]> hvals(byte[] key) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    /**
     * Return all the fields and associated values in a hash.
     * <p>
     * <b>Time complexity:</b> O(N), where N is the total number of entries
     *
     * @param key
     * @return All the fields and values contained into a hash.
     */
    @Override
    @Suspendable
    public final Map<byte[], byte[]> hgetAll(byte[] key) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    /**
     * Add the string value to the head (LPUSH) or tail (RPUSH) of the list stored at key. If the key
     * does not exist an empty list is created just before the append operation. If the key exists but
     * is not a List an error is returned.
     * <p>
     * Time complexity: O(1)
     *
     * @param key
     * @param strings
     * @return Integer reply, specifically, the number of elements inside the list after the push
     * operation.
     * @see BinaryJedis#rpush(byte[], byte[]...)
     */
    @Override
    @Suspendable
    public final Long rpush(byte[] key, byte[]... strings) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    /**
     * Add the string value to the head (LPUSH) or tail (RPUSH) of the list stored at key. If the key
     * does not exist an empty list is created just before the append operation. If the key exists but
     * is not a List an error is returned.
     * <p>
     * Time complexity: O(1)
     *
     * @param key
     * @param strings
     * @return Integer reply, specifically, the number of elements inside the list after the push
     * operation.
     * @see BinaryJedis#rpush(byte[], byte[]...)
     */
    @Override
    @Suspendable
    public final Long lpush(byte[] key, byte[]... strings) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    /**
     * Return the length of the list stored at the specified key. If the key does not exist zero is
     * returned (the same behaviour as for empty lists). If the value stored at key is not a list an
     * error is returned.
     * <p>
     * Time complexity: O(1)
     *
     * @param key
     * @return The length of the list.
     */
    @Override
    @Suspendable
    public final Long llen(byte[] key) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    /**
     * Return the specified elements of the list stored at the specified key. Start and end are
     * zero-based indexes. 0 is the first element of the list (the list head), 1 the next element and
     * so on.
     * <p>
     * For example LRANGE foobar 0 2 will return the first three elements of the list.
     * <p>
     * start and end can also be negative numbers indicating offsets from the end of the list. For
     * example -1 is the last element of the list, -2 the penultimate element and so on.
     * <p>
     * <b>Consistency with range functions in various programming languages</b>
     * <p>
     * Note that if you have a list of numbers from 0 to 100, LRANGE 0 10 will return 11 elements,
     * that is, rightmost item is included. This may or may not be consistent with behavior of
     * range-related functions in your programming language of choice (think Ruby's Range.new,
     * Array#slice or Python's range() function).
     * <p>
     * LRANGE behavior is consistent with one of Tcl.
     * <p>
     * <b>Out-of-range indexes</b>
     * <p>
     * Indexes out of range will not produce an error: if start is over the end of the list, or start
     * &gt; end, an empty list is returned. If end is over the end of the list Redis will threat it
     * just like the last element of the list.
     * <p>
     * Time complexity: O(start+n) (with n being the length of the range and start being the start
     * offset)
     *
     * @param key
     * @param start
     * @param end
     * @return Multi bulk reply, specifically a list of elements in the specified range.
     */
    @Override
    @Suspendable
    public final List<byte[]> lrange(byte[] key, long start, long end) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    /**
     * Trim an existing list so that it will contain only the specified range of elements specified.
     * Start and end are zero-based indexes. 0 is the first element of the list (the list head), 1 the
     * next element and so on.
     * <p>
     * For example LTRIM foobar 0 2 will modify the list stored at foobar key so that only the first
     * three elements of the list will remain.
     * <p>
     * start and end can also be negative numbers indicating offsets from the end of the list. For
     * example -1 is the last element of the list, -2 the penultimate element and so on.
     * <p>
     * Indexes out of range will not produce an error: if start is over the end of the list, or start
     * &gt; end, an empty list is left as value. If end over the end of the list Redis will threat it
     * just like the last element of the list.
     * <p>
     * Hint: the obvious use of LTRIM is together with LPUSH/RPUSH. For example:
     * <p>
     * {@code lpush("mylist", "someelement"); ltrim("mylist", 0, 99); * }
     * <p>
     * The above two stringCommands will push elements in the list taking care that the list will not grow
     * without limits. This is very useful when using Redis to store logs for example. It is important
     * to note that when used in this way LTRIM is an O(1) operation because in the average case just
     * one element is removed from the tail of the list.
     * <p>
     * Time complexity: O(n) (with n being len of list - len of range)
     *
     * @param key
     * @param start
     * @param end
     * @return Status code reply
     */
    @Override
    @Suspendable
    public final String ltrim(byte[] key, long start, long end) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    /**
     * Return the specified element of the list stored at the specified key. 0 is the first element, 1
     * the second and so on. Negative indexes are supported, for example -1 is the last element, -2
     * the penultimate and so on.
     * <p>
     * If the value stored at key is not of list type an error is returned. If the index is out of
     * range a 'nil' reply is returned.
     * <p>
     * Note that even if the average time complexity is O(n) asking for the first or the last element
     * of the list is O(1).
     * <p>
     * Time complexity: O(n) (with n being the length of the list)
     *
     * @param key
     * @param index
     * @return Bulk reply, specifically the requested element
     */
    @Override
    @Suspendable
    public final byte[] lindex(byte[] key, long index) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    /**
     * Set a new value as the element at index position of the List at key.
     * <p>
     * Out of range indexes will generate an error.
     * <p>
     * Similarly to other list stringCommands accepting indexes, the index can be negative to access
     * elements starting from the end of the list. So -1 is the last element, -2 is the penultimate,
     * and so forth.
     * <p>
     * <b>Time complexity:</b>
     * <p>
     * O(N) (with N being the length of the list), setting the first or last elements of the list is
     * O(1).
     *
     * @param key
     * @param index
     * @param value
     * @return Status code reply
     * @see #lindex(byte[], long)
     */
    @Override
    @Suspendable
    public final String lset(byte[] key, long index, byte[] value) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    /**
     * Remove the first count occurrences of the value element from the list. If count is zero all the
     * elements are removed. If count is negative elements are removed from tail to head, instead to
     * go from head to tail that is the normal behaviour. So for example LREM with count -2 and hello
     * as value to remove against the list (a,b,c,hello,x,hello,hello) will have the list
     * (a,b,c,hello,x). The number of removed elements is returned as an integer, see below for more
     * information about the returned value. Note that non existing keys are considered like empty
     * lists by LREM, so LREM against non existing keys will always return 0.
     * <p>
     * Time complexity: O(N) (with N being the length of the list)
     *
     * @param key
     * @param count
     * @param value
     * @return Integer Reply, specifically: The number of removed elements if the operation succeeded
     */
    @Override
    @Suspendable
    public final Long lrem(byte[] key, long count, byte[] value) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    /**
     * Atomically return and remove the first (LPOP) or last (RPOP) element of the list. For example
     * if the list contains the elements "a","b","c" LPOP will return "a" and the list will become
     * "b","c".
     * <p>
     * If the key does not exist or the list is already empty the special value 'nil' is returned.
     *
     * @param key
     * @return Bulk reply
     * @see #rpop(byte[])
     */
    @Override
    @Suspendable
    public final byte[] lpop(byte[] key) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    /**
     * Atomically return and remove the first (LPOP) or last (RPOP) element of the list. For example
     * if the list contains the elements "a","b","c" LPOP will return "a" and the list will become
     * "b","c".
     * <p>
     * If the key does not exist or the list is already empty the special value 'nil' is returned.
     *
     * @param key
     * @return Bulk reply
     * @see #lpop(byte[])
     */
    @Override
    @Suspendable
    public final byte[] rpop(byte[] key) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    /**
     * Atomically return and remove the last (tail) element of the srckey list, and push the element
     * as the first (head) element of the dstkey list. For example if the source list contains the
     * elements "a","b","c" and the destination list contains the elements "foo","bar" after an
     * RPOPLPUSH command the content of the two lists will be "a","b" and "c","foo","bar".
     * <p>
     * If the key does not exist or the list is already empty the special value 'nil' is returned. If
     * the srckey and dstkey are the same the operation is equivalent to removing the last element
     * from the list and pusing it as first element of the list, so it's a "list rotation" command.
     * <p>
     * Time complexity: O(1)
     *
     * @param srckey
     * @param dstkey
     * @return Bulk reply
     */
    @Override
    @Suspendable
    public final byte[] rpoplpush(byte[] srckey, byte[] dstkey) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    /**
     * Add the specified member to the set value stored at key. If member is already a member of the
     * set no operation is performed. If key does not exist a new set with the specified member as
     * sole member is created. If the key exists but does not hold a set value an error is returned.
     * <p>
     * Time complexity O(1)
     *
     * @param key
     * @param members
     * @return Integer reply, specifically: 1 if the new element was added 0 if the element was
     * already a member of the set
     */
    @Override
    @Suspendable
    public final Long sadd(byte[] key, byte[]... members) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    /**
     * Return all the members (elements) of the set value stored at key. This is just syntax glue for
     * {@link #sinter(byte[]...)} SINTER}.
     * <p>
     * Time complexity O(N)
     *
     * @param key the key of the set
     * @return Multi bulk reply
     */
    @Override
    @Suspendable
    public final Set<byte[]> smembers(byte[] key) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    /**
     * Remove the specified member from the set value stored at key. If member was not a member of the
     * set no operation is performed. If key does not hold a set value an error is returned.
     * <p>
     * Time complexity O(1)
     *
     * @param key    the key of the set
     * @param member the set member to remove
     * @return Integer reply, specifically: 1 if the new element was removed 0 if the new element was
     * not a member of the set
     */
    @Override
    @Suspendable
    public final Long srem(byte[] key, byte[]... member) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    /**
     * Remove a random element from a Set returning it as return value. If the Set is empty or the key
     * does not exist, a nil object is returned.
     * <p>
     * The {@link #srandmember(byte[])} command does a similar work but the returned element is not
     * removed from the Set.
     * <p>
     * Time complexity O(1)
     *
     * @param key
     * @return Bulk reply
     */
    @Override
    @Suspendable
    public final byte[] spop(byte[] key) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    @Override
    @Suspendable
    public final Set<byte[]> spop(byte[] key, long count) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    /**
     * Move the specified member from the set at srckey to the set at dstkey. This operation is
     * atomic, in every given moment the element will appear to be in the source or destination set
     * for accessing clients.
     * <p>
     * If the source set does not exist or does not contain the specified element no operation is
     * performed and zero is returned, otherwise the element is removed from the source set and added
     * to the destination set. On success one is returned, even if the element was already present in
     * the destination set.
     * <p>
     * An error is raised if the source or destination keys contain a non Set value.
     * <p>
     * Time complexity O(1)
     *
     * @param srckey
     * @param dstkey
     * @param member
     * @return Integer reply, specifically: 1 if the element was moved 0 if the element was not found
     * on the first set and no operation was performed
     */
    @Override
    @Suspendable
    public final Long smove(byte[] srckey, byte[] dstkey, byte[] member) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    /**
     * Return the set cardinality (number of elements). If the key does not exist 0 is returned, like
     * for empty sets.
     *
     * @param key
     * @return Integer reply, specifically: the cardinality (number of elements) of the set as an
     * integer.
     */
    @Override
    @Suspendable
    public final Long scard(byte[] key) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    /**
     * Return 1 if member is a member of the set stored at key, otherwise 0 is returned.
     * <p>
     * Time complexity O(1)
     *
     * @param key
     * @param member
     * @return Integer reply, specifically: 1 if the element is a member of the set 0 if the element
     * is not a member of the set OR if the key does not exist
     */
    @Override
    @Suspendable
    public final Boolean sismember(byte[] key, byte[] member) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    /**
     * Return the members of a set resulting from the intersection of all the sets hold at the
     * specified keys. Like in {@link #lrange(byte[], long, long)} LRANGE} the result is sent to the
     * client as a multi-bulk reply (see the protocol specification for more information). If just a
     * single key is specified, then this command produces the same result as
     * {@link #smembers(byte[]) SMEMBERS}. Actually SMEMBERS is just syntax sugar for SINTER.
     * <p>
     * Non existing keys are considered like empty sets, so if one of the keys is missing an empty set
     * is returned (since the intersection with an empty set always is an empty set).
     * <p>
     * Time complexity O(N*M) worst case where N is the cardinality of the smallest set and M the
     * number of sets
     *
     * @param keys
     * @return Multi bulk reply, specifically the list of common elements.
     */
    @Override
    @Suspendable
    public final Set<byte[]> sinter(byte[]... keys) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    /**
     * This commnad works exactly like {@link #sinter(byte[]...) SINTER} but instead of being returned
     * the resulting set is sotred as dstkey.
     * <p>
     * Time complexity O(N*M) worst case where N is the cardinality of the smallest set and M the
     * number of sets
     *
     * @param dstkey
     * @param keys
     * @return Status code reply
     */
    @Override
    @Suspendable
    public final Long sinterstore(byte[] dstkey, byte[]... keys) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    /**
     * Return the members of a set resulting from the union of all the sets hold at the specified
     * keys. Like in {@link #lrange(byte[], long, long)} LRANGE} the result is sent to the client as a
     * multi-bulk reply (see the protocol specification for more information). If just a single key is
     * specified, then this command produces the same result as {@link #smembers(byte[]) SMEMBERS}.
     * <p>
     * Non existing keys are considered like empty sets.
     * <p>
     * Time complexity O(N) where N is the total number of elements in all the provided sets
     *
     * @param keys
     * @return Multi bulk reply, specifically the list of common elements.
     */
    @Override
    @Suspendable
    public final Set<byte[]> sunion(byte[]... keys) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    /**
     * This command works exactly like {@link #sunion(byte[]...) SUNION} but instead of being returned
     * the resulting set is stored as dstkey. Any existing value in dstkey will be over-written.
     * <p>
     * Time complexity O(N) where N is the total number of elements in all the provided sets
     *
     * @param dstkey
     * @param keys
     * @return Status code reply
     */
    @Override
    @Suspendable
    public final Long sunionstore(byte[] dstkey, byte[]... keys) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    /**
     * Return the difference between the Set stored at key1 and all the Sets key2, ..., keyN
     * <p>
     * <b>Example:</b>
     * <p>
     * <pre>
     * key1 = [x, a, b, c]
     * key2 = [c]
     * key3 = [a, d]
     * SDIFF key1,key2,key3 =&gt; [x, b]
     * </pre>
     * <p>
     * Non existing keys are considered like empty sets.
     * <p>
     * <b>Time complexity:</b>
     * <p>
     * O(N) with N being the total number of elements of all the sets
     *
     * @param keys
     * @return Return the members of a set resulting from the difference between the first set
     * provided and all the successive sets.
     */
    @Override
    @Suspendable
    public final Set<byte[]> sdiff(byte[]... keys) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    /**
     * This command works exactly like {@link #sdiff(byte[]...) SDIFF} but instead of being returned
     * the resulting set is stored in dstkey.
     *
     * @param dstkey
     * @param keys
     * @return Status code reply
     */
    @Override
    @Suspendable
    public final Long sdiffstore(byte[] dstkey, byte[]... keys) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    /**
     * Return a random element from a Set, without removing the element. If the Set is empty or the
     * key does not exist, a nil object is returned.
     * <p>
     * The SPOP command does a similar work but the returned element is popped (removed) from the Set.
     * <p>
     * Time complexity O(1)
     *
     * @param key
     * @return Bulk reply
     */
    @Override
    @Suspendable
    public final byte[] srandmember(byte[] key) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    @Override
    @Suspendable
    public final List<byte[]> srandmember(byte[] key, int count) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    /**
     * Add the specified member having the specifeid score to the sorted set stored at key. If member
     * is already a member of the sorted set the score is updated, and the element reinserted in the
     * right position to ensure sorting. If key does not exist a new sorted set with the specified
     * member as sole member is crated. If the key exists but does not hold a sorted set value an
     * error is returned.
     * <p>
     * The score value can be the string representation of a double precision floating point number.
     * <p>
     * Time complexity O(log(N)) with N being the number of elements in the sorted set
     *
     * @param key
     * @param score
     * @param member
     * @return Integer reply, specifically: 1 if the new element was added 0 if the element was
     * already a member of the sorted set and the score was updated
     */
    @Override
    @Suspendable
    public final Long zadd(byte[] key, double score, byte[] member) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    @Override
    @Suspendable
    public final Long zadd(byte[] key, double score, byte[] member, ZAddParams params) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    @Override
    @Suspendable
    public final Long zadd(byte[] key, Map<byte[], Double> scoreMembers) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    @Override
    @Suspendable
    public final Long zadd(byte[] key, Map<byte[], Double> scoreMembers, ZAddParams params) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    @Override
    @Suspendable
    public final Set<byte[]> zrange(byte[] key, long start, long end) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    /**
     * Remove the specified member from the sorted set value stored at key. If member was not a member
     * of the set no operation is performed. If key does not not hold a set value an error is
     * returned.
     * <p>
     * Time complexity O(log(N)) with N being the number of elements in the sorted set
     *
     * @param key
     * @param members
     * @return Integer reply, specifically: 1 if the new element was removed 0 if the new element was
     * not a member of the set
     */
    @Override
    @Suspendable
    public final Long zrem(byte[] key, byte[]... members) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    /**
     * If member already exists in the sorted set adds the increment to its score and updates the
     * position of the element in the sorted set accordingly. If member does not already exist in the
     * sorted set it is added with increment as score (that is, like if the previous score was
     * virtually zero). If key does not exist a new sorted set with the specified member as sole
     * member is crated. If the key exists but does not hold a sorted set value an error is returned.
     * <p>
     * The score value can be the string representation of a double precision floating point number.
     * It's possible to provide a negative value to perform a decrement.
     * <p>
     * For an introduction to sorted sets check the Introduction to Redis data types page.
     * <p>
     * Time complexity O(log(N)) with N being the number of elements in the sorted set
     *
     * @param key
     * @param score
     * @param member
     * @return The new score
     */
    @Override
    @Suspendable
    public final Double zincrby(byte[] key, double score, byte[] member) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    @Override
    @Suspendable
    public final Double zincrby(byte[] key, double score, byte[] member, ZIncrByParams params) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    /**
     * Return the rank (or index) or member in the sorted set at key, with scores being ordered from
     * low to high.
     * <p>
     * When the given member does not exist in the sorted set, the special value 'nil' is returned.
     * The returned rank (or index) of the member is 0-based for both stringCommands.
     * <p>
     * <b>Time complexity:</b>
     * <p>
     * O(log(N))
     *
     * @param key
     * @param member
     * @return Integer reply or a nil bulk reply, specifically: the rank of the element as an integer
     * reply if the element exists. A nil bulk reply if there is no such element.
     * @see #zrevrank(byte[], byte[])
     */
    @Override
    @Suspendable
    public final Long zrank(byte[] key, byte[] member) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    /**
     * Return the rank (or index) or member in the sorted set at key, with scores being ordered from
     * high to low.
     * <p>
     * When the given member does not exist in the sorted set, the special value 'nil' is returned.
     * The returned rank (or index) of the member is 0-based for both stringCommands.
     * <p>
     * <b>Time complexity:</b>
     * <p>
     * O(log(N))
     *
     * @param key
     * @param member
     * @return Integer reply or a nil bulk reply, specifically: the rank of the element as an integer
     * reply if the element exists. A nil bulk reply if there is no such element.
     * @see #zrank(byte[], byte[])
     */
    @Override
    @Suspendable
    public final Long zrevrank(byte[] key, byte[] member) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    @Override
    @Suspendable
    public final Set<byte[]> zrevrange(byte[] key, long start, long end) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    @Override
    @Suspendable
    public final Set<Tuple> zrangeWithScores(byte[] key, long start, long end) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    @Override
    @Suspendable
    public final Set<Tuple> zrevrangeWithScores(byte[] key, long start, long end) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    /**
     * Return the sorted set cardinality (number of elements). If the key does not exist 0 is
     * returned, like for empty sorted sets.
     * <p>
     * Time complexity O(1)
     *
     * @param key
     * @return the cardinality (number of elements) of the set as an integer.
     */
    @Override
    @Suspendable
    public final Long zcard(byte[] key) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    /**
     * Return the score of the specified element of the sorted set at key. If the specified element
     * does not exist in the sorted set, or the key does not exist at all, a special 'nil' value is
     * returned.
     * <p>
     * <b>Time complexity:</b> O(1)
     *
     * @param key
     * @param member
     * @return the score
     */
    @Override
    @Suspendable
    public final Double zscore(byte[] key, byte[] member) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    @Override
    @Suspendable
    public final String watch(byte[]... keys) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    /**
     * Sort a Set or a List.
     * <p>
     * Sort the elements contained in the List, Set, or Sorted Set value at key. By default sorting is
     * numeric with elements being compared as double precision floating point numbers. This is the
     * simplest form of SORT.
     *
     * @param key
     * @return Assuming the Set/List at key contains a list of numbers, the return value will be the
     * list of numbers ordered from the smallest to the biggest number.
     * @see #sort(byte[], byte[])
     * @see #sort(byte[], SortingParams)
     * @see #sort(byte[], SortingParams, byte[])
     */
    @Override
    @Suspendable
    public final List<byte[]> sort(byte[] key) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    /**
     * Sort a Set or a List accordingly to the specified parameters.
     * <p>
     * <b>examples:</b>
     * <p>
     * Given are the following sets and key/values:
     * <p>
     * <pre>
     * x = [1, 2, 3]
     * y = [a, b, c]
     *
     * k1 = z
     * k2 = y
     * k3 = x
     *
     * w1 = 9
     * w2 = 8
     * w3 = 7
     * </pre>
     * <p>
     * Sort Order:
     * <p>
     * <pre>
     * sort(x) or sort(x, sp.asc())
     * -&gt; [1, 2, 3]
     *
     * sort(x, sp.desc())
     * -&gt; [3, 2, 1]
     *
     * sort(y)
     * -&gt; [c, a, b]
     *
     * sort(y, sp.alpha())
     * -&gt; [a, b, c]
     *
     * sort(y, sp.alpha().desc())
     * -&gt; [c, a, b]
     * </pre>
     * <p>
     * Limit (e.g. for Pagination):
     * <p>
     * <pre>
     * sort(x, sp.limit(0, 2))
     * -&gt; [1, 2]
     *
     * sort(y, sp.alpha().desc().limit(1, 2))
     * -&gt; [b, a]
     * </pre>
     * <p>
     * Sorting by external keys:
     * <p>
     * <pre>
     * sort(x, sb.by(w*))
     * -&gt; [3, 2, 1]
     *
     * sort(x, sb.by(w*).desc())
     * -&gt; [1, 2, 3]
     * </pre>
     * <p>
     * Getting external keys:
     * <p>
     * <pre>
     * sort(x, sp.by(w*).get(k*))
     * -&gt; [x, y, z]
     *
     * sort(x, sp.by(w*).get(#).get(k*))
     * -&gt; [3, x, 2, y, 1, z]
     * </pre>
     *
     * @param key
     * @param sortingParameters
     * @return a list of sorted elements.
     * @see #sort(byte[])
     * @see #sort(byte[], SortingParams, byte[])
     */
    @Override
    @Suspendable
    public final List<byte[]> sort(byte[] key, SortingParams sortingParameters) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    /**
     * BLPOP (and BRPOP) is a blocking list pop primitive. You can see this stringCommands as blocking
     * versions of LPOP and RPOP able to block if the specified keys don't exist or contain empty
     * lists.
     * <p>
     * The following is a description of the exact semantic. We describe BLPOP but the two stringCommands
     * are identical, the only difference is that BLPOP pops the element from the left (head) of the
     * list, and BRPOP pops from the right (tail).
     * <p>
     * <b>Non blocking behavior</b>
     * <p>
     * When BLPOP is called, if at least one of the specified keys contain a non empty list, an
     * element is popped from the head of the list and returned to the caller together with the name
     * of the key (BLPOP returns a two elements array, the first element is the key, the second the
     * popped value).
     * <p>
     * Keys are scanned from left to right, so for instance if you issue BLPOP list1 list2 list3 0
     * against a dataset where list1 does not exist but list2 and list3 contain non empty lists, BLPOP
     * guarantees to return an element from the list stored at list2 (since it is the first non empty
     * list starting from the left).
     * <p>
     * <b>Blocking behavior</b>
     * <p>
     * If none of the specified keys exist or contain non empty lists, BLPOP blocks until some other
     * client performs a LPUSH or an RPUSH operation against one of the lists.
     * <p>
     * Once new data is present on one of the lists, the client finally returns with the name of the
     * key unblocking it and the popped value.
     * <p>
     * When blocking, if a non-zero timeout is specified, the client will unblock returning a nil
     * special value if the specified amount of seconds passed without a push operation against at
     * least one of the specified keys.
     * <p>
     * The timeout argument is interpreted as an integer value. A timeout of zero means instead to
     * block forever.
     * <p>
     * <b>Multiple clients blocking for the same keys</b>
     * <p>
     * Multiple clients can block for the same key. They are put into a queue, so the first to be
     * served will be the one that started to wait earlier, in a first-blpopping first-served fashion.
     * <p>
     * <b>blocking POP inside a MULTI/EXEC transaction</b>
     * <p>
     * BLPOP and BRPOP can be used with pipelining (sending multiple stringCommands and reading the replies
     * in batch), but it does not make sense to use BLPOP or BRPOP inside a MULTI/EXEC block (a Redis
     * transaction).
     * <p>
     * The behavior of BLPOP inside MULTI/EXEC when the list is empty is to return a multi-bulk nil
     * reply, exactly what happens when the timeout is reached. If you like science fiction, think at
     * it like if inside MULTI/EXEC the time will flow at infinite speed :)
     * <p>
     * Time complexity: O(1)
     *
     * @param timeout
     * @param keys
     * @return BLPOP returns a two-elements array via a multi bulk reply in order to return both the
     * unblocking key and the popped value.
     * <p>
     * When a non-zero timeout is specified, and the BLPOP operation timed out, the return
     * value is a nil multi bulk reply. Most client values will return false or nil
     * accordingly to the programming language used.
     * @see #brpop(int, byte[]...)
     */
    @Override
    @Suspendable
    public final List<byte[]> blpop(int timeout, byte[]... keys) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    /**
     * Sort a Set or a List accordingly to the specified parameters and store the result at dstkey.
     *
     * @param key
     * @param sortingParameters
     * @param dstkey
     * @return The number of elements of the list at dstkey.
     * @see #sort(byte[], SortingParams)
     * @see #sort(byte[])
     * @see #sort(byte[], byte[])
     */
    @Override
    @Suspendable
    public final Long sort(byte[] key, SortingParams sortingParameters, byte[] dstkey) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    /**
     * Sort a Set or a List and Store the Result at dstkey.
     * <p>
     * Sort the elements contained in the List, Set, or Sorted Set value at key and store the result
     * at dstkey. By default sorting is numeric with elements being compared as double precision
     * floating point numbers. This is the simplest form of SORT.
     *
     * @param key
     * @param dstkey
     * @return The number of elements of the list at dstkey.
     * @see #sort(byte[])
     * @see #sort(byte[], SortingParams)
     * @see #sort(byte[], SortingParams, byte[])
     */
    @Override
    @Suspendable
    public final Long sort(byte[] key, byte[] dstkey) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    /**
     * BLPOP (and BRPOP) is a blocking list pop primitive. You can see this stringCommands as blocking
     * versions of LPOP and RPOP able to block if the specified keys don't exist or contain empty
     * lists.
     * <p>
     * The following is a description of the exact semantic. We describe BLPOP but the two stringCommands
     * are identical, the only difference is that BLPOP pops the element from the left (head) of the
     * list, and BRPOP pops from the right (tail).
     * <p>
     * <b>Non blocking behavior</b>
     * <p>
     * When BLPOP is called, if at least one of the specified keys contain a non empty list, an
     * element is popped from the head of the list and returned to the caller together with the name
     * of the key (BLPOP returns a two elements array, the first element is the key, the second the
     * popped value).
     * <p>
     * Keys are scanned from left to right, so for instance if you issue BLPOP list1 list2 list3 0
     * against a dataset where list1 does not exist but list2 and list3 contain non empty lists, BLPOP
     * guarantees to return an element from the list stored at list2 (since it is the first non empty
     * list starting from the left).
     * <p>
     * <b>Blocking behavior</b>
     * <p>
     * If none of the specified keys exist or contain non empty lists, BLPOP blocks until some other
     * client performs a LPUSH or an RPUSH operation against one of the lists.
     * <p>
     * Once new data is present on one of the lists, the client finally returns with the name of the
     * key unblocking it and the popped value.
     * <p>
     * When blocking, if a non-zero timeout is specified, the client will unblock returning a nil
     * special value if the specified amount of seconds passed without a push operation against at
     * least one of the specified keys.
     * <p>
     * The timeout argument is interpreted as an integer value. A timeout of zero means instead to
     * block forever.
     * <p>
     * <b>Multiple clients blocking for the same keys</b>
     * <p>
     * Multiple clients can block for the same key. They are put into a queue, so the first to be
     * served will be the one that started to wait earlier, in a first-blpopping first-served fashion.
     * <p>
     * <b>blocking POP inside a MULTI/EXEC transaction</b>
     * <p>
     * BLPOP and BRPOP can be used with pipelining (sending multiple stringCommands and reading the replies
     * in batch), but it does not make sense to use BLPOP or BRPOP inside a MULTI/EXEC block (a Redis
     * transaction).
     * <p>
     * The behavior of BLPOP inside MULTI/EXEC when the list is empty is to return a multi-bulk nil
     * reply, exactly what happens when the timeout is reached. If you like science fiction, think at
     * it like if inside MULTI/EXEC the time will flow at infinite speed :)
     * <p>
     * Time complexity: O(1)
     *
     * @param timeout
     * @param keys
     * @return BLPOP returns a two-elements array via a multi bulk reply in order to return both the
     * unblocking key and the popped value.
     * <p>
     * When a non-zero timeout is specified, and the BLPOP operation timed out, the return
     * value is a nil multi bulk reply. Most client values will return false or nil
     * accordingly to the programming language used.
     * @see #blpop(int, byte[]...)
     */
    @Override
    @Suspendable
    public final List<byte[]> brpop(int timeout, byte[]... keys) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    /**
     * @param arg
     * @deprecated unusable command, this command will be removed in 3.0.0.
     */
    @Override
    @Suspendable
    public final List<byte[]> blpop(byte[] arg) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    /**
     * @param arg
     * @deprecated unusable command, this command will be removed in 3.0.0.
     */
    @Override
    @Suspendable
    public final List<byte[]> brpop(byte[] arg) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    @Override
    public List<byte[]> blpop(byte[]... args) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    @Override
    @Suspendable
    public final List<byte[]> brpop(byte[]... args) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    @Override
    @Suspendable
    public final Long zcount(byte[] key, double min, double max) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    @Override
    @Suspendable
    public final Long zcount(byte[] key, byte[] min, byte[] max) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    /**
     * Return the all the elements in the sorted set at key with a score between min and max
     * (including elements with score equal to min or max).
     * <p>
     * The elements having the same score are returned sorted lexicographically as ASCII strings (this
     * follows from a property of Redis sorted sets and does not involve further computation).
     * <p>
     * Using the optional {@link #zrangeByScore(byte[], double, double, int, int) LIMIT} it's possible
     * to get only a range of the matching elements in an SQL-alike way. Note that if offset is large
     * the stringCommands needs to traverse the list for offset elements and this adds up to the O(M)
     * figure.
     * <p>
     * The {@link #zcount(byte[], double, double) ZCOUNT} command is similar to
     * {@link #zrangeByScore(byte[], double, double) ZRANGEBYSCORE} but instead of returning the
     * actual elements in the specified interval, it just returns the number of matching elements.
     * <p>
     * <b>Exclusive intervals and infinity</b>
     * <p>
     * min and max can be -inf and +inf, so that you are not required to know what's the greatest or
     * smallest element in order to take, for instance, elements "up to a given value".
     * <p>
     * Also while the interval is for default closed (inclusive) it's possible to specify open
     * intervals prefixing the score with a "(" character, so for instance:
     * <p>
     * {@code ZRANGEBYSCORE zset (1.3 5}
     * <p>
     * Will return all the values with score &gt; 1.3 and &lt;= 5, while for instance:
     * <p>
     * {@code ZRANGEBYSCORE zset (5 (10}
     * <p>
     * Will return all the values with score &gt; 5 and &lt; 10 (5 and 10 excluded).
     * <p>
     * <b>Time complexity:</b>
     * <p>
     * O(log(N))+O(M) with N being the number of elements in the sorted set and M the number of
     * elements returned by the command, so if M is constant (for instance you always ask for the
     * first ten elements with LIMIT) you can consider it O(log(N))
     *
     * @param key
     * @param min
     * @param max
     * @return Multi bulk reply specifically a list of elements in the specified score range.
     * @see #zrangeByScore(byte[], double, double)
     * @see #zrangeByScore(byte[], double, double, int, int)
     * @see #zrangeByScoreWithScores(byte[], double, double)
     * @see #zrangeByScoreWithScores(byte[], double, double, int, int)
     * @see #zcount(byte[], double, double)
     */
    @Override
    @Suspendable
    public final Set<byte[]> zrangeByScore(byte[] key, double min, double max) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    @Override
    @Suspendable
    public final Set<byte[]> zrangeByScore(byte[] key, byte[] min, byte[] max) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    /**
     * Return the all the elements in the sorted set at key with a score between min and max
     * (including elements with score equal to min or max).
     * <p>
     * The elements having the same score are returned sorted lexicographically as ASCII strings (this
     * follows from a property of Redis sorted sets and does not involve further computation).
     * <p>
     * Using the optional {@link #zrangeByScore(byte[], double, double, int, int) LIMIT} it's possible
     * to get only a range of the matching elements in an SQL-alike way. Note that if offset is large
     * the stringCommands needs to traverse the list for offset elements and this adds up to the O(M)
     * figure.
     * <p>
     * The {@link #zcount(byte[], double, double) ZCOUNT} command is similar to
     * {@link #zrangeByScore(byte[], double, double) ZRANGEBYSCORE} but instead of returning the
     * actual elements in the specified interval, it just returns the number of matching elements.
     * <p>
     * <b>Exclusive intervals and infinity</b>
     * <p>
     * min and max can be -inf and +inf, so that you are not required to know what's the greatest or
     * smallest element in order to take, for instance, elements "up to a given value".
     * <p>
     * Also while the interval is for default closed (inclusive) it's possible to specify open
     * intervals prefixing the score with a "(" character, so for instance:
     * <p>
     * {@code ZRANGEBYSCORE zset (1.3 5}
     * <p>
     * Will return all the values with score &gt; 1.3 and &lt;= 5, while for instance:
     * <p>
     * {@code ZRANGEBYSCORE zset (5 (10}
     * <p>
     * Will return all the values with score &gt; 5 and &lt; 10 (5 and 10 excluded).
     * <p>
     * <b>Time complexity:</b>
     * <p>
     * O(log(N))+O(M) with N being the number of elements in the sorted set and M the number of
     * elements returned by the command, so if M is constant (for instance you always ask for the
     * first ten elements with LIMIT) you can consider it O(log(N))
     *
     * @param key
     * @param min
     * @param max
     * @param offset
     * @param count  @return Multi bulk reply specifically a list of elements in the specified score range.
     * @see #zrangeByScore(byte[], double, double)
     * @see #zrangeByScore(byte[], double, double, int, int)
     * @see #zrangeByScoreWithScores(byte[], double, double)
     * @see #zrangeByScoreWithScores(byte[], double, double, int, int)
     * @see #zcount(byte[], double, double)
     */
    @Override
    @Suspendable
    public final Set<byte[]> zrangeByScore(byte[] key, double min, double max, int offset, int count) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    @Override
    @Suspendable
    public final Set<byte[]> zrangeByScore(byte[] key, byte[] min, byte[] max, int offset, int count) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    /**
     * Return the all the elements in the sorted set at key with a score between min and max
     * (including elements with score equal to min or max).
     * <p>
     * The elements having the same score are returned sorted lexicographically as ASCII strings (this
     * follows from a property of Redis sorted sets and does not involve further computation).
     * <p>
     * Using the optional {@link #zrangeByScore(byte[], double, double, int, int) LIMIT} it's possible
     * to get only a range of the matching elements in an SQL-alike way. Note that if offset is large
     * the stringCommands needs to traverse the list for offset elements and this adds up to the O(M)
     * figure.
     * <p>
     * The {@link #zcount(byte[], double, double) ZCOUNT} command is similar to
     * {@link #zrangeByScore(byte[], double, double) ZRANGEBYSCORE} but instead of returning the
     * actual elements in the specified interval, it just returns the number of matching elements.
     * <p>
     * <b>Exclusive intervals and infinity</b>
     * <p>
     * min and max can be -inf and +inf, so that you are not required to know what's the greatest or
     * smallest element in order to take, for instance, elements "up to a given value".
     * <p>
     * Also while the interval is for default closed (inclusive) it's possible to specify open
     * intervals prefixing the score with a "(" character, so for instance:
     * <p>
     * {@code ZRANGEBYSCORE zset (1.3 5}
     * <p>
     * Will return all the values with score &gt; 1.3 and &lt;= 5, while for instance:
     * <p>
     * {@code ZRANGEBYSCORE zset (5 (10}
     * <p>
     * Will return all the values with score &gt; 5 and &lt; 10 (5 and 10 excluded).
     * <p>
     * <b>Time complexity:</b>
     * <p>
     * O(log(N))+O(M) with N being the number of elements in the sorted set and M the number of
     * elements returned by the command, so if M is constant (for instance you always ask for the
     * first ten elements with LIMIT) you can consider it O(log(N))
     *
     * @param key
     * @param min
     * @param max
     * @return Multi bulk reply specifically a list of elements in the specified score range.
     * @see #zrangeByScore(byte[], double, double)
     * @see #zrangeByScore(byte[], double, double, int, int)
     * @see #zrangeByScoreWithScores(byte[], double, double)
     * @see #zrangeByScoreWithScores(byte[], double, double, int, int)
     * @see #zcount(byte[], double, double)
     */
    @Override
    @Suspendable
    public final Set<Tuple> zrangeByScoreWithScores(byte[] key, double min, double max) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    @Override
    @Suspendable
    public final Set<Tuple> zrangeByScoreWithScores(byte[] key, byte[] min, byte[] max) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    /**
     * Return the all the elements in the sorted set at key with a score between min and max
     * (including elements with score equal to min or max).
     * <p>
     * The elements having the same score are returned sorted lexicographically as ASCII strings (this
     * follows from a property of Redis sorted sets and does not involve further computation).
     * <p>
     * Using the optional {@link #zrangeByScore(byte[], double, double, int, int) LIMIT} it's possible
     * to get only a range of the matching elements in an SQL-alike way. Note that if offset is large
     * the stringCommands needs to traverse the list for offset elements and this adds up to the O(M)
     * figure.
     * <p>
     * The {@link #zcount(byte[], double, double) ZCOUNT} command is similar to
     * {@link #zrangeByScore(byte[], double, double) ZRANGEBYSCORE} but instead of returning the
     * actual elements in the specified interval, it just returns the number of matching elements.
     * <p>
     * <b>Exclusive intervals and infinity</b>
     * <p>
     * min and max can be -inf and +inf, so that you are not required to know what's the greatest or
     * smallest element in order to take, for instance, elements "up to a given value".
     * <p>
     * Also while the interval is for default closed (inclusive) it's possible to specify open
     * intervals prefixing the score with a "(" character, so for instance:
     * <p>
     * {@code ZRANGEBYSCORE zset (1.3 5}
     * <p>
     * Will return all the values with score &gt; 1.3 and &lt;= 5, while for instance:
     * <p>
     * {@code ZRANGEBYSCORE zset (5 (10}
     * <p>
     * Will return all the values with score &gt; 5 and &lt; 10 (5 and 10 excluded).
     * <p>
     * <b>Time complexity:</b>
     * <p>
     * O(log(N))+O(M) with N being the number of elements in the sorted set and M the number of
     * elements returned by the command, so if M is constant (for instance you always ask for the
     * first ten elements with LIMIT) you can consider it O(log(N))
     *
     * @param key
     * @param min
     * @param max
     * @param offset
     * @param count  @return Multi bulk reply specifically a list of elements in the specified score range.
     * @see #zrangeByScore(byte[], double, double)
     * @see #zrangeByScore(byte[], double, double, int, int)
     * @see #zrangeByScoreWithScores(byte[], double, double)
     * @see #zrangeByScoreWithScores(byte[], double, double, int, int)
     * @see #zcount(byte[], double, double)
     */
    @Override
    @Suspendable
    public final Set<Tuple> zrangeByScoreWithScores(byte[] key, double min, double max, int offset, int count) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    @Override
    @Suspendable
    public final Set<Tuple> zrangeByScoreWithScores(byte[] key, byte[] min, byte[] max, int offset, int count) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    @Override
    @Suspendable
    public final Set<byte[]> zrevrangeByScore(byte[] key, double max, double min) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    @Override
    @Suspendable
    public final Set<byte[]> zrevrangeByScore(byte[] key, byte[] max, byte[] min) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    @Override
    @Suspendable
    public final Set<byte[]> zrevrangeByScore(byte[] key, double max, double min, int offset, int count) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    @Override
    @Suspendable
    public final Set<byte[]> zrevrangeByScore(byte[] key, byte[] max, byte[] min, int offset, int count) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    @Override
    @Suspendable
    public final Set<Tuple> zrevrangeByScoreWithScores(byte[] key, double max, double min) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    @Override
    @Suspendable
    public final Set<Tuple> zrevrangeByScoreWithScores(byte[] key, double max, double min, int offset, int count) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    @Override
    @Suspendable
    public final Set<Tuple> zrevrangeByScoreWithScores(byte[] key, byte[] max, byte[] min) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    @Override
    @Suspendable
    public final Set<Tuple> zrevrangeByScoreWithScores(byte[] key, byte[] max, byte[] min, int offset, int count) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    /**
     * Remove all elements in the sorted set at key with rank between start and end. Start and end are
     * 0-based with rank 0 being the element with the lowest score. Both start and end can be negative
     * numbers, where they indicate offsets starting at the element with the highest rank. For
     * example: -1 is the element with the highest score, -2 the element with the second highest score
     * and so forth.
     * <p>
     * <b>Time complexity:</b> O(log(N))+O(M) with N being the number of elements in the sorted set
     * and M the number of elements removed by the operation
     *
     * @param key
     * @param start
     * @param end
     */
    @Override
    @Suspendable
    public final Long zremrangeByRank(byte[] key, long start, long end) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    /**
     * Remove all the elements in the sorted set at key with a score between min and max (including
     * elements with score equal to min or max).
     * <p>
     * <b>Time complexity:</b>
     * <p>
     * O(log(N))+O(M) with N being the number of elements in the sorted set and M the number of
     * elements removed by the operation
     *
     * @param key
     * @param start
     * @param end
     * @return Integer reply, specifically the number of elements removed.
     */
    @Override
    @Suspendable
    public final Long zremrangeByScore(byte[] key, double start, double end) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    @Override
    @Suspendable
    public final Long zremrangeByScore(byte[] key, byte[] start, byte[] end) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    /**
     * Creates a union or intersection of N sorted sets given by keys k1 through kN, and stores it at
     * dstkey. It is mandatory to provide the number of input keys N, before passing the input keys
     * and the other (optional) arguments.
     * <p>
     * As the terms imply, the {@link #zinterstore(byte[], byte[]...)} ZINTERSTORE} command requires
     * an element to be present in each of the given inputs to be inserted in the result. The {@link
     * #zunionstore(byte[], byte[]...)} command inserts all elements across all inputs.
     * <p>
     * Using the WEIGHTS option, it is possible to add weight to each input sorted set. This means
     * that the score of each element in the sorted set is first multiplied by this weight before
     * being passed to the aggregation. When this option is not given, all weights default to 1.
     * <p>
     * With the AGGREGATE option, it's possible to specify how the results of the union or
     * intersection are aggregated. This option defaults to SUM, where the score of an element is
     * summed across the inputs where it exists. When this option is set to be either MIN or MAX, the
     * resulting set will contain the minimum or maximum score of an element across the inputs where
     * it exists.
     * <p>
     * <b>Time complexity:</b> O(N) + O(M log(M)) with N being the sum of the sizes of the input
     * sorted sets, and M being the number of elements in the resulting sorted set
     *
     * @param dstkey
     * @param sets
     * @return Integer reply, specifically the number of elements in the sorted set at dstkey
     * @see #zunionstore(byte[], byte[]...)
     * @see #zunionstore(byte[], ZParams, byte[]...)
     * @see #zinterstore(byte[], byte[]...)
     * @see #zinterstore(byte[], ZParams, byte[]...)
     */
    @Override
    @Suspendable
    public final Long zunionstore(byte[] dstkey, byte[]... sets) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    /**
     * Creates a union or intersection of N sorted sets given by keys k1 through kN, and stores it at
     * dstkey. It is mandatory to provide the number of input keys N, before passing the input keys
     * and the other (optional) arguments.
     * <p>
     * As the terms imply, the {@link #zinterstore(byte[], byte[]...) ZINTERSTORE} command requires an
     * element to be present in each of the given inputs to be inserted in the result. The {@link
     * #zunionstore(byte[], byte[]...) ZUNIONSTORE} command inserts all elements across all inputs.
     * <p>
     * Using the WEIGHTS option, it is possible to add weight to each input sorted set. This means
     * that the score of each element in the sorted set is first multiplied by this weight before
     * being passed to the aggregation. When this option is not given, all weights default to 1.
     * <p>
     * With the AGGREGATE option, it's possible to specify how the results of the union or
     * intersection are aggregated. This option defaults to SUM, where the score of an element is
     * summed across the inputs where it exists. When this option is set to be either MIN or MAX, the
     * resulting set will contain the minimum or maximum score of an element across the inputs where
     * it exists.
     * <p>
     * <b>Time complexity:</b> O(N) + O(M log(M)) with N being the sum of the sizes of the input
     * sorted sets, and M being the number of elements in the resulting sorted set
     *
     * @param dstkey
     * @param params
     * @param sets
     * @return Integer reply, specifically the number of elements in the sorted set at dstkey
     * @see #zunionstore(byte[], byte[]...)
     * @see #zunionstore(byte[], ZParams, byte[]...)
     * @see #zinterstore(byte[], byte[]...)
     * @see #zinterstore(byte[], ZParams, byte[]...)
     */
    @Override
    @Suspendable
    public final Long zunionstore(byte[] dstkey, ZParams params, byte[]... sets) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    /**
     * Creates a union or intersection of N sorted sets given by keys k1 through kN, and stores it at
     * dstkey. It is mandatory to provide the number of input keys N, before passing the input keys
     * and the other (optional) arguments.
     * <p>
     * As the terms imply, the {@link #zinterstore(byte[], byte[]...) ZINTERSTORE} command requires an
     * element to be present in each of the given inputs to be inserted in the result. The {@link
     * #zunionstore(byte[], byte[]...) ZUNIONSTORE} command inserts all elements across all inputs.
     * <p>
     * Using the WEIGHTS option, it is possible to add weight to each input sorted set. This means
     * that the score of each element in the sorted set is first multiplied by this weight before
     * being passed to the aggregation. When this option is not given, all weights default to 1.
     * <p>
     * With the AGGREGATE option, it's possible to specify how the results of the union or
     * intersection are aggregated. This option defaults to SUM, where the score of an element is
     * summed across the inputs where it exists. When this option is set to be either MIN or MAX, the
     * resulting set will contain the minimum or maximum score of an element across the inputs where
     * it exists.
     * <p>
     * <b>Time complexity:</b> O(N) + O(M log(M)) with N being the sum of the sizes of the input
     * sorted sets, and M being the number of elements in the resulting sorted set
     *
     * @param dstkey
     * @param sets
     * @return Integer reply, specifically the number of elements in the sorted set at dstkey
     * @see #zunionstore(byte[], byte[]...)
     * @see #zunionstore(byte[], ZParams, byte[]...)
     * @see #zinterstore(byte[], byte[]...)
     * @see #zinterstore(byte[], ZParams, byte[]...)
     */
    @Override
    @Suspendable
    public final Long zinterstore(byte[] dstkey, byte[]... sets) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    /**
     * Creates a union or intersection of N sorted sets given by keys k1 through kN, and stores it at
     * dstkey. It is mandatory to provide the number of input keys N, before passing the input keys
     * and the other (optional) arguments.
     * <p>
     * As the terms imply, the {@link #zinterstore(byte[], byte[]...) ZINTERSTORE} command requires an
     * element to be present in each of the given inputs to be inserted in the result. The {@link
     * #zunionstore(byte[], byte[]...) ZUNIONSTORE} command inserts all elements across all inputs.
     * <p>
     * Using the WEIGHTS option, it is possible to add weight to each input sorted set. This means
     * that the score of each element in the sorted set is first multiplied by this weight before
     * being passed to the aggregation. When this option is not given, all weights default to 1.
     * <p>
     * With the AGGREGATE option, it's possible to specify how the results of the union or
     * intersection are aggregated. This option defaults to SUM, where the score of an element is
     * summed across the inputs where it exists. When this option is set to be either MIN or MAX, the
     * resulting set will contain the minimum or maximum score of an element across the inputs where
     * it exists.
     * <p>
     * <b>Time complexity:</b> O(N) + O(M log(M)) with N being the sum of the sizes of the input
     * sorted sets, and M being the number of elements in the resulting sorted set
     *
     * @param dstkey
     * @param params
     * @param sets
     * @return Integer reply, specifically the number of elements in the sorted set at dstkey
     * @see #zunionstore(byte[], byte[]...)
     * @see #zunionstore(byte[], ZParams, byte[]...)
     * @see #zinterstore(byte[], byte[]...)
     * @see #zinterstore(byte[], ZParams, byte[]...)
     */
    @Override
    @Suspendable
    public final Long zinterstore(byte[] dstkey, ZParams params, byte[]... sets) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    @Override
    @Suspendable
    public final Long zlexcount(byte[] key, byte[] min, byte[] max) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    @Override
    @Suspendable
    public final Set<byte[]> zrangeByLex(byte[] key, byte[] min, byte[] max) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    @Override
    @Suspendable
    public final Set<byte[]> zrangeByLex(byte[] key, byte[] min, byte[] max, int offset, int count) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    @Override
    @Suspendable
    public final Set<byte[]> zrevrangeByLex(byte[] key, byte[] max, byte[] min) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    @Override
    @Suspendable
    public final Set<byte[]> zrevrangeByLex(byte[] key, byte[] max, byte[] min, int offset, int count) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    @Override
    @Suspendable
    public final Long zremrangeByLex(byte[] key, byte[] min, byte[] max) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    /**
     * Change the replication settings.
     * <p>
     * The SLAVEOF command can change the replication settings of a slave on the fly. If a Redis
     * server is arleady acting as slave, the command SLAVEOF NO ONE will turn off the replicaiton
     * turning the Redis server into a MASTER. In the proper form SLAVEOF hostname port will make the
     * server a slave of the specific server listening at the specified hostname and port.
     * <p>
     * If a server is already a slave of some master, SLAVEOF hostname port will stop the replication
     * against the old server and start the synchrnonization against the new one discarding the old
     * dataset.
     * <p>
     * The form SLAVEOF no one will stop replication turning the server into a MASTER but will not
     * discard the replication. So if the old master stop working it is possible to turn the slave
     * into a master and set the application to use the new master in read/write. Later when the other
     * Redis server will be fixed it can be configured in order to work as slave.
     * <p>
     *
     * @param host
     * @param port
     * @return Status code reply
     */
    @Override
    @Suspendable
    public final String slaveof(String host, int port) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    @Override
    @Suspendable
    public final String slaveofNoOne() {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    /**
     * Retrieve the configuration of a running Redis server. Not all the configuration parameters are
     * supported.
     * <p>
     * CONFIG GET returns the current configuration parameters. This sub command only accepts a single
     * argument, that is glob style pattern. All the configuration parameters matching this parameter
     * are reported as a list of key-value pairs.
     * <p>
     * <b>Example:</b>
     * <p>
     * <pre>
     * $ redis-cli config get '*'
     * 1. "dbfilename"
     * 2. "dump.rdb"
     * 3. "requirepass"
     * 4. (nil)
     * 5. "masterauth"
     * 6. (nil)
     * 7. "maxmemory"
     * 8. "0\n"
     * 9. "appendfsync"
     * 10. "everysec"
     * 11. "save"
     * 12. "3600 1 300 100 60 10000"
     *
     * $ redis-cli config get 'm*'
     * 1. "masterauth"
     * 2. (nil)
     * 3. "maxmemory"
     * 4. "0\n"
     * </pre>
     *
     * @param pattern
     * @return Bulk reply.
     */
    @Override
    @Suspendable
    public final List<byte[]> configGet(byte[] pattern) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    /**
     * Reset the stats returned by INFO
     *
     * @return
     */
    @Override
    @Suspendable
    public final String configResetStat() {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    /**
     * Alter the configuration of a running Redis server. Not all the configuration parameters are
     * supported.
     * <p>
     * The list of configuration parameters supported by CONFIG SET can be obtained issuing a
     * {@link #configGet(byte[]) CONFIG GET *} command.
     * <p>
     * The configuration set using CONFIG SET is immediately loaded by the Redis server that will
     * start acting as specified starting from the next command.
     * <p>
     * <b>Parameters value format</b>
     * <p>
     * The value of the configuration parameter is the same as the one of the same parameter in the
     * Redis configuration file, with the following exceptions:
     * <p>
     * <ul>
     * <li>The save paramter is a list of space-separated integers. Every pair of integers specify the
     * time and number of changes limit to trigger a save. For instance the command CONFIG SET save
     * "3600 10 60 10000" will configure the server to issue a background saving of the RDB file every
     * 3600 seconds if there are at least 10 changes in the dataset, and every 60 seconds if there are
     * at least 10000 changes. To completely disable automatic snapshots just set the parameter as an
     * empty string.
     * <li>All the integer parameters representing memory are returned and accepted only using bytes
     * as unit.
     * </ul>
     *
     * @param parameter
     * @param value
     * @return Status code reply
     */
    @Override
    @Suspendable
    public final byte[] configSet(byte[] parameter, byte[] value) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    @Override
    @Suspendable
    public final Long strlen(byte[] key) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    @Override
    @Suspendable
    public final Long lpushx(byte[] key, byte[]... string) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    /**
     * Undo a {@link #expire(byte[], int) expire} at turning the expire key into a normal key.
     * <p>
     * Time complexity: O(1)
     *
     * @param key
     * @return Integer reply, specifically: 1: the key is now persist. 0: the key is not persist (only
     * happens when key not set).
     */
    @Override
    @Suspendable
    public final Long persist(byte[] key) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    @Override
    @Suspendable
    public final Long rpushx(byte[] key, byte[]... string) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    @Override
    public byte[] echo(byte[] string) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    @Override
    @Suspendable
    public final Long linsert(byte[] key, BinaryClient.LIST_POSITION where, byte[] pivot, byte[] value) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    @Override
    @Suspendable
    public final String debug(DebugParams params) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    @Override
    @Suspendable
    public final Client getClient() {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    /**
     * Pop a value from a list, push it to another list and return it; or block until one is available
     *
     * @param source
     * @param destination
     * @param timeout
     * @return the element
     */
    @Override
    @Suspendable
    public final byte[] brpoplpush(byte[] source, byte[] destination, int timeout) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    /**
     * Sets or clears the bit at offset in the string value stored at key
     *
     * @param key
     * @param offset
     * @param value
     * @return
     */
    @Override
    @Suspendable
    public final Boolean setbit(byte[] key, long offset, boolean value) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    @Override
    @Suspendable
    public final Boolean setbit(byte[] key, long offset, byte[] value) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    /**
     * Returns the bit value at offset in the string value stored at key
     *
     * @param key
     * @param offset
     * @return
     */
    @Override
    @Suspendable
    public final Boolean getbit(byte[] key, long offset) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    @Override
    @Suspendable
    public final Long bitpos(byte[] key, boolean value) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    @Override
    @Suspendable
    public final Long bitpos(byte[] key, boolean value, BitPosParams params) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    @Override
    @Suspendable
    public final Long setrange(byte[] key, long offset, byte[] value) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    @Override
    @Suspendable
    public final byte[] getrange(byte[] key, long startOffset, long endOffset) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    @Override
    @Suspendable
    public final List<byte[]> slowlogGetBinary() {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    @Override
    @Suspendable
    public final List<byte[]> slowlogGetBinary(long entries) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    @Override
    @Suspendable
    public final Long objectRefcount(byte[] key) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    @Override
    @Suspendable
    public final byte[] objectEncoding(byte[] key) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    @Override
    @Suspendable
    public final Long objectIdletime(byte[] key) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    @Override
    @Suspendable
    public final Long bitcount(byte[] key) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    @Override
    @Suspendable
    public final Long bitcount(byte[] key, long start, long end) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    @Override
    @Suspendable
    public final Long bitop(BitOP op, byte[] destKey, byte[]... srcKeys) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    @Override
    @Suspendable
    public final byte[] dump(byte[] key) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    @Override
    @Suspendable
    public final String restore(byte[] key, int ttl, byte[] serializedValue) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    @Override
    @Suspendable
    public final Long pexpire(byte[] key, int milliseconds) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    @Override
    @Suspendable
    public final Long pexpire(byte[] key, long milliseconds) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    @Override
    @Suspendable
    public final Long pexpireAt(byte[] key, long millisecondsTimestamp) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    @Override
    @Suspendable
    public final Long pttl(byte[] key) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    @Override
    @Suspendable
    public final String psetex(byte[] key, int milliseconds, byte[] value) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    /**
     * PSETEX works exactly like {@link #setex(byte[], int, byte[])} with the sole difference that the
     * expire time is specified in milliseconds instead of seconds. Time complexity: O(1)
     *
     * @param key
     * @param milliseconds
     * @param value
     * @return Status code reply
     */
    @Override
    @Suspendable
    public final String psetex(byte[] key, long milliseconds, byte[] value) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    @Override
    @Suspendable
    public final String set(byte[] key, byte[] value, byte[] nxxx) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    @Override
    @Suspendable
    public final String set(byte[] key, byte[] value, byte[] nxxx, byte[] expx, int time) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
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

    @Override
    @Deprecated
    @Suspendable
    @SuppressWarnings("deprecation")
    public final List<String> blpop(String arg) {
        throw new UnsupportedOperationException("Unusable, will be removed by Jedis");
    }

    @Override
    @Deprecated
    @Suspendable
    @SuppressWarnings("deprecation")
    public final List<String> brpop(String arg) {
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

    /////////////////////////// MEANINGLESS
    @Override
    public void setDataSource(Pool<redis.clients.jedis.Jedis> jedisPool) {
        throw new UnsupportedOperationException();
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
            for (byte[] ba : channels) {
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
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        } catch (SuspendExecution e) {
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

    private static Map<String, String> kvArrayToMap(String... keysValues) {
        if (keysValues == null)
            return null;

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

    private static Set<Tuple> toTupleSet(List<ScoredValue<String>> l) {
        if (l == null)
            return null;

        return l.stream().map(e -> new Tuple(e.value, e.score)).collect(Collectors.toCollection(HashSet::new));
    }

    private static SortArgs toSortArgs(SortingParams sp) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    private static List<String> toList(KeyValue<String, String> kv) {
        if (kv == null)
            return null;

        final List<String> ret = new ArrayList<>(2);
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
}
