package co.paralleluniverse.fibers.redis;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import co.paralleluniverse.fibers.FiberUtil;
import co.paralleluniverse.strands.Strand;
import org.junit.Test;

import redis.clients.jedis.exceptions.InvalidURIException;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.exceptions.JedisException;
import co.paralleluniverse.fibers.redis.commands.JedisCommandTestBase;
import redis.clients.util.SafeEncoder;

public class JedisTest extends JedisCommandTestBase {
    @Test
    public void useWithoutConnecting() throws ExecutionException, InterruptedException {
        FiberUtil.runInFiber(() -> {
            Jedis jedis = new Jedis("localhost");
            jedis.auth("foobared");
            jedis.dbSize();
        });
    }

    @Test
    public void checkBinaryData() throws ExecutionException, InterruptedException {
        FiberUtil.runInFiber(() -> {
            byte[] bigdata = new byte[1777];
            for (int b = 0; b < bigdata.length; b++) {
                bigdata[b] = (byte) ((byte) b % 255);
            }
            Map<String, String> hash = new HashMap<>();
            hash.put("data", SafeEncoder.encode(bigdata));

            String status = jedis.hmset("foo", hash);
            assertEquals("OK", status);
            assertEquals(hash, jedis.hgetAll("foo"));
        });
    }

    @Test(expected = JedisConnectionException.class)
    public void timeoutConnection() throws Exception {
        FiberUtil.runInFiber(() -> {
            jedis = new Jedis("localhost", 6379, 15000);
            jedis.auth("foobared");
            jedis.configSet("timeout", "1");
            Strand.sleep(2000);
            jedis.hmget("foobar", "foo");
        });
    }

    @Test(expected = JedisConnectionException.class)
    public void timeoutConnectionWithURI() throws Exception {
        FiberUtil.runInFiber(() -> {
            try {
                jedis = new Jedis(new URI("redis://:foobared@localhost:6380/2"), 15000);
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
            jedis.configSet("timeout", "1");
            Strand.sleep(2000);
            jedis.hmget("foobar", "foo");
        });
    }

    @Test(expected = JedisDataException.class)
    public void failWhenSendingNullValues() throws ExecutionException, InterruptedException {
        FiberUtil.runInFiber(() -> jedis.set("foo", null));
    }

    @Test(expected = InvalidURIException.class)
    public void shouldThrowInvalidURIExceptionForInvalidURI() throws ExecutionException, InterruptedException {
        FiberUtil.runInFiber(() -> {
            Jedis j = null;
            try {
                j = new Jedis(new URI("localhost:6380"));
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
            j.ping();
        });
    }

    @Test
    public void shouldReconnectToSameDB() throws ExecutionException, InterruptedException {
        FiberUtil.runInFiber(() -> {
            jedis.select(1);
            jedis.set("foo", "bar");
            try {
                jedis.getClient().getSocket().shutdownInput();
                jedis.getClient().getSocket().shutdownOutput();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            assertEquals("bar", jedis.get("foo"));
        });
    }

    @Test
    public void startWithUrlString() throws ExecutionException, InterruptedException {
        FiberUtil.runInFiber(() -> {
            Jedis j = new Jedis("localhost", 6380);
            j.auth("foobared");
            j.select(2);
            j.set("foo", "bar");
            Jedis jedis = new Jedis("redis://:foobared@localhost:6380/2");
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
            Jedis jedis = null;
            try {
                jedis = new Jedis(new URI("redis://:foobared@localhost:6380/2"));
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
            assertEquals("PONG", jedis.ping());
            assertEquals("bar", jedis.get("foo"));
        });
    }

    @Test
    public void shouldNotUpdateDbIndexIfSelectFails() throws ExecutionException, InterruptedException {
        FiberUtil.runInFiber(() -> {
            long currentDb = jedis.getDB();
            try {
                int invalidDb = -1;
                jedis.select(invalidDb);

                fail("Should throw an exception if tried to select invalid db");
            } catch (JedisException e) {
                assertEquals(currentDb, jedis.getDB().intValue());
            }
        });
    }

    @Test
    public void allowUrlWithNoDBAndNoPassword() throws ExecutionException, InterruptedException {
        FiberUtil.runInFiber(() -> {
            Jedis jedis = new Jedis("redis://localhost:6380");
            jedis.auth("foobared");
            assertEquals(jedis.getClient().getHost(), "localhost");
            assertEquals(jedis.getClient().getPort(), 6380);
            assertEquals(jedis.getDB(), (Long) 0L);

            jedis = new Jedis("redis://localhost:6380/");
            jedis.auth("foobared");
            assertEquals(jedis.getClient().getHost(), "localhost");
            assertEquals(jedis.getClient().getPort(), 6380);
            assertEquals(jedis.getDB(), (Long) 0L);
        });
    }

    @Test
    public void checkCloseable() {
        jedis.close();
    }
}