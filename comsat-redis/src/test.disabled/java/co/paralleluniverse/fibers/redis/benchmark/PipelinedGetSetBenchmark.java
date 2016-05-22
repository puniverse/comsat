package co.paralleluniverse.fibers.redis.benchmark;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Calendar;

import co.paralleluniverse.fibers.redis.HostAndPortUtil;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;

public class PipelinedGetSetBenchmark {
  private static HostAndPort hnp = HostAndPortUtil.getRedisServers().get(0);
  private static final int TOTAL_OPERATIONS = 200000;

  public static void main(String[] args) throws UnknownHostException, IOException {
    Jedis jedis = new Jedis(hnp.getHost(), hnp.getPort());
    jedis.connect();
    jedis.auth("foobared");
    jedis.flushAll();

    long begin = Calendar.getInstance().getTimeInMillis();

    Pipeline p = jedis.pipelined();
    for (int n = 0; n <= TOTAL_OPERATIONS; n++) {
      String key = "foo" + n;
      p.set(key, "bar" + n);
      p.get(key);
    }
    p.sync();

    long elapsed = Calendar.getInstance().getTimeInMillis() - begin;

    jedis.disconnect();

    System.out.println(((1000 * 2 * TOTAL_OPERATIONS) / elapsed) + " ops");
  }
}