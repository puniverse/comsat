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
import com.lambdaworks.redis.*;
import redis.clients.jedis.*;
import redis.clients.util.Pool;
import redis.clients.jedis.params.geo.GeoRadiusParam;
import redis.clients.jedis.params.sortedset.ZAddParams;
import redis.clients.jedis.params.sortedset.ZIncrByParams;
import redis.clients.util.Slowlog;

import java.net.URI;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import static co.paralleluniverse.fibers.redis.Utils.validateFiberPubSub;

/**
 * @author circlespainter
 */
public final class Jedis extends BinaryJedis {
    public Jedis(Callable<RedisClient> cp) {
        super(cp);
    }

    public Jedis() {
        super();
    }

    public Jedis(String host) {
        super(host);
    }

    public Jedis(String host, int port) {
        super(host, port);
    }

    public Jedis(String host, int port, int timeout) {
        super(host, port, timeout);
    }

    public Jedis(URI uri) {
        super(uri);
    }

    public Jedis(URI uri, int timeout) {
        super(uri, timeout);
    }

    @Override
    @Suspendable
    public final String set(String key, String value) {
        return await(() -> stringCommands.set(key, value));
    }

    @Override
    @Suspendable
    public final String set(String key, String value, String nxxx, String expx, long time) {
        return await(() -> stringCommands.set(key, value, Utils.toSetArgs(nxxx, expx, time)));
    }

    @Override
    @Suspendable
    public final String get(String key) {
        return await(() -> stringCommands.get(key));
    }

    @Override
    @Suspendable
    public final Long exists(String... keys) {
        return await(() -> stringCommands.exists(keys));
    }

    @Override
    @Deprecated
    @Suspendable
    public final Boolean exists(String key) {
        //noinspection deprecation
        return await(() -> stringCommands.exists(key));
    }

    @Override
    @Suspendable
    public final Long del(String... keys) {
        return await(() -> stringCommands.del(keys));
    }

    @Override
    @Suspendable
    public final Long del(String key) {
        return await(() -> stringCommands.del(key));
    }

    @Override
    @Suspendable
    public final String type(String key) {
        return await(() -> stringCommands.type(key));
    }

    @Override
    @Suspendable
    public final Set<String> keys(String pattern) {
        return new HashSet<>(await(() -> stringCommands.keys(pattern)));
    }

    @Override
    @Suspendable
    public final String randomKey() {
        return await(() -> stringCommands.randomkey());
    }

    @Override
    @Suspendable
    public final String rename(String oldKey, String newKey) {
        return await(() -> stringCommands.rename(oldKey, newKey));
    }

    @Override
    @Suspendable
    public final Long renamenx(String oldKey, String newKey) {
        return await(() -> stringCommands.renamenx(oldKey, newKey)) ? 1L : 0L;
    }

    @Override
    @Suspendable
    public final Long expire(String key, int seconds) {
        return await(() -> stringCommands.expire(key, seconds)) ? 1L : 0L;
    }

    @Override
    @Suspendable
    public final Long expireAt(String key, long unixTime) {
        return await(() -> stringCommands.expireat(key, unixTime)) ? 1L : 0L;
    }

    @Override
    @Suspendable
    public final Long ttl(String key) {
        return await(() -> stringCommands.ttl(key));
    }

    @Override
    @Suspendable
    public final Long move(String key, int dbIndex) {
        return await(() -> stringCommands.move(key, dbIndex)) ? 1L : 0L;
    }

    @Override
    @Suspendable
    public final String getSet(String key, String value) {
        return await(() -> stringCommands.getset(key, value));
    }

    @Override
    @Suspendable
    public final List<String> mget(String... keys) {
        return await(() -> stringCommands.mget(keys));
    }

    @Override
    @Suspendable
    public final Long setnx(String key, String value) {
        return await(() -> stringCommands.setnx(key, value)) ? 1L : 0L;
    }

    @Override
    @Suspendable
    public final String setex(String key, int seconds, String value) {
        return await(() -> stringCommands.setex(key, seconds, value));
    }

    @Override
    @Suspendable
    public final String mset(String... keysValues) {
        return await(() -> stringCommands.mset(Utils.kvArrayToMap(keysValues)));
    }

    @Override
    @Suspendable
    public final Long msetnx(String... keysValues) {
        return await(() -> stringCommands.msetnx(Utils.kvArrayToMap(keysValues))) ? 1L : 0L;
    }

    @Override
    @Suspendable
    public final Long decrBy(String key, long integer) {
        return await(() -> stringCommands.decrby(key, integer));
    }

    @Override
    @Suspendable
    public final Long decr(String key) {
        return await(() -> stringCommands.decr(key));
    }

    @Override
    @Suspendable
    public final Long incrBy(String key, long integer) {
        return await(() -> stringCommands.incrby(key, integer));
    }

    @Override
    @Suspendable
    public final Double incrByFloat(String key, double value) {
        return await(() -> stringCommands.incrbyfloat(key, value));
    }

    @Override
    @Suspendable
    public final Long incr(String key) {
        return await(() -> stringCommands.incr(key));
    }

    @Override
    @Suspendable
    public final Long append(String key, String value) {
        return await(() -> stringCommands.append(key, value));
    }

    @Override
    @Suspendable
    public final String substr(String key, int start, int end) {
        return await(() -> stringCommands.getrange(key, start, end));
    }

    @Override
    @Suspendable
    public final Long hset(String key, String field, String value) {
        return await(() -> stringCommands.hset(key, field, value)) ? 1L : 0L;
    }

    @Override
    @Suspendable
    public final String hget(String key, String field) {
        return await(() -> stringCommands.hget(key, field));
    }

    @Override
    @Suspendable
    public final Long hsetnx(String key, String field, String value) {
        return await(() -> stringCommands.hsetnx(key, field, value)) ? 1L : 0L;
    }

    @Override
    @Suspendable
    public final String hmset(String key, Map<String, String> hash) {
        return await(() -> stringCommands.hmset(key, hash));
    }

    @Override
    @Suspendable
    public final List<String> hmget(String key, String... fields) {
        return await(() -> stringCommands.hmget(key, fields));
    }

    @Override
    @Suspendable
    public final Long hincrBy(String key, String field, long value) {
        return await(() -> stringCommands.hincrby(key, field, value));
    }

    @Override
    @Suspendable
    public final Double hincrByFloat(String key, String field, double value) {
        return await(() -> stringCommands.hincrbyfloat(key, field, value));
    }

    @Override
    @Suspendable
    public final Boolean hexists(String key, String field) {
        return await(() -> stringCommands.hexists(key, field));
    }

    @Override
    @Suspendable
    public final Long hdel(String key, String... fields) {
        return await(() -> stringCommands.hdel(key, fields));
    }

    @Override
    @Suspendable
    public final Long hlen(String key) {
        return await(() -> stringCommands.hlen(key));
    }

    @Override
    @Suspendable
    public final Set<String> hkeys(String key) {
        return new HashSet<>(await(() -> stringCommands.hkeys(key)));
    }

    @Override
    @Suspendable
    public final List<String> hvals(String key) {
        return await(() -> stringCommands.hvals(key));
    }

    @Override
    @Suspendable
    public final Map<String, String> hgetAll(String key) {
        return await(() -> stringCommands.hgetall(key));
    }

    @Override
    @Suspendable
    public final Long rpush(String key, String... strings) {
        return await(() -> stringCommands.rpush(key, strings));
    }

    @Override
    @Suspendable
    public final Long lpush(String key, String... strings) {
        return await(() -> stringCommands.lpush(key, strings));
    }

    @Override
    @Suspendable
    public final Long llen(String key) {
        return await(() -> stringCommands.llen(key));
    }

    @Override
    @Suspendable
    public final List<String> lrange(String key, long start, long end) {
        return await(() -> stringCommands.lrange(key, start, end));
    }

    @Override
    @Suspendable
    public final String ltrim(String key, long start, long end) {
        return await(() -> stringCommands.ltrim(key, start, end));
    }

    @Override
    @Suspendable
    public final String lindex(String key, long index) {
        return await(() -> stringCommands.lindex(key, index));
    }

    @Override
    @Suspendable
    public final String lset(String key, long index, String value) {
        return await(() -> stringCommands.lset(key, index, value));
    }

    @Override
    @Suspendable
    public final Long lrem(String key, long count, String value) {
        return await(() -> stringCommands.lrem(key, count, value));
    }

    @Override
    @Suspendable
    public final String lpop(String key) {
        return await(() -> stringCommands.lpop(key));
    }

    @Override
    @Suspendable
    public final String rpop(String key) {
        return await(() -> stringCommands.rpop(key));
    }

    @Override
    @Suspendable
    public final String rpoplpush(String srcKey, String dstKey) {
        return await(() -> stringCommands.rpoplpush(srcKey, dstKey));
    }

    @Override
    @Suspendable
    public final Long sadd(String key, String... members) {
        return await(() -> stringCommands.sadd(key, members));
    }

    @Override
    @Suspendable
    public final Set<String> smembers(String key) {
        return await(() -> stringCommands.smembers(key));
    }

    @Override
    @Suspendable
    public final Long srem(String key, String... members) {
        return await(() -> stringCommands.srem(key, members));
    }

    @Override
    @Suspendable
    public final String spop(String key) {
        return await(() -> stringCommands.spop(key));
    }

    @Override
    @Suspendable
    public final Long smove(String srcKey, String dstKey, String member) {
        return await(() -> stringCommands.smove(srcKey, dstKey, member)) ? 1L : 0L;
    }

    @Override
    @Suspendable
    public final Long scard(String key) {
        return await(() -> stringCommands.scard(key));
    }

    @Override
    @Suspendable
    public final Boolean sismember(String key, String member) {
        return await(() -> stringCommands.sismember(key, member));
    }

    @Override
    @Suspendable
    public final Set<String> sinter(String... keys) {
        return await(() -> stringCommands.sinter(keys));
    }

    @Override
    @Suspendable
    public final Long sinterstore(String dstKey, String... keys) {
        return await(() -> stringCommands.sinterstore(dstKey, keys));
    }

    @Override
    @Suspendable
    public final Set<String> sunion(String... keys) {
        return await(() -> stringCommands.sunion(keys));
    }

    @Override
    @Suspendable
    public final Long sunionstore(String dstKey, String... keys) {
        return await(() -> stringCommands.sunionstore(dstKey, keys));
    }

    @Override
    @Suspendable
    public final Set<String> sdiff(String... keys) {
        return await(() -> stringCommands.sdiff(keys));
    }

    @Override
    @Suspendable
    public final Long sdiffstore(String dstKey, String... keys) {
        return await(() -> stringCommands.sdiffstore(dstKey, keys));
    }

    @Override
    @Suspendable
    public final String srandmember(String key) {
        return await(() -> stringCommands.srandmember(key));
    }

    @Override
    @Suspendable
    public final List<String> srandmember(String key, int count) {
        return new ArrayList<>(await(() -> stringCommands.srandmember(key, count)));
    }

    @Override
    @Suspendable
    public final Long zadd(String key, double score, String member) {
        return await(() -> stringCommands.zadd(key, score, member));
    }

    @Override
    @Suspendable
    public final Long zadd(String key, double score, String member, ZAddParams params) {
        return await(() -> stringCommands.zadd(key, Utils.toZAddArgs(params), score, member));
    }

    @Override
    @Suspendable
    public final Long zadd(String key, Map<String, Double> scoreMembers) {
        return await(() -> stringCommands.zadd(key, Utils.toObjectScoreValueArray(scoreMembers)));
    }

    @Override
    @Suspendable
    public final Long zadd(String key, Map<String, Double> scoreMembers, ZAddParams params) {
        return await(() -> stringCommands.zadd(key, Utils.toZAddArgs(params), Utils.toObjectScoreValueArray(scoreMembers)));
    }

    @Override
    @Suspendable
    public final Set<String> zrange(String key, long start, long end) {
        return new HashSet<>(await(() -> stringCommands.zrange(key, start, end)));
    }

    @Override
    @Suspendable
    public final Long zrem(String key, String... members) {
        return await(() -> stringCommands.zrem(key, members));
    }

    @Override
    @Suspendable
    public final Double zincrby(String key, double score, String member) {
        return await(() -> stringCommands.zincrby(key, score, member));
    }

    @Override
    @Suspendable
    public final Long zrank(String key, String member) {
        return await(() -> stringCommands.zrank(key, member));
    }

    @Override
    @Suspendable
    public final Long zrevrank(String key, String member) {
        return await(() -> stringCommands.zrevrank(key, member));
    }

    @Override
    @Suspendable
    public final Set<String> zrevrange(String key, long start, long end) {
        return new HashSet<>(await(() -> stringCommands.zrevrange(key, start, end)));
    }

    @Override
    @Suspendable
    public final Set<Tuple> zrangeWithScores(String key, long start, long end) {
        return Utils.toTupleSet((List) await(() -> stringCommands.zrangeWithScores(key, start, end)));
    }

    @Override
    @Suspendable
    public final Set<Tuple> zrevrangeWithScores(String key, long start, long end) {
        return Utils.toTupleSet((List) await(() -> stringCommands.zrevrangeWithScores(key, start, end)));
    }

    @Override
    @Suspendable
    public final Long zcard(String key) {
        return await(() -> stringCommands.zcard(key));
    }

    @Override
    @Suspendable
    public final Double zscore(String key, String member) {
        return await(() -> stringCommands.zscore(key, member));
    }

    @Override
    @Suspendable
    public final String watch(String... keys) {
        return await(() -> stringCommands.watch(keys));
    }

    @Override
    @Suspendable
    public final List<String> sort(String key) {
        return await(() -> stringCommands.sort(key));
    }

    @Override
    @Suspendable
    public final List<String> sort(String key, SortingParams sortingParameters) {
        return await(() -> stringCommands.sort(key, Utils.toSortArgs(sortingParameters)));
    }

    @Override
    @Suspendable
    public final List<String> blpop(int timeout, String... keys) {
        return Utils.toList(await(() -> stringCommands.blpop(timeout, keys)));
    }

    @Override
    @Suspendable
    public final List<String> blpop(String... args) {
        return Utils.toList(await(() -> stringCommands.blpop(0 /* Indefinitely */, args)));
    }

    @Override
    @Suspendable
    public final List<String> brpop(String... args) {
        return Utils.toList(await(() -> stringCommands.brpop(0 /* Indefinitely */, args)));
    }

    @Override
    @Suspendable
    public final Long sort(String key, SortingParams sortingParameters, String dstKey) {
        return await(() -> stringCommands.sortStore(key, Utils.toSortArgs(sortingParameters), dstKey));
    }

    @Override
    @Suspendable
    public final Long sort(String key, String dstKey) {
        return await(() -> stringCommands.sortStore(key, new SortArgs(), dstKey));
    }

    @Override
    @Suspendable
    public final List<String> brpop(int timeout, String... keys) {
        return Utils.toList(await(() -> stringCommands.brpop(timeout, keys)));
    }

    @Override
    @Suspendable
    public final Long zcount(String key, double min, double max) {
        return await(() -> stringCommands.zcount(key, min, max));
    }

    @Override
    @Suspendable
    public final Long zcount(String key, String min, String max) {
        return await(() -> stringCommands.zcount(key, min, max));
    }

    @Override
    @Suspendable
    public final Set<String> zrangeByScore(String key, double min, double max) {
        return new HashSet<>(await(() -> stringCommands.zrangebyscore(key, min, max)));
    }

    @Override
    @Suspendable
    public final Set<String> zrangeByScore(String key, String min, String max) {
        return new HashSet<>(await(() -> stringCommands.zrangebyscore(key, min, max)));
    }

    @Override
    @Suspendable
    public final Set<String> zrangeByScore(String key, double min, double max, int offset, int count) {
        return new HashSet<>(await(() -> stringCommands.zrangebyscore(key, min, max, offset, count)));
    }

    @Override
    @Suspendable
    public final Set<String> zrangeByScore(String key, String min, String max, int offset, int count) {
        return new HashSet<>(await(() -> stringCommands.zrangebyscore(key, min, max, offset, count)));
    }

    @Override
    @Suspendable
    public final Set<Tuple> zrangeByScoreWithScores(String key, double min, double max) {
        return Utils.toTupleSet((List) await(() -> stringCommands.zrangebyscoreWithScores(key, min, max)));
    }

    @Override
    @Suspendable
    public final Set<Tuple> zrangeByScoreWithScores(String key, String min, String max) {
        return Utils.toTupleSet((List) await(() -> stringCommands.zrangebyscoreWithScores(key, min, max)));
    }

    @Override
    @Suspendable
    public final Set<Tuple> zrangeByScoreWithScores(String key, double min, double max, int offset, int count) {
        return Utils.toTupleSet((List) await(() -> stringCommands.zrangebyscoreWithScores(key, min, max, offset, count)));
    }

    @Override
    @Suspendable
    public final Set<Tuple> zrangeByScoreWithScores(String key, String min, String max, int offset, int count) {
        return Utils.toTupleSet((List) await(() -> stringCommands.zrangebyscoreWithScores(key, min, max, offset, count)));
    }

    @Override
    @Suspendable
    public final Set<String> zrevrangeByScore(String key, double max, double min) {
        return new HashSet<>(await(() -> stringCommands.zrevrangebyscore(key, min, max)));
    }

    @Override
    @Suspendable
    public final Set<String> zrevrangeByScore(String key, String max, String min) {
        return new HashSet<>(await(() -> stringCommands.zrevrangebyscore(key, min, max)));
    }

    @Override
    @Suspendable
    public final Set<String> zrevrangeByScore(String key, double max, double min, int offset, int count) {
        return new HashSet<>(await(() -> stringCommands.zrevrangebyscore(key, min, max)));
    }

    @Override
    @Suspendable
    public final Set<Tuple> zrevrangeByScoreWithScores(String key, double max, double min) {
        return Utils.toTupleSet((List) await(() -> stringCommands.zrevrangebyscoreWithScores(key, min, max)));
    }

    @Override
    @Suspendable
    public final Set<Tuple> zrevrangeByScoreWithScores(String key, double max, double min, int offset, int count) {
        return Utils.toTupleSet((List) await(() -> stringCommands.zrevrangebyscoreWithScores(key, min, max, offset, count)));
    }

    @Override
    @Suspendable
    public final Set<Tuple> zrevrangeByScoreWithScores(String key, String max, String min, int offset, int count) {
        return Utils.toTupleSet((List) await(() -> stringCommands.zrevrangebyscoreWithScores(key, min, max, offset, count)));
    }

    @Override
    @Suspendable
    public final Set<String> zrevrangeByScore(String key, String max, String min, int offset, int count) {
        return new HashSet<>(await(() -> stringCommands.zrevrangebyscore(key, min, max, offset, count)));
    }

    @Override
    @Suspendable
    public final Set<Tuple> zrevrangeByScoreWithScores(String key, String max, String min) {
        return Utils.toTupleSet((List) await(() -> stringCommands.zrevrangebyscoreWithScores(key, min, max)));
    }

    @Override
    @Suspendable
    public final Long zremrangeByRank(String key, long start, long end) {
        return await(() -> stringCommands.zremrangebyrank(key, start, end));
    }

    @Override
    @Suspendable
    public final Long zremrangeByScore(String key, double start, double end) {
        return await(() -> stringCommands.zremrangebyscore(key, start, end));
    }

    @Override
    @Suspendable
    public final Long zremrangeByScore(String key, String start, String end) {
        return await(() -> stringCommands.zremrangebyscore(key, start, end));
    }

    @Override
    @Suspendable
    public final Long zunionstore(String dstKey, String... sets) {
        return await(() -> stringCommands.zunionstore(dstKey, sets));
    }

    @Override
    @Suspendable
    public final Long zunionstore(String dstKey, ZParams params, String... sets) {
        return await(() -> stringCommands.zunionstore(dstKey, Utils.toZStoreArgs(params), sets));
    }

    @Override
    @Suspendable
    public final Long zinterstore(String dstKey, String... sets) {
        return await(() -> stringCommands.zinterstore(dstKey, sets));
    }

    @Override
    @Suspendable
    public final Long zinterstore(String dstKey, ZParams params, String... sets) {
        return await(() -> stringCommands.zinterstore(dstKey, Utils.toZStoreArgs(params), sets));
    }

    @Override
    @Suspendable
    public final Long zlexcount(String key, String min, String max) {
        return await(() -> stringCommands.zlexcount(key, min, max));
    }

    @Override
    @Suspendable
    public final Set<String> zrangeByLex(String key, String min, String max) {
        return new HashSet<>(await(() -> stringCommands.zrangebylex(key, min, max)));
    }

    @Override
    @Suspendable
    public final Set<String> zrangeByLex(String key, String min, String max, int offset, int count) {
        return new HashSet<>(await(() -> stringCommands.zrangebylex(key, min, max, offset, count)));
    }

    @Override
    @Suspendable
    public final Set<String> zrevrangeByLex(String key, String max, String min) {
        return new HashSet<>(await(() -> stringCommands.zrangebylex(key, min, max)));
    }

    @Override
    @Suspendable
    public final Set<String> zrevrangeByLex(String key, String max, String min, int offset, int count) {
        return new HashSet<>(await(() -> stringCommands.zrangebylex(key, min, max, offset, count)));
    }

    @Override
    @Suspendable
    public final Long zremrangeByLex(String key, String min, String max) {
        return await(() -> stringCommands.zremrangebylex(key, min, max));
    }

    @Override
    @Suspendable
    public final Long strlen(String key) {
        return await(() -> stringCommands.strlen(key));
    }

    @Override
    @Suspendable
    public final Long persist(String key) {
        return await(() -> stringCommands.persist(key)) ? 1L : 0L;
    }

    @Override
    @Suspendable
    public final String echo(String string) {
        return await(() -> stringCommands.echo(string));
    }

    @Override
    @Suspendable
    public final Long linsert(String key, BinaryClient.LIST_POSITION where, String pivot, String value) {
        return await(() -> stringCommands.linsert(key, BinaryClient.LIST_POSITION.BEFORE.equals(where), pivot, value));
    }

    @Override
    @Suspendable
    public final String brpoplpush(String source, String destination, int timeout) {
        return await(() -> stringCommands.brpoplpush(timeout, source, destination));
    }

    @Override
    @Suspendable
    public final Boolean setbit(String key, long offset, boolean value) {
        return await(() -> stringCommands.setbit(key, offset, value ? 1 : 0)) > 0;
    }

    @Override
    @Suspendable
    public final Boolean setbit(String key, long offset, String value) {
        return await(() -> stringCommands.setbit(key, offset, Boolean.parseBoolean(value) ? 1 : 0)) > 0;
    }

    @Override
    @Suspendable
    public final Boolean getbit(String key, long offset) {
        return await(() -> stringCommands.getbit(key, offset)) > 0;
    }

    @Override
    @Suspendable
    public final Long setrange(String key, long offset, String value) {
        return await(() -> stringCommands.setrange(key, offset, value));
    }

    @Override
    @Suspendable
    public final String getrange(String key, long startOffset, long endOffset) {
        return await(() -> stringCommands.getrange(key, startOffset, endOffset));
    }

    @Override
    @Suspendable
    public final Long bitpos(String key, boolean value) {
        return await(() -> stringCommands.bitpos(key, value));
    }

    @Override
    @Suspendable
    public final Long bitpos(String key, boolean value, BitPosParams params) {
        return await(() -> stringCommands.bitpos(key, value, Utils.getStart(params), Utils.getEnd(params)));
    }

    @Override
    @Suspendable
    public final List<String> configGet(String pattern) {
        return await(() -> stringCommands.configGet(pattern));
    }

    @Override
    @Suspendable
    public final String configSet(String parameter, String value) {
        return await(() -> stringCommands.configSet(parameter, value));
    }

    @Override
    @Suspendable
    public final List<Slowlog> slowlogGet() {
        return Slowlog.from(await(() -> stringCommands.slowlogGet()));
    }

    @Override
    @Suspendable
    public final List<Slowlog> slowlogGet(long entries) {
        return Slowlog.from(await(() -> stringCommands.slowlogGet(Utils.validateInt(entries))));
    }

    @Override
    @Suspendable
    public final Long objectRefcount(String string) {
        return await(() -> stringCommands.objectRefcount(string));
    }

    @Override
    @Suspendable
    public final String objectEncoding(String string) {
        return await(() -> stringCommands.objectEncoding(string));
    }

    @Override
    @Suspendable
    public final Long objectIdletime(String string) {
        return await(() -> stringCommands.objectIdletime(string));
    }

    @Override
    @Suspendable
    public final Long bitcount(String key) {
        return await(() -> stringCommands.bitcount(key));
    }

    @Override
    @Suspendable
    public final Long bitcount(String key, long start, long end) {
        return await(() -> stringCommands.bitcount(key, start, end));
    }

    @Override
    @Suspendable
    public final Long bitop(BitOP op, String destKey, String... srcKeys) {
        final Long res;
        switch (op) {
            case AND:
                res = await(() -> stringCommands.bitopAnd(destKey, srcKeys));
                break;
            case OR:
                res = await(() -> stringCommands.bitopOr(destKey, srcKeys));
                break;
            case XOR:
                res = await(() -> stringCommands.bitopXor(destKey, srcKeys));
                break;
            case NOT:
                if (srcKeys == null || srcKeys.length != 1)
                    throw new IllegalArgumentException("'not' requires exactly one argument");
                res = await(() -> stringCommands.bitopNot(destKey, srcKeys[0]));
                break;
            default:
                throw new IllegalArgumentException("Unsupported operation: " + op);
        }
        return res;
    }

    @Override
    @Suspendable
    public final byte[] dump(String key) {
        return await(() -> stringCommands.dump(key));
    }

    @Override
    @Suspendable
    public final String restore(String key, int ttl, byte[] serializedValue) {
        return await(() -> stringCommands.restore(key, ttl, serializedValue));
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
        return await(() -> stringCommands.pexpire(key, milliseconds)) ? 1L : 0;
    }

    @Override
    @Suspendable
    public final Long pexpireAt(String key, long millisecondsTimestamp) {
        return await(() -> stringCommands.pexpireat(key, millisecondsTimestamp)) ? 1L : 0;
    }

    @Override
    @Suspendable
    public final Long pttl(String key) {
        return await(() -> stringCommands.pttl(key));
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
        return await(() -> stringCommands.psetex(key, milliseconds, value));
    }

    @Override
    @Suspendable
    public final String set(String key, String value, String nxxx) {
        return await(() -> stringCommands.set(key, value, Utils.toSetArgs(nxxx)));
    }

    @Override
    @Suspendable
    public final String set(String key, String value, String nxxx, String expx, int time) {
        return await(() -> stringCommands.set(key, value, Utils.toSetArgs(nxxx, expx, time)));
    }

    @Override
    @Suspendable
    public final String clientKill(String client) {
        return await(() -> stringCommands.clientKill(client));
    }

    @Override
    @Suspendable
    public final String clientSetname(String name) {
        return await(() -> stringCommands.clientSetname(name));
    }

    @Override
    @Suspendable
    public final String migrate(String host, int port, String key, int destinationDb, int timeout) {
        return await(() -> stringCommands.migrate(host, port, key, destinationDb, timeout));
    }

    @Override
    @Suspendable
    public final Long pfadd(String key, String... elements) {
        return await(() -> stringCommands.pfadd(key, elements));
    }

    @Override
    @Suspendable
    public final long pfcount(String key) {
        return await(() -> stringCommands.pfcount(new String[] { key }));
    }

    @Override
    @Suspendable
    public final long pfcount(String... keys) {
        return await(() -> stringCommands.pfcount(keys));
    }

    @Override
    @Suspendable
    public final String pfmerge(String destKey, String... sourceKeys) {
        return await(() -> stringCommands.pfmerge(destKey, sourceKeys));
    }

    @Override
    @Suspendable
    public final List<String> blpop(int timeout, String key) {
        return Utils.toList(await(() -> stringCommands.blpop(timeout, key)));
    }

    @Override
    @Suspendable
    public final List<String> brpop(int timeout, String key) {
        return Utils.toList(await(() -> stringCommands.brpop(timeout, key)));
    }

    @Override
    @Suspendable
    public final Long geoadd(String key, double longitude, double latitude, String member) {
        return await(() -> stringCommands.geoadd(key, longitude, latitude, member));
    }

    @Override
    @Suspendable
    public final Long geoadd(String key, Map<String, GeoCoordinate> memberCoordinateMap) {
        return await(() -> stringCommands.geoadd(key, Utils.toTripletArray(memberCoordinateMap)));
    }

    @Override
    @Suspendable
    public final Double geodist(String key, String member1, String member2) {
        return await(() -> stringCommands.geodist(key, member1, member2, null));
    }

    @Override
    @Suspendable
    public final Double geodist(String key, String member1, String member2, GeoUnit unit) {
        return await(() -> stringCommands.geodist(key, member1, member2, Utils.toGeoArgsUnit(unit)));
    }

    @Override
    @Suspendable
    public final List<GeoCoordinate> geopos(String key, String... members) {
        return await(() -> stringCommands.geopos(key, members)).stream().map(Utils::toGeoCoordinate).collect(Collectors.toList());
    }

    @Override
    @Suspendable
    public final List<GeoRadiusResponse> georadius(String key, double longitude, double latitude, double radius, GeoUnit unit) {
        return await(() -> stringCommands.georadius(key, longitude, latitude, radius, Utils.toUnit(unit))).stream().map(Utils::toGeoRadius).collect(Collectors.toList());
    }

    @Override
    @Suspendable
    public final List<GeoRadiusResponse> georadius(String key, double longitude, double latitude, double radius, GeoUnit unit, GeoRadiusParam param) {
        return await(() -> stringCommands.georadius(key, longitude, latitude, radius, Utils.toUnit(unit), Utils.toGeoArgs(param))).stream().map(Utils::toGeoRadius).collect(Collectors.toList());
    }

    @Override
    @Suspendable
    public final List<GeoRadiusResponse> georadiusByMember(String key, String member, double radius, GeoUnit unit) {
        return await(() -> stringCommands.georadiusbymember(key, member, radius, Utils.toUnit(unit))).stream().map(Utils::toGeoRadius).collect(Collectors.toList());
    }

    @Override
    @Suspendable
    public final List<GeoRadiusResponse> georadiusByMember(String key, String member, double radius, GeoUnit unit, GeoRadiusParam param) {
        return await(() -> stringCommands.georadiusbymember(key, member, radius, Utils.toUnit(unit), Utils.toGeoArgs(param))).stream().map(Utils::toGeoRadius).collect(Collectors.toList());
    }

    @Override
    @Suspendable
    public final String readonly() {
        return await(() -> stringCommands.readOnly());
    }

    @Override
    @Suspendable
    public final String asking() {
        return await(() -> stringCommands.asking());
    }

    @Override
    @Suspendable
    public final ScanResult<String> scan(String cursor) {
        return Utils.toScanResult(await(() -> stringCommands.scan(ScanCursor.of(cursor))));
    }

    @Override
    @Suspendable
    public final ScanResult<String> scan(String cursor, ScanParams params) {
        return Utils.toScanResult(await(() -> stringCommands.scan(ScanCursor.of(cursor), Utils.toScanArgs(params))));
    }

    @Override
    @Suspendable
    public final ScanResult<Map.Entry<String, String>> hscan(String key, String cursor) {
        return Utils.toScanResult(await(() -> stringCommands.hscan(key, ScanCursor.of(cursor))));
    }

    @Override
    @Suspendable
    public final ScanResult<Map.Entry<String, String>> hscan(String key, String cursor, ScanParams params) {
        return Utils.toScanResult(await(() -> stringCommands.hscan(key, ScanCursor.of(cursor), Utils.toScanArgs(params))));
    }

    @Override
    @Suspendable
    public final ScanResult<String> sscan(String key, String cursor) {
        return Utils.toScanResult(await(() -> stringCommands.sscan(key, ScanCursor.of(cursor))));
    }

    @Override
    @Suspendable
    public final ScanResult<String> sscan(String key, String cursor, ScanParams params) {
        return Utils.toScanResult(await(() -> stringCommands.sscan(key, ScanCursor.of(cursor), Utils.toScanArgs(params))));
    }

    @Override
    @Suspendable
    public final ScanResult<Tuple> zscan(String key, String cursor) {
        return Utils.toScanResult(await(() -> stringCommands.zscan(key, ScanCursor.of(cursor))));
    }

    @Override
    @Suspendable
    public final ScanResult<Tuple> zscan(String key, String cursor, ScanParams params) {
        return Utils.toScanResult(await(() -> stringCommands.zscan(key, ScanCursor.of(cursor), Utils.toScanArgs(params))));
    }

    /////////////////////////// PUBSUB

    private Utils.FiberRedisStringPubSubListener registerPubSubListener(Utils.FiberRedisStringPubSubListener l) {
        pubSubListenersLock.lock();
        try {
            stringPubSubListeners.add(l);
        } finally {
            pubSubListenersLock.unlock();
        }
        return l;
    }

    @Override
    @Suspendable
    public final void subscribe(redis.clients.jedis.JedisPubSub jedisPubSub, String... channels) {
        final JedisPubSub ps = validateFiberPubSub(jedisPubSub);
        ps.jedis = this;
        ps.conn = redisClient.connectPubSub();
        ps.conn.addListener(registerPubSubListener(new Utils.FiberRedisStringPubSubListener(ps)));
        ps.commands = ps.conn.async();
        if (password != null)
            ps.commands.auth(password);
        await(() -> ps.commands.subscribe(channels));
    }

    @Override
    @Suspendable
    public final Long publish(String channel, String message) {
        return await(() -> stringCommands.publish(channel, message));
    }

    @Override
    @Suspendable
    public final void psubscribe(redis.clients.jedis.JedisPubSub jedisPubSub, String... patterns) {
        final JedisPubSub ps = validateFiberPubSub(jedisPubSub);
        ps.jedis = this;
        ps.conn = redisClient.connectPubSub();
        ps.conn.addListener(registerPubSubListener(new Utils.FiberRedisStringPubSubListener(ps)));
        ps.commands = ps.conn.async();
        if (password != null)
            ps.commands.auth(password);
        await(() -> ps.commands.psubscribe(patterns));
    }

    @Override
    @Suspendable
    public final List<String> pubsubChannels(String pattern) {
        return await(() -> stringCommands.pubsubChannels(pattern));
    }

    @Override
    @Suspendable
    public final Long pubsubNumPat() {
        return await(() -> stringCommands.pubsubNumpat());
    }

    @Override
    @Suspendable
    public final Map<String, String> pubsubNumSub(String... channels) {
        return Utils.toStringMap(await(() -> stringCommands.pubsubNumsub(channels)));
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
    public final List<byte[]> geohash(byte[] key, byte[]... members) {
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
}
