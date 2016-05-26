/*
Copyright (c) 2011 Jonathan Leibiusky

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*/
package co.paralleluniverse.fibers.redis;

import java.util.ArrayList;
import java.util.List;

import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Protocol;

public final class HostAndPortUtil {
  private static List<HostAndPort> redisHostAndPortList = new ArrayList<HostAndPort>();
  private static List<HostAndPort> sentinelHostAndPortList = new ArrayList<HostAndPort>();
  private static List<HostAndPort> clusterHostAndPortList = new ArrayList<HostAndPort>();

  private HostAndPortUtil(){
    throw new InstantiationError( "Must not instantiate this class" );
  }

  static {
    redisHostAndPortList.add(new HostAndPort("localhost", Protocol.DEFAULT_PORT));
    redisHostAndPortList.add(new HostAndPort("localhost", Protocol.DEFAULT_PORT + 1));
    redisHostAndPortList.add(new HostAndPort("localhost", Protocol.DEFAULT_PORT + 2));
    redisHostAndPortList.add(new HostAndPort("localhost", Protocol.DEFAULT_PORT + 3));
    redisHostAndPortList.add(new HostAndPort("localhost", Protocol.DEFAULT_PORT + 4));
    redisHostAndPortList.add(new HostAndPort("localhost", Protocol.DEFAULT_PORT + 5));
    redisHostAndPortList.add(new HostAndPort("localhost", Protocol.DEFAULT_PORT + 6));

    sentinelHostAndPortList.add(new HostAndPort("localhost", Protocol.DEFAULT_SENTINEL_PORT));
    sentinelHostAndPortList.add(new HostAndPort("localhost", Protocol.DEFAULT_SENTINEL_PORT + 1));
    sentinelHostAndPortList.add(new HostAndPort("localhost", Protocol.DEFAULT_SENTINEL_PORT + 2));
    sentinelHostAndPortList.add(new HostAndPort("localhost", Protocol.DEFAULT_SENTINEL_PORT + 3));

    clusterHostAndPortList.add(new HostAndPort("localhost", 7379));
    clusterHostAndPortList.add(new HostAndPort("localhost", 7380));
    clusterHostAndPortList.add(new HostAndPort("localhost", 7381));
    clusterHostAndPortList.add(new HostAndPort("localhost", 7382));
    clusterHostAndPortList.add(new HostAndPort("localhost", 7383));
    clusterHostAndPortList.add(new HostAndPort("localhost", 7384));

    String envRedisHosts = System.getProperty("redis-hosts");
    String envSentinelHosts = System.getProperty("sentinel-hosts");
    String envClusterHosts = System.getProperty("cluster-hosts");

    redisHostAndPortList = parseHosts(envRedisHosts, redisHostAndPortList);
    sentinelHostAndPortList = parseHosts(envSentinelHosts, sentinelHostAndPortList);
    clusterHostAndPortList = parseHosts(envClusterHosts, clusterHostAndPortList);
  }

  public static List<HostAndPort> parseHosts(String envHosts,
      List<HostAndPort> existingHostsAndPorts) {

    if (null != envHosts && 0 < envHosts.length()) {

      String[] hostDefs = envHosts.split(",");

      if (null != hostDefs && 2 <= hostDefs.length) {

        List<HostAndPort> envHostsAndPorts = new ArrayList<HostAndPort>(hostDefs.length);

        for (String hostDef : hostDefs) {

          String[] hostAndPort = hostDef.split(":");

          if (null != hostAndPort && 2 == hostAndPort.length) {
            String host = hostAndPort[0];
            int port = Protocol.DEFAULT_PORT;

            try {
              port = Integer.parseInt(hostAndPort[1]);
            } catch (final NumberFormatException nfe) {
            }

            envHostsAndPorts.add(new HostAndPort(host, port));
          }
        }

        return envHostsAndPorts;
      }
    }

    return existingHostsAndPorts;
  }

  public static List<HostAndPort> getRedisServers() {
    return redisHostAndPortList;
  }

  public static List<HostAndPort> getSentinelServers() {
    return sentinelHostAndPortList;
  }

  public static List<HostAndPort> getClusterServers() {
    return clusterHostAndPortList;
  }
}
