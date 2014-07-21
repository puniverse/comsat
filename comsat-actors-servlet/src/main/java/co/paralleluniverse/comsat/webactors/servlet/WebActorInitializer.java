/*
 * COMSAT
 * Copyright (c) 2013-2014, Parallel Universe Software Co. All rights reserved.
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

import co.paralleluniverse.common.reflection.AnnotationUtil;
import co.paralleluniverse.common.reflection.ClassLoaderUtil;
import co.paralleluniverse.comsat.webactors.WebActor;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletRegistration.Dynamic;
import javax.servlet.annotation.WebListener;
import javax.websocket.DeploymentException;
import javax.websocket.server.ServerContainer;
import javax.websocket.server.ServerEndpointConfig;

/**
 * Registers WebActors annotated with the {@link WebActor} annotation.
 * Scan classes of the servletContext classLoader for WebActor annotated classes
 * You can set a userClassLoader with 
 */
@WebListener
public class WebActorInitializer implements ServletContextListener {
    private static ClassLoader userClassLoader;

    /**
     *
     * @param userClassLoader Scan classes of this classLoader WebActor annotated classes
     * @return WebActorInitializer.class
     */
    public static Class<? extends WebActorInitializer> setUserClassLoader(ClassLoader userClassLoader) {
        WebActorInitializer.userClassLoader = userClassLoader;
        return WebActorInitializer.class;
    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        final ServletContext sc = sce.getServletContext();
        try {
            ClassLoader classLoader = userClassLoader != null ? userClassLoader : sc.getClassLoader();
            ClassLoaderUtil.accept((URLClassLoader)classLoader, new ClassLoaderUtil.Visitor() {
                @Override
                public void visit(String resource, URL url, ClassLoader cl) {
                    if (!ClassLoaderUtil.isClassFile(resource))
                        return;
                    final String className = ClassLoaderUtil.resourceToClass(resource);
                    try (InputStream is = cl.getResourceAsStream(resource)) {
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

    public static void registerWebActor(ServletContext sc, Class<?> webActorClass) {
        final WebActor waAnn = webActorClass.getAnnotation(WebActor.class);
        final String name = (waAnn.name() != null && !waAnn.name().isEmpty()) ? waAnn.name() : webActorClass.getName();
        // servlet
        Dynamic d = sc.addServlet(name, WebActorServlet.class);
        d.setInitParameter(WebActorServlet.ACTOR_CLASS_PARAM, webActorClass.getName());
        d.setAsyncSupported(true);
        d.addMapping(waAnn.httpUrlPatterns());
        d.addMapping(waAnn.value());

        // web socket
        ServerContainer scon = (ServerContainer) sc.getAttribute("javax.websocket.server.ServerContainer");
        assert scon!=null : "Container does not support websockets !!!";
        for (String wsPath : waAnn.webSocketUrlPatterns()) {
            try {
                scon.addEndpoint(ServerEndpointConfig.Builder.create(WebActorEndpoint.class, wsPath).configurator(new EmbedHttpSessionWsConfigurator()).build());
            } catch (DeploymentException ex) {
                sc.log("Unable to deploy endpoint", ex);
            }
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
    }
}
