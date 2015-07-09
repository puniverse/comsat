/*
 * COMSAT
 * Copyright (c) 2015, Parallel Universe Software Co. All rights reserved.
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
package co.paralleluniverse.comsat.webactors.netty;

import co.paralleluniverse.actors.Actor;
import co.paralleluniverse.actors.ActorImpl;
import co.paralleluniverse.actors.ActorSpec;
import co.paralleluniverse.common.reflection.AnnotationUtil;
import co.paralleluniverse.common.reflection.ClassLoaderUtil;
import co.paralleluniverse.comsat.webactors.WebActor;
import co.paralleluniverse.comsat.webactors.WebMessage;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author circlespainter
 */
public final class AutoWebActorHandler extends WebActorHandler {
    private static final AttributeKey<Session> SESSION_KEY = AttributeKey.newInstance(AutoWebActorHandler.class.getName() + ".session");

    private static final InternalLogger log = InternalLoggerFactory.getInstance(AutoWebActorHandler.class);
    private static final List<Class<?>> actorClasses = new ArrayList<>(4);
    private static final Object[] EMPTY_OBJECT_ARRAY = new Object[0];

    public AutoWebActorHandler() {
        this(null, null, null);
    }

    public AutoWebActorHandler(String httpResponseEncoderName) {
        this(httpResponseEncoderName, null, null);
    }

    public AutoWebActorHandler(String httpResponseEncoderName, ClassLoader userClassLoader) {
        this(httpResponseEncoderName, userClassLoader, null);
    }

    public AutoWebActorHandler(String httpResponseEncoderName, Map<Class<?>, Object[]> actorParams) {
        this(httpResponseEncoderName, null, actorParams);
    }

    public AutoWebActorHandler(String httpResponseEncoderName, final ClassLoader userClassLoader, final Map<Class<?>, Object[]> actorParams) {
        super(new SessionSelector() {
            @Override
            public Session select(ChannelHandlerContext ctx, final FullHttpRequest req) {
                final Attribute<Session> s = ctx.attr(SESSION_KEY);
                if (s.get() == null) {
                    final String sessionId = getSessionId(req);
                    if (sessionId != null) {
                        final Session session = sessions.get(sessionId);
                        if (session != null && session.isValid())
                            s.setIfAbsent(session);
                    } else
                        s.setIfAbsent(new DefaultSessionImpl() {
                            private ActorImpl<? extends WebMessage> actor;

                            @Override
                            public ActorImpl<? extends WebMessage> getActor() {
                                if (actor != null)
                                    return actor;
                                else
                                    return (actor = autoCreateActor(req));
                            }

                            @SuppressWarnings("unchecked")
                            private ActorImpl<? extends WebMessage> autoCreateActor(FullHttpRequest req) {
                                registerActorClasses();
                                final String uri = req.getUri();
                                for (final Class<?> c : actorClasses) {
                                    if (handlesWithHttp(uri, c) || handlesWithWebSocket(uri, c)) {
                                        final Actor ret = Actor.newActor(new ActorSpec(c, actorParams != null ? actorParams.get(c) : EMPTY_OBJECT_ARRAY));
                                        ret.spawn();
                                        return ret;
                                    }
                                }
                                return null;
                            }

                            private synchronized void registerActorClasses() {
                                if (actorClasses.isEmpty()) {
                                    try {
                                        final ClassLoader classLoader = userClassLoader != null ? userClassLoader : this.getClass().getClassLoader();
                                        ClassLoaderUtil.accept((URLClassLoader) classLoader, new ClassLoaderUtil.Visitor() {
                                            @Override
                                            public void visit(String resource, URL url, ClassLoader cl) {
                                                if (!ClassLoaderUtil.isClassFile(resource))
                                                    return;
                                                final String className = ClassLoaderUtil.resourceToClass(resource);
                                                try (InputStream is = cl.getResourceAsStream(resource)) {
                                                    if (AnnotationUtil.hasClassAnnotation(WebActor.class, is))
                                                        registerWebActor(cl.loadClass(className));
                                                } catch (IOException | ClassNotFoundException e) {
                                                    log.error("Exception while scanning class " + className + " for WebActor annotation", e);
                                                    throw new RuntimeException(e);
                                                }
                                            }

                                            private void registerWebActor(Class<?> c) {
                                                actorClasses.add(c);
                                            }
                                        });
                                    } catch (IOException e) {
                                        log.error("IOException while scanning classes for WebActor annotation", e);
                                    }
                                }
                            }
                        }
                    );
                }
                return s.get();
            }

            private String getSessionId(FullHttpRequest req) {
                final String cookiesString = req.headers().get(HttpHeaders.Names.COOKIE);
                if (cookiesString != null) {
                    final Set<Cookie> cookies = ServerCookieDecoder.LAX.decode(cookiesString);
                    if (cookies != null) {
                        for (final Cookie c : cookies) {
                            if (c != null && SESSION_COOKIE_KEY.equals(c.name()))
                                return c.value();
                        }
                    }
                }
                return null;
            }
        }, httpResponseEncoderName);
    }
}
