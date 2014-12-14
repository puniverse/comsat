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
package co.paralleluniverse.fibers.servlet;

import co.paralleluniverse.embedded.containers.EmbeddedServer;
import co.paralleluniverse.embedded.containers.JettyServer;
import co.paralleluniverse.embedded.containers.TomcatServer;
import co.paralleluniverse.embedded.containers.UndertowServer;
import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.Suspendable;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collection;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.After;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class FiberHttpServletTest {

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
            {JettyServer.class},
            {TomcatServer.class},
            {UndertowServer.class},});
    }
    private final Class<? extends EmbeddedServer> cls;
    private EmbeddedServer server;
    private CloseableHttpClient client;

    public FiberHttpServletTest(Class<? extends EmbeddedServer> cls) {
        this.cls = cls;
    }

    @Before
    public void setUp() throws Exception {
        this.server = cls.newInstance();
        // snippet servlet registration
        server.addServlet("test", FiberTestServlet.class, "/");
        // end of snippet
        server.addServlet("forward", FiberForwardServlet.class, "/forward");
        server.addServlet("inline", FiberForwardServlet.class, "/inline");
        server.start();
        this.client = HttpClients.custom().setDefaultRequestConfig(RequestConfig.custom()
                .setSocketTimeout(5000).setConnectTimeout(5000).setConnectionRequestTimeout(5000)
                .build()).build();
    }

    @After
    public void tearDown() throws Exception {
        server.stop();
        client.close();
    }

    @Test
    public void testGet() throws IOException, InterruptedException, Exception {
        for (int i = 0; i < 10; i++)
            assertEquals("testGet", client.execute(new HttpGet("http://localhost:8080"), BASIC_RESPONSE_HANDLER));
    }

//    @Test
    // Fails on jetty after 5-10 iterations
    // Fails on tomcat after 1000-2000 iterations
    // Passes on undertow
    public void testForward() throws IOException, InterruptedException, Exception {
        for (int i = 0; i < 5000; i++)
            assertEquals("Faild on iteration " + i, "testGet", client.execute(new HttpGet("http://localhost:8080/forward"), BASIC_RESPONSE_HANDLER));
    }

//    @Test
    // Inline is not supported yet by fiberServlet
    public void testInline() throws IOException, InterruptedException, Exception {
        for (int i = 0; i < 10; i++)
            assertEquals("testInlinetestGet", client.execute(new HttpGet("http://localhost:8080/inline"), BASIC_RESPONSE_HANDLER));
    }

    @Test
    public void testPost() throws IOException, InterruptedException, Exception {
        for (int i = 0; i < 10; i++)
            assertEquals("testPost", client.execute(new HttpPost("http://localhost:8080"), BASIC_RESPONSE_HANDLER));
    }

    // snippet FiberHttpServlet example 
    public static class FiberTestServlet extends FiberHttpServlet {
        // snippet_exclude_begin
        @Override
        @Suspendable
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            try (PrintWriter out = resp.getWriter()) {
                Fiber.sleep(100); // <== Some blocking code
                out.print("testPost");
            } catch (InterruptedException | SuspendExecution e) {
            }
        }
        // snippet_exclude_end
        @Override
        @Suspendable
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            try (PrintWriter out = resp.getWriter()) {
                Fiber.sleep(100); // <== Some blocking code
                out.print("testGet");
            } catch (InterruptedException | SuspendExecution e) {
            }
        }
    }
    // end of snippet

    public static class FiberForwardServlet extends FiberHttpServlet {
        @Override
        @Suspendable
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            try {
                Fiber.sleep(100);
                getServletContext().getRequestDispatcher("/").forward(req, resp);
            } catch (InterruptedException | SuspendExecution e) {
            }
        }
    }

    public static class FiberInlineServlet extends FiberHttpServlet {
        @Override
        @Suspendable
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            try (PrintWriter out = resp.getWriter()) {
                Fiber.sleep(1);
                out.print("testInline");
                getServletContext().getRequestDispatcher("/").include(req, resp);
            } catch (InterruptedException | SuspendExecution e) {
            }
        }
    }

    private static final BasicResponseHandler BASIC_RESPONSE_HANDLER = new BasicResponseHandler();

}
