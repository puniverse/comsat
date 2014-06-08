/*
 * COMSAT
 * Copyright (C) 2014, Parallel Universe Software Co. All rights reserved.
 *
 * This program and the accompanying materials are dual-licensed under
 * either the terms of the Eclipse Public License v1.0 as published by
 * the Eclipse Foundation
 *
 *   or (per the licensee's choosing)
 *
 * under the terms of the GNU Lesser General Public License version 3.0
 * as published by the Free Software Foundation.
 */
package co.paralleluniverse.fibers.retrofit;

import co.paralleluniverse.embedded.containers.AbstractEmbeddedServer;
import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.Suspendable;
import co.paralleluniverse.fibers.retrofit.HelloWorldApplication.Contributor;
import co.paralleluniverse.strands.SuspendableRunnable;
import java.io.IOException;
import java.util.List;
import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import retrofit.RestAdapter;
import retrofit.http.GET;
import retrofit.http.Path;

public class FiberServletContainerTest {
    @BeforeClass
    public static void setUpClass() throws InterruptedException, IOException {
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    new HelloWorldApplication().run(new String[]{"server"});
                } catch (Exception ex) {
                }
            }
        });
        t.setDaemon(true);
        t.start();
        AbstractEmbeddedServer.waitUrlAvailable("http://localhost:8080");
    }

    @Test
    public void testGet() throws IOException, InterruptedException, Exception {
        final GitHub github = new FiberRestAdaptherBuilder().setEndpoint("http://localhost:8080").build().create(GitHub.class);
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                List<Contributor> contributors = github.contributors("puniverse", "comsat");
                assertEquals("puniverse", contributors.get(1).login);
                assertEquals(4, contributors.size());                
            }
        }).start().join();
    }

    @Suspendable
    public static interface GitHub {
        @GET(value = "/repos/{owner}/{repo}/contributors")
        List<Contributor> contributors(@Path(value = "owner") String owner, @Path(value = "repo") String repo);
    }
}
