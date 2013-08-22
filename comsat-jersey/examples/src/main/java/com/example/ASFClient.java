package com.example;

import co.paralleluniverse.common.benchmark.StripedHistogram;
import co.paralleluniverse.common.benchmark.StripedLongTimeSeries;
import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.SuspendExecution;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.HdrHistogram.HistogramData;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;

public class ASFClient {
    private final static int size = 10;
    private final static AtomicInteger ai = new AtomicInteger();
//    private static final String URI = "http://localhost:8080/myresource";
    private static final String URI = "http://localhost:8080/fiber/hello";
//    private static final String URI = "http://localhost:8080/helloworld";
//    private static final String URI = "http://localhost:8080/sync/hello";
//    private static final String URI = "http://localhost:8080/async/hello";
    private static final int fibers = 20;

    public static void main(final String[] args) throws Exception {
        final StripedLongTimeSeries sts = new StripedLongTimeSeries(100000, false);
        final StripedHistogram sh = new StripedHistogram(1500000L, 5);

        final RequestConfig requestConfig = RequestConfig.custom()
                .setSocketTimeout(10000)
                .setConnectTimeout(10000).build();
        try (CloseableHttpAsyncClient httpclient = HttpAsyncClients.custom()
                        .setDefaultRequestConfig(requestConfig)
                        .build()) {
            httpclient.start();

            final CountDownLatch latch = new CountDownLatch(fibers * size);
            //            final CountDownLatch latch = new CountDownLatch(size);


            for (int i = 0; i < fibers; i++) {
                Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        for (int j = 0; j < size; j++) {
                            try {
                                long start = System.nanoTime();
                                asyncToSync();
                                final long latency = (System.nanoTime() - start) / 1000;
                                sts.record(start, latency);
                                sh.recordValue(latency);
                                ai.incrementAndGet();
                                Thread.sleep(10);
                            } catch (ExecutionException ex) {
                                Logger.getLogger(ASFClient.class.getName()).log(Level.SEVERE, null, ex);
                            } catch (TimeoutException ex) {
                                Logger.getLogger(ASFClient.class.getName()).log(Level.SEVERE, null, ex);
                            } catch (InterruptedException ex) {
                                Logger.getLogger(ASFClient.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }
                    }

                    private void asyncToSync() throws ExecutionException, TimeoutException, InterruptedException {
                        httpclient.execute(new HttpGet(URI), new FutureCallback<HttpResponse>() {
                            @Override
                            public void completed(HttpResponse result) {
                                latch.countDown();
                            }

                            @Override
                            public void failed(Exception ex) {
                                System.out.println(ex);
                                latch.countDown();
                            }

                            @Override
                            public void cancelled() {
                                System.out.println("cancelled");
                                latch.countDown();
                            }
                        }).get();
                    }
                });
                thread.setDaemon(true);
                thread.start();
                //                new Fiber<Void>(new SuspendableRunnable() {
                //                    @Override
                //                    public void run() throws SuspendExecution, InterruptedException {
                //                        for (int j = 0; j < size; j++) {
                //                            long start = System.nanoTime();
                //                            FiberExececute(httpclient, URI);
                //                            final long latency = (System.nanoTime() - start) / 1000;
                //                            sts.record(start, latency);
                //                            sh.recordValue(latency);
                //                            Strand.sleep(10);
                //                            latch.countDown();
                //                        }
                //                    }
                //                }).start();
            }
            latch.await(20000, TimeUnit.MILLISECONDS);
            System.out.println("Done " + latch.getCount() + " , " + (fibers * size - ai.get()));
        }

        for (StripedLongTimeSeries.Record rec : sts.getRecords())
            System.out.println("STS: " + rec.timestamp + " " + rec.value);
        final HistogramData hd = sh.getHistogramData(); // sh.getHistogramDataCorrectedForCoordinatedOmission(10 * 1000);
        hd.outputPercentileDistribution(System.out, 5, 1.0);
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
