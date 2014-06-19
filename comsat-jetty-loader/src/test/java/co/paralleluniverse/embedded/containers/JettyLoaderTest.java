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

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import org.apache.catalina.startup.Tomcat;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.eclipse.jetty.annotations.AnnotationConfiguration;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.webapp.Configuration;
import org.eclipse.jetty.webapp.WebAppContext;
import org.eclipse.jetty.webapp.WebInfConfiguration;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class JettyLoaderTest {

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
            //            {JettyServer.class},
            {TomcatServer.class}, //            {UndertowServer.class},
        });
    }
    private final Class<? extends EmbeddedServer> cls;
//    private EmbeddedServer instance;
    private CloseableHttpClient client;
    private Tomcat tomcat;

    public JettyLoaderTest(Class<? extends EmbeddedServer> cls) {
        this.cls = cls;
    }

    @Before
    public void setUp() throws Exception {
        Server server = new Server(8080);
        WebAppContext wap = new WebAppContext("/Users/eitan/Projects/comsat-examples/test-servlet/build/libs/test-servlet.war", "/");
        wap.setConfigurations(new Configuration[]{new AnnotationConfiguration(), new WebInfConfiguration()});
        //http://www.eclipse.org/jetty/documentation/current/jndi-embedded.html
        server.setHandler(wap);
        server.start();
        this.client = HttpClients.custom().setDefaultRequestConfig(RequestConfig.custom()
                .setSocketTimeout(5000).setConnectTimeout(5000).setConnectionRequestTimeout(5000)
                .build()).build();
    }

    @After
    public void tearDown() throws Exception {
//        tomcat.stop();
        client.close();
    }

//    @Test
    public void testGetDeployedWar() throws IOException, InterruptedException, Exception {
        Thread.sleep(30000);
//        for (int i = 0; i < 10; i++) {
//            String result = client.execute(new HttpGet("http://localhost:8080/"), BASIC_RESPONSE_HANDLER);
//            assertTrue(result.contains("h2testdb"));
//        }
    }

    private static final BasicResponseHandler BASIC_RESPONSE_HANDLER = new BasicResponseHandler();

}
