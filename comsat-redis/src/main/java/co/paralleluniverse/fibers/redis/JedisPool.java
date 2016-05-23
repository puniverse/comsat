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
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.exceptions.JedisException;
import redis.clients.util.JedisURIHelper;

import java.net.URI;

/**
 * @author circlespainter
 */
public class JedisPool extends redis.clients.jedis.JedisPool {
    public JedisPool() {
        this(Protocol.DEFAULT_HOST, Protocol.DEFAULT_PORT);
    }

    public JedisPool(final GenericObjectPoolConfig poolConfig, final String host) {
        this(poolConfig, host, Protocol.DEFAULT_PORT, Protocol.DEFAULT_TIMEOUT, null,
            Protocol.DEFAULT_DATABASE, null);
    }

    public JedisPool(String host, int port) {
        this(new GenericObjectPoolConfig(), host, port, Protocol.DEFAULT_TIMEOUT, null,
            Protocol.DEFAULT_DATABASE, null);
    }

    public JedisPool(final String host) {
        URI uri = URI.create(host);
        if (JedisURIHelper.isValid(uri)) {
            String h = uri.getHost();
            int port = uri.getPort();
            String password = JedisURIHelper.getPassword(uri);
            int database = JedisURIHelper.getDBIndex(uri);
            this.internalPool = new GenericObjectPool<>(new JedisFactory(h, port,
                Protocol.DEFAULT_TIMEOUT, password, database, null),
                new GenericObjectPoolConfig());
        } else {
            this.internalPool = new GenericObjectPool<>(new JedisFactory(host,
                Protocol.DEFAULT_PORT, Protocol.DEFAULT_TIMEOUT, null,
                Protocol.DEFAULT_DATABASE, null), new GenericObjectPoolConfig());
        }
    }

    public JedisPool(final URI uri) {
        this(new GenericObjectPoolConfig(), uri, Protocol.DEFAULT_TIMEOUT);
    }

    public JedisPool(final URI uri, final int timeout) {
        this(new GenericObjectPoolConfig(), uri, timeout);
    }

    public JedisPool(final GenericObjectPoolConfig poolConfig, final String host, int port,
                     int timeout, final String password) {
        this(poolConfig, host, port, timeout, password, Protocol.DEFAULT_DATABASE, null);
    }

    public JedisPool(final GenericObjectPoolConfig poolConfig, final String host, final int port) {
        this(poolConfig, host, port, Protocol.DEFAULT_TIMEOUT, null, Protocol.DEFAULT_DATABASE, null);
    }

    public JedisPool(final GenericObjectPoolConfig poolConfig, final String host, final int port,
                     final int timeout) {
        this(poolConfig, host, port, timeout, null, Protocol.DEFAULT_DATABASE, null);
    }

    public JedisPool(final GenericObjectPoolConfig poolConfig, final String host, int port,
                     int timeout, final String password, final int database) {
        this(poolConfig, host, port, timeout, password, database, null);
    }

    public JedisPool(final GenericObjectPoolConfig poolConfig, final String host, int port,
                     int timeout, final String password, final int database, final String clientName) {
        initPool(poolConfig, new JedisFactory(host, port, timeout, password, database, clientName));
    }

    public JedisPool(final GenericObjectPoolConfig poolConfig, final URI uri) {
        this(poolConfig, uri, Protocol.DEFAULT_TIMEOUT);
    }

    public JedisPool(final GenericObjectPoolConfig poolConfig, final URI uri, final int timeout) {
        initPool(poolConfig, new JedisFactory(uri, timeout, null));
    }

    @Override
    @Suspendable
    public final Jedis getResource() {
        Jedis jedis = (Jedis) super.getResource();
        jedis.setDataSource(this);
        return jedis;
    }

    @Override
    @Deprecated
    @SuppressWarnings("deprecation")
    public final void returnBrokenResource(final redis.clients.jedis.Jedis resource) {
        if (resource != null) {
            returnBrokenResourceObject(resource);
        }
    }

    @Override
    @Deprecated
    @SuppressWarnings("deprecation")
    public final void returnResource(final redis.clients.jedis.Jedis resource) {
        if (resource != null) {
            try {
                resource.resetState();
                returnResourceObject(resource);
            } catch (Exception e) {
                returnBrokenResource(resource);
                throw new JedisException("Could not return the resource to the pool", e);
            }
        }
    }
}
