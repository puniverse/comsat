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
import co.paralleluniverse.fibers.instrument.*;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
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
            ClassLoaderUtil.accept(sc.getClassLoader(), new ClassLoaderUtil.Visitor() {
                @Override
                public void visit(String resource, URL url, ClassLoader cl) {
                    if (!ClassLoaderUtil.isClassfile(resource))
                        return;
                    final String className = ClassLoaderUtil.resourceToClass(resource);
                    try(InputStream is = cl.getResourceAsStream(resource)) {
                        if (AnnotationUtil.hasClassAnnotation(WebActor.class, is))
                            registerWebActor(sc, cl.loadClass(className));
                    } catch (IOException | ClassNotFoundException e) {
                        sc.log("Exception while scanning class " + className + " for WebActor annotation", e);
                        throw new RuntimeException(e);
                    }
                }
            });
//            final ClassPath classpath = ClassPath.from(cl);
//            for (ClassPath.ClassInfo ci : classpath.getTopLevelClasses()) {
//                try {
//                    if (AnnotationUtil.hasClassAnnotation(WebActor.class, cl.getResourceAsStream(ci.getName().replace('.', '/') + ".class")))
//                        registerWebActor(sc, ci.load());
//                } catch (IOException e) {
//                    sc.log("IOException while scanning class " + ci.getName() + " for WebActor annotation", e);
//                }
//            }
        } catch (IOException e) {
            sc.log("IOException while scanning classes for WebActor annotation", e);
        }
    }

    private void registerWebActor(ServletContext sc, Class<?> webActorClass) {
        final WebActor waAnn = webActorClass.getAnnotation(WebActor.class);
        final String name = (waAnn.name() != null && !waAnn.name().isEmpty()) ? waAnn.name() : webActorClass.getName();
        Dynamic d = sc.addServlet(name, WebActorServlet.class);
        d.setInitParameter(WebActorServlet.ACTOR_CLASS_PARAM, webActorClass.getName());
        d.setAsyncSupported(true);
        d.addMapping(waAnn.httpUrlPatterns());
        d.addMapping(waAnn.value());
        for (String wsPath : waAnn.webSocketUrlPatterns())
            WebSocketEndpointRegistry.registerEndpoint(sc, ServerEndpointConfig.Builder.create(WebActorEndpoint.class, wsPath));
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
    }
}
