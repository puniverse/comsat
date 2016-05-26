package co.paralleluniverse.fibers.redis;

import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.Suspendable;
import co.paralleluniverse.fibers.futures.AsyncCompletionStage;
import co.paralleluniverse.strands.SuspendableCallable;
import co.paralleluniverse.strands.concurrent.ReentrantLock;
import com.google.common.base.Charsets;
import com.lambdaworks.redis.*;
import com.lambdaworks.redis.api.StatefulRedisConnection;
import com.lambdaworks.redis.api.async.RedisAsyncCommands;
import com.lambdaworks.redis.codec.ByteArrayCodec;
import redis.clients.jedis.*;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.exceptions.JedisException;
import redis.clients.jedis.params.geo.GeoRadiusParam;
import redis.clients.jedis.params.sortedset.ZAddParams;

import java.net.URI;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static co.paralleluniverse.fibers.redis.Utils.*;

public abstract class BinaryJedis extends redis.clients.jedis.Jedis {
    private final Callable<RedisClient> redisClientProvider;

    String password;
    RedisClient redisClient;

    private StatefulRedisConnection<String, String> stringCommandsConn;
    RedisAsyncCommands<String, String> stringCommands;
    private StatefulRedisConnection<byte[], byte[]> binaryCommandsConn;
    private RedisAsyncCommands<byte[], byte[]> binaryCommands;

    final List<Utils.FiberRedisStringPubSubListener> stringPubSubListeners = new ArrayList<>();
    private final List<Utils.FiberRedisBinaryPubSubListener> binaryPubSubListeners = new ArrayList<>();
    final ReentrantLock pubSubListenersLock = new ReentrantLock();

    private volatile boolean firstConnection = true;

    public BinaryJedis(Callable<RedisClient> cp) {
        redisClientProvider = cp;
    }

    public BinaryJedis() {
        this(() -> setDefaultOptions(RedisClient.create()));
    }

    public BinaryJedis(String host) {
        URI uri = URI.create(host);
        if (uri.getScheme() != null && uri.getScheme().equals("redis")) {
            redisClientProvider = () -> newClient(uri);
        } else {
            redisClientProvider = () -> setDefaultOptions(RedisClient.create(RedisURI.create("redis://" + host)));
        }
    }

    public BinaryJedis(String host, int port) {
        this(() -> setDefaultOptions(RedisClient.create(RedisURI.create(host, port))));
    }

    public BinaryJedis(String host, int port, int timeout) {
        redisClientProvider = () -> setDefaultOptions(RedisClient.create(RedisURI.Builder.redis(host, port).withTimeout(timeout, TimeUnit.MILLISECONDS).build()));
    }

    public BinaryJedis(URI uri) {
        validateRedisURI(uri);
        redisClientProvider = () -> newClient(uri);
    }

    public BinaryJedis(URI uri, int timeout) {
        validateRedisURI(uri);
        redisClientProvider = () -> newClient(uri, (long) timeout);
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
    }

    @Override
    public final String auth(String password) {
        firstTimeConnect();
        this.password = password;
        final String a1 = stringCommands.auth(password);
        final String a2 = binaryCommands.auth(password);
        if (a1.equals(a2)) {
            // TODO Check same results
            pubSubListenersLock.lock();
            try {
                for (final Utils.FiberRedisStringPubSubListener l : stringPubSubListeners)
                    l.pubSub.commands.auth(password);
                for (final Utils.FiberRedisBinaryPubSubListener l : binaryPubSubListeners)
                    l.pubSub.commands.auth(password);
            } finally {
                pubSubListenersLock.unlock();
            }
            return a1;
        }
        throw new IllegalStateException("Authentications returned different result codes");
    }

    @Override
    public final void close() {
        if (dataSource != null) {
            if (client.isBroken()) {
                //noinspection deprecation
                dataSource.returnBrokenResource(this);
            } else {
                //noinspection deprecation
                dataSource.returnResource(this);
            }
        }
        disconnect();
        if (redisClient != null) {
            redisClient.shutdown();
            redisClient = null;
        }
    }

    @Override
    public final void disconnect() {
        if (!isConnected())
            return;
        binaryCommands = null;
        stringCommands = null;
        binaryCommandsConn = null;
        stringCommandsConn = null;
        pubSubListenersLock.lock();
        try {
            for (final Utils.FiberRedisStringPubSubListener l : stringPubSubListeners) {
                l.pubSub.conn.removeListener(l);
                l.pubSub.close();
            }
            for (final Utils.FiberRedisBinaryPubSubListener l : binaryPubSubListeners) {
                l.pubSub.conn.removeListener(l);
                l.pubSub.close();
            }
        } finally {
            pubSubListenersLock.unlock();
        }
    }

    @Override
    public final boolean isConnected() {
        return
            stringCommands != null && stringCommands.isOpen() &&
            binaryCommands != null && binaryCommands.isOpen();
    }

    @Override
    @Suspendable
    public final String flushAll() {
        return await(() -> stringCommands.flushall());
    }

    @Override
    @Suspendable
    public final String flushDB() {
        return await(() -> stringCommands.flushdb());
    }

    @Override
    @Suspendable
    public final String ping() {
        return await(() -> stringCommands.ping());
    }

    @Override
    @Suspendable
    public final Long dbSize() {
        return await(() -> stringCommands.dbsize());
    }

    @Override
    public final void resetState() {
        // Nothing to do
    }

    @Override
    @Suspendable
    public final String save() {
        return await(() -> stringCommands.save());
    }


    @Override
    @Suspendable
    public final String bgsave() {
        return await(() -> stringCommands.bgsave());
    }

    @Override
    @Suspendable
    public final String bgrewriteaof() {
        return await(() -> stringCommands.bgrewriteaof());
    }

    @Override
    @Suspendable
    public final Long lastsave() {
        return await(() -> stringCommands.lastsave()).getTime();
    }

    @Override
    public final String shutdown() {
        if (!isConnected())
            connect();
        stringCommands.shutdown(true);
        return null;
    }

    @Override
    @Suspendable
    public final String info() {
        return await(() -> stringCommands.info());
    }

    @Override
    @Suspendable
    public final String info(String section) {
        return await(() -> stringCommands.info(section));
    }

    @Override
    @Suspendable
    public final void sync() {
        await(() -> stringCommands.sync());
    }

    @Override
    @Suspendable
    public final String slowlogReset() {
        return await(() -> stringCommands.slowlogReset());
    }

    @Override
    @Suspendable
    public final Long slowlogLen() {
        return await(() -> stringCommands.slowlogLen());
    }

    @Override
    @Suspendable
    public final String unwatch() {
        return await(() -> stringCommands.unwatch());
    }


    @Override
    @Suspendable
    public final String debug(DebugParams params) {
        if (!isConnected())
            connect();
        if ("SEGFAULT".equalsIgnoreCase(params.getCommand()[0]))
            binaryCommands.debugSegfault();
        else if ("RELOAD".equalsIgnoreCase(params.getCommand()[0]))
            return await(() -> binaryCommands.debugReload());
        else if ("OBJECT".equalsIgnoreCase(params.getCommand()[0]))
            return await(() -> binaryCommands.debugObject(params.getCommand()[1].getBytes(Charsets.UTF_8)));
        return null;
    }

    @Override
    @Suspendable
    public byte[] echo(byte[] string) {
        return await(() -> binaryCommands.echo(string));
    }

    @Override
    @Suspendable
    public final String set(byte[] key, byte[] value) {
        return await(() -> binaryCommands.set(key, value));
    }

    @Override
    @Suspendable
    public final String set(byte[] key, byte[] value, byte[] nxxx, byte[] expx, long time) {
        return await(() -> binaryCommands.set(key, value, Utils.toSetArgs(nxxx, expx, time)));
    }

    @Override
    @Suspendable
    public final byte[] get(byte[] key) {
        return await(() -> binaryCommands.get(key));
    }

    @Override
    @Suspendable
    public final String quit() {
        return await(() -> binaryCommands.quit());
    }

    @Override
    @Suspendable
    public final Long exists(byte[]... keys) {
        return await(() -> binaryCommands.exists(keys));
    }

    @Override
    @Suspendable
    public final Boolean exists(byte[] key) {
        //noinspection deprecation
        return await(() -> binaryCommands.exists(key));
    }

    @Override
    @Suspendable
    public final Long del(byte[]... keys) {
        return await(() -> binaryCommands.del(keys));
    }

    @Override
    @Suspendable
    public final Long del(byte[] key) {
        return await(() -> binaryCommands.del(key));
    }

    @Override
    @Suspendable
    public final String type(byte[] key) {
        return await(() -> binaryCommands.type(key));
    }


    @Override
    @Suspendable
    public final Set<byte[]> keys(byte[] pattern) {
        return new HashSet<>(await(() -> binaryCommands.keys(pattern)));
    }

    @Override
    @Suspendable
    public final byte[] randomBinaryKey() {
        return await(() -> binaryCommands.randomkey());
    }

    @Override
    @Suspendable
    public final String rename(byte[] oldKey, byte[] newKey) {
        return await(() -> binaryCommands.rename(oldKey, newKey));
    }

    @Override
    @Suspendable
    public final Long renamenx(byte[] oldKey, byte[] newKey) {
        return await(() -> binaryCommands.renamenx(oldKey, newKey)) ? 1L : 0L;
    }

    @Override
    @Suspendable
    public final Long expire(byte[] key, int seconds) {
        return await(() -> binaryCommands.expire(key, seconds)) ? 1L : 0L;
    }

    @Override
    @Suspendable
    public final Long expireAt(byte[] key, long unixTime) {
        return await(() -> binaryCommands.expireat(key, unixTime)) ? 1L : 0L;
    }

    @Override
    @Suspendable
    public final Long ttl(byte[] key) {
        return await(() -> binaryCommands.ttl(key));
    }

    @Override
    public final String select(int index) {
        return exc(() -> {
            // TODO Check same results
            for (final Utils.FiberRedisStringPubSubListener l : stringPubSubListeners)
                l.pubSub.commands.select(index);
            for (final Utils.FiberRedisBinaryPubSubListener l : binaryPubSubListeners)
                l.pubSub.commands.select(index);
            stringCommands.select(index);
            return binaryCommands.select(index);
        });
    }

    @Override
    @Suspendable
    public final Long move(byte[] key, int dbIndex) {
        return await(() -> binaryCommands.move(key, dbIndex)) ? 1L : 0L;
    }

    @Override
    @Suspendable
    public final byte[] getSet(byte[] key, byte[] value) {
        validateNotNull(key, value);
        return await(() -> binaryCommands.getset(key, value));
    }

    @Override
    @Suspendable
    public final List<byte[]> mget(byte[]... keys) {
        return await(() -> binaryCommands.mget(keys));
    }

    @Override
    @Suspendable
    public final Long setnx(byte[] key, byte[] value) {
        validateNotNull(key, value);
        return await(() -> binaryCommands.setnx(key, value)) ? 1L : 0L;
    }

    @Override
    @Suspendable
    public final String setex(byte[] key, int seconds, byte[] value) {
        validateNotNull(key, value);
        return await(() -> binaryCommands.setex(key, seconds, value));
    }

    @Override
    @Suspendable
    public final String mset(byte[]... keysValues) {
        validateNotNull(keysValues);
        return await(() -> binaryCommands.mset(Utils.kvArrayToMap(keysValues)));
    }

    @Override
    @Suspendable
    public final Long msetnx(byte[]... keysValues) {
        validateNotNull(keysValues);
        return await(() -> binaryCommands.msetnx(Utils.kvArrayToMap(keysValues))) ? 1L : 0L;
    }

    @Override
    @Suspendable
    public final Long decrBy(byte[] key, long amount) {
        return await(() -> binaryCommands.decrby(key, amount));
    }

    @Override
    @Suspendable
    public final Long decr(byte[] key) {
        return await(() -> binaryCommands.decr(key));
    }

    @Override
    @Suspendable
    public final Long incrBy(byte[] key, long amount) {
        return await(() -> binaryCommands.incrby(key, amount));
    }

    @Override
    @Suspendable
    public final Double incrByFloat(byte[] key, double amount) {
        return await(() -> binaryCommands.incrbyfloat(key, amount));
    }

    @Override
    @Suspendable
    public final Long incr(byte[] key) {
        return await(() -> binaryCommands.incr(key));
    }

    @Override
    @Suspendable
    public final Long append(byte[] key, byte[] value) {
        return await(() -> binaryCommands.append(key, value));
    }

    @Override
    @Suspendable
    public final byte[] substr(byte[] key, int start, int end) {
        return await(() -> binaryCommands.getrange(key, start, end));
    }

    @Override
    @Suspendable
    public final Long hset(byte[] key, byte[] field, byte[] value) {
        validateNotNull(key, value);
        return await(() -> binaryCommands.hset(key, field, value)) ? 1L : 0L;
    }

    @Override
    @Suspendable
    public final byte[] hget(byte[] key, byte[] field) {
        return await(() -> binaryCommands.hget(key, field));
    }

    @Override
    @Suspendable
    public final Long hsetnx(byte[] key, byte[] field, byte[] value) {
        validateNotNull(key, value);
        return await(() -> binaryCommands.hsetnx(key, field, value)) ? 1L : 0L;
    }

    @Override
    @Suspendable
    public final String hmset(byte[] key, Map<byte[], byte[]> hash) {
        validateNotNull(key);
        return await(() -> binaryCommands.hmset(key, hash));
    }

    @Override
    @Suspendable
    public final List<byte[]> hmget(byte[] key, byte[]... fields) {
        return await(() -> binaryCommands.hmget(key, fields));
    }

    @Override
    @Suspendable
    public final Long hincrBy(byte[] key, byte[] field, long value) {
        return await(() -> binaryCommands.hincrby(key, field, value));
    }

    @Override
    @Suspendable
    public final Double hincrByFloat(byte[] key, byte[] field, double value) {
        return await(() -> binaryCommands.hincrbyfloat(key, field, value));
    }

    @Override
    @Suspendable
    public final Boolean hexists(byte[] key, byte[] field) {
        return await(() -> binaryCommands.hexists(key, field));
    }

    @Override
    @Suspendable
    public final Long hdel(byte[] key, byte[]... fields) {
        return await(() -> binaryCommands.hdel(key, fields));
    }

    @Override
    @Suspendable
    public final Long hlen(byte[] key) {
        return await(() -> binaryCommands.hlen(key));
    }

    @Override
    @Suspendable
    public final Set<byte[]> hkeys(byte[] key) {
        return new HashSet<>(await(() -> binaryCommands.hkeys(key)));
    }

    @Override
    @Suspendable
    public final List<byte[]> hvals(byte[] key) {
        return await(() -> binaryCommands.hvals(key));
    }
    @Override
    @Suspendable
    public final Map<byte[], byte[]> hgetAll(byte[] key) {
        return await(() -> binaryCommands.hgetall(key));
    }

    @Override
    @Suspendable
    public final Long rpush(byte[] key, byte[]... strings) {
        return await(() -> binaryCommands.rpush(key, strings));
    }

    @Override
    @Suspendable
    public final Long lpush(byte[] key, byte[]... strings) {
        return await(() -> binaryCommands.lpush(key, strings));
    }

    @Override
    @Suspendable
    public final Long llen(byte[] key) {
        return await(() -> binaryCommands.llen(key));
    }

    @Override
    @Suspendable
    public final List<byte[]> lrange(byte[] key, long start, long end) {
        return await(() -> binaryCommands.lrange(key, start, end));
    }

    @Override
    @Suspendable
    public final String ltrim(byte[] key, long start, long end) {
        return await(() -> binaryCommands.ltrim(key, start, end));
    }

    @Override
    @Suspendable
    public final byte[] lindex(byte[] key, long index) {
        return await(() -> binaryCommands.lindex(key, index));
    }

    @Override
    @Suspendable
    public final String lset(byte[] key, long index, byte[] value) {
        validateNotNull(key, value);
        return await(() -> binaryCommands.lset(key, index, value));
    }

    @Override
    @Suspendable
    public final Long lrem(byte[] key, long count, byte[] value) {
        return await(() -> binaryCommands.lrem(key, count, value));
    }

    @Override
    @Suspendable
    public final byte[] lpop(byte[] key) {
        return await(() -> binaryCommands.lpop(key));
    }

    @Override
    @Suspendable
    public final byte[] rpop(byte[] key) {
        return await(() -> binaryCommands.rpop(key));
    }

    @Override
    @Suspendable
    public final byte[] rpoplpush(byte[] srcKey, byte[] dstKey) {
        return await(() -> binaryCommands.rpoplpush(srcKey, dstKey));
    }

    @Override
    @Suspendable
    public final Long sadd(byte[] key, byte[]... members) {
        return await(() -> binaryCommands.sadd(key, members));
    }

    @Override
    @Suspendable
    public final Set<byte[]> smembers(byte[] key) {
        return await(() -> binaryCommands.smembers(key));
    }
    @Override
    @Suspendable
    public final Long srem(byte[] key, byte[]... members) {
        return await(() -> binaryCommands.srem(key, members));
    }

    @Override
    @Suspendable
    public final byte[] spop(byte[] key) {
        return await(() -> binaryCommands.spop(key));
    }

    @Override
    @Suspendable
    public final Long smove(byte[] srcKey, byte[] dstKey, byte[] member) {
        return await(() -> binaryCommands.smove(srcKey, dstKey, member)) ? 1L : 0L;
    }

    @Override
    @Suspendable
    public final Long scard(byte[] key) {
        return await(() -> binaryCommands.scard(key));
    }

    @Override
    @Suspendable
    public final Boolean sismember(byte[] key, byte[] member) {
        return await(() -> binaryCommands.sismember(key, member));
    }

    @Override
    @Suspendable
    public final Set<byte[]> sinter(byte[]... keys) {
        return await(() -> binaryCommands.sinter(keys));
    }

    @Override
    @Suspendable
    public final Long sinterstore(byte[] dstKey, byte[]... keys) {
        return await(() -> binaryCommands.sinterstore(dstKey, keys));
    }

    @Override
    @Suspendable
    public final Set<byte[]> sunion(byte[]... keys) {
        return await(() -> binaryCommands.sunion(keys));
    }

    @Override
    @Suspendable
    public final Long sunionstore(byte[] dstKey, byte[]... keys) {
        return await(() -> binaryCommands.sunionstore(dstKey, keys));
    }

    @Override
    @Suspendable
    public final Set<byte[]> sdiff(byte[]... keys) {
        return await(() -> binaryCommands.sdiff(keys));
    }

    @Override
    @Suspendable
    public final Long sdiffstore(byte[] dstKey, byte[]... keys) {
        return await(() -> binaryCommands.sdiffstore(dstKey, keys));
    }

    @Override
    @Suspendable
    public final byte[] srandmember(byte[] key) {
        return await(() -> binaryCommands.srandmember(key));
    }

    @Override
    @Suspendable
    public final List<byte[]> srandmember(byte[] key, int count) {
        return new ArrayList<>(await(() -> binaryCommands.srandmember(key, count)));
    }

    @Override
    @Suspendable
    public final Long zadd(byte[] key, double score, byte[] member) {
        return await(() -> binaryCommands.zadd(key, score, member));
    }

    @Override
    @Suspendable
    public final Long zadd(byte[] key, double score, byte[] member, ZAddParams params) {
        return await(() -> binaryCommands.zadd(key, Utils.toZAddArgs(params), score, member));
    }

    @Override
    @Suspendable
    public final Long zadd(byte[] key, Map<byte[], Double> scoreMembers) {
        return await(() -> binaryCommands.zadd(key, scoreMembers));
    }

    @Override
    @Suspendable
    public final Long zadd(byte[] key, Map<byte[], Double> scoreMembers, ZAddParams params) {
        return await(() -> binaryCommands.zadd(key, Utils.toZAddArgs(params), scoreMembers));
    }

    @Override
    @Suspendable
    public final Set<byte[]> zrange(byte[] key, long start, long end) {
        return new HashSet<>(await(() -> binaryCommands.zrange(key, start, end)));
    }

    @Override
    @Suspendable
    public final Long zrem(byte[] key, byte[]... members) {
        return await(() -> binaryCommands.zrem(key, members));
    }

    @Override
    @Suspendable
    public final Double zincrby(byte[] key, double score, byte[] member) {
        return await(() -> binaryCommands.zincrby(key, score, member));
    }

    @Override
    @Suspendable
    public final Long zrank(byte[] key, byte[] member) {
        return await(() -> binaryCommands.zrank(key, member));
    }

    @Override
    @Suspendable
    public final Long zrevrank(byte[] key, byte[] member) {
        return await(() -> binaryCommands.zrevrank(key, member));
    }

    @Override
    @Suspendable
    public final Set<byte[]> zrevrange(byte[] key, long start, long end) {
        return new HashSet<>(await(() -> binaryCommands.zrevrange(key, start, end)));
    }

    @Override
    @Suspendable
    public final Set<Tuple> zrangeWithScores(byte[] key, long start, long end) {
        return Utils.toTupleSet((List) await(() -> binaryCommands.zrangeWithScores(key, start, end)));
    }

    @Override
    @Suspendable
    public final Set<Tuple> zrevrangeWithScores(byte[] key, long start, long end) {
        return Utils.toTupleSet((List) await(() -> binaryCommands.zrevrangeWithScores(key, start, end)));
    }

    @Override
    @Suspendable
    public final Long zcard(byte[] key) {
        return await(() -> binaryCommands.zcard(key));
    }

    @Override
    @Suspendable
    public final Double zscore(byte[] key, byte[] member) {
        return await(() -> binaryCommands.zscore(key, member));
    }

    @Override
    @Suspendable
    public final String watch(byte[]... keys) {
        return await(() -> binaryCommands.watch(keys));
    }

    @Override
    @Suspendable
    public final List<byte[]> sort(byte[] key) {
        return await(() -> binaryCommands.sort(key));
    }

    @Override
    @Suspendable
    public final List<byte[]> sort(byte[] key, SortingParams sp) {
        return await(() -> binaryCommands.sort(key, Utils.toSortArgs(sp)));
    }

    @Override
    @Suspendable
    public final List<byte[]> blpop(int timeout, byte[]... keys) {
        return Utils.toList(await(() -> binaryCommands.blpop(timeout, keys)));
    }

    @Override
    @Suspendable
    public final Long sort(byte[] key, SortingParams sp, byte[] dstKey) {
        return await(() -> binaryCommands.sortStore(key, Utils.toSortArgs(sp), dstKey));
    }

    @Override
    @Suspendable
    public final Long sort(byte[] key, byte[] dstKey) {
        return await(() -> binaryCommands.sortStore(key, new SortArgs(), dstKey));
    }

    @Override
    @Suspendable
    public final List<byte[]> brpop(int timeout, byte[]... keys) {
        return Utils.toList(await(() -> binaryCommands.brpop(timeout, keys)));
    }

    @Override
    public List<byte[]> blpop(byte[]... args) {
        return Utils.toList(await(() -> binaryCommands.blpop(0 /* Indefinitely */, args)));
    }

    @Override
    @Suspendable
    public final List<byte[]> brpop(byte[]... args) {
        return Utils.toList(await(() -> binaryCommands.brpop(0 /* Indefinitely */, args)));
    }

    @Override
    @Suspendable
    public final Long zcount(byte[] key, double min, double max) {
        return await(() -> binaryCommands.zcount(key, min, max));
    }

    @Override
    @Suspendable
    public final Long zcount(byte[] key, byte[] min, byte[] max) {
        return await(() -> binaryCommands.zcount(key, Utils.toString(min), Utils.toString(max)));
    }

    @Override
    @Suspendable
    public final Set<byte[]> zrangeByScore(byte[] key, double min, double max) {
        return new HashSet<>(await(() -> binaryCommands.zrangebyscore(key, min, max)));
    }

    @Override
    @Suspendable
    public final Set<byte[]> zrangeByScore(byte[] key, byte[] min, byte[] max) {
        return new HashSet<>(await(() -> binaryCommands.zrangebyscore(key, Utils.toString(min), Utils.toString(max))));
    }

    @Override
    @Suspendable
    public final Set<byte[]> zrangeByScore(byte[] key, double min, double max, int offset, int count) {
        return new HashSet<>(await(() -> binaryCommands.zrangebyscore(key, min, max, offset, count)));
    }

    @Override
    @Suspendable
    public final Set<byte[]> zrangeByScore(byte[] key, byte[] min, byte[] max, int offset, int count) {
        return new HashSet<>(await(() -> binaryCommands.zrangebyscore(key, Utils.toString(min), Utils.toString(max), offset, count)));
    }

    @Override
    @Suspendable
    public final Set<Tuple> zrangeByScoreWithScores(byte[] key, double min, double max) {
        return Utils.toTupleSet((List) await(() -> binaryCommands.zrangebyscoreWithScores(key, min, max)));
    }

    @Override
    @Suspendable
    public final Set<Tuple> zrangeByScoreWithScores(byte[] key, byte[] min, byte[] max) {
        return Utils.toTupleSet((List) await(() -> binaryCommands.zrangebyscoreWithScores(key, Utils.toString(min), Utils.toString(max))));
    }

    @Override
    @Suspendable
    public final Set<Tuple> zrangeByScoreWithScores(byte[] key, double min, double max, int offset, int count) {
        return Utils.toTupleSet((List) await(() -> binaryCommands.zrangebyscoreWithScores(key, min, max, offset, count)));
    }

    @Override
    @Suspendable
    public final Set<Tuple> zrangeByScoreWithScores(byte[] key, byte[] min, byte[] max, int offset, int count) {
        return Utils.toTupleSet((List) await(() -> binaryCommands.zrangebyscoreWithScores(key, Utils.toString(min), Utils.toString(max), offset, count)));
    }

    @Override
    @Suspendable
    public final Set<byte[]> zrevrangeByScore(byte[] key, double max, double min) {
        return new HashSet<>(await(() -> binaryCommands.zrevrangebyscore(key, min, max)));
    }

    @Override
    @Suspendable
    public final Set<byte[]> zrevrangeByScore(byte[] key, byte[] max, byte[] min) {
        return new HashSet<>(await(() -> binaryCommands.zrevrangebyscore(key, Utils.toString(min), Utils.toString(max))));
    }

    @Override
    @Suspendable
    public final Set<byte[]> zrevrangeByScore(byte[] key, double max, double min, int offset, int count) {
        return new HashSet<>(await(() -> binaryCommands.zrevrangebyscore(key, min, max, offset, count)));
    }

    @Override
    @Suspendable
    public final Set<byte[]> zrevrangeByScore(byte[] key, byte[] max, byte[] min, int offset, int count) {
        return new HashSet<>(await(() -> binaryCommands.zrevrangebyscore(key, Utils.toString(min), Utils.toString(max), offset, count)));
    }

    @Override
    @Suspendable
    public final Set<Tuple> zrevrangeByScoreWithScores(byte[] key, double max, double min) {
        return Utils.toTupleSet((List) await(() -> binaryCommands.zrevrangebyscoreWithScores(key, min, max)));
    }

    @Override
    @Suspendable
    public final Set<Tuple> zrevrangeByScoreWithScores(byte[] key, double max, double min, int offset, int count) {
        return Utils.toTupleSet((List) await(() -> binaryCommands.zrevrangebyscoreWithScores(key, min, max, offset, count)));
    }

    @Override
    @Suspendable
    public final Set<Tuple> zrevrangeByScoreWithScores(byte[] key, byte[] max, byte[] min) {
        return Utils.toTupleSet((List) await(() -> binaryCommands.zrevrangebyscoreWithScores(key, Utils.toString(min), Utils.toString(max))));
    }

    @Override
    @Suspendable
    public final Set<Tuple> zrevrangeByScoreWithScores(byte[] key, byte[] max, byte[] min, int offset, int count) {
        return Utils.toTupleSet((List) await(() -> binaryCommands.zrevrangebyscoreWithScores(key, Utils.toString(min), Utils.toString(max), offset, count)));
    }

    @Override
    @Suspendable
    public final Long zremrangeByRank(byte[] key, long start, long end) {
        return await(() -> binaryCommands.zremrangebyrank(key, start, end));
    }

    @Override
    @Suspendable
    public final Long zremrangeByScore(byte[] key, double start, double end) {
        return await(() -> binaryCommands.zremrangebyscore(key, start, end));
    }

    @Override
    @Suspendable
    public final Long zremrangeByScore(byte[] key, byte[] start, byte[] end) {
        return await(() -> binaryCommands.zremrangebyscore(key, Utils.toString(start), Utils.toString(end)));
    }

    @Override
    @Suspendable
    public final Long zunionstore(byte[] dstKey, byte[]... sets) {
        return await(() -> binaryCommands.zunionstore(dstKey, sets));
    }

    @Override
    @Suspendable
    public final Long zunionstore(byte[] dstKey, ZParams params, byte[]... sets) {
        return await(() -> binaryCommands.zunionstore(dstKey, Utils.toZStoreArgs(params), sets));
    }

    @Override
    @Suspendable
    public final Long zinterstore(byte[] dstKey, byte[]... sets) {
        return await(() -> binaryCommands.zinterstore(dstKey, sets));
    }

    @Override
    @Suspendable
    public final Long zinterstore(byte[] dstKey, ZParams params, byte[]... sets) {
        return await(() -> binaryCommands.zinterstore(dstKey, Utils.toZStoreArgs(params), sets));
    }

    @Override
    @Suspendable
    public final Long zlexcount(byte[] key, byte[] min, byte[] max) {
        return await(() -> binaryCommands.zlexcount(key, Utils.toString(min), Utils.toString(max)));
    }

    @Override
    @Suspendable
    public final Set<byte[]> zrangeByLex(byte[] key, byte[] min, byte[] max) {
        return new HashSet<>(await(() -> binaryCommands.zrangebylex(key, Utils.toString(min), Utils.toString(max))));
    }

    @Override
    @Suspendable
    public final Set<byte[]> zrangeByLex(byte[] key, byte[] min, byte[] max, int offset, int count) {
        return new HashSet<>(await(() -> binaryCommands.zrangebylex(key, Utils.toString(min), Utils.toString(max), offset, count)));
    }

    @Override
    @Suspendable
    public final Long zremrangeByLex(byte[] key, byte[] min, byte[] max) {
        return await(() -> binaryCommands.zremrangebylex(key, Utils.toString(min), Utils.toString(max)));
    }

    @Override
    @Suspendable
    public final String slaveof(String host, int port) {
        return await(() -> binaryCommands.slaveof(host, port));
    }

    @Override
    @Suspendable
    public final String slaveofNoOne() {
        return await(() -> binaryCommands.slaveofNoOne());
    }

    @Override
    @Suspendable
    public final List<byte[]> configGet(byte[] pattern) {
        return await(() -> binaryCommands.configGet(Utils.toString(pattern))).stream().map(s -> s.getBytes(Charsets.UTF_8)).collect(Collectors.toList());
    }

    @Override
    @Suspendable
    public final String configResetStat() {
        return await(() -> binaryCommands.configResetstat());
    }

    @Override
    @Suspendable
    public final byte[] configSet(byte[] parameter, byte[] value) {
        return await(() -> binaryCommands.configSet(Utils.toString(parameter), Utils.toString(value))).getBytes(Charsets.UTF_8);
    }

    @Override
    @Suspendable
    public final Long strlen(byte[] key) {
        return await(() -> binaryCommands.strlen(key));
    }

    @Override
    @Suspendable
    public final Long persist(byte[] key) {
        return await(() -> binaryCommands.persist(key)) ? 1L : 0L;
    }

    @Override
    @Suspendable
    public final Long linsert(byte[] key, BinaryClient.LIST_POSITION where, byte[] pivot, byte[] value) {
        return await(() -> binaryCommands.linsert(key, BinaryClient.LIST_POSITION.BEFORE.equals(where), pivot, value));
    }

    @Override
    @Suspendable
    public final byte[] brpoplpush(byte[] source, byte[] destination, int timeout) {
        return await(() -> binaryCommands.brpoplpush(timeout, source, destination));
    }

    @Override
    @Suspendable
    public final Boolean setbit(byte[] key, long offset, boolean value) {
        validateNotNull(key, value);
        return await(() -> binaryCommands.setbit(key, offset, value ? 1 : 0)) > 0;
    }

    @Override
    @Suspendable
    public final Boolean setbit(byte[] key, long offset, byte[] value) {
        validateNotNull(key, value);
        return await(() -> binaryCommands.setbit(key, offset, Integer.parseInt(Utils.toString(value)))) > 0;
    }

    @Override
    @Suspendable
    public final Boolean getbit(byte[] key, long offset) {
        return await(() -> binaryCommands.getbit(key, offset)) > 0;
    }

    @Override
    @Suspendable
    public final Long bitpos(byte[] key, boolean value) {
        return await(() -> binaryCommands.bitpos(key, value));
    }

    @Override
    @Suspendable
    public final Long bitpos(byte[] key, boolean value, BitPosParams params) {
        return await(() -> binaryCommands.bitpos(key, value, Utils.getStart(params), Utils.getEnd(params)));
    }

    @Override
    @Suspendable
    public final Long setrange(byte[] key, long offset, byte[] value) {
        validateNotNull(key, value);
        return await(() -> binaryCommands.setrange(key, offset, value));
    }

    @Override
    @Suspendable
    public final byte[] getrange(byte[] key, long startOffset, long endOffset) {
        return await(() -> binaryCommands.getrange(key, startOffset, endOffset));
    }

    @Override
    @Suspendable
    public final List<byte[]> slowlogGetBinary() {
        return Utils.toByteArrayList(await(() -> binaryCommands.slowlogGet()));
    }

    @Override
    @Suspendable
    public final List<byte[]> slowlogGetBinary(long entries) {
        return Utils.toByteArrayList(await(() -> binaryCommands.slowlogGet(Utils.validateInt(entries))));
    }

    @Override
    @Suspendable
    public final Long objectRefcount(byte[] key) {
        return await(() -> binaryCommands.objectRefcount(key));
    }

    @Override
    @Suspendable
    public final byte[] objectEncoding(byte[] key) {
        return await(() -> binaryCommands.objectEncoding(key)).getBytes(Charsets.UTF_8);
    }

    @Override
    @Suspendable
    public final Long objectIdletime(byte[] key) {
        return await(() -> binaryCommands.objectIdletime(key));
    }

    @Override
    @Suspendable
    public final Long bitcount(byte[] key) {
        return await(() -> binaryCommands.bitcount(key));
    }

    @Override
    @Suspendable
    public final Long bitcount(byte[] key, long start, long end) {
        return await(() -> binaryCommands.bitcount(key, start, end));
    }

    @Override
    @Suspendable
    public final Long bitop(BitOP op, byte[] destKey, byte[]... srcKeys) {
        final Long res;
        switch (op) {
            case AND:
                res = await(() -> binaryCommands.bitopAnd(destKey, srcKeys));
                break;
            case OR:
                res = await(() -> binaryCommands.bitopOr(destKey, srcKeys));
                break;
            case XOR:
                res = await(() -> binaryCommands.bitopXor(destKey, srcKeys));
                break;
            case NOT:
                if (srcKeys == null || srcKeys.length != 1)
                    throw new IllegalArgumentException("'not' requires exactly one argument");
                res = await(() -> binaryCommands.bitopNot(destKey, srcKeys[0]));
                break;
            default:
                throw new IllegalArgumentException("Unsupported operation: " + op);
        }
        return res;
    }

    @Override
    @Suspendable
    public final byte[] dump(byte[] key) {
        return await(() -> binaryCommands.dump(key));
    }

    @Override
    @Suspendable
    public final String restore(byte[] key, int ttl, byte[] serializedValue) {
        return await(() -> binaryCommands.restore(key, ttl, serializedValue));
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
        return await(() -> binaryCommands.pexpire(key, milliseconds)) ? 1L : 0L;
    }

    @Override
    @Suspendable
    public final Long pexpireAt(byte[] key, long millisecondsTimestamp) {
        return await(() -> binaryCommands.pexpireat(key, millisecondsTimestamp)) ? 1L : 0L;
    }

    @Override
    @Suspendable
    public final Long pttl(byte[] key) {
        return await(() -> binaryCommands.pttl(key));
    }

    @Override
    @Deprecated
    @Suspendable
    @SuppressWarnings("deprecation")
    public final String psetex(byte[] key, int milliseconds, byte[] value) {
        validateNotNull(key, value);
        return psetex(key, milliseconds, value);
    }

    @Override
    @Suspendable
    public final String psetex(byte[] key, long milliseconds, byte[] value) {
        validateNotNull(key, value);
        return await(() -> binaryCommands.psetex(key, milliseconds, value));
    }

    @Override
    @Suspendable
    public final String set(byte[] key, byte[] value, byte[] nxxx) {
        validateNotNull(key, value);
        return await(() -> binaryCommands.set(key, value, Utils.toSetArgs(Utils.toString(nxxx))));
    }

    @Override
    @Suspendable
    public final String set(byte[] key, byte[] value, byte[] nxxx, byte[] expx, int time) {
        validateNotNull(key, value);
        return await(() -> binaryCommands.set(key, value, Utils.toSetArgs(Utils.toString(nxxx), Utils.toString(expx), time)));
    }

    @Override
    @Suspendable
    public final String clientKill(byte[] client) {
        return await(() -> binaryCommands.clientKill(Utils.toString(client)));
    }

    @Override
    @Suspendable
    public final String clientGetname() {
        return await(() -> stringCommands.clientGetname());
    }

    @Override
    @Suspendable
    public final String clientList() {
        return await(() -> stringCommands.clientList());
    }

    @Override
    @Suspendable
    public final String clientSetname(byte[] name) {
        return await(() -> stringCommands.clientSetname(Utils.toString(name)));
    }

    @Override
    @Suspendable
    public final List<String> time() {
        return await(() -> stringCommands.time());
    }

    @Override
    @Suspendable
    public final String migrate(byte[] host, int port, byte[] key, int destinationDb, int timeout) {
        return await(() -> binaryCommands.migrate(Utils.toString(host), port, key, destinationDb, timeout));
    }

    @Override
    @Suspendable
    public final Long pfadd(byte[] key, byte[]... elements) {
        return await(() -> binaryCommands.pfadd(key, elements));
    }

    @Override
    @Suspendable
    public final long pfcount(byte[] key) {
        return await(() -> binaryCommands.pfcount(new byte[][] { key }));
    }

    @Override
    @Suspendable
    public final String pfmerge(byte[] destKey, byte[]... sourceKeys) {
        return await(() -> binaryCommands.pfmerge(destKey, sourceKeys));
    }

    @Override
    @Suspendable
    public final Long pfcount(byte[]... keys) {
        return await(() -> binaryCommands.pfcount(keys));
    }

    @Override
    @Suspendable
    public final ScanResult<byte[]> scan(byte[] cursor) {
        return Utils.toScanResult(await(() -> binaryCommands.scan(ScanCursor.of(Utils.toString(cursor)))));
    }

    @Override
    @Suspendable
    public final ScanResult<byte[]> scan(byte[] cursor, ScanParams ps) {
        return Utils.toScanResult(await(() -> binaryCommands.scan(ScanCursor.of(Utils.toString(cursor)), Utils.toScanArgs(ps))));
    }

    @Override
    @Suspendable
    public final ScanResult<Map.Entry<byte[], byte[]>> hscan(byte[] key, byte[] cursor) {
        return Utils.toScanResult(await(() -> binaryCommands.hscan(key, ScanCursor.of(Utils.toString(cursor)))));
    }

    @Override
    @Suspendable
    public final ScanResult<Map.Entry<byte[], byte[]>> hscan(byte[] key, byte[] cursor, ScanParams ps) {
        return Utils.toScanResult(await(() -> binaryCommands.hscan(key, ScanCursor.of(Utils.toString(cursor)), Utils.toScanArgs(ps))));
    }

    @Override
    @Suspendable
    public final ScanResult<byte[]> sscan(byte[] key, byte[] cursor) {
        return Utils.toScanResult(await(() -> binaryCommands.sscan(key, ScanCursor.of(Utils.toString(cursor)))));
    }

    @Override
    public ScanResult<byte[]> sscan(byte[] key, byte[] cursor, ScanParams ps) {
        return Utils.toScanResult(await(() -> binaryCommands.sscan(key, ScanCursor.of(Utils.toString(cursor)), Utils.toScanArgs(ps))));
    }

    @Override
    @Suspendable
    public final ScanResult<Tuple> zscan(byte[] key, byte[] cursor) {
        return Utils.toScanResultBinary(await(() -> binaryCommands.zscan(key, ScanCursor.of(Utils.toString(cursor)))));
    }

    @Override
    @Suspendable
    public final ScanResult<Tuple> zscan(byte[] key, byte[] cursor, ScanParams ps) {
        return Utils.toScanResultBinary(await(() -> binaryCommands.zscan(key, ScanCursor.of(Utils.toString(cursor)), Utils.toScanArgs(ps))));
    }

    @Override
    @Suspendable
    public final Long geoadd(byte[] key, double longitude, double latitude, byte[] member) {
        return await(() -> binaryCommands.geoadd(key, longitude, latitude, member));
    }

    @Override
    @Suspendable
    public final Long geoadd(byte[] key, Map<byte[], GeoCoordinate> memberCoordinateMap) {
        return await(() -> binaryCommands.geoadd(key, Utils.toTripletArray(memberCoordinateMap)));
    }

    @Override
    @Suspendable
    public final Double geodist(byte[] key, byte[] member1, byte[] member2) {
        return await(() -> binaryCommands.geodist(key, member1, member2, null));
    }

    @Override
    @Suspendable
    public final Double geodist(byte[] key, byte[] member1, byte[] member2, GeoUnit unit) {
        return await(() -> binaryCommands.geodist(key, member1, member2, Utils.toGeoArgsUnit(unit)));
    }

    @Override
    @Suspendable
    public final List<GeoCoordinate> geopos(byte[] key, byte[]... members) {
        return await(() -> binaryCommands.geopos(key, members)).stream().map(Utils::toGeoCoordinate).collect(Collectors.toList());
    }

    @Override
    @Suspendable
    public final List<GeoRadiusResponse> georadius(byte[] key, double longitude, double latitude, double radius, GeoUnit unit) {
        return await(() -> binaryCommands.georadius(key, longitude, latitude, radius, Utils.toGeoArgsUnit(unit))).stream().map(Utils::toGeoRadiusBinary).collect(Collectors.toList());
    }

    @Override
    @Suspendable
    public final List<GeoRadiusResponse> georadius(byte[] key, double longitude, double latitude, double radius, GeoUnit unit, GeoRadiusParam param) {
        return await(() -> binaryCommands.georadius(key, longitude, latitude, radius, Utils.toGeoArgsUnit(unit), Utils.toGeoArgs(param))).stream().map(Utils::toGeoRadiusBinary).collect(Collectors.toList());
    }

    @Override
    @Suspendable
    public final List<GeoRadiusResponse> georadiusByMember(byte[] key, byte[] member, double radius, GeoUnit unit) {
        return await(() -> binaryCommands.georadiusbymember(key, member, radius, Utils.toGeoArgsUnit(unit))).stream().map(Utils::toGeoRadiusBinary).collect(Collectors.toList());
    }

    @Override
    @Suspendable
    public final List<GeoRadiusResponse> georadiusByMember(byte[] key, byte[] member, double radius, GeoUnit unit, GeoRadiusParam param) {
        return await(() -> binaryCommands.georadiusbymember(key, member, radius, Utils.toGeoArgsUnit(unit), Utils.toGeoArgs(param))).stream().map(Utils::toGeoRadiusBinary).collect(Collectors.toList());
    }

    /////////////////////////// PUBSUB

    @Override
    @Suspendable
    public final Long publish(byte[] channel, byte[] message) {
        return await(() -> binaryCommands.publish(channel, message));
    }

    @Override
    @Suspendable
    public final void subscribe(redis.clients.jedis.BinaryJedisPubSub jedisPubSub, byte[]... channels) {
        final BinaryJedisPubSub ps = validateFiberPubSub(jedisPubSub);
        ps.jedis = this;
        ps.conn = redisClient.connectPubSub(new ByteArrayCodec());
        ps.conn.addListener(registerPubSubListener(new Utils.FiberRedisBinaryPubSubListener(ps)));
        ps.commands = ps.conn.async();
        if (password != null)
            ps.commands.auth(password);
        await(() -> ps.commands.subscribe(channels));
    }

    private Utils.FiberRedisBinaryPubSubListener registerPubSubListener(Utils.FiberRedisBinaryPubSubListener l) {
        pubSubListenersLock.lock();
        try {
            binaryPubSubListeners.add(l);
        } finally {
            pubSubListenersLock.unlock();
        }
        return l;
    }

    @Override
    @Suspendable
    public final void psubscribe(redis.clients.jedis.BinaryJedisPubSub jedisPubSub, byte[]... patterns) {
        final BinaryJedisPubSub ps = validateFiberPubSub(jedisPubSub);
        ps.jedis = this;
        ps.conn = redisClient.connectPubSub(new ByteArrayCodec());
        ps.conn.addListener(registerPubSubListener(new Utils.FiberRedisBinaryPubSubListener(ps)));
        ps.commands = ps.conn.async();
        if (password != null)
            ps.commands.auth(password);
        await(() -> ps.commands.psubscribe(patterns));
    }

    /////////////////////////// UTILS
    @Suspendable
    <T> T await(SuspendableCallable<RedisFuture<T>> f) {
        firstTimeConnect();
        return exc(() -> {
            try {
                return AsyncCompletionStage.get(f.run());
            } catch (final ExecutionException e) {
                if (e.getCause() != null)
                    throw new RuntimeException(e.getCause());
                else
                    throw new RuntimeException(e);
            }
        });
    }

    private void firstTimeConnect() {
        if (firstConnection && !isConnected()) {
            connect();
            firstConnection = false;
        }
    }

    @Suspendable
    private <T> T exc(SuspendableCallable<T> c) {
        try {
            return c.run();
        } catch (final SuspendExecution e) {
            throw new AssertionError(e);
        } catch (final RedisException e) {
            translateRedisExc(e);
            throw new RuntimeException(e);
        } catch (final RuntimeException e) {
            if (e.getCause() != null) {
                final Throwable t = e.getCause();
                translateRedisExc(t);
            }
            throw new RuntimeException(e);
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void translateRedisExc(Throwable t) {
        if (t instanceof RedisConnectionException) {
            final RedisConnectionException rce = (RedisConnectionException) t;
            throw new JedisConnectionException(t.getMessage(), rce.getCause());
        } else if (t instanceof RedisCommandExecutionException) {
            final RedisCommandExecutionException rcce = (RedisCommandExecutionException) t;
            throw new JedisDataException(rcce.getMessage(), rcce.getCause());
        } else if (t instanceof RedisException) {
            final RedisException re = (RedisException) t;
            throw new JedisConnectionException(t.getMessage(), re.getCause());
        } else if (t instanceof JedisException) {
            throw (JedisException) t;
        }
    }
}
