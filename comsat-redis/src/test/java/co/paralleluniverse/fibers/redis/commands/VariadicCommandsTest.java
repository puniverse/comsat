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
/*
 * Ported from the corresponding Jedis test, Copyright (c) 2011 Jonathan Leibiusky
 */
package co.paralleluniverse.fibers.redis.commands;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import co.paralleluniverse.fibers.FiberUtil;
import org.junit.Ignore;
import org.junit.Test;

/**
 * @author circlespainter
 */
public class VariadicCommandsTest extends JedisCommandTestBase {
    final byte[] bfoo = {0x01, 0x02, 0x03, 0x04};
    final byte[] bbar = {0x05, 0x06, 0x07, 0x08};
    final byte[] bcar = {0x09, 0x0A, 0x0B, 0x0C};
    final byte[] bfoo1 = {0x01, 0x02, 0x03, 0x04, 0x0A};
    final byte[] bfoo2 = {0x01, 0x02, 0x03, 0x04, 0x0B};

    @Test
    public void hdel() throws ExecutionException, InterruptedException {
        FiberUtil.runInFiber(() -> {
            Map<String, String> hash = new HashMap<>();
            hash.put("bar", "car");
            hash.put("car", "bar");
            hash.put("foo2", "bar");
            jedis.hmset("foo", hash);

            assertEquals(0, jedis.hdel("bar", "foo", "foo1").intValue());
            assertEquals(0, jedis.hdel("foo", "foo", "foo1").intValue());
            assertEquals(2, jedis.hdel("foo", "bar", "foo2").intValue());
            assertEquals(null, jedis.hget("foo", "bar"));

            // Binary
            Map<byte[], byte[]> bhash = new HashMap<>();
            bhash.put(bbar, bcar);
            bhash.put(bcar, bbar);
            bhash.put(bfoo2, bbar);
            jedis.hmset(bfoo, bhash);

            assertEquals(0, jedis.hdel(bbar, bfoo, bfoo1).intValue());
            assertEquals(0, jedis.hdel(bfoo, bfoo, bfoo1).intValue());
            assertEquals(2, jedis.hdel(bfoo, bbar, bfoo2).intValue());
            assertEquals(null, jedis.hget(bfoo, bbar));
        });
    }

    @Test
    public void rpush() throws ExecutionException, InterruptedException {
        FiberUtil.runInFiber(() -> {
            long size = jedis.rpush("foo", "bar", "foo");
            assertEquals(2, size);

            List<String> expected = new ArrayList<>();
            expected.add("bar");
            expected.add("foo");

            List<String> values = jedis.lrange("foo", 0, -1);
            assertEquals(expected, values);

            // Binary
            size = jedis.rpush(bfoo, bbar, bfoo);
            assertEquals(2, size);

            List<byte[]> bexpected = new ArrayList<>();
            bexpected.add(bbar);
            bexpected.add(bfoo);

            List<byte[]> bvalues = jedis.lrange(bfoo, 0, -1);
            assertEquals(bexpected, bvalues);
        });
    }

    @Test
    public void lpush() throws ExecutionException, InterruptedException {
        FiberUtil.runInFiber(() -> {
            long size = jedis.lpush("foo", "bar", "foo");
            assertEquals(2, size);

            List<String> expected = new ArrayList<>();
            expected.add("foo");
            expected.add("bar");

            List<String> values = jedis.lrange("foo", 0, -1);
            assertEquals(expected, values);

            // Binary
            size = jedis.lpush(bfoo, bbar, bfoo);
            assertEquals(2, size);

            List<byte[]> bexpected = new ArrayList<>();
            bexpected.add(bfoo);
            bexpected.add(bbar);

            List<byte[]> bvalues = jedis.lrange(bfoo, 0, -1);
            assertEquals(bexpected, bvalues);
        });
    }

    @Test
    public void sadd() throws ExecutionException, InterruptedException {
        FiberUtil.runInFiber(() -> {
            long status = jedis.sadd("foo", "bar", "foo1");
            assertEquals(2, status);

            status = jedis.sadd("foo", "bar", "car");
            assertEquals(1, status);

            status = jedis.sadd("foo", "bar", "foo1");
            assertEquals(0, status);

            status = jedis.sadd(bfoo, bbar, bfoo1);
            assertEquals(2, status);

            status = jedis.sadd(bfoo, bbar, bcar);
            assertEquals(1, status);

            status = jedis.sadd(bfoo, bbar, bfoo1);
            assertEquals(0, status);
        });
    }

    @Test @Ignore // TODO prio 1
    public void zadd() throws ExecutionException, InterruptedException {
        FiberUtil.runInFiber(() -> {
            Map<String, Double> scoreMembers = new HashMap<>();
            scoreMembers.put("bar", 1d);
            scoreMembers.put("foo", 10d);

            long status = jedis.zadd("foo", scoreMembers);
            assertEquals(2, status);

            scoreMembers.clear();
            scoreMembers.put("car", 0.1d);
            scoreMembers.put("bar", 2d);

            status = jedis.zadd("foo", scoreMembers);
            assertEquals(1, status);

            Map<byte[], Double> bscoreMembers = new HashMap<>();
            bscoreMembers.put(bbar, 1d);
            bscoreMembers.put(bfoo, 10d);

            status = jedis.zadd(bfoo, bscoreMembers);
            assertEquals(2, status);

            bscoreMembers.clear();
            bscoreMembers.put(bcar, 0.1d);
            bscoreMembers.put(bbar, 2d);

            status = jedis.zadd(bfoo, bscoreMembers);
            assertEquals(1, status);
        });
    }

    @Test
    public void zrem() throws ExecutionException, InterruptedException {
        FiberUtil.runInFiber(() -> {
            jedis.zadd("foo", 1d, "bar");
            jedis.zadd("foo", 2d, "car");
            jedis.zadd("foo", 3d, "foo1");

            long status = jedis.zrem("foo", "bar", "car");

            Set<String> expected = new LinkedHashSet<>();
            expected.add("foo1");

            assertEquals(2, status);
            assertEquals(expected, jedis.zrange("foo", 0, 100));

            status = jedis.zrem("foo", "bar", "car");
            assertEquals(0, status);

            status = jedis.zrem("foo", "bar", "foo1");
            assertEquals(1, status);

            // Binary
            jedis.zadd(bfoo, 1d, bbar);
            jedis.zadd(bfoo, 2d, bcar);
            jedis.zadd(bfoo, 3d, bfoo1);

            status = jedis.zrem(bfoo, bbar, bcar);

            Set<byte[]> bexpected = new LinkedHashSet<>();
            bexpected.add(bfoo);

            assertEquals(2, status);
            assertEquals(bexpected, jedis.zrange(bfoo, 0, 100));

            status = jedis.zrem(bfoo, bbar, bcar);
            assertEquals(0, status);

            status = jedis.zrem(bfoo, bbar, bfoo1);
            assertEquals(1, status);
        });
    }
}
