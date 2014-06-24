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
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.eclipse.jetty.deploy.DeploymentManager;
import org.eclipse.jetty.deploy.providers.WebAppProvider;
import org.eclipse.jetty.plus.jndi.Resource;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.After;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

public class JettyLoaderTest {

    private CloseableHttpClient client;
    private Server server;

    @Before
    public void setUp() throws Exception {
        this.server = new Server(8080);
        scanDirForWebApps(server, "build/resources/test/webapps");
        Resource r = new Resource("jdbc/globalds",
                createDataSource("jdbc:h2:./build/h2testdb"));
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
        for (int i = 0; i < 10; i++) {
            String result = client.execute(new HttpGet("http://localhost:8080/"), BASIC_RESPONSE_HANDLER);
            assertTrue(result.contains("h2testdb"));
        }
    }

    private static final BasicResponseHandler BASIC_RESPONSE_HANDLER = new BasicResponseHandler();

    private static JdbcDataSource createDataSource(final String url) {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL(url);
        return ds;
    }

    private static void scanDirForWebApps(final Server server, final String path) {
        WebAppProvider wap = new WebAppProvider();
        wap.setMonitoredDirName(path);
        wap.setExtractWars(true);

        DeploymentManager dm = new DeploymentManager();
        dm.setContexts(new ContextHandlerCollection());
        dm.addAppProvider(wap);

        server.setHandler(dm.getContexts());
        server.addBean(dm);
    }
}
