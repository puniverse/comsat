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

import co.paralleluniverse.fibers.FiberUtil;
import org.junit.Test;

import redis.clients.util.SafeEncoder;

import java.util.concurrent.ExecutionException;

/**
 * @author circlespainter
 */
public class ObjectCommandsTest extends JedisCommandTestBase {

    private String key = "mylist";
    private byte[] binaryKey = SafeEncoder.encode(key);

    @Test
    public void objectRefcount() throws ExecutionException, InterruptedException {
        FiberUtil.runInFiber(() -> {
            jedis.lpush(key, "hello world");
            Long refcount = jedis.objectRefcount(key);
            assertEquals(new Long(1), refcount);

            // Binary
            refcount = jedis.objectRefcount(binaryKey);
            assertEquals(new Long(1), refcount);
        });
    }

    @Test
    public void objectEncoding() throws ExecutionException, InterruptedException {
        FiberUtil.runInFiber(() -> {
            jedis.lpush(key, "hello world");
            String encoding = jedis.objectEncoding(key);
            assertEquals("quicklist", encoding);

            // Binary
            encoding = SafeEncoder.encode(jedis.objectEncoding(binaryKey));
            assertEquals("quicklist", encoding);
        });
    }

    @Test
    public void objectIdletime() throws InterruptedException, ExecutionException {
        FiberUtil.runInFiber(() -> {
            jedis.lpush(key, "hello world");

            Long time = jedis.objectIdletime(key);
            assertEquals(new Long(0), time);

            // Binary
            time = jedis.objectIdletime(binaryKey);
            assertEquals(new Long(0), time);
        });
    }
}