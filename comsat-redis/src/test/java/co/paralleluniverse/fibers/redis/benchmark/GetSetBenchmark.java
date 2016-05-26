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
 * Ported from the corresponding Jedis benchmark, Copyright (c) 2011 Jonathan Leibiusky
 */
package co.paralleluniverse.fibers.redis.benchmark;

import java.io.IOException;
import java.util.Calendar;
import java.util.concurrent.ExecutionException;

import co.paralleluniverse.fibers.FiberUtil;
import co.paralleluniverse.fibers.redis.HostAndPortUtil;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;

/**
 * @author circlespainter
 */
public class GetSetBenchmark {
    private static HostAndPort hnp = HostAndPortUtil.getRedisServers().get(0);
    private static final int TOTAL_OPERATIONS = 100000;

    public static void main(String[] args) throws IOException, ExecutionException, InterruptedException {
        FiberUtil.runInFiber(() -> {
            Jedis jedis = new Jedis(hnp.getHost(), hnp.getPort());
            jedis.connect();
            jedis.auth("foobared");
            jedis.flushAll();

            long begin = Calendar.getInstance().getTimeInMillis();

            for (int n = 0; n <= TOTAL_OPERATIONS; n++) {
                String key = "foo" + n;
                jedis.set(key, "bar" + n);
                jedis.get(key);
            }

            long elapsed = Calendar.getInstance().getTimeInMillis() - begin;

            jedis.disconnect();

            System.out.println(((1000 * 2 * TOTAL_OPERATIONS) / elapsed) + " ops");
        });
    }
}