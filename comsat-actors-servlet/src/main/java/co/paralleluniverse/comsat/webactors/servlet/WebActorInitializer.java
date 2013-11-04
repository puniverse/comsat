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

import co.paralleluniverse.comsat.webactors.WebActor;
import co.paralleluniverse.fibers.instrument.AnnotationUtil;
import com.google.common.reflect.ClassPath;
import java.io.IOException;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletRegistration.Dynamic;
import javax.servlet.annotation.WebListener;
import javax.websocket.server.ServerEndpointConfig;

/**
 * Registers WebActors annotated with the {@link WebActor} annotation.
 *
 * @author pron
 */
@WebListener
public class WebActorInitializer implements ServletContextListener {
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        final ServletContext sc = sce.getServletContext();
        try {
            final ClassLoader cl = sc.getClassLoader();
            final ClassPath classpath = ClassPath.from(cl);
            for (ClassPath.ClassInfo ci : classpath.getTopLevelClasses()) {
                try {
                    if (AnnotationUtil.hasClassAnnotation(WebActor.class, cl.getResourceAsStream(ci.getName())))
                        registerWebActor(sc, ci.load());
                } catch (IOException e) {
                    sc.log("IOException while scanning class " + ci.getName() + " for WebActor annotation", e);
                }
            }
        } catch (IOException e) {
            sc.log("IOException while scanning classes for WebActor annotation", e);
        }
    }

    private void registerWebActor(ServletContext sc, Class<?> webActorClass) {
        final WebActor waAnn = webActorClass.getAnnotation(WebActor.class);
        
        String servletName = waAnn.name();
        Dynamic d = sc.addServlet(servletName, new WebActorServlet());
        d.setInitParameter("actor", webActorClass.getName());
        d.addMapping(waAnn.httpUrlPatterns());

        for (String wsPath : waAnn.webSocketUrlPatterns())
            WebSocketEndpointRegistry.registerEndpoint(sc, ServerEndpointConfig.Builder.create(WebActorEndpoint.class, wsPath));
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
    }
}
