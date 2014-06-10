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
package co.paralleluniverse.fibers.httpasyncclient;

import co.paralleluniverse.embedded.containers.EmbeddedServer;
import co.paralleluniverse.embedded.containers.JettyServer;
import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.SuspendableRunnable;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.util.EntityUtils;
import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class FiberHttpAsyncClient {

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
            {JettyServer.class},});
    }
    private final Class<? extends EmbeddedServer> cls;
    private EmbeddedServer server;

    public FiberHttpAsyncClient(Class<? extends EmbeddedServer> cls) {
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
    public void testAsync() throws IOException, InterruptedException, ExecutionException, TimeoutException {
        final int concurrencyLevel = 20;
        try (CloseableHttpAsyncClient client = FiberCloseableHttpAsyncClient.wrap(HttpAsyncClients.custom().setMaxConnPerRoute(concurrencyLevel).setMaxConnTotal(concurrencyLevel).build())) {
            client.start();
            new Fiber<Void>(new SuspendableRunnable() {
                @Override
                public void run() throws SuspendExecution, InterruptedException {
                    try {
                        ArrayList<Future<HttpResponse>> futures = new ArrayList<>();
                        for (int i = 0; i < concurrencyLevel; i++)
                            futures.add(client.execute(new HttpGet("http://localhost:8080"), null));
                        for (Future<HttpResponse> future : futures)
                            assertEquals("testGet", EntityUtils.toString(future.get().getEntity()));
                    } catch (ExecutionException | IOException | ParseException ex) {
                        fail(ex.getMessage());
                    }
                }
            }).start().join(2000, TimeUnit.MILLISECONDS);
        }
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
