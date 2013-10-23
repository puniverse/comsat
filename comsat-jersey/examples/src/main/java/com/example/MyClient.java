/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.example;

import co.paralleluniverse.common.benchmark.StripedHistogram;
import co.paralleluniverse.common.benchmark.StripedLongTimeSeries;
import co.paralleluniverse.common.util.SameThreadExecutorService;
import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.ws.rs.client.AsyncClientBuilder;
//import co.paralleluniverse.fibers.ws.rs.client.AsyncClientBuilder;
import co.paralleluniverse.jersey.connector.AsyncHttpConnector;
import co.paralleluniverse.strands.Strand;
import co.paralleluniverse.strands.SuspendableRunnable;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.spi.RequestExecutorsProvider;

/**
 *
 * @author eitan
 */
public class MyClient {
    private final static AtomicInteger ai = new AtomicInteger();
    private final static int size = 10;
//    private static final String URI = "http://localhost:8080/myresource";
    private static final String URI = "http://localhost:8080/WebAppA/NewServlet?sleep=10&times=1";
    //    private static final String URI = "http://localhost:8080/fiber/hello";
//    private static final String URI = "http://localhost:8080/helloworld";
//    private static final String URI = "http://localhost:8080/sync/hello";
//    private static final String URI = "http://localhost:8080/async/hello";
    private static final int fibers = 1;
    private final static CountDownLatch cdl = new CountDownLatch(size * fibers);

    private static void userMethod(final Client cli) throws SuspendExecution {
        Response resp = cli.target(URI).request().get();
        if (resp.getStatus() == 200)
            ai.incrementAndGet();
        cdl.countDown();

    }

    private static Future<Response> asyncUserMethodSubmit(final Client cli) throws SuspendExecution {
        return cli.target(URI).request().buildGet().submit();
    }

    private static void asyncUserMethodJoin(Future<Response> future) throws SuspendExecution, InterruptedException, ExecutionException {
        Response resp = future.get();
        if (resp.getStatus() == 200)
            ai.incrementAndGet();
        cdl.countDown();
    }

    public static void main(String[] args) throws Exception {
        mainFiber(args);
        mainThread(args);
    }

    public static void mainThread(String[] args) throws Exception {
        final StripedLongTimeSeries sts = new StripedLongTimeSeries(100000, false);
        final StripedHistogram sh = new StripedHistogram(1500000L, 5);
        List<Fiber<Void>> list = new ArrayList<>(size);
        ClientConfig cc = new ClientConfig()
                .property(ClientProperties.ASYNC_THREADPOOL_SIZE, 20)
                .property(ClientProperties.READ_TIMEOUT, 5000)
                .property(ClientProperties.CONNECT_TIMEOUT, 5000);
        cc.connector(new AsyncHttpConnector(cc));
        final Client newClient = ClientBuilder.newClient(cc).register(new RequestExecutorsProvider() {
            private ExecutorService tp = SameThreadExecutorService.getExecutorService();

            @Override
            public ExecutorService getRequestingExecutor() {
                return tp;
            }
        }, RequestExecutorsProvider.class);
        final int NUM = 20;
        ExecutorService es = Executors.newFixedThreadPool(NUM, new ThreadFactoryBuilder().setDaemon(true).build());

        final Semaphore sem = new Semaphore(NUM);
        boolean full = false;
        final int stat = 500;
        for (int i = stat;;) {
            if (!full && sem.availablePermits() < 2)
                full = true;
            long start = System.nanoTime();
            sem.acquire();
            es.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        List<Future<Response>> list = new ArrayList<>();
//                        for (int j = 0; j < size; j++)
//                            userMethod(newClient);
                        for (int j = 0; j < size; j++)
                            list.add(asyncUserMethodSubmit(newClient));
                        for (Future<Response> future : list) {
                            asyncUserMethodJoin(future);
                        }
                    } catch (InterruptedException | ExecutionException | SuspendExecution ex) {
                        throw new AssertionError(ex);
                    }
                    sem.release();
                }
            });
            if (full) {
                final long latency = (System.nanoTime() - start) / 1000;
                if (latency < 1500000L)
//                System.out.println(latency);
//                sts.record(start, latency);
                    sh.recordValue(latency); // sh.recordValue(latency, 10000);
                if (--i == 0) {
                    i = stat;
//                    System.out.println("===========");
//                    System.out.println(sh.getHistogramData().getMaxValue());
                    System.out.println(sh.getHistogramData().getMean());
                    sh.reset();
                }
//                ThreadUtil.dumpThreads();
            }
            if (false)
                break;
        }
    }

    public static void mainFiber(String[] args) throws Exception {
        final StripedLongTimeSeries sts = new StripedLongTimeSeries(100000, false);
        final StripedHistogram sh = new StripedHistogram(1500000L, 5);
        List<Fiber<Void>> list = new ArrayList<>(size);
        ClientConfig cc = new ClientConfig()
                .property(ClientProperties.ASYNC_THREADPOOL_SIZE, 20)
                .property(ClientProperties.READ_TIMEOUT, 5000)
                .property(ClientProperties.CONNECT_TIMEOUT, 5000);
        cc.connector(new AsyncHttpConnector(cc));
        final Client newClient = AsyncClientBuilder.newClient(cc).register(new RequestExecutorsProvider() {
////            private ExecutorService tp = Executors.newFixedThreadPool(20,
////                    new ThreadFactoryBuilder().setDaemon(true).setNameFormat("jersey-ron-async-executor-%d").build());
            private ExecutorService tp = SameThreadExecutorService.getExecutorService();
//                    Executors.newFixedThreadPool(20,
////                    new ThreadFactoryBuilder().setDaemon(true).setNameFormat("jersey-ron-async-executor-%d").build());

            @Override
            public ExecutorService getRequestingExecutor() {
                return tp;
            }
        }, RequestExecutorsProvider.class);
        final int NUM = 20;

        final Semaphore sem = new Semaphore(NUM);
        boolean full = false;
        final int stat = 1000;
        for (int i = stat;;) {
            if (!full && sem.availablePermits() < 2)
                full = true;
            long start = System.nanoTime();
            sem.acquire();
            new Fiber<Void>(new SuspendableRunnable() {
                @Override
                public void run() throws SuspendExecution, InterruptedException {
                    try {
                        List<Future<Response>> list = new ArrayList<>();
                        for (int j = 0; j < size; j++)
                            list.add(asyncUserMethodSubmit(newClient));
                        for (Future<Response> future : list) {
                            asyncUserMethodJoin(future);
                        }
                    } catch (InterruptedException | ExecutionException ex) {
                        throw new AssertionError(ex);
                    }
                    sem.release();
//                    for (int j = 0; j < size; j++)
//                        userMethod(newClient);
//                    sem.release();
                }
            }).start();
            if (full) {
                final long latency = (System.nanoTime() - start) / 1000;
                if (latency < 1500000L)
//                System.out.println(latency);
//                sts.record(start, latency);
                    sh.recordValue(latency); // sh.recordValue(latency, 10000);
                if (--i == 0) {
                    i = stat;
//                    System.out.println("===========");
//                    System.out.println(sh.getHistogramData().getMaxValue());
                    System.out.println(sh.getHistogramData().getMean());
                    sh.reset();
                }
//                ThreadUtil.dumpThreads();
            }
            if (false)
                break;
        }

        long start = System.nanoTime();
        for (int i = 0; i < fibers; i++) {
            new Fiber<Void>(new SuspendableRunnable() {
                @Override
                public void run() throws SuspendExecution, InterruptedException {
                    for (int j = 0; j < size; j++) {
                        long start = System.nanoTime();
                        userMethod(newClient);
                        final long latency = (System.nanoTime() - start) / 1000;
                        sts.record(start, latency);
                        sh.recordValue(latency); // sh.recordValue(latency, 10000);
                        Strand.sleep(1);
                    }

                }
            }).start();
//            userMethod(ClientBuilder.newClient());

        }

        System.out.println("waiting");
        cdl.await(30, TimeUnit.SECONDS);
        for (StripedLongTimeSeries.Record rec : sts.getRecords())
            System.out.println("STS: " + rec.timestamp + " " + rec.value);
        sh.getHistogramData().outputPercentileDistribution(System.out, 5, 1.0);
//        Thread.sleep(10000);
        System.out.println("finished successfully");
//        ThreadUtil.dumpThreads();

        //jersey-client-async-executor will finish in 1 minute 
        //in order to do it quicker we have to provide another RequestExecutorsProvider.class
        System.out.println("finished");
        System.exit(0);
    }
//
//    public static abstract class AsyncRs extends FiberAsync<Response, InvocationCallback<Response>, Void, RuntimeException> implements InvocationCallback<Response> {
//        private final Fiber fiber;
//
//        public AsyncRs() {
//            this.fiber = Fiber.currentFiber();
//        }
//
//        @Override
//        public Response run() throws SuspendExecution {
//            try {
//                final Response run = super.run();
//                return run;
//            } catch (InterruptedException e) {
//                throw new AssertionError(e);
//            }
//        }
//
//        @Override
//        public void completed(Response response) {
//            super.completed(response, fiber);
//        }
//
//        @Override
//        public void failed(Throwable throwable) {
//            super.failed(throwable, fiber);
//        }
//    }
//
//    public static class SpiedEs implements ExecutorService {
//        ExecutorService es;
//
//        public SpiedEs(ExecutorService es) {
//            pmn();
//            this.es = es;
//        }
//
//        public void shutdown() {
//            pmn();
//            es.shutdown();
//        }
//
//        public List<Runnable> shutdownNow() {
//            pmn();
//            return es.shutdownNow();
//        }
//
//        public boolean isShutdown() {
//            pmn();
//            return es.isShutdown();
//        }
//
//        public boolean isTerminated() {
//            pmn();
//            return es.isTerminated();
//        }
//
//        public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
//            pmn();
//            return es.awaitTermination(timeout, unit);
//        }
//
//        public <T> Future<T> submit(Callable<T> task) {
//            pmn();
//            return es.submit(task);
//        }
//
//        public <T> Future<T> submit(Runnable task, T result) {
//            pmn();
//            return es.submit(task, result);
//        }
//
//        public Future<?> submit(Runnable task) {
//            return es.submit(task);
//        }
//
//        public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
//            pmn();
//            return es.invokeAll(tasks);
//        }
//
//        public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
//            pmn();
//            return es.invokeAll(tasks, timeout, unit);
//        }
//
//        public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
//            pmn();
//            return es.invokeAny(tasks);
//        }
//
//        public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
//            pmn();
//            return es.invokeAny(tasks, timeout, unit);
//        }
//
//        public void execute(Runnable command) {
//            pmn();
//            es.execute(command);
//        }
//
//        public int hashCode() {
//            pmn();
//            return es.hashCode();
//        }
//
//        public boolean equals(Object obj) {
//            pmn();
//            return es.equals(obj);
//        }
//
//        public String toString() {
//            pmn();
//            return es.toString();
//        }
//
//        private void pmn() {
//            StringBuilder sb = new StringBuilder();
//            for (StackTraceElement ste : Thread.currentThread().getStackTrace())
//                sb.append(ste.getFileName())
//                        .append('.')
//                        .append(ste.getClassName())
//                        .append('.')
//                        .append(ste.getMethodName())
//                        .append(", ");
//            System.out.println(sb.toString());
//        }
//    }
}
