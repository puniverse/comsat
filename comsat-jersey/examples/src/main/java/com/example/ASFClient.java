package com.example;

import co.paralleluniverse.common.benchmark.StripedHistogram;
import co.paralleluniverse.common.benchmark.StripedTimeSeries;
import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.io.FiberFileChannel;
import co.paralleluniverse.strands.Strand;
import co.paralleluniverse.strands.SuspendableRunnable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;


import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;

public class ASFClient {
    private final static int size = 30;
//    private static final String URI = "http://localhost:8080/myresource";
//    private static final String URI = "http://localhost:8080/fiber/hello";
    private static final String URI = "http://localhost:8080/helloworld";
//    private static final String URI = "http://localhost:8080/sync/hello";
//    private static final String URI = "http://localhost:8080/async/hello";
    private static final int fibers = 20;

    public static void main(final String[] args) throws Exception {
        final StripedTimeSeries<Long> sts = new StripedTimeSeries<Long>(100000, false);
        final StripedHistogram sh = new StripedHistogram(1500000L, 5);

        final RequestConfig requestConfig = RequestConfig.custom()
                .setSocketTimeout(3000)
                .setConnectTimeout(3000).build();
        try (CloseableHttpAsyncClient httpclient = HttpAsyncClients.custom()
                        .setDefaultRequestConfig(requestConfig)
                        .build()) {
            httpclient.start();


            final CountDownLatch latch = new CountDownLatch(fibers * size);
//            final CountDownLatch latch = new CountDownLatch(size);


            for (int i = 0; i < fibers; i++) {
                new Fiber<Void>(new SuspendableRunnable() {
                    @Override
                    public void run() throws SuspendExecution, InterruptedException {
                        for (int j = 0; j < size; j++) {
                            long start = System.nanoTime();
                            FiberExececute(httpclient, URI);
                            final long latency = (System.nanoTime() - start) / 1000;
                            sts.record(start, latency);
                            sh.recordValue(latency);
                            Strand.sleep(10);
                            latch.countDown();
                        }
                    }
                }).start();
            }
            latch.await(20000, TimeUnit.MILLISECONDS);
            System.out.println("Done " + latch.getCount());
        }

        for (StripedTimeSeries.Record rec : sts.getRecords())
            System.out.println("STS: " + rec.timestamp + " " + rec.value);
        sh.getHistogramData().outputPercentileDistribution(System.out, 5, 1.0);
    }

    public static HttpResponse FiberExececute(final CloseableHttpAsyncClient httpclient, final String URI) throws SuspendExecution {
        return new AsyncHttpReq() {
            @Override
            protected Void requestAsync(Fiber current, FutureCallback<HttpResponse> callback) {
                httpclient.execute(new HttpGet(URI), callback);
                return null;
            }
        }.run();
    }
}
//
//        try {
//            final CountDownLatch latch = new CountDownLatch(fibers * size);
//            System.out.println("start");
//            for (int i = 0; i < fibers; i++) {
//                System.out.println("fiber " + i);
//                new Fiber<Void>(new SuspendableRunnable() {
//                    @Override
//                    public void run() throws SuspendExecution, InterruptedException {
//                        for (int j = 0; j < size; j++) {
//                            System.out.println("iter " + j);
//                            long start = System.nanoTime();
//                            try {
//                                HttpResponse get = httpclient.execute(new HttpGet(URI), null).get(1000, TimeUnit.MILLISECONDS);
//                                System.out.println(get.toString());
//                            } catch (ExecutionException | TimeoutException ex) {
//                                Logger.getLogger(ASFClient.class.getName()).log(Level.SEVERE, null, ex);
//                            }
////                            new AsyncHttpReq() {
////                                @Override
////                                protected Void requestAsync(Fiber current, FutureCallback<HttpResponse> callback) {
////                                    httpclient.execute(new HttpGet("http://127.0.0.1/helloworld"), callback);
////                                    return null;
////                                }
////                            }.run();
//                            final long latency = (System.nanoTime() - start) / 1000;
//                            sts.record(start, latency);
//                            sh.recordValue(latency); // sh.recordValue(latency, 10000);
//                            System.out.println("latency " + latency);
//                            Strand.sleep(10);
//                        }
//
//                    }
//                }).start();
//            }
//            latch.await();
//            System.out.println("Shutting down");
//        } finally {
//            httpclient.close();
//        }
