package co.paralleluniverse.fibers.redis.commands;

import java.util.List;
import java.util.concurrent.ExecutionException;

import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.FiberUtil;
import co.paralleluniverse.strands.Strand;
import org.junit.Ignore;
import org.junit.Test;

import redis.clients.jedis.DebugParams;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisMonitor;
import redis.clients.jedis.exceptions.JedisDataException;

public class ControlCommandsTest extends JedisCommandTestBase {
    @Test
    public void save() throws ExecutionException, InterruptedException {
        FiberUtil.runInFiber(() -> {
            try {
                String status = jedis.save();
                assertEquals("OK", status);
            } catch (JedisDataException e) {
                assertTrue("ERR Background save already in progress".equalsIgnoreCase(e.getMessage()));
            }
        });
    }

    @Test
    public void bgsave() throws ExecutionException, InterruptedException {
        FiberUtil.runInFiber(() -> {
            try {
                String status = jedis.bgsave();
                assertEquals("Background saving started", status);
            } catch (JedisDataException e) {
                assertTrue("ERR Background save already in progress".equalsIgnoreCase(e.getMessage()));
            }
        });
    }

    @Test
    public void bgrewriteaof() throws ExecutionException, InterruptedException {
        FiberUtil.runInFiber(() -> {
            String scheduled = "Background append only file rewriting scheduled";
            String started = "Background append only file rewriting started";

            String status = jedis.bgrewriteaof();

            boolean ok = status.equals(scheduled) || status.equals(started);
            assertTrue(ok);
        });
    }

    @Test
    public void lastsave() throws InterruptedException, ExecutionException {
        FiberUtil.runInFiber(() -> {
            long saved = jedis.lastsave();
            assertTrue(saved > 0);
        });
    }

    @Test
    public void info() throws ExecutionException, InterruptedException {
        FiberUtil.runInFiber(() -> {
            String info = jedis.info();
            assertNotNull(info);
            info = jedis.info("server");
            assertNotNull(info);
        });
    }

    @Test
    public void readonly() throws ExecutionException, InterruptedException {
        FiberUtil.runInFiber(() -> {
            try {
                jedis.readonly();
            } catch (JedisDataException e) {
                assertTrue("ERR This instance has cluster support disabled".equalsIgnoreCase(e.getMessage()));
            }
        });
    }

    @Test @Ignore
    public void monitor() throws ExecutionException, InterruptedException {
        new Fiber(() -> {
            try {
                // sleep 100ms to make sure that monitor thread runs first
                Strand.sleep(100);
            } catch (InterruptedException ignored) {
            }
            Jedis j = new Jedis("localhost");
            j.auth("foobared");
            for (int i = 0; i < 5; i++) {
                j.incr("foobared");
            }
            j.disconnect();
        }).start();

        FiberUtil.runInFiber(() -> {
            jedis.monitor(new JedisMonitor() {
                private int count = 0;

                public void onCommand(String command) {
                    if (command.contains("INCR")) {
                        count++;
                    }
                    if (count == 5) {
                        client.disconnect();
                    }
                }
            });
        });
    }

    @Test
    public void configGet() throws ExecutionException, InterruptedException {
        FiberUtil.runInFiber(() -> {
            List<String> info = jedis.configGet("m*");
            assertNotNull(info);
        });
    }

    @Test
    public void configSet() throws ExecutionException, InterruptedException {
        FiberUtil.runInFiber(() -> {
            List<String> info = jedis.configGet("maxmemory");
            String memory = info.get(1);
            String status = jedis.configSet("maxmemory", "200");
            assertEquals("OK", status);
            jedis.configSet("maxmemory", memory);
        });
    }

    @Test @Ignore // TODO prio 1
    public void sync() throws ExecutionException, InterruptedException {
        FiberUtil.runInFiber(() -> jedis.sync());
    }

    @Test
    public void debug() throws ExecutionException, InterruptedException {
        FiberUtil.runInFiber(() -> {
            jedis.set("foo", "bar");
            String resp = jedis.debug(DebugParams.OBJECT("foo"));
            assertNotNull(resp);
            resp = jedis.debug(DebugParams.RELOAD());
            assertNotNull(resp);
        });
    }

    @Test @Ignore
    public void waitReplicas() throws ExecutionException, InterruptedException {
        FiberUtil.runInFiber(() -> {
            Long replicas = jedis.waitReplicas(1, 100);
            assertEquals(1, replicas.longValue());
        });
    }
}