/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.example;

import co.paralleluniverse.concurrent.util.ThreadUtil;
import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.FiberAsync;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.ws.rs.client.AsyncClientBuilder;
import co.paralleluniverse.strands.SuspendableRunnable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import javax.ws.rs.client.InvocationCallback;
import javax.ws.rs.core.Response;

/**
 *
 * @author eitan
 */
public class MyClient {
    private final static AtomicInteger ai = new AtomicInteger();
    private static void userMethod() throws SuspendExecution {
//        Response resp = ClientBuilder.newClient().target("http://localhost:8080/myresource").request().get();
        Response resp = AsyncClientBuilder.newClient().target("http://localhost:8080/myresource").request().get();
        System.out.println("hi "+ai.incrementAndGet() +"-"+resp.getStatus());
//        System.out.println("reponse is " + resp.readEntity(String.class));
    }

    public static void main(String[] args) throws Exception {
        final int size = 100;
        List<Fiber<Void>> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
//            Thread.sleep(10);
            list.add(new Fiber<Void>(new SuspendableRunnable() {
                @Override
                public void run() throws SuspendExecution, InterruptedException {
                    userMethod();
                }
            }).start());
        }
        System.out.println("waiting");
        Thread.sleep(5000);
        System.out.println("finished successfully");
        ThreadUtil.dumpThreads();
        System.exit(0);

        //jersey-client-async-executor will finish in 1 minute 
        //in order to do it quicker we have to provide another RequestExecutorsProvider.class
        System.out.println("finished");
    }

    public static abstract class AsyncRs extends FiberAsync<Response, InvocationCallback<Response>, Void, RuntimeException> implements InvocationCallback<Response> {
        private final Fiber fiber;

        public AsyncRs() {
            this.fiber = Fiber.currentFiber();
        }

        @Override
        public Response run() throws SuspendExecution {
            try {
                final Response run = super.run();
                return run;
            } catch (InterruptedException e) {
                throw new AssertionError(e);
            }
        }

        @Override
        public void completed(Response response) {
            super.completed(response, fiber);
        }

        @Override
        public void failed(Throwable throwable) {
            super.failed(throwable, fiber);
        }
    }
}
