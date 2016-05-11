/*
 * COMSAT
 * Copyright (c) 2015-2016, Parallel Universe Software Co. All rights reserved.
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
package co.paralleluniverse.comsat.webactors.undertow;

import co.paralleluniverse.actors.*;
import co.paralleluniverse.common.reflection.AnnotationUtil;
import co.paralleluniverse.common.reflection.ClassLoaderUtil;
import co.paralleluniverse.common.util.Pair;
import co.paralleluniverse.comsat.webactors.WebActor;
import co.paralleluniverse.comsat.webactors.WebMessage;

import io.undertow.UndertowLogger;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.session.Session;
import io.undertow.util.Sessions;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

/**
 * @author circlespainter
 */
public final class AutoWebActorHandler extends WebActorHandler {
    private static final List<Class<?>> actorClasses = new ArrayList<>(4);
    private static final Object[] EMPTY_OBJECT_ARRAY = new Object[0];

    public AutoWebActorHandler() {
        this(null, null, null);
    }

    public AutoWebActorHandler(List<String> packagePrefixes) {
        this(null, packagePrefixes, null);
    }

    public AutoWebActorHandler(ClassLoader userClassLoader, List<String> packagePrefixes) {
        this(userClassLoader, packagePrefixes, null);
    }

    public AutoWebActorHandler(List<String> packagePrefixes, Map<Class<?>, Object[]> actorParams) {
        this(null, packagePrefixes, actorParams);
    }

    public AutoWebActorHandler(ClassLoader userClassLoader, List<String> packagePrefixes, Map<Class<?>, Object[]> actorParams) {
        super(null);
        super.contextProvider = newContextProvider(userClassLoader != null ? userClassLoader : ClassLoader.getSystemClassLoader(), packagePrefixes, actorParams);
    }

    public AutoWebActorHandler(AutoContextProvider prov) {
        super(prov);
    }

    protected AutoContextProvider newContextProvider(ClassLoader userClassLoader, List<String> packagePrefixes, Map<Class<?>, Object[]> actorParams) {
        return new AutoContextProvider(userClassLoader, packagePrefixes, actorParams);
    }

    private static class AutoContextProvider implements ContextProvider {
        private final ClassLoader userClassLoader;
        private final Map<Class<?>, Object[]> actorParams;
        private final Long defaultContextValidityMS;
        private final List<String> packagePrefixes;

        public AutoContextProvider(ClassLoader userClassLoader, List<String> packagePrefixes, Map<Class<?>, Object[]> actorParams) {
            this(userClassLoader, packagePrefixes, actorParams, null);
        }

        public AutoContextProvider(ClassLoader userClassLoader, List<String> packagePrefixes, Map<Class<?>, Object[]> actorParams, Long defaultContextValidityMS) {
            this.userClassLoader = userClassLoader;
            this.packagePrefixes = packagePrefixes;
            this.actorParams = actorParams;
            this.defaultContextValidityMS = defaultContextValidityMS;
        }

        @Override
        public final Context get(final HttpServerExchange xch) {
            Session session = null;
            Context actorContext = newContext(xch);
            if (actorContext != null) {
                try {
                    session = Sessions.getOrCreateSession(xch);
                } catch (final IllegalStateException ignored) {
                } // No session handler

                if (session != null) {
                    Context sessionContext = (Context) session.getAttribute(ACTOR_KEY);
                    if (sessionContext == null || !sessionContext.renew()) {
                        session.setAttribute(ACTOR_KEY, actorContext);
                    } else {
                        actorContext = sessionContext;
                    }
                }
            }

            return actorContext;
        }

        private Context newContext(final HttpServerExchange xch) {
            final AutoActorContext c = new AutoActorContext(packagePrefixes, actorParams, userClassLoader);
            boolean valid = c.fillActor(xch);
            if (!valid) {
                return null;
            }

            if (defaultContextValidityMS != null)
                c.setValidityMS(defaultContextValidityMS);
            return c;
        }
    }

    private static final class AutoActorContext extends DefaultContextImpl {
        private String id;

        private final List<String> packagePrefixes;
        private final Map<Class<?>, Object[]> actorParams;
        private final ClassLoader userClassLoader;
        private Class<? extends ActorImpl<? extends WebMessage>> actorClass;
        private ActorRef<? extends WebMessage> actorRef;

        public AutoActorContext(List<String> packagePrefixes, Map<Class<?>, Object[]> actorParams, ClassLoader userClassLoader) {
            this.packagePrefixes = packagePrefixes;
            this.actorParams = actorParams;
            this.userClassLoader = userClassLoader;
        }

        /**
         * Associates a {@link WebActor} annotated class to an {@link HttpServerExchange}
         * @param xch The exchange
         * @return Whether or not an actor has been associated
         */
        private boolean fillActor(HttpServerExchange xch) {
            final Pair<ActorRef<? extends WebMessage>, Class<? extends ActorImpl<? extends WebMessage>>> p = autoCreateActor(xch);
            if (p != null) {
                actorRef = p.getFirst();
                actorClass = p.getSecond();
                return true;
            }
            return false;
        }

        @Override
        public final String getId() {
            return id != null ? id : (id = UUID.randomUUID().toString());
        }

        @Override
        public final void restart(HttpServerExchange xch) {
            renewed = new Date().getTime();
            fillActor(xch);
        }

        @Override
        public final ActorRef<? extends WebMessage> getWebActor() {
            return actorRef;
        }

        @Override
        public final boolean handlesWithWebSocket(String uri) {
            return WebActorHandler.handlesWithWebSocket(uri, actorClass);
        }

        @Override
        public final boolean handlesWithHttp(String uri) {
            return WebActorHandler.handlesWithHttp(uri, actorClass);
        }

        @Override
        public WatchPolicy watch() {
            return WatchPolicy.DIE_IF_EXCEPTION_ELSE_RESTART;
        }

        @SuppressWarnings("unchecked")
        private Pair<ActorRef<? extends WebMessage>, Class<? extends ActorImpl<? extends WebMessage>>> autoCreateActor(HttpServerExchange xch) {
            registerActorClasses();
            final String uri = xch.getRequestURI();
            for (final Class<?> c : actorClasses) {
                if (WebActorHandler.handlesWithHttp(uri, c) || WebActorHandler.handlesWithWebSocket(uri, c))
                    return new Pair<ActorRef<? extends WebMessage>, Class<? extends ActorImpl<? extends WebMessage>>>(
                        Actor.newActor(
                            new ActorSpec(c, actorParams != null ? actorParams.get(c) : EMPTY_OBJECT_ARRAY)
                        ).spawn(),
                        (Class<? extends ActorImpl<? extends WebMessage>>) c
                    );
            }

            return null;
        }

        private synchronized void registerActorClasses() {
            if (actorClasses.isEmpty()) {
                try {
                    final ClassLoader classLoader = userClassLoader != null ? userClassLoader : this.getClass().getClassLoader();
                    ClassLoaderUtil.accept((URLClassLoader) classLoader, new ClassLoaderUtil.Visitor() {
                        @Override
                        public final void visit(String resource, URL url, ClassLoader cl) {
                            if (packagePrefixes != null) {
                                boolean found = false;
                                for (final String packagePrefix : packagePrefixes) {
                                    if (packagePrefix != null && resource.startsWith(packagePrefix.replace('.', '/'))) {
                                        found = true;
                                        break;
                                    }
                                }
                                if (!found)
                                    return;
                            }
                            if (!ClassLoaderUtil.isClassFile(resource))
                                return;
                            final String className = ClassLoaderUtil.resourceToClass(resource);
                            try (final InputStream is = cl.getResourceAsStream(resource)) {
                                if (AnnotationUtil.hasClassAnnotation(WebActor.class, is))
                                    registerWebActor(cl.loadClass(className));
                            } catch (final IOException | ClassNotFoundException e) {
                                UndertowLogger.ROOT_LOGGER.error("Exception while scanning class " + className + " for WebActor annotation", e);
                                throw new RuntimeException(e);
                            }
                        }

                        private void registerWebActor(Class<?> c) {
                            actorClasses.add(c);
                        }
                    });
                } catch (final IOException e) {
                    UndertowLogger.ROOT_LOGGER.error("IOException while scanning classes for WebActor annotation", e);
                }
            }
        }
    }
}
