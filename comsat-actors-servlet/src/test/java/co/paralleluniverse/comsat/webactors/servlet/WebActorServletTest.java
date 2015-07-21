/*
 * COMSAT
 * Copyright (C) 2014-2015, Parallel Universe Software Co. All rights reserved.
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
package co.paralleluniverse.comsat.webactors.servlet;

import co.paralleluniverse.comsat.webactors.AbstractWebActorTest;
import co.paralleluniverse.embedded.containers.AbstractEmbeddedServer;
import co.paralleluniverse.embedded.containers.EmbeddedServer;
import co.paralleluniverse.embedded.containers.JettyServer;
import co.paralleluniverse.embedded.containers.TomcatServer;
import co.paralleluniverse.embedded.containers.UndertowServer;
import com.google.common.collect.Lists;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import javax.websocket.ClientEndpointConfig;
import org.apache.http.client.CookieStore;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class WebActorServletTest extends AbstractWebActorTest {
    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
            {JettyServer.class}, 
            {TomcatServer.class},
            {UndertowServer.class},
        });
    }
    private final Class<? extends EmbeddedServer> cls;
    private EmbeddedServer embeddedServer;

    public WebActorServletTest(Class<? extends EmbeddedServer> cls) {
        this.cls = cls;
    }

//    public WebActorServletTest() {
//        this.cls = JettyServer.class;
//    }

    @Before
    public void setUp() throws Exception {
        this.embeddedServer = cls.newInstance();
        // snippet WebActorInitializer
        WebActorInitializer.setUserClassLoader(ClassLoader.getSystemClassLoader());
        embeddedServer.addServletContextListener(WebActorInitializer.class);
        // end of snippet
        embeddedServer.enableWebsockets();
        embeddedServer.start();
        AbstractEmbeddedServer.waitUrlAvailable("http://localhost:8080");
        System.out.println("Server is up");
    }

    @After
    public void tearDown() throws Exception {       
        embeddedServer.stop();
    }

    @Override
    protected String getSessionIdCookieName() {
        return JSESSIONID;
    }

    @Override
    protected ClientEndpointConfig getClientEndPointConfig(final CookieStore cookieStore) {
        return ClientEndpointConfig.Builder.create().configurator(new ClientEndpointConfig.Configurator() {
            @Override
            public void beforeRequest(Map<String, List<String>> headers) {
                headers.put("Cookie", Lists.newArrayList(JSESSIONID + '=' + getByName(cookieStore, JSESSIONID)));
            }
        }).build();
    }

    public static String getByName(final CookieStore cookieStore, String name) {
        for (org.apache.http.cookie.Cookie cookie : cookieStore.getCookies())
            if (name.equals(cookie.getName()))
                return cookie.getValue();
        return null;
    }

    private static final String JSESSIONID = "JSESSIONID";
}
