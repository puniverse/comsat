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
 * Ported from the corresponding Jedis test, Copyright (c) 2011 Jonathan Leibiusky
 */
package co.paralleluniverse.fibers.redis.commands;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.FiberUtil;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.Suspendable;
import co.paralleluniverse.fibers.redis.BinaryJedisPubSub;
import co.paralleluniverse.fibers.redis.Jedis;
import co.paralleluniverse.fibers.redis.JedisPubSub;
import co.paralleluniverse.strands.Strand;
import co.paralleluniverse.strands.channels.Channel;
import co.paralleluniverse.strands.channels.Channels;
import org.junit.Ignore;
import org.junit.Test;

import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisException;
import redis.clients.util.SafeEncoder;

/**
 * @author circlespainter
 */
public class PublishSubscribeCommandsTest extends JedisCommandTestBase {
    private void publishOne(final String channel, final String message) {
        Strand t = new Fiber(() -> {
            try {
                Jedis j = createJedis();
                j.publish(channel, message);
                j.disconnect();
            } catch (Exception ignored) {
            }
        });
        t.start();
    }

    @Test
    public void subscribe() throws InterruptedException, ExecutionException, SuspendExecution {
        final Channel<Object> resCh = Channels.newChannel(1);
        FiberUtil.runInFiber(() -> jedis.subscribe(new JedisPubSub() {
            @Suspendable
            public void onMessage(String channel, String message) {
                assertEquals("foo", channel);
                assertEquals("exit", message);
                unsubscribe();
            }

            @Suspendable
            public void onSubscribe(String channel, int subscribedChannels) {
                assertEquals("foo", channel);
                assertEquals(1, subscribedChannels);

                // now that I'm subscribed... publish
                publishOne("foo", "exit");
            }

            @Suspendable
            public void onUnsubscribe(String channel, int subscribedChannels) {
                assertEquals("foo", channel);
                assertEquals(0, subscribedChannels);
                ping(resCh);
            }
        }, "foo"));

        resCh.receive(); // Wait for asserts
    }

    @Test
    public void pubSubChannels() throws ExecutionException, InterruptedException, SuspendExecution {
        final Channel<Object> resCh = Channels.newChannel(1);

        FiberUtil.runInFiber(() -> {
            final List<String> expectedActiveChannels = Arrays
                .asList("testchan1", "testchan2", "testchan3");
            jedis.subscribe(new JedisPubSub() {
                private int count = 0;

                @Override
                @Suspendable
                public void onSubscribe(String channel, int subscribedChannels) {
                    count++;
                    // All channels are subscribed
                    if (count == 3) {
                        Jedis otherJedis = createJedis();
                        List<String> activeChannels = otherJedis.pubsubChannels("test*");
                        assertTrue(expectedActiveChannels.containsAll(activeChannels));
                        ping(resCh);
                    }
                }
            }, "testchan1", "testchan2", "testchan3");
        });

        resCh.receive(); // Wait for all asserts
    }

    @Test
    public void pubSubNumPat() throws ExecutionException, InterruptedException, SuspendExecution {
        final Channel<Object> resCh = Channels.newChannel(1);

        FiberUtil.runInFiber(() -> jedis.psubscribe(new JedisPubSub() {
            private int count = 0;

            @Override
            @Suspendable
            public void onPSubscribe(String pattern, int subscribedChannels) {
                count++;
                if (count == 3) {
                    Jedis otherJedis = createJedis();
                    Long numPatterns = otherJedis.pubsubNumPat();
                    assertEquals(new Long(2L), numPatterns);
                    punsubscribe();
                    ping(resCh);
                }
            }

        }, "test*", "test*", "chan*"));

        resCh.receive(); // Wait for all asserts
    }

    @Test
    public void pubSubNumSub() throws ExecutionException, InterruptedException, SuspendExecution {
        final Channel<Object> resCh = Channels.newChannel(1);

        FiberUtil.runInFiber(() -> {
            final Map<String, String> expectedNumSub = new HashMap<>();
            expectedNumSub.put("testchannel2", "1");
            expectedNumSub.put("testchannel1", "1");
            jedis.subscribe(new JedisPubSub() {
                private int count = 0;

                @Override
                @Suspendable
                public void onSubscribe(String channel, int subscribedChannels) {
                    count++;
                    if (count == 2) {
                        Jedis otherJedis = createJedis();
                        Map<String, String> numSub = otherJedis.pubsubNumSub("testchannel1", "testchannel2");
                        assertEquals(expectedNumSub, numSub);
                        unsubscribe();
                        ping(resCh);
                    }
                }
            }, "testchannel1", "testchannel2");
        });

        resCh.receive(); // Wait for all asserts
    }

    @Test
    public void subscribeMany() throws InterruptedException, ExecutionException, SuspendExecution {
        final Channel<Object> resCh = Channels.newChannel(1);

        FiberUtil.runInFiber(() -> jedis.subscribe(new JedisPubSub() {
            @Suspendable
            public void onMessage(String channel, String message) {
                unsubscribe(channel);
                ping(resCh);
            }

            @Suspendable
            public void onSubscribe(String channel, int subscribedChannels) {
                publishOne(channel, "exit");
            }

        }, "foo", "bar"));

        resCh.receive(); // Wait for all asserts
    }

    @Test
    public void psubscribe() throws InterruptedException, ExecutionException, SuspendExecution {
        final Channel<Object> resCh = Channels.newChannel(1);

        FiberUtil.runInFiber(() -> jedis.psubscribe(new JedisPubSub() {
            @Suspendable
            public void onPSubscribe(String pattern, int subscribedChannels) {
                assertEquals("foo.*", pattern);
                assertEquals(1, subscribedChannels);
                publishOne("foo.bar", "exit");
            }

            @Suspendable
            public void onPMessage(String pattern, String channel, String message) {
                assertEquals("foo.*", pattern);
                assertEquals("foo.bar", channel);
                assertEquals("exit", message);
                punsubscribe();
            }

            @Suspendable
            public void onPUnsubscribe(String pattern, int subscribedChannels) {
                assertEquals("foo.*", pattern);
                assertEquals(0, subscribedChannels);
                ping(resCh);
            }
        }, "foo.*"));

        resCh.receive(); // Wait for all asserts
    }

    @Test
    public void psubscribeMany() throws InterruptedException, ExecutionException, SuspendExecution {
        final Channel<Object> resCh = Channels.newChannel(1);

        FiberUtil.runInFiber(() -> jedis.psubscribe(new JedisPubSub() {
            @Suspendable
            public void onPSubscribe(String pattern, int subscribedChannels) {
                publishOne(pattern.replace("*", "123"), "exit");
            }

            @Suspendable
            public void onPMessage(String pattern, String channel, String message) {
                punsubscribe(pattern);
                ping(resCh);
            }
        }, "foo.*", "bar.*"));

        resCh.receive(); // Wait for all asserts
    }

    @Test
    public void subscribeLazily() throws InterruptedException, ExecutionException, SuspendExecution {
        final Channel<Object> resCh = Channels.newChannel(1);

        FiberUtil.runInFiber(() -> {
            final JedisPubSub pubsub = new JedisPubSub() {
                @Suspendable
                public void onSubscribe(String channel, int subscribedChannels) {
                    publishOne(channel, "exit");
                    if (!channel.equals("bar")) {
                        this.subscribe("bar");
                        this.psubscribe("bar.*");
                    }
                }

                @Suspendable
                public void onPSubscribe(String pattern, int subscribedChannels) {
                    publishOne(pattern.replace("*", "123"), "exit");
                }

                @Suspendable
                public void onMessage(String channel, String message) {
                    unsubscribe(channel);
                    ping(resCh);
                }

                @Suspendable
                public void onPMessage(String pattern, String channel, String message) {
                    punsubscribe(pattern);
                    ping(resCh);
                }
            };

            jedis.subscribe(pubsub, "foo");
        });

        resCh.receive(); // Wait for all asserts
        resCh.receive(); // Wait for all asserts
    }

    @Test
    public void binarySubscribe() throws InterruptedException, ExecutionException, SuspendExecution {
        final Channel<Object> resCh = Channels.newChannel(1);

        FiberUtil.runInFiber(() -> jedis.subscribe(new BinaryJedisPubSub() {
            @Suspendable
            public void onSubscribe(byte[] channel, int subscribedChannels) {
                assertTrue(Arrays.equals(SafeEncoder.encode("foo"), channel));
                assertEquals(1, subscribedChannels);
                publishOne(SafeEncoder.encode(channel), "exit");
            }

            @Suspendable
            public void onMessage(byte[] channel, byte[] message) {
                assertTrue(Arrays.equals(SafeEncoder.encode("foo"), channel));
                assertTrue(Arrays.equals(SafeEncoder.encode("exit"), message));
                unsubscribe();
            }

            @Suspendable
            public void onUnsubscribe(byte[] channel, int subscribedChannels) {
                assertTrue(Arrays.equals(SafeEncoder.encode("foo"), channel));
                assertEquals(0, subscribedChannels);
                ping(resCh);
            }
        }, SafeEncoder.encode("foo")));

        resCh.receive(); // Wait for all asserts
    }

    @Test
    public void binarySubscribeMany() throws InterruptedException, ExecutionException, SuspendExecution {
        final Channel<Object> resCh = Channels.newChannel(1);

        FiberUtil.runInFiber(() -> jedis.subscribe(new BinaryJedisPubSub() {
            @Suspendable
            public void onSubscribe(byte[] channel, int subscribedChannels) {
                publishOne(SafeEncoder.encode(channel), "exit");
            }

            @Suspendable
            public void onMessage(byte[] channel, byte[] message) {
                unsubscribe(channel);
                ping(resCh);
            }
        }, SafeEncoder.encode("foo"), SafeEncoder.encode("bar")));

        resCh.receive(); // Wait for all asserts
    }

    @Test
    public void binaryPsubscribe() throws InterruptedException, ExecutionException, SuspendExecution {
        final Channel<Object> resCh = Channels.newChannel(1);

        FiberUtil.runInFiber(() -> jedis.psubscribe(new BinaryJedisPubSub() {
            @Suspendable
            public void onPSubscribe(byte[] pattern, int subscribedChannels) {
                assertTrue(Arrays.equals(SafeEncoder.encode("foo.*"), pattern));
                assertEquals(1, subscribedChannels);
                publishOne(SafeEncoder.encode(pattern).replace("*", "bar"), "exit");
            }

            @Suspendable
            public void onPMessage(byte[] pattern, byte[] channel, byte[] message) {
                assertTrue(Arrays.equals(SafeEncoder.encode("foo.*"), pattern));
                assertTrue(Arrays.equals(SafeEncoder.encode("foo.bar"), channel));
                assertTrue(Arrays.equals(SafeEncoder.encode("exit"), message));
                punsubscribe();
            }

            @Suspendable
            public void onPUnsubscribe(byte[] pattern, int subscribedChannels) {
                assertTrue(Arrays.equals(SafeEncoder.encode("foo.*"), pattern));
                assertEquals(0, subscribedChannels);
                ping(resCh);
            }
        }, SafeEncoder.encode("foo.*")));

        resCh.receive(); // Wait for all asserts
    }

    @Test
    public void binaryPsubscribeMany() throws InterruptedException, ExecutionException, SuspendExecution {
        final Channel<Object> resCh = Channels.newChannel(1);

        FiberUtil.runInFiber(() -> jedis.psubscribe(new BinaryJedisPubSub() {
            @Suspendable
            public void onPSubscribe(byte[] pattern, int subscribedChannels) {
                publishOne(SafeEncoder.encode(pattern).replace("*", "123"), "exit");
            }

            @Suspendable
            public void onPMessage(byte[] pattern, byte[] channel, byte[] message) {
                punsubscribe(pattern);
                ping(resCh);
            }
        }, SafeEncoder.encode("foo.*"), SafeEncoder.encode("bar.*")));

        resCh.receive(); // Wait for all asserts
    }

    @Test
    public void binarySubscribeLazily() throws InterruptedException, ExecutionException, SuspendExecution {
        final Channel<Object> resCh = Channels.newChannel(1);

        FiberUtil.runInFiber(() -> {
            final BinaryJedisPubSub pubsub = new BinaryJedisPubSub() {
                @Suspendable
                public void onSubscribe(byte[] channel, int subscribedChannels) {
                    publishOne(SafeEncoder.encode(channel), "exit");

                    if (!SafeEncoder.encode(channel).equals("bar")) {
                        this.subscribe(SafeEncoder.encode("bar"));
                        this.psubscribe(SafeEncoder.encode("bar.*"));
                    }
                }

                @Suspendable
                public void onPSubscribe(byte[] pattern, int subscribedChannels) {
                    publishOne(SafeEncoder.encode(pattern).replace("*", "123"), "exit");
                }

                @Suspendable
                public void onMessage(byte[] channel, byte[] message) {
                    unsubscribe(channel);
                    ping(resCh);
                }

                @Suspendable
                public void onPMessage(byte[] pattern, byte[] channel, byte[] message) {
                    punsubscribe(pattern);
                    ping(resCh);
                }
            };

            jedis.subscribe(pubsub, SafeEncoder.encode("foo"));
        });

        resCh.receive(); // Wait for all asserts
        resCh.receive(); // Wait for all asserts
    }

    @Test(expected = JedisConnectionException.class)
    public void unsubscribeWhenNotSubscribed() throws InterruptedException, ExecutionException {
        try {
            FiberUtil.runInFiber(() -> {
                JedisPubSub pubsub = new JedisPubSub() {
                };
                pubsub.unsubscribe();
            });
        } catch (final ExecutionException e) {
            final Throwable t = e.getCause();
            if (t != null && t instanceof JedisException)
                throw (JedisException) t;
        }
    }

    @Test(expected = JedisConnectionException.class) @Ignore // Requires replication
    public void handleClientOutputBufferLimitForSubscribeTooSlow() throws InterruptedException, ExecutionException {
        FiberUtil.runInFiber(() -> {
            final Jedis j = createJedis();
            final AtomicBoolean exit = new AtomicBoolean(false);

            final Strand t = new Fiber(() -> {
                try {

                    // we already set jedis1 config to
                    // client-output-buffer-limit pubsub 256k 128k 5
                    // it means if subscriber delayed to receive over 256k or
                    // 128k continuously 5 sec,
                    // redis disconnects subscriber

                    // we publish over 100M data for making situation for exceed
                    // client-output-buffer-limit
                    String veryLargeString = makeLargeString(10485760);

                    // 10M * 10 = 100M
                    for (int i = 0; i < 10 && !exit.get(); i++) {
                        j.publish("foo", veryLargeString);
                    }

                    j.disconnect();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            try {
                jedis.subscribe(new JedisPubSub() {
                    @Suspendable
                    public void onMessage(String channel, String message) {
                        try {
                            // wait 0.5 secs to slow down subscribe and
                            // client-output-buffer exceed
                            // System.out.println("channel - " + channel +
                            // " / message - " + message);
                            Strand.sleep(100);
                        } catch (Exception e) {
                            try {
                                t.join();
                            } catch (InterruptedException ignored) {
                            } catch (ExecutionException e1) {
                                throw new RuntimeException(e1);
                            }

                            fail(e.getMessage());
                        }
                    }
                }, "foo");
                t.start();
                Strand.sleep(100000);
            } finally {
                // exit the publisher thread. if exception is thrown, thread might
                // still keep publishing things.
                exit.set(true);
                if (t.isAlive())
                    t.interrupt();
            }
        });
    }

    private String makeLargeString(int size) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < size; i++)
            sb.append((char) ('a' + i % 26));

        return sb.toString();
    }

    @Suspendable
    private static void ping(Channel<Object> resCh) {
        try {
            resCh.send(new Object());
        } catch (final SuspendExecution e) {
            throw new AssertionError(e);
        } catch (final InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
