package com.example;

import co.paralleluniverse.common.benchmark.StripedHistogram;
import co.paralleluniverse.common.benchmark.StripedLongTimeSeries;
import co.paralleluniverse.concurrent.util.ThreadUtil;
import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.Strand;
import co.paralleluniverse.strands.SuspendableRunnable;
import com.google.common.util.concurrent.RateLimiter;
import com.ning.http.client.*;
import com.ning.http.client.AsyncHttpClient.BoundRequestBuilder;
import java.io.IOException;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.HdrHistogram.HistogramData;

public class AHClient {
    private final static int numOfRequests = 50;
    private final static AtomicInteger ai = new AtomicInteger();
    private static final int concurrentUsers = 4000;
    private static final int workDuration = 20;
    private static final int specialDuration = 200;
    private static final String URI = "http://localhost:8081/sync/hello?sleep=";
//    private static final String URI = "http://localhost:8080/fiber/hello?sleep=";
//    private static final String URI = "http://localhost:8080/async/hello?sleep=";
    //private static final int gapBetweenRequests = 10;
    private static final double specialProb = 0.2;
    public static final int REQUESTS_RATE = 20000;
    public static final int TEST_DURATION_SECONDS = 20;
    public static final int WARM_UP_SECONDS = 10;

    public static void main(final String[] args) throws Exception {
//        fiberAs();
        try {
            System.out.println("start");
            As();
        } catch (TimeoutException ex) {
            System.out.println(ex);
            System.exit(0);
        }
    }

    public static void As() throws InterruptedException, IOException, TimeoutException {
//        final StripedLongTimeSeries sts = new StripedLongTimeSeries(100000, false);
        final StripedHistogram sh = new StripedHistogram(10000L, 4);
//        final AtomicInteger ai = new AtomicInteger();
        final AsyncHttpClient ahc = new AsyncHttpClient(new AsyncHttpClientConfig.Builder().setMaximumConnectionsPerHost(concurrentUsers).build());
        final Semaphore sem = new Semaphore(concurrentUsers);
        Random random = new Random();
        RateLimiter rl = RateLimiter.create((double)REQUESTS_RATE, WARM_UP_SECONDS,TimeUnit.SECONDS);
        
        long last = 0;
        for (int i = 0; i < REQUESTS_RATE*TEST_DURATION_SECONDS; i++) {          
            final int duration = (random.nextDouble() < specialProb) ? specialDuration : workDuration;
            final int id = i;
            BoundRequestBuilder req = ahc.prepareGet(URI + duration);// + "&id="+id);
//            long k = System.nanoTime();
            rl.acquire();
//            long lat = System.nanoTime() - k;
            
//            if (!sem.tryAcquire(2000, TimeUnit.MILLISECONDS))
//                throw new TimeoutException("request time out");
            final long start = System.nanoTime();
            req.execute(new AsyncCompletionHandler<Void>() {
                @Override
                public Void onCompleted(Response response) throws Exception {
                    final long latency = (System.nanoTime() - start) / 1000000 - duration;
                    final int statusCode = response.getStatusCode();
                    if (statusCode!=200) System.out.println("statusCode is "+statusCode);;
//                    sts.record(start, latency);
                    sh.recordValue(latency);
//                    sem.release();
                    return null;
                }

                @Override
                public void onThrowable(Throwable t) {
//                    sem.release();
                    System.out.println("id="+id +" excep"+ t);
                }
            });
            last = start;
            if (i%(REQUESTS_RATE)==0) System.out.println("startet "+i);;
        }
        System.out.println("waiting for the last " + (concurrentUsers - sem.availablePermits()));
        Thread.sleep(2000);
        System.out.println("waiting for the last " + (concurrentUsers - sem.availablePermits()));
        if (!sem.tryAcquire(concurrentUsers, 10000, TimeUnit.MILLISECONDS)) {
            System.out.println("stucked with " + (concurrentUsers - sem.availablePermits()));
            throw new TimeoutException("finish time out");
        }
//        final HistogramData hd = sh.getHistogramData(); // sh.getHistogramDataCorrectedForCoordinatedOmission(10 * 1000);
        final HistogramData hd = sh.getHistogramDataCorrectedForCoordinatedOmission(10 * 1000);
        hd.outputPercentileDistribution(System.out, 5, 1.0);
//        ThreadUtil.dumpThreads();
        System.exit(0);
    }

    public static void fiberAs() throws InterruptedException {
        final StripedLongTimeSeries sts = new StripedLongTimeSeries(100000, false);
        final StripedHistogram sh = new StripedHistogram(1500000L, 5);

        final CountDownLatch latch = new CountDownLatch(concurrentUsers);
        final AsyncHttpClient asyncHttpClient = new AsyncHttpClient();


        for (int i = 0; i < concurrentUsers; i++) {
            final int fiberCount = i;
            new Fiber<Void>(new SuspendableRunnable() {
                public static final int SLEEP_GAPS = 10;

                @Override
                public void run() throws SuspendExecution {
                    for (int j = 0; j < numOfRequests; j++) {
                        try {
                            long start = System.nanoTime();
                            if (j % 20 == 0)
                                System.out.println("fiber " + fiberCount + ", status " + j);

                            new AsyncAHC() {
                                @Override
                                protected Void requestAsync(Fiber current, AsyncCompletionHandler<Response> callback) {
                                    try {
                                        asyncHttpClient.prepareGet(URI).execute(callback);
                                    } catch (IOException ex) {
                                        System.out.println("err " + ex);
                                        callback.onThrowable(ex);
                                    }
                                    return null;
                                }
                            }.run();


                            final long latency = (System.nanoTime() - start) / 1000;
                            sts.record(start, latency);
                            sh.recordValue(latency);
                            ai.incrementAndGet();
                            Strand.sleep(SLEEP_GAPS);
                        } catch (IOException ex) {
                            System.out.println("io " + ex);
                            Logger.getLogger(AHClient.class.getName()).log(Level.SEVERE, null, ex);
                        } catch (InterruptedException ex) {
                            Logger.getLogger(AHClient.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                    latch.countDown();
                }
            }).start();
        }
        System.out.println("waiting");
        latch.await(40000, TimeUnit.MILLISECONDS);

//        for (StripedLongTimeSeries.Record rec : sts.getRecords())
//            System.out.println("STS: " + rec.timestamp + " " + rec.value);
        final HistogramData hd = sh.getHistogramData(); // sh.getHistogramDataCorrectedForCoordinatedOmission(10 * 1000);
        hd.outputPercentileDistribution(System.out, 5, 1.0);
        System.out.println("finished " + latch.getCount());
        ThreadUtil.dumpThreads();
        System.exit(0);
    }
}
