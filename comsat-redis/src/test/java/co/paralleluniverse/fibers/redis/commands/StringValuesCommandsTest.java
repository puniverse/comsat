package co.paralleluniverse.fibers.redis.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import co.paralleluniverse.fibers.FiberUtil;
import org.junit.Ignore;
import org.junit.Test;

import redis.clients.jedis.exceptions.JedisDataException;

public class StringValuesCommandsTest extends JedisCommandTestBase {
    @Test
    public void setAndGet() throws ExecutionException, InterruptedException {
        FiberUtil.runInFiber(() -> {
            String status = jedis.set("foo", "bar");
            assertEquals("OK", status);

            String value = jedis.get("foo");
            assertEquals("bar", value);

            assertEquals(null, jedis.get("bar"));
        });
    }

    @Test
    public void getSet() throws ExecutionException, InterruptedException {
        FiberUtil.runInFiber(() -> {
            String value = jedis.getSet("foo", "bar");
            assertEquals(null, value);
            value = jedis.get("foo");
            assertEquals("bar", value);
        });
    }

    @Test
    public void mget() throws ExecutionException, InterruptedException {
        FiberUtil.runInFiber(() -> {
            List<String> values = jedis.mget("foo", "bar");
            List<String> expected = new ArrayList<>();
            expected.add(null);
            expected.add(null);

            assertEquals(expected, values);

            jedis.set("foo", "bar");

            expected = new ArrayList<>();
            expected.add("bar");
            expected.add(null);
            values = jedis.mget("foo", "bar");

            assertEquals(expected, values);

            jedis.set("bar", "foo");

            expected = new ArrayList<>();
            expected.add("bar");
            expected.add("foo");
            values = jedis.mget("foo", "bar");

            assertEquals(expected, values);
        });
    }

    @Test
    public void setnx() throws ExecutionException, InterruptedException {
        FiberUtil.runInFiber(() -> {
            long status = jedis.setnx("foo", "bar");
            assertEquals(1, status);
            assertEquals("bar", jedis.get("foo"));

            status = jedis.setnx("foo", "bar2");
            assertEquals(0, status);
            assertEquals("bar", jedis.get("foo"));
        });
    }

    @Test
    public void setex() throws ExecutionException, InterruptedException {
        FiberUtil.runInFiber(() -> {
            String status = jedis.setex("foo", 20, "bar");
            assertEquals("OK", status);
            long ttl = jedis.ttl("foo");
            assertTrue(ttl > 0 && ttl <= 20);
        });
    }

    @Test
    public void mset() throws ExecutionException, InterruptedException {
        FiberUtil.runInFiber(() -> {
            String status = jedis.mset("foo", "bar", "bar", "foo");
            assertEquals("OK", status);
            assertEquals("bar", jedis.get("foo"));
            assertEquals("foo", jedis.get("bar"));
        });
    }

    @Test
    public void msetnx() throws ExecutionException, InterruptedException {
        FiberUtil.runInFiber(() -> {
            long status = jedis.msetnx("foo", "bar", "bar", "foo");
            assertEquals(1, status);
            assertEquals("bar", jedis.get("foo"));
            assertEquals("foo", jedis.get("bar"));

            status = jedis.msetnx("foo", "bar1", "bar2", "foo2");
            assertEquals(0, status);
            assertEquals("bar", jedis.get("foo"));
            assertEquals("foo", jedis.get("bar"));
        });
    }

    @Test(expected = JedisDataException.class) @Ignore // TODO prio 2
    public void incrWrongValue() throws ExecutionException, InterruptedException {
        FiberUtil.runInFiber(() -> {
            jedis.set("foo", "bar");
            jedis.incr("foo");
        });
    }

    @Test
    public void incr() throws ExecutionException, InterruptedException {
        FiberUtil.runInFiber(() -> {
            long value = jedis.incr("foo");
            assertEquals(1, value);
            value = jedis.incr("foo");
            assertEquals(2, value);
        });
    }

    @Test(expected = JedisDataException.class) @Ignore // TODO prio 1
    public void incrByWrongValue() throws ExecutionException, InterruptedException {
        FiberUtil.runInFiber(() -> {
            jedis.set("foo", "bar");
            jedis.incrBy("foo", 2);
        });
    }

    @Test
    public void incrBy() throws ExecutionException, InterruptedException {
        FiberUtil.runInFiber(() -> {
            long value = jedis.incrBy("foo", 2);
            assertEquals(2, value);
            value = jedis.incrBy("foo", 2);
            assertEquals(4, value);
        });
    }

    @Test(expected = JedisDataException.class) @Ignore // TODO prio 1
    public void incrByFloatWrongValue() throws ExecutionException, InterruptedException {
        FiberUtil.runInFiber(() -> {
            jedis.set("foo", "bar");
            jedis.incrByFloat("foo", 2d);
        });
    }

    @Test(expected = JedisDataException.class) @Ignore // TODO prio 1
    public void decrWrongValue() throws ExecutionException, InterruptedException {
        FiberUtil.runInFiber(() -> {
            jedis.set("foo", "bar");
            jedis.decr("foo");
        });
    }

    @Test
    public void decr() throws ExecutionException, InterruptedException {
        FiberUtil.runInFiber(() -> {
            long value = jedis.decr("foo");
            assertEquals(-1, value);
            value = jedis.decr("foo");
            assertEquals(-2, value);
        });
    }

    @Test(expected = JedisDataException.class)
    public void decrByWrongValue() {
        jedis.set("foo", "bar");
        jedis.decrBy("foo", 2);
    }

    @Test
    public void decrBy() throws ExecutionException, InterruptedException {
        FiberUtil.runInFiber(() -> {
            long value = jedis.decrBy("foo", 2);
            assertEquals(-2, value);
            value = jedis.decrBy("foo", 2);
            assertEquals(-4, value);
        });
    }

    @Test
    public void append() throws ExecutionException, InterruptedException {
        FiberUtil.runInFiber(() -> {
            long value = jedis.append("foo", "bar");
            assertEquals(3, value);
            assertEquals("bar", jedis.get("foo"));
            value = jedis.append("foo", "bar");
            assertEquals(6, value);
            assertEquals("barbar", jedis.get("foo"));
        });
    }

    @Test
    public void substr() throws ExecutionException, InterruptedException {
        FiberUtil.runInFiber(() -> {
            jedis.set("s", "This is a string");
            assertEquals("This", jedis.substr("s", 0, 3));
            assertEquals("ing", jedis.substr("s", -3, -1));
            assertEquals("This is a string", jedis.substr("s", 0, -1));
            assertEquals(" string", jedis.substr("s", 9, 100000));
        });
    }

    @Test
    public void strlen() throws ExecutionException, InterruptedException {
        FiberUtil.runInFiber(() -> {
            jedis.set("s", "This is a string");
            assertEquals("This is a string".length(), jedis.strlen("s").intValue());
        });
    }

    @Test
    public void incrLargeNumbers() throws ExecutionException, InterruptedException {
        FiberUtil.runInFiber(() -> {
            long value = jedis.incr("foo");
            assertEquals(1, value);
            assertEquals(1L + Integer.MAX_VALUE, (long) jedis.incrBy("foo", Integer.MAX_VALUE));
        });
    }

    @Test(expected = JedisDataException.class) @Ignore // TODO prio 1
    public void incrReallyLargeNumbers() throws ExecutionException, InterruptedException {
        FiberUtil.runInFiber(() -> {
            jedis.set("foo", Long.toString(Long.MAX_VALUE));
            long value = jedis.incr("foo");
            assertEquals(Long.MIN_VALUE, value);
        });
    }

    @Test
    public void incrByFloat() throws ExecutionException, InterruptedException {
        FiberUtil.runInFiber(() -> {
            double value = jedis.incrByFloat("foo", 10.5);
            assertEquals(10.5, value, 0.0);
            value = jedis.incrByFloat("foo", 0.1);
            assertEquals(10.6, value, 0.0);
        });
    }

    @Test
    public void psetex() throws ExecutionException, InterruptedException {
        FiberUtil.runInFiber(() -> {
            //noinspection deprecation
            String status = jedis.psetex("foo", 20000, "bar");
            assertEquals("OK", status);
            long ttl = jedis.ttl("foo");
            assertTrue(ttl > 0 && ttl <= 20000);
        });
    }
}