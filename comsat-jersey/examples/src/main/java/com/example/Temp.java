package com.example;

import co.paralleluniverse.concurrent.util.ThreadUtil;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Temp {
    public static void main(String args[]) throws InterruptedException {
        final CountDownLatch cdl = new CountDownLatch(1000);
        ExecutorService ste = Executors.newFixedThreadPool(10);
        for (int i = 0; i < 1000; i++) {
            ste.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException ex) {
                    } finally {
                        cdl.countDown();
                    }
                }
            });
        }
        System.out.println(cdl.getCount());
        boolean await = cdl.await(10000, TimeUnit.MILLISECONDS);
        System.out.println(cdl.getCount());
        System.out.println(await);
        ThreadUtil.dumpThreads();
        System.exit(0);
    }
}
