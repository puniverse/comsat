package com.example;

import co.paralleluniverse.common.benchmark.StripedHistogram;
import co.paralleluniverse.common.benchmark.StripedLongTimeSeries;
import com.google.common.util.concurrent.RateLimiter;
import com.ning.http.client.*;
import com.ning.http.client.AsyncHttpClient.BoundRequestBuilder;
import java.io.IOException;
import java.util.Random;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.HdrHistogram.HistogramData;

public class AHClient {
    private final static int numOfRequests = 50;
    private final static AtomicInteger ai = new AtomicInteger();
//    private static final int concurrentUsers = 4000;
    private static final int workDuration = 20;
    public static final int TIMES = 90;
    private static final int specialDuration = 1; //millis
    private final static String HOST = "http://ec2-54-219-1-209.us-west-1.compute.amazonaws.com";
//    private final static String HOST = "http://localhost";
//    private static final String URI = HOST+":8081/sync/hello?sleep=";
//    private static final String URI = HOST+":8080/sync/hello?sleep=";
//    private static final String URI = HOST+":8080/fiber/hello?sleep=";
//    private static final String URI = HOST + ":8081/WebAppA/simple?sleep=";
//    private static final String URI = HOST+":8080/WebAppA/NewServlet?times="+TIMES+"&sleep=";
    private static final String URI = HOST+":8081/WebAppA/FiberServlet?times="+TIMES+"&sleep=";
//    private static final String URI = HOST+":8081/WebAppA/TestFiberServlet?sleep=";
//    private static final String URI = HOST+":8080/WebAppA/TestSyncServlet?sleep=";
    //private static final int gapBetweenRequests = 10;
    private static final double specialProb = 1;
    public static final int REQUESTS_RATE = 100;//3050;
//    public static final int SERVER_THREADS = 2000;
    public static final long TEST_DURATION_SECONDS = 60;
    public static final int WARM_UP_SECONDS = 10;
    public static final double MAX_SYNC_RATE = 2000; // sync 1100;; //SERVER_THREADS * 1000 / (specialDuration*specialProb+workDuration*(1-specialProb));
    public static final int RATE_STEP = 100;
    public static final int MAX_SEMAPHORE = 3800;

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
        final AtomicInteger errCounter = new AtomicInteger();
        final AsyncHttpClient ahc = new AsyncHttpClient(new AsyncHttpClientConfig.Builder().setMaximumConnectionsPerHost(10000).build());
//        final CountDownLatch cdl = new CountDownLatch(REQUESTS_RATE * TEST_DURATION_SECONDS);
        final Semaphore sem = new Semaphore(MAX_SEMAPHORE);
        Random random = new Random();
        RateLimiter rl = RateLimiter.create((double) REQUESTS_RATE);

        long last = 0;
        long lastErrCount = 0;
        final long baseTime = System.nanoTime();
        long lastForRate = baseTime;
        System.out.println("basetime " + baseTime);
        int currRate = REQUESTS_RATE;
        int countToRate = 0;
        final AtomicLong maxLat = new AtomicLong();
        for (int i = 0;; i++) {
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
                    final long latencyMicro = (System.nanoTime() - start) / 1000 - 1000 * duration * TIMES;
                    //                  cdl.countDown();
                    final int statusCode = response.getStatusCode();
                    if (statusCode != 200) {
                        System.out.println("statusCode is " + statusCode);
                    }
//                    sts.record(start, latencyMicro);
                    if (latencyMicro > maxLat.get())
                        maxLat.set(latencyMicro);
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
                    errCounter.incrementAndGet();
//                    t.printStackTrace();
//                    ai.decrementAndGet();
                    System.out.println("id=" + id + " excep " + t);
                }
            });
            last = start;

            if (++countToRate > currRate) {
                if (currRate < MAX_SYNC_RATE)
                    currRate += RATE_STEP;
                rl.setRate(Math.min(currRate, MAX_SYNC_RATE));
                long curr = System.nanoTime();
                double d = 1e9 * currRate / (curr - lastForRate);
                final int get = errCounter.get();
                if (get - lastErrCount > 300) {
                    System.out.println("too many errors");
                    System.exit(0);
                }
                lastErrCount = get;
                System.out.println("RATE_NOW " + curr + " " + d + " " + (MAX_SEMAPHORE - sem.availablePermits()) + " " + get + " " + maxLat.get());
                maxLat.set(0);
                lastForRate = curr;
                countToRate = 0;
                if (curr - baseTime > TEST_DURATION_SECONDS * 1000000000L) {
                    break;
                }
//                Thread.sleep(600/currRate*1100);
//                Thread.sleep(600);
            }

        }

        int r;
        sem.acquire(MAX_SEMAPHORE);
//        System.out.println("waiting for the last " + (concurrentUsers - sem.availablePermits()));
//        Thread.sleep(2000);
//        System.out.println("waiting for the last " + (concurrentUsers - sem.availablePermits()));
//        if (!sem.tryAcquire(concurrentUsers, 10000, TimeUnit.MILLISECONDS)) {
//            System.out.println("stucked with " + (concurrentUsers - sem.availablePermits()));
//            throw new TimeoutException("finish time out");
//        }
//        for (StripedLongTimeSeries.Record rec : sts.getRecords())
//            System.out.println("STS: " + rec.timestamp + " " + (rec.value));
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