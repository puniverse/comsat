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
package co.paralleluniverse.embedded.containers;

import co.paralleluniverse.common.util.Debug;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collection;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.catalina.startup.Tomcat;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.After;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class EmbeddedServerTest {

    @Parameterized.Parameters(name="{0}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
//            {JettyServer.class},
            {TomcatServer.class},
//            {UndertowServer.class},
        });
    }
    private final Class<? extends EmbeddedServer> cls;
//    private EmbeddedServer instance;
    private CloseableHttpClient client;
    private Tomcat tomcat;

    public EmbeddedServerTest(Class<? extends EmbeddedServer> cls) {
        this.cls = cls;
    }

    @Before
    public void setUp() throws Exception {
                this.tomcat = new Tomcat();
        String buildWebAppDir = new File(".").getAbsolutePath() + "/build/webapp";
        System.out.println("DDDDDDD "+buildWebAppDir);
        tomcat.setBaseDir(buildWebAppDir);

        for (final File fileEntry : new File(buildWebAppDir).listFiles()) {
            System.out.println("FFFFFF "+fileEntry.getName());
            if (fileEntry.getName().endsWith(".war")) {
                String war = fileEntry.getName().substring(0, fileEntry.getName().length() - ".war".length());
                System.out.println("Loading WAR: " + war + " to: http://localhost:8080/"+war);
                tomcat.addWebapp("/webapp", fileEntry.getAbsolutePath());
                break;
            }
        }

        tomcat.start();

//        this.instance = cls.newInstance();
//        instance.addServlet("test", TestServlet.class, "/");
//        instance.start();
        this.client = HttpClients.custom().setDefaultRequestConfig(RequestConfig.custom()
                .setSocketTimeout(5000).setConnectTimeout(5000).setConnectionRequestTimeout(5000)
                .build()).build();
    }

    @After
    public void tearDown() throws Exception {
//        instance.stop();
        tomcat.stop();
        client.close();
    }

    @Test
    public void testGet() throws IOException, InterruptedException, Exception {
//        Thread.sleep(20000);
        for (int i = 0; i < 10; i++)
            assertEquals("testGet", client.execute(new HttpGet("http://localhost:8080/webapp/"), BASIC_RESPONSE_HANDLER));
    }

//    @Test
    public void testPost() throws IOException, InterruptedException, Exception {
        for (int i = 0; i < 10; i++)
            assertEquals("testPost", client.execute(new HttpPost("http://localhost:8080"), BASIC_RESPONSE_HANDLER));
    }

    private static final BasicResponseHandler BASIC_RESPONSE_HANDLER = new BasicResponseHandler();

}
