package co.paralleluniverse.fibers.redis;

import co.paralleluniverse.fibers.FiberUtil;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.Transaction;
import redis.clients.jedis.exceptions.InvalidURIException;
import redis.clients.jedis.exceptions.JedisException;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.ExecutionException;

/**
 * @author circlespainter
 */
public class JedisPoolTest extends Assert {
    private static HostAndPort hnp = HostAndPortUtil.getRedisServers().get(0);

    @Test
    public void checkConnections() throws ExecutionException, InterruptedException {
        FiberUtil.runInFiber(() -> {
            JedisPool pool = new JedisPool(new JedisPoolConfig(), hnp.getHost(), hnp.getPort(), 2000);
            Jedis jedis = pool.getResource();
            jedis.auth("foobared");
            jedis.set("foo", "bar");
            assertEquals("bar", jedis.get("foo"));
            //noinspection deprecation
            pool.returnResource(jedis);
            pool.destroy();
            assertTrue(pool.isClosed());
        });
    }

    @Test
    public void checkCloseableConnections() throws Exception {
        FiberUtil.runInFiber(() -> {
            JedisPool pool = new JedisPool(new JedisPoolConfig(), hnp.getHost(), hnp.getPort(), 2000);
            Jedis jedis = pool.getResource();
            jedis.auth("foobared");
            jedis.set("foo", "bar");
            assertEquals("bar", jedis.get("foo"));
            //noinspection deprecation
            pool.returnResource(jedis);
            pool.close();
            assertTrue(pool.isClosed());
        });
    }

    @Test
    public void checkConnectionWithDefaultPort() throws ExecutionException, InterruptedException {
        FiberUtil.runInFiber(() -> {
            JedisPool pool = new JedisPool(new JedisPoolConfig(), hnp.getHost(), hnp.getPort());
            Jedis jedis = pool.getResource();
            jedis.auth("foobared");
            jedis.set("foo", "bar");
            assertEquals("bar", jedis.get("foo"));
            //noinspection deprecation
            pool.returnResource(jedis);
            pool.destroy();
            assertTrue(pool.isClosed());
        });
    }

    @SuppressWarnings("deprecation")
    @Test
    public void checkJedisIsReusedWhenReturned() throws ExecutionException, InterruptedException {
        FiberUtil.runInFiber(() -> {
            JedisPool pool = new JedisPool(new JedisPoolConfig(), hnp.getHost(), hnp.getPort());
            Jedis jedis = pool.getResource();
            jedis.auth("foobared");
            jedis.set("foo", "0");
            pool.returnResource(jedis);

            jedis = pool.getResource();
            jedis.auth("foobared");
            jedis.incr("foo");
            pool.returnResource(jedis);
            pool.destroy();
            assertTrue(pool.isClosed());
        });
    }

    @SuppressWarnings("deprecation")
    @Test
    public void checkPoolRepairedWhenJedisIsBroken() throws ExecutionException, InterruptedException {
        FiberUtil.runInFiber(() -> {
            JedisPool pool = new JedisPool(new JedisPoolConfig(), hnp.getHost(), hnp.getPort());
            Jedis jedis = pool.getResource();
            jedis.auth("foobared");
            jedis.quit();
            pool.returnBrokenResource(jedis);

            jedis = pool.getResource();
            jedis.auth("foobared");
            jedis.incr("foo");
            pool.returnResource(jedis);
            pool.destroy();
            assertTrue(pool.isClosed());
        });
    }

    @Test(expected = JedisException.class)
    public void checkPoolOverflow() throws ExecutionException, InterruptedException {
        try {
            FiberUtil.runInFiber(() -> {
                GenericObjectPoolConfig config = new GenericObjectPoolConfig();
                config.setMaxTotal(1);
                config.setBlockWhenExhausted(false);
                JedisPool pool = new JedisPool(config, hnp.getHost(), hnp.getPort());
                Jedis jedis = pool.getResource();
                jedis.auth("foobared");
                jedis.set("foo", "0");

                Jedis newJedis = pool.getResource();
                newJedis.auth("foobared");
                newJedis.incr("foo");
            });
        } catch (final ExecutionException e) {
            final Throwable t = e.getCause();
            if (t != null && t instanceof JedisException)
                throw (JedisException) t;
            else throw e;
        }
    }

    @Test @Ignore // TODO prio 2
    public void securePool() throws ExecutionException, InterruptedException {
        FiberUtil.runInFiber(() -> {
            JedisPoolConfig config = new JedisPoolConfig();
            config.setTestOnBorrow(true);
            JedisPool pool = new JedisPool(config, hnp.getHost(), hnp.getPort(), 2000, "foobared");
            Jedis jedis = pool.getResource();
            jedis.set("foo", "bar");
            //noinspection deprecation
            pool.returnResource(jedis);
            pool.destroy();
            assertTrue(pool.isClosed());
        });
    }

    @SuppressWarnings("deprecation")
    @Test
    public void nonDefaultDatabase() throws ExecutionException, InterruptedException {
        FiberUtil.runInFiber(() -> {
            JedisPool pool0 = new JedisPool(new JedisPoolConfig(), hnp.getHost(), hnp.getPort(), 2000,
                "foobared");
            Jedis jedis0 = pool0.getResource();
            jedis0.set("foo", "bar");
            assertEquals("bar", jedis0.get("foo"));
            pool0.returnResource(jedis0);
            pool0.destroy();
            assertTrue(pool0.isClosed());

            JedisPool pool1 = new JedisPool(new JedisPoolConfig(), hnp.getHost(), hnp.getPort(), 2000,
                "foobared", 1);
            Jedis jedis1 = pool1.getResource();
            assertNull(jedis1.get("foo"));
            pool1.returnResource(jedis1);
            pool1.destroy();
            assertTrue(pool1.isClosed());
        });
    }

    @Test
    public void startWithUrlString() throws ExecutionException, InterruptedException {
        FiberUtil.runInFiber(() -> {
            Jedis j = new Jedis("localhost", 6380);
            j.auth("foobared");
            j.select(2);
            j.set("foo", "bar");
            JedisPool pool = new JedisPool("redis://:foobared@localhost:6380/2");
            Jedis jedis = pool.getResource();
            assertEquals("PONG", jedis.ping());
            assertEquals("bar", jedis.get("foo"));
        });
    }

    @Test
    public void startWithUrl() throws ExecutionException, InterruptedException {
        FiberUtil.runInFiber(() -> {
            Jedis j = new Jedis("localhost", 6380);
            j.auth("foobared");
            j.select(2);
            j.set("foo", "bar");
            JedisPool pool = null;
            try {
                pool = new JedisPool(new URI("redis://:foobared@localhost:6380/2"));
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
            Jedis jedis = pool.getResource();
            assertEquals("PONG", jedis.ping());
            assertEquals("bar", jedis.get("foo"));
        });
    }

    @Test(expected = InvalidURIException.class)
    public void shouldThrowInvalidURIExceptionForInvalidURI() throws ExecutionException, InterruptedException {
        try {
            FiberUtil.runInFiber(() -> {
                try {
                    new JedisPool(new URI("localhost:6380"));
                } catch (URISyntaxException e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (final ExecutionException e) {
            final Throwable t = e.getCause();
            if (t != null && t instanceof JedisException)
                throw (JedisException) t;
            else throw e;
        }
    }

    @Test
    public void allowUrlWithNoDBAndNoPassword() throws ExecutionException, InterruptedException {
        FiberUtil.runInFiber(() -> {
            new JedisPool("redis://localhost:6380");
            try {
                new JedisPool(new URI("redis://localhost:6380"));
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @SuppressWarnings("deprecation")
    @Test @Ignore // TODO prio 2
    public void selectDatabaseOnActivation() throws ExecutionException, InterruptedException {
        FiberUtil.runInFiber(() -> {
            JedisPool pool = new JedisPool(new JedisPoolConfig(), hnp.getHost(), hnp.getPort(), 2000,
                "foobared");

            Jedis jedis0 = pool.getResource();
            assertEquals(0L, jedis0.getDB().longValue());

            jedis0.select(1);
            assertEquals(1L, jedis0.getDB().longValue());

            pool.returnResource(jedis0);

            Jedis jedis1 = pool.getResource();
            assertTrue("Jedis instance was not reused", jedis1 == jedis0);
            assertEquals(0L, jedis1.getDB().longValue());

            pool.returnResource(jedis1);
            pool.destroy();
            assertTrue(pool.isClosed());
        });
    }

    @Test
    public void customClientName() throws ExecutionException, InterruptedException {
        FiberUtil.runInFiber(() -> {
            JedisPool pool0 = new JedisPool(new JedisPoolConfig(), hnp.getHost(), hnp.getPort(), 2000,
                "foobared", 0, "my_shiny_client_name");

            Jedis jedis = pool0.getResource();

            assertEquals("my_shiny_client_name", jedis.clientGetname());

            //noinspection deprecation
            pool0.returnResource(jedis);
            pool0.destroy();
            assertTrue(pool0.isClosed());
        });
    }

    @Test @Ignore
    public void returnResourceShouldResetState() throws ExecutionException, InterruptedException {
        FiberUtil.runInFiber(() -> {
            GenericObjectPoolConfig config = new GenericObjectPoolConfig();
            config.setMaxTotal(1);
            config.setBlockWhenExhausted(false);
            JedisPool pool = new JedisPool(config, hnp.getHost(), hnp.getPort(), 2000, "foobared");

            Jedis jedis = pool.getResource();
            try {
                jedis.set("hello", "jedis");
                Transaction t = jedis.multi();
                t.set("hello", "world");
            } finally {
                jedis.close();
            }

            try (final Jedis jedis2 = pool.getResource()) {
                assertTrue(jedis == jedis2);
                assertEquals("jedis", jedis2.get("hello"));
            }

            pool.destroy();
            assertTrue(pool.isClosed());
        });
    }

    @Test
    public void checkResourceIsCloseable() throws ExecutionException, InterruptedException {
        FiberUtil.runInFiber(() -> {
            GenericObjectPoolConfig config = new GenericObjectPoolConfig();
            config.setMaxTotal(1);
            config.setBlockWhenExhausted(false);
            JedisPool pool = new JedisPool(config, hnp.getHost(), hnp.getPort(), 2000, "foobared");

            Jedis jedis = pool.getResource();
            try {
                jedis.set("hello", "jedis");
            } finally {
                jedis.close();
            }

            try (Jedis jedis2 = pool.getResource()) {
                assertEquals(jedis, jedis2);
            }
        });
    }

    @SuppressWarnings("deprecation")
    @Test
    public void returnNullObjectShouldNotFail() throws ExecutionException, InterruptedException {
        FiberUtil.runInFiber(() -> {
            JedisPool pool = new JedisPool(new JedisPoolConfig(), hnp.getHost(), hnp.getPort(), 2000,
                "foobared", 0, "my_shiny_client_name");

            pool.returnBrokenResource(null);
            pool.returnResource(null);
            pool.returnResourceObject(null);
        });
    }

    @Test
    public void getNumActiveIsNegativeWhenPoolIsClosed() throws ExecutionException, InterruptedException {
        FiberUtil.runInFiber(() -> {
            JedisPool pool = new JedisPool(new JedisPoolConfig(), hnp.getHost(), hnp.getPort(), 2000,
                "foobared", 0, "my_shiny_client_name");

            pool.destroy();
            assertTrue(pool.getNumActive() < 0);
        });
    }

    @SuppressWarnings("deprecation")
    @Test
    public void getNumActiveReturnsTheCorrectNumber() throws ExecutionException, InterruptedException {
        FiberUtil.runInFiber(() -> {
            JedisPool pool = new JedisPool(new JedisPoolConfig(), hnp.getHost(), hnp.getPort(), 2000);
            Jedis jedis = pool.getResource();
            jedis.auth("foobared");
            jedis.set("foo", "bar");
            assertEquals("bar", jedis.get("foo"));

            assertEquals(1, pool.getNumActive());

            Jedis jedis2 = pool.getResource();
            jedis.auth("foobared");
            jedis.set("foo", "bar");

            assertEquals(2, pool.getNumActive());

            pool.returnResource(jedis);
            assertEquals(1, pool.getNumActive());

            pool.returnResource(jedis2);

            assertEquals(0, pool.getNumActive());

            pool.destroy();
        });
    }

    @Test
    public void testAddObject() throws ExecutionException, InterruptedException {
        FiberUtil.runInFiber(() -> {
            JedisPool pool = new JedisPool(new JedisPoolConfig(), hnp.getHost(), hnp.getPort(), 2000);
            pool.addObjects(1);
            assertEquals(pool.getNumIdle(), 1);
            pool.destroy();
        });
    }

    @Test @Ignore // TODO prio 2
    public void testCloseConnectionOnMakeObject() throws ExecutionException, InterruptedException {
        FiberUtil.runInFiber(() -> {
            JedisPoolConfig config = new JedisPoolConfig();
            config.setTestOnBorrow(true);
            JedisPool pool = new JedisPool(new JedisPoolConfig(), hnp.getHost(), hnp.getPort(), 2000,
                "wrong pass");
            Jedis jedis = new Jedis("redis://:foobared@localhost:6379/");
            int currentClientCount = getClientCount(jedis.clientList());
            try {
                pool.getResource();
                fail("Should throw exception as password is incorrect.");
            } catch (Exception e) {
                assertEquals(currentClientCount, getClientCount(jedis.clientList()));
            }
        });
    }

    private int getClientCount(final String clientList) {
        return clientList.split("\n").length;
    }
}
