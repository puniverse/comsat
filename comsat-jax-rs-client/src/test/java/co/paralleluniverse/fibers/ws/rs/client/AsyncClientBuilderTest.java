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
package co.paralleluniverse.fibers.ws.rs.client;

import co.paralleluniverse.concurrent.util.ThreadUtil;
import co.paralleluniverse.embedded.containers.EmbeddedServer;
import co.paralleluniverse.embedded.containers.JettyServer;
import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.ws.rs.client.AsyncClientBuilder;
import co.paralleluniverse.strands.SuspendableRunnable;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.client.Client;
import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class AsyncClientBuilderTest {

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
            {JettyServer.class},});
    }
    private final Class<? extends EmbeddedServer> cls;
    private EmbeddedServer server;

    public AsyncClientBuilderTest(Class<? extends EmbeddedServer> cls) {
        this.cls = cls;
    }

    @Before
    public void setUp() throws Exception {
        this.server = cls.newInstance();
        server.addServlet("test", TestServlet.class, "/");
        server.start();
    }

    @After
    public void tearDown() throws Exception {
        server.stop();
    }

    @Test
    public void testConcurrency() throws IOException, InterruptedException, Exception {
        final int concurrencyLevel = 40;
        // snippet client creation
        final Client client = AsyncClientBuilder.newBuilder().build();
        // end of snippet
        final CountDownLatch cdl = new CountDownLatch(concurrencyLevel);
        for (int i = 0; i < concurrencyLevel; i++)
            new Fiber<Void>(new SuspendableRunnable() {
                @Override
                public void run() throws SuspendExecution, InterruptedException {
                    // snippet http call
                    String response = client.target("http://localhost:8080").request().get(String.class);
                    // end of snippet
                    assertEquals("testGet", response);
                    System.out.println("done "+cdl.getCount());
                    cdl.countDown();
                }
            }).start();
        cdl.await(4000, TimeUnit.MILLISECONDS);
        
        assertEquals(0, cdl.getCount());
        ThreadUtil.dumpThreads();
    }

    public static class TestServlet extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            try (PrintWriter out = resp.getWriter()) {
                Thread.sleep(300);
                out.print("testGet");
            } catch (InterruptedException ex) {
            }
        }
    }

}
