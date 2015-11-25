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
package co.paralleluniverse.fibers.dropwizard;

import com.google.common.io.Resources;
import java.io.IOException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class FiberDropwizardTest {
    private static final int TIMEOUT = 10_000; // ms
    public static final String SERVER_BASE_URL = "http://localhost:8080";

    @BeforeClass
    public static void setUpClass() throws InterruptedException, IOException {
        Thread t = new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    new MyDropwizardApp().run(new String[]{"server", Resources.getResource("server.yml").getPath()});
                } catch (final Exception ignored) {}
            }
        });
        t.setDaemon(true);
        t.start();
        waitUrlAvailable(SERVER_BASE_URL);
    }
    private CloseableHttpClient client;

    @Before
    public void setUp() throws Exception {
        this.client = HttpClients.custom().setDefaultRequestConfig(RequestConfig.custom()
                .setSocketTimeout(TIMEOUT).setConnectTimeout(TIMEOUT).setConnectionRequestTimeout(TIMEOUT)
                .build()).build();
    }

    @After
    public void tearDown() throws Exception {
        client.close();
    }

    @Test
    public void testGet() throws IOException, InterruptedException, Exception {
        for (int i = 0; i < 10; i++)
            assertTrue(client.execute(new HttpGet(SERVER_BASE_URL + "/?name=foo"), BASIC_RESPONSE_HANDLER).contains("foo"));
    }

    @Test
    public void testHttp() throws IOException, InterruptedException, Exception {
        for (int i = 0; i < 10; i++)
            assertTrue(client.execute(new HttpGet(SERVER_BASE_URL + "/http?name=bar"), BASIC_RESPONSE_HANDLER).contains("bar"));
    }

    @Test
    public void testFluentAPI() throws IOException, InterruptedException {
        for (int i = 0; i < 10; i++)
            assertEquals("37", client.execute(new HttpGet(SERVER_BASE_URL + "/fluent?id=37"), BASIC_RESPONSE_HANDLER));
    }

    @Test
    public void testDao() throws Exception {
        for (int i = 0; i < 10; i++)
            assertEquals("name37", client.execute(new HttpGet(SERVER_BASE_URL + "/dao?id=37"), BASIC_RESPONSE_HANDLER));
    }
    private static final BasicResponseHandler BASIC_RESPONSE_HANDLER = new BasicResponseHandler();

    public static void waitUrlAvailable(final String url) throws InterruptedException, IOException {
        for (;;) {
            Thread.sleep(10);
            try {
                if (HttpClients.createDefault().execute(new HttpGet(url)).getStatusLine().getStatusCode() > -100)
                    break;
            } catch (final HttpHostConnectException ignored) {}
        }
    }

}
