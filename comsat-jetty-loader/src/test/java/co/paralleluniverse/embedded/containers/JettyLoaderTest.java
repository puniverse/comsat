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

import co.paralleluniverse.comsat.jetty.QuasarWebAppClassLoader;
import co.paralleluniverse.fibers.jdbc.FiberDataSource;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.tomcat.dbcp.dbcp2.BasicDataSource;
import org.eclipse.jetty.annotations.AnnotationConfiguration;
import org.eclipse.jetty.plus.jndi.Resource;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.webapp.Configuration;
import org.eclipse.jetty.webapp.WebAppContext;
import org.eclipse.jetty.webapp.WebInfConfiguration;
import org.junit.After;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;
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
    private Server server;

    public JettyLoaderTest(Class<? extends EmbeddedServer> cls) {
        this.cls = cls;
    }

    @Before
    public void setUp() throws Exception {
        this.server = new Server(8080);
        BasicDataSource ds = new BasicDataSource();
        ds.setDriverClassName("org.h2.Driver");
        ds.setUrl("jdbc:h2:./build/h2testdb");
//        new Resource("java:comp/env/jdbc/globalds", ds);
        Resource resource = new Resource("java:comp/env/jdbc/fiberds", FiberDataSource.wrap(ds));
        WebAppContext wap = new WebAppContext();

        wap.setWar("build/wars/dep.war");
        wap.setConfigurations(new Configuration[]{new AnnotationConfiguration(), new WebInfConfiguration()});//, new JettyWebXmlConfiguration()});
        wap.setClassLoader(new QuasarWebAppClassLoader(wap));

        server.setHandler(wap);
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
    public void testGetDeployedWar() throws IOException, InterruptedException, Exception {
//        Thread.sleep(30000);
        for (int i = 0; i < 10; i++) {
            String result = client.execute(new HttpGet("http://localhost:8080/"), BASIC_RESPONSE_HANDLER);
            assertTrue(result.contains("h2testdb"));
        }
    }

    private static final BasicResponseHandler BASIC_RESPONSE_HANDLER = new BasicResponseHandler();

}
