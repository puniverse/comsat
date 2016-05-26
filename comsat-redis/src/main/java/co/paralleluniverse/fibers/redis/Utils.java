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

import co.paralleluniverse.fibers.Fiber;
import com.google.common.base.Charsets;
import com.lambdaworks.redis.*;
import com.lambdaworks.redis.api.async.RedisAsyncCommands;
import com.lambdaworks.redis.pubsub.RedisPubSubListener;
import com.lambdaworks.redis.pubsub.StatefulRedisPubSubConnection;
import redis.clients.jedis.*;
import redis.clients.jedis.exceptions.InvalidURIException;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.params.geo.GeoRadiusParam;
import redis.clients.jedis.params.sortedset.ZAddParams;
import redis.clients.util.JedisURIHelper;

import java.net.URI;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @autho circlespainter
 */
public final class Utils {
    private static final ClientOptions DEFAULT_OPTS =
        new ClientOptions.Builder()
            .autoReconnect(false)
            .build();

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

    static SetArgs toSetArgs(String nxxx) {
        final SetArgs b = new SetArgs();

        if ("XX".equalsIgnoreCase(nxxx))
            b.xx();
        else if ("NX".equalsIgnoreCase(nxxx))
            b.nx();

        return b;
    }

    static SetArgs toSetArgs(String nxxx, String expx, long time) {
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

    static SetArgs toSetArgs(byte[] nxxx, byte[] expx, long time) {
        return toSetArgs(toString(nxxx), toString(expx), time);
    }

    static <T> Map<T, T> kvArrayToMap(T... keysValues) {
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

    static ZAddArgs toZAddArgs(ZAddParams params) {
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

    static Object[] toObjectScoreValueArray(Map<String, Double> scoreMembers) {
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

    static Set<Tuple> toTupleSet(List<ScoredValue> l) {
        if (l == null)
            return null;

        return l.stream().map(e -> {
            if (e.value instanceof String)
                return new Tuple((String) e.value, e.score);
            return new Tuple((byte[]) e.value, e.score);
        }).collect(Collectors.toCollection(HashSet::new));
    }

    static SortArgs toSortArgs(SortingParams sp) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    static <T> List<T> toList(KeyValue<T, T> kv) {
        if (kv == null)
            return null;

        final List<T> ret = new ArrayList<>(2);
        ret.add(kv.key);
        ret.add(kv.value);
        return ret;
    }

    static ZStoreArgs toZStoreArgs(ZParams ps) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    static long getStart(BitPosParams params) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    static long getEnd(BitPosParams params) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    static <T> Object[] toTripletArray(Map<T, GeoCoordinate> memberCoordinateMap) {
        if (memberCoordinateMap == null)
            return null;

        final Object[] ret = new Object[memberCoordinateMap.size() * 3];
        int i = 0;
        for (final Map.Entry<T, GeoCoordinate> e : memberCoordinateMap.entrySet()) {
            final GeoCoordinate v = e.getValue();
            ret[i++] = v.getLongitude();
            ret[i++] = v.getLatitude();
            ret[i++] = e.getKey();
        }
        return ret;
    }

    static GeoArgs.Unit toGeoArgsUnit(GeoUnit unit) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    static GeoCoordinate toGeoCoordinate(GeoCoordinates c) {
        if (c == null)
            return null;

        return new GeoCoordinate(c.x.doubleValue(), c.y.doubleValue());
    }

    static GeoArgs.Unit toUnit(GeoUnit unit) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    static GeoRadiusResponse toGeoRadius(String s) {
        if (s == null)
            return null;

        return new GeoRadiusResponse(s.getBytes());
    }

    static GeoRadiusResponse toGeoRadiusBinary(byte[] s) {
        if (s == null)
            return null;

        return new GeoRadiusResponse(s);
    }

    static GeoRadiusResponse toGeoRadius(GeoWithin<String> s) {
        if (s == null)
            return null;

        final GeoRadiusResponse r = new GeoRadiusResponse(s.member.getBytes());
        r.setDistance(s.distance);
        r.setCoordinate(toGeoCoordinate(s.coordinates));
        return r;
    }

    static GeoRadiusResponse toGeoRadiusBinary(GeoWithin<byte[]> s) {
        if (s == null)
            return null;

        final GeoRadiusResponse r = new GeoRadiusResponse(s.member);
        r.setDistance(s.distance);
        r.setCoordinate(toGeoCoordinate(s.coordinates));
        return r;
    }

    static GeoArgs toGeoArgs(GeoRadiusParam param) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    static <T> ScanResult<T> toScanResult(KeyScanCursor<T> c) {
        return new ScanResult<>(c.getCursor(), c.getKeys());
    }

    static ScanArgs toScanArgs(ScanParams params) {
        throw new UnsupportedOperationException("Unimplemented"); // TODO
    }

    static <T1, T2> ScanResult<Map.Entry<T1, T2>> toScanResult(MapScanCursor<T1, T2> c) {
        if (c == null)
            return null;

        return new ScanResult<>(c.getCursor(), new ArrayList<>(c.getMap().entrySet()));
    }

    static <T> ScanResult<T> toScanResult(ValueScanCursor<T> c) {
        return new ScanResult<>(c.getCursor(), c.getValues());
    }

    static ScanResult<Tuple> toScanResult(ScoredValueScanCursor<String> c) {
        if (c == null)
            return null;

        return new ScanResult<>(c.getCursor(), c.getValues().stream().map((sv) -> new Tuple(sv.value, sv.score)).collect(Collectors.toList()));
    }

    static ScanResult<Tuple> toScanResultBinary(ScoredValueScanCursor<byte[]> c) {
        if (c == null)
            return null;

        return new ScanResult<>(c.getCursor(), c.getValues().stream().map((sv) -> new Tuple(sv.value, sv.score)).collect(Collectors.toList()));
    }

    static JedisPubSub validateFiberPubSub(redis.clients.jedis.JedisPubSub jedisPubSub) {
        if (!(jedisPubSub instanceof JedisPubSub))
            throw new IllegalArgumentException("Only subclasses of '" + JedisPubSub.class.getName() + "' can be used with '" + Jedis.class.getName() + "'");
        return (JedisPubSub) jedisPubSub;
    }

    static BinaryJedisPubSub validateFiberPubSub(redis.clients.jedis.BinaryJedisPubSub jedisPubSub) {
        if (!(jedisPubSub instanceof BinaryJedisPubSub))
            throw new IllegalArgumentException("Only subclasses of '" + BinaryJedisPubSub.class.getName() + "' can be used with '" + Jedis.class.getName() + "'");
        return (BinaryJedisPubSub) jedisPubSub;
    }

    static <K, V> Map<String, String> toStringMap(Map<K, V> m) {
        if (m == null)
            return null;

        final Map<String, String> ret = new HashMap<>();
        for (final Map.Entry<K, V> e : m.entrySet())
            ret.put(e.getKey().toString(), e.getValue().toString());
        return ret;
    }

    static List<byte[]> toByteArrayList(List<Object> l) {
        return (List<byte[]>) ((List) l);
    }

    static String toString(byte[] bs) {
        return new String(bs, Charsets.UTF_8);
    }

    static <T> void checkPubSubConnected(BinaryJedis jedis, StatefulRedisPubSubConnection<T, T> conn, RedisAsyncCommands<T, T> commands) {
        if (jedis == null || !jedis.isConnected() || conn == null || !conn.isOpen() || commands == null)
            throw new JedisConnectionException("Not connected");
    }

    static <T> void  validateNotNull(T... os) {
        for (final T o : os) {
            if (o == null)
                throw new JedisDataException("Value is null");
        }
    }


    static RedisClient newClient(URI uri) {
        return newClient(uri, null);
    }

    static RedisClient newClient(URI uri, Long timeout) {
        validateRedisURI(uri);

        RedisURI.Builder b = RedisURI.Builder.redis(uri.getHost(), uri.getPort());

        String password = JedisURIHelper.getPassword(uri);
        if (password != null)
            b = b.withPassword(password);

        int dbIndex = JedisURIHelper.getDBIndex(uri);
        if (dbIndex > 0)
            b = b.withDatabase(dbIndex);

        if (timeout != null)
            b = b.withTimeout(timeout, TimeUnit.MILLISECONDS);

        return setDefaultOptions(RedisClient.create(b.build()));
    }

    static RedisClient setDefaultOptions(RedisClient redisClient) {
        redisClient.setOptions(DEFAULT_OPTS);
        return redisClient;
    }

    static void validateRedisURI(URI uri) {
        if (!JedisURIHelper.isValid(uri))
            throw new InvalidURIException(String.format("Cannot open Redis connection due invalid URI. %s", uri.toString()));
    }

    static final class FiberRedisStringPubSubListener implements RedisPubSubListener<String, String> {
        final JedisPubSub pubSub;

        FiberRedisStringPubSubListener(JedisPubSub pubSub) {
            this.pubSub = pubSub;
        }

        @Override
        public final void message(String channel, String message) {
            new Fiber(() -> pubSub.onMessage(channel, message)).start();
        }

        @Override
        public final void message(String pattern, String channel, String message) {
            new Fiber(() -> pubSub.onPMessage(pattern, channel, message)).start();
        }

        @Override
        public final void subscribed(String channel, long count) {
            new Fiber(() -> {
                int c = validateInt(count);
                pubSub.subscribedChannels.addAndGet(c);
                pubSub.onSubscribe(channel, c);
            }).start();
        }

        @Override
        public final void psubscribed(String pattern, long count) {
            new Fiber(() -> {
                int c = validateInt(count);
                pubSub.subscribedChannels.addAndGet(c);
                pubSub.onPSubscribe(pattern, c);
            }).start();
        }

        @Override
        public final void unsubscribed(String channel, long count) {
            new Fiber(() -> {
                int c = validateInt(count);
                pubSub.subscribedChannels.addAndGet(-c);
                pubSub.onUnsubscribe(channel, c);
            }).start();
        }

        @Override
        public final void punsubscribed(String pattern, long count) {
            new Fiber(() -> {
                int c = validateInt(count);
                pubSub.subscribedChannels.addAndGet(-c);
                pubSub.onPUnsubscribe(pattern, (int) count);
            }).start();
        }
    }

    static final class FiberRedisBinaryPubSubListener implements RedisPubSubListener<byte[], byte[]> {
        final BinaryJedisPubSub pubSub;

        FiberRedisBinaryPubSubListener(BinaryJedisPubSub pubSub) {
            this.pubSub = pubSub;
        }

        @Override
        public final void message(byte[] channel, byte[] message) {
            new Fiber(() -> pubSub.onMessage(channel, message)).start();
        }

        @Override
        public final void message(byte[] pattern, byte[] channel, byte[] message) {
            new Fiber(() -> pubSub.onPMessage(pattern, channel, message)).start();
        }

        @Override
        public final void subscribed(byte[] channel, long count) {
            new Fiber(() -> {
                int c = validateInt(count);
                pubSub.subscribedChannels.addAndGet(c);
                pubSub.onSubscribe(channel, c);
            }).start();
        }

        @Override
        public final void psubscribed(byte[] pattern, long count) {
            new Fiber(() -> {
                int c = validateInt(count);
                pubSub.subscribedChannels.addAndGet(c);
                pubSub.onPSubscribe(pattern, c);
            }).start();
        }

        @Override
        public final void unsubscribed(byte[] channel, long count) {
            new Fiber(() -> {
                int c = validateInt(count);
                pubSub.subscribedChannels.addAndGet(-c);
                pubSub.onUnsubscribe(channel, c);
            }).start();
        }

        @Override
        public final void punsubscribed(byte[] pattern, long count) {
            new Fiber(() -> {
                int c = validateInt(count);
                pubSub.subscribedChannels.addAndGet(-c);
                pubSub.onPUnsubscribe(pattern, (int) count);
            }).start();
        }
    }
}
