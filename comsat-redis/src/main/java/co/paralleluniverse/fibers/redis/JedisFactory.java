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
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import redis.clients.jedis.*;
import redis.clients.jedis.exceptions.InvalidURIException;
import redis.clients.jedis.exceptions.JedisException;
import redis.clients.util.JedisURIHelper;

import java.net.URI;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author circlespainter
 */
final class JedisFactory implements PooledObjectFactory<redis.clients.jedis.Jedis> {
    private final AtomicReference<HostAndPort> hostAndPort = new AtomicReference<>();
    private final int connectionTimeout;
    private final String password;
    private final int database;
    private final String clientName;

    public JedisFactory(final String host, final int port, final int connectionTimeout, final String password, final int database, final String clientName) {
        this.hostAndPort.set(new HostAndPort(host, port));
        this.connectionTimeout = connectionTimeout;
        this.password = password;
        this.database = database;
        this.clientName = clientName;
    }

    public JedisFactory(final URI uri, final int connectionTimeout, final String clientName) {
        if (!JedisURIHelper.isValid(uri)) {
            throw new InvalidURIException(String.format(
                "Cannot open Redis connection due invalid URI. %s", uri.toString()));
        }

        this.hostAndPort.set(new HostAndPort(uri.getHost(), uri.getPort()));
        this.connectionTimeout = connectionTimeout;
        this.password = JedisURIHelper.getPassword(uri);
        this.database = JedisURIHelper.getDBIndex(uri);
        this.clientName = clientName;
    }

    public final void setHostAndPort(final HostAndPort hostAndPort) {
        this.hostAndPort.set(hostAndPort);
    }

    @Override
    @Suspendable
    public final void activateObject(PooledObject<redis.clients.jedis.Jedis> pooledJedis) throws Exception {
        final BinaryJedis jedis = pooledJedis.getObject();
        if (jedis.getDB() != database) {
            jedis.select(database);
        }

    }

    @Override
    @Suspendable
    public final void destroyObject(PooledObject<redis.clients.jedis.Jedis> pooledJedis) throws Exception {
        final BinaryJedis jedis = pooledJedis.getObject();
        if (jedis.isConnected()) {
            try {
                try {
                    jedis.quit();
                } catch (Exception e) {
                }
                jedis.disconnect();
            } catch (Exception e) {

            }
        }

    }

    @Override
    @Suspendable
    public final PooledObject<redis.clients.jedis.Jedis> makeObject() throws Exception {
        final HostAndPort hostAndPort = this.hostAndPort.get();
        final Jedis jedis = new Jedis(hostAndPort.getHost(), hostAndPort.getPort(), connectionTimeout);

        try {
            jedis.connect();
            if (null != this.password) {
                jedis.auth(this.password);
            }
            if (database != 0) {
                jedis.select(database);
            }
            if (clientName != null) {
                jedis.clientSetname(clientName);
            }
        } catch (JedisException je) {
            jedis.close();
            throw je;
        }

        return new DefaultPooledObject<>(jedis);
    }

    @Override
    public final void passivateObject(PooledObject<redis.clients.jedis.Jedis> pooledJedis) throws Exception {
        // TODO maybe should select db 0? Not sure right now.
    }

    @Override
    @Suspendable
    public final boolean validateObject(PooledObject<redis.clients.jedis.Jedis> pooledJedis) {
        final BinaryJedis jedis = pooledJedis.getObject();
        try {
            HostAndPort hostAndPort = this.hostAndPort.get();

            String connectionHost = jedis.getClient().getHost();
            int connectionPort = jedis.getClient().getPort();

            return hostAndPort.getHost().equals(connectionHost)
                && hostAndPort.getPort() == connectionPort && jedis.isConnected()
                && jedis.ping().equals("PONG");
        } catch (final Exception e) {
            return false;
        }
    }
}
