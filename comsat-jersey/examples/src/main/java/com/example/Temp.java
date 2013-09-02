package com.example;

import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.Strand;
import co.paralleluniverse.strands.SuspendableRunnable;
import com.google.common.util.concurrent.RateLimiter;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class Temp {
    public static ExecutorService es = Executors.newCachedThreadPool(new ThreadFactoryBuilder().setDaemon(true).setNameFormat("eitan-").build());
    public static final int RATE = 4000;

    public static void main(String args[]) throws InterruptedException {
        final AtomicInteger ai = new AtomicInteger();
        RateLimiter rl = RateLimiter.create(RATE);
        long start = System.nanoTime();
        long last = start;
        double rate = RATE;
        for (int i = 0;; i++) {
            rl.acquire();
            ai.incrementAndGet();
            submitToTP(ai);
//            submitToFiber(ai);
            if (i > rate) {
                i=0;
                long curr = System.nanoTime();
                if (curr - start > 120000000000L)
                    break;
                double d = 1e9 * rate / (curr - last);
                final int get = ai.get();
                if (rate < 140000) {
                    rate+=1000;
                    rl.setRate(rate);
                }
                System.out.println("current rate is " + d + " open: " + get);
                last = curr;

            }
        }
        long lat = System.nanoTime() - start;
        System.out.println(lat / 1000000);
    }

    private static void submitToFiber(final AtomicInteger ai) {
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                Strand.sleep(200);
                ai.decrementAndGet();
            }
        }).start();

    }

    private static void submitToTP(final AtomicInteger ai) {
        es.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(200);
                    ai.decrementAndGet();
                } catch (InterruptedException ex) {
                    throw new AssertionError();
                }
            }
        });
    }
}
