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

import co.paralleluniverse.fibers.FiberUtil;
import co.paralleluniverse.fibers.redis.HostAndPortUtil;
import co.paralleluniverse.fibers.redis.Jedis;
import co.paralleluniverse.fibers.redis.JedisPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import redis.clients.jedis.HostAndPort;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author circlespainter
 */
public class PoolBenchmark {
    private static HostAndPort hnp = HostAndPortUtil.getRedisServers().get(0);
    private static final int TOTAL_OPERATIONS = 100000;

    public static void main(String[] args) throws Exception {
        FiberUtil.runInFiber(() -> {
            Jedis j = new Jedis(hnp.getHost(), hnp.getPort());
            j.connect();
            j.auth("foobared");
            j.flushAll();
            j.quit();
            j.disconnect();
            long t = System.currentTimeMillis();
            // withoutPool();
            try {
                withPool();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            long elapsed = System.currentTimeMillis() - t;
            System.out.println(((1000 * 2 * TOTAL_OPERATIONS) / elapsed) + " ops");
        });
    }

    private static void withPool() throws Exception {
        final JedisPool pool = new JedisPool(new GenericObjectPoolConfig(), hnp.getHost(),
            hnp.getPort(), 2000, "foobared");
        List<Thread> tds = new ArrayList<>();

        final AtomicInteger ind = new AtomicInteger();
        for (int i = 0; i < 50; i++) {
            Thread hj = new Thread(() -> {
                for (int i1 = 0; (i1 = ind.getAndIncrement()) < TOTAL_OPERATIONS; ) {
                    try {
                        Jedis j = pool.getResource();
                        final String key = "foo" + i1;
                        j.set(key, key);
                        j.get(key);
                        //noinspection deprecation
                        pool.returnResource(j);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
            tds.add(hj);
            hj.start();
        }

        for (Thread t : tds)
            t.join();

        pool.destroy();

    }
}