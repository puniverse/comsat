package com.example;

import com.google.common.util.concurrent.RateLimiter;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class Temp {
    public static final int RATE = 30000;
    public static void main(String args[]) throws InterruptedException {
//        RateLimiter rl = RateLimiter.create(RATE,30,TimeUnit.MILLISECONDS);
//        long start = System.nanoTime();
//        long last = start;
//        for (int i = 0; i < RATE; i++) {
//            rl.acquire();
//            if (i%1000==0) {
//                long curr = System.nanoTime();
//                double d = 1e12/(curr - last); 
//                System.out.println("current rate is "+d);
//                last = curr;
//            }
//        }
//        long lat = System.nanoTime() - start;
//        System.out.println(lat / 1000000);
    }
}
