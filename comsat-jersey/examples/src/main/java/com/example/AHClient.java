package com.example;

import co.paralleluniverse.common.benchmark.StripedHistogram;
import co.paralleluniverse.common.benchmark.StripedLongTimeSeries;
import co.paralleluniverse.concurrent.util.ThreadUtil;
import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.Strand;
import co.paralleluniverse.strands.SuspendableRunnable;
import com.ning.http.client.*;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.HdrHistogram.HistogramData;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;

public class AHClient {
    private final static int size = 100;
    private final static AtomicInteger ai = new AtomicInteger();
    private static final String URI = "http://localhost:8080/fiber/hello";
//    private static final String URI = "http://localhost:8080/sync/hello";
//    private static final String URI = "http://localhost:8080/async/hello";
    private static final int fibers = 1000;

    public static void main(final String[] args) throws Exception {
        final StripedLongTimeSeries sts = new StripedLongTimeSeries(100000, false);
        final StripedHistogram sh = new StripedHistogram(1500000L, 5);

        final CountDownLatch latch = new CountDownLatch(fibers);
        final AsyncHttpClient asyncHttpClient = new AsyncHttpClient();


        for (int i = 0; i < fibers; i++) {
            final int fiberCount = i;
            new Fiber<Void>(new SuspendableRunnable() {
                @Override
                public void run() throws SuspendExecution {
                    for (int j = 0; j < size; j++) {
                        try {
                            long start = System.nanoTime();

                            new AsyncAHC() {
                                @Override
                                protected Void requestAsync(Fiber current, AsyncCompletionHandler<Response> callback) {
                                    try {
                                        asyncHttpClient.prepareGet(URI).execute(callback);
                                    } catch (IOException ex) {
                                        System.out.println("err "+ex);
                                        callback.onThrowable(ex);
                                    }
                                    return null;
                                }
                            }.run();


                            final long latency = (System.nanoTime() - start) / 1000;
                            sts.record(start, latency);
                            sh.recordValue(latency);
                            ai.incrementAndGet();
                            Strand.sleep(10);
                        } catch (IOException ex) {
                            System.out.println("io "+ex);
                            Logger.getLogger(AHClient.class.getName()).log(Level.SEVERE, null, ex);
                        } catch (InterruptedException ex) {
                            Logger.getLogger(AHClient.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                    System.out.println("fiber finished "+fiberCount);
                    latch.countDown();
                }
            }).start();
        }
        System.out.println("waiting");
        latch.await(60000, TimeUnit.MILLISECONDS);

//        for (StripedLongTimeSeries.Record rec : sts.getRecords())
//            System.out.println("STS: " + rec.timestamp + " " + rec.value);
        final HistogramData hd = sh.getHistogramData(); // sh.getHistogramDataCorrectedForCoordinatedOmission(10 * 1000);
        hd.outputPercentileDistribution(System.out, 5, 1.0);
        System.out.println("finished " + latch.getCount());
        ThreadUtil.dumpThreads();
        System.exit(0);
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
