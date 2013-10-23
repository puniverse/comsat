package com.example;

import co.paralleluniverse.concurrent.util.ThreadUtil;
import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.ws.rs.client.AsyncClientBuilder;
import co.paralleluniverse.strands.SuspendableRunnable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;

public class JaxRSClientExample {
    public static void main(String[] args) throws Exception {

        final Client newClient = AsyncClientBuilder.newClient();
        final int NUM = 2000;
        final AtomicInteger ai = new AtomicInteger();

        final CountDownLatch cdl = new CountDownLatch(NUM);
        System.out.println("starting");
        for (int i = 0; i < NUM; i++) {
            new Fiber<Void>(new SuspendableRunnable() {
                @Override
                public void run() throws SuspendExecution, InterruptedException {
                    try {
                        Response resp = newClient.target("http://localhost:8080/test").request().buildGet().submit().get(5, TimeUnit.SECONDS);
                        if (resp.getStatus() == 200)
                            ai.incrementAndGet();
                    } catch (ExecutionException | TimeoutException ex) {
                        System.out.println(ex);
                    } finally {
                        cdl.countDown();
                    }
                }
            }).start();
        }
        cdl.await();

        ThreadUtil.dumpThreads();
        System.out.println("finished " + ai);
        System.exit(0);
    }
}
