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
//    private static final int concurrentUsers = 4000;
    private static final int workDuration = 20;
    private static final int specialDuration = 1000;
    private final static String HOST = "http://192.168.15.104";
//    private static final String URI = HOST+":8081/sync/hello?sleep=";
    private static final String URI = HOST+":8080/fiber/hello?sleep=";
//    private static final String URI = "http://localhost:8080/async/hello?sleep=";
    //private static final int gapBetweenRequests = 10;
    private static final double specialProb = 0.2;
    public static final int REQUESTS_RATE = 1000;
    public static final int SERVER_THREADS = 2100;
    public static final long TEST_DURATION_SECONDS = 60;
    public static final int WARM_UP_SECONDS = 10;
    public static final double MAX_SYNC_RATE = 2000; //SERVER_THREADS * 1000 / (specialDuration*specialProb+workDuration*(1-specialProb));
    public static final int RATE_STEP = 200;

    public static void main(final String[] args) throws Exception {
//        fiberAs();
        try {
            As();
        } catch (TimeoutException ex) {
            System.out.println(ex);
            System.exit(0);
        }
    }

    public static void As() throws InterruptedException, IOException, TimeoutException {
        final StripedLongTimeSeries sts = new StripedLongTimeSeries(100000, false);
        final StripedHistogram sh = new StripedHistogram(10000000L, 4);
        final AtomicInteger ai = new AtomicInteger();
        final AsyncHttpClient ahc = new AsyncHttpClient(new AsyncHttpClientConfig.Builder().setMaximumConnectionsPerHost(10000).build());
//        final CountDownLatch cdl = new CountDownLatch(REQUESTS_RATE * TEST_DURATION_SECONDS);
        final Semaphore sem = new Semaphore(5000);
        Random random = new Random();
        RateLimiter rl = RateLimiter.create((double) REQUESTS_RATE);

        long last = 0;
        final long baseTime = System.nanoTime();
        long lastForRate = baseTime;
        System.out.println("basetime " + baseTime);
        int currRate = REQUESTS_RATE;
        int countToRate = 0;
        for (int i = 0; ; i++) {
            final int duration = (random.nextDouble() < specialProb) ? specialDuration : workDuration;
            final int id = i;
            BoundRequestBuilder req = ahc.prepareGet(URI + duration);// + "&id="+id);
//            long k = System.nanoTime();
            rl.acquire();
            sem.acquire();
//            ai.incrementAndGet();
//            long lat = System.nanoTime() - k;

//            if (!sem.tryAcquire(2000, TimeUnit.MILLISECONDS))
//                throw new TimeoutException("request time out");
            final long start = System.nanoTime();
            req.execute(new AsyncCompletionHandler<Void>() {
                @Override
                public Void onCompleted(Response response) throws Exception {
                    final long latencyMicro = (System.nanoTime() - start) / 1000 - 1000 * duration;
                    //                  cdl.countDown();
                    final int statusCode = response.getStatusCode();
                    if (statusCode != 200) {
                        System.out.println("statusCode is " + statusCode);
                    }
                    sts.record(start, latencyMicro);
                    try {
                        sh.recordValue(latencyMicro);
                    } catch (ArrayIndexOutOfBoundsException ex) {
                        System.out.println(ex);
                    }
//                    ai.decrementAndGet();
                    sem.release();

                    return null;
                }

                @Override
                public void onThrowable(Throwable t) {
//                    cdl.countDown();
                    sem.release();
//                    t.printStackTrace();
//                    ai.decrementAndGet();
                    System.out.println("id=" + id + " excep" + t);
                }
            });
            last = start;
            if (++countToRate > currRate) {
                if (currRate < MAX_SYNC_RATE) 
                    currRate+=RATE_STEP;
                rl.setRate(Math.min(currRate,MAX_SYNC_RATE));
                long curr = System.nanoTime();
                double d = 1e9 * currRate / (curr - lastForRate);
                System.out.println("RATE_NOW " + curr + " " + d+" "+sem.getQueueLength());
                lastForRate = curr;
                countToRate = 0;
                if (curr-baseTime>TEST_DURATION_SECONDS*1000000000L) {
                    break;                    
                }
            }
        }
        
        int r;
        while ((r = ai.get()) > 0) {
            System.out.println("remained "+r);
            Thread.sleep(1000);
        }
//        System.out.println("waiting for the last " + (concurrentUsers - sem.availablePermits()));
//        Thread.sleep(2000);
//        System.out.println("waiting for the last " + (concurrentUsers - sem.availablePermits()));
//        if (!sem.tryAcquire(concurrentUsers, 10000, TimeUnit.MILLISECONDS)) {
//            System.out.println("stucked with " + (concurrentUsers - sem.availablePermits()));
//            throw new TimeoutException("finish time out");
//        }
        for (StripedLongTimeSeries.Record rec : sts.getRecords())
            System.out.println("STS: " + rec.timestamp + " " + (rec.value));
        final HistogramData hd = sh.getHistogramData(); // sh.getHistogramDataCorrectedForCoordinatedOmission(10 * 1000);

//        final HistogramData hd = sh.getHistogramDataCorrectedForCoordinatedOmission(10 * 1000);

        hd.outputPercentileDistribution(System.out,
                5, 1.0);
//        ThreadUtil.dumpThreads();
        System.exit(
                0);
    }
}
    /*
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
     */