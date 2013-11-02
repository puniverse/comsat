/*
 * COMSAT
 * Copyright (C) 2013, Parallel Universe Software Co. All rights reserved.
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

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.websocket.DeploymentException;
import javax.websocket.server.ServerContainer;
import javax.websocket.server.ServerEndpointConfig;
import javax.websocket.server.ServerEndpointConfig.Builder;

//@WebListener
public class WebSocketEndpointRegistry implements ServletContextListener {
    public static void registerEndpoint(ServletContext sc, ServerEndpointConfig.Builder sec) {
        getConfigsList(sc, true).add(sec);
    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        ServletContext sc = sce.getServletContext();
        List<ServerEndpointConfig.Builder> secs = getConfigsList(sc, false);

        final ServerContainer scon = (ServerContainer) sc.getAttribute("javax.websocket.server.ServerContainer");
        if (scon != null & secs != null) {
            for (ServerEndpointConfig.Builder sec : secs) {
                try {
                    scon.addEndpoint(sec.configurator(new EmbedHttpSessionWsConfigurator()).build());
                } catch (DeploymentException ex) {
                    sc.log("Unable to deploy endpoint", ex);
                }

            }
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
    }

    private static List<Builder> getConfigsList(ServletContext sc, final boolean create) {
        List<ServerEndpointConfig.Builder> secs = (List<ServerEndpointConfig.Builder>) sc.getAttribute("secs");
        if (secs == null & create) {
            secs = new CopyOnWriteArrayList<>();
            sc.setAttribute("secs", secs);
            sc.addListener(WebSocketEndpointRegistry.class);
        }
        return secs;
    }
}
