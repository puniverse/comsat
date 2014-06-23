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

import io.undertow.util.FileUtils;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import javax.servlet.ServletException;
import org.apache.catalina.startup.Tomcat;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.tomcat.util.descriptor.web.ContextResource;
import org.junit.After;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class TomcatLoaderTest {

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

    public TomcatLoaderTest(Class<? extends EmbeddedServer> cls) {
        this.cls = cls;
    }

    @Before
    public void setUp() throws Exception {
        this.tomcat = new Tomcat();
        loadWars(tomcat, "build", "build/wars", "/");
        registerDB(tomcat, "jdbc/globalds", "org.h2.Driver", "jdbc:h2:./build/h2testdb");
        tomcat.start();
        this.client = HttpClients.custom().setDefaultRequestConfig(RequestConfig.custom()
                .setSocketTimeout(5000).setConnectTimeout(5000).setConnectionRequestTimeout(5000)
                .build()).build();
    }

    @After
    public void tearDown() throws Exception {
        tomcat.stop();
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
    public static void registerDB(final Tomcat tomcat, final String name, final String driver, final String url) {
        ContextResource dbDsRes = new ContextResource();
        dbDsRes.setName(name);
        dbDsRes.setAuth("Container");
        dbDsRes.setType("javax.sql.DataSource");
        dbDsRes.setProperty("maxActive", "100");
        dbDsRes.setProperty("maxIdle", "30");
        dbDsRes.setProperty("maxWait", "10000");
        dbDsRes.setScope("Sharable");
        dbDsRes.setProperty("driverClassName", driver);
        dbDsRes.setProperty("url", url);
        dbDsRes.setProperty("username", "");
        dbDsRes.setProperty("password", "");
        tomcat.enableNaming();
        tomcat.getServer().getGlobalNamingResources().addResource(dbDsRes);
    }

    public static void loadWars(final Tomcat tomcat, String baseDir, String warDir, String path) throws ServletException {
        File webapps = new File(baseDir + "/webapps");
        FileUtils.deleteRecursive(webapps);
        webapps.mkdirs();
        tomcat.setBaseDir(baseDir);

        // scan for the first war
        for (final File fileEntry : new File(warDir).listFiles()) {
            System.out.println("Found: " + fileEntry.getName());
            if (fileEntry.getName().endsWith(".war")) {
                String war = fileEntry.getName().substring(0, fileEntry.getName().length() - ".war".length());
                System.out.println("Loading WAR: " + war + " to: http://localhost:8080" + path);
                tomcat.addWebapp(path, fileEntry.getAbsolutePath());
                break;
            }
        }
    }


}
