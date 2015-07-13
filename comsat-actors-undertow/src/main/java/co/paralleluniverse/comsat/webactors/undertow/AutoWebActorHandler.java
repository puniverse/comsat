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
package co.paralleluniverse.comsat.webactors.undertow;

import co.paralleluniverse.actors.Actor;
import co.paralleluniverse.actors.ActorImpl;
import co.paralleluniverse.actors.ActorSpec;
import co.paralleluniverse.common.reflection.AnnotationUtil;
import co.paralleluniverse.common.reflection.ClassLoaderUtil;
import co.paralleluniverse.comsat.webactors.WebActor;
import co.paralleluniverse.comsat.webactors.WebMessage;
import io.undertow.UndertowLogger;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.session.Session;
import io.undertow.server.session.SessionConfig;
import io.undertow.server.session.SessionManager;
import io.undertow.util.AttachmentKey;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author circlespainter
 */
public final class AutoWebActorHandler extends WebActorHandler {
	private static final AttachmentKey<ActorContext> SESSION_KEY = AttachmentKey.create(ActorContext.class);

	private static final List<Class<?>> actorClasses = new ArrayList<>(4);
	private static final Object[] EMPTY_OBJECT_ARRAY = new Object[0];

	public AutoWebActorHandler() {
		this(null, null);
	}

	public AutoWebActorHandler(ClassLoader userClassLoader) {
		this(userClassLoader, null);
	}

	public AutoWebActorHandler(Map<Class<?>, Object[]> actorParams) {
		this(null, actorParams);
	}

	public AutoWebActorHandler(final ClassLoader userClassLoader, final Map<Class<?>, Object[]> actorParams) {
		super(new ActorContextProvider() {
			@Override
			public ActorContext get(final HttpServerExchange xch) {
				ActorContext s = xch.getAttachment(SESSION_KEY);
				if (s == null) {
					final Session session = getSession(xch);

					if (session != null) {
						final ActorContext actorContext = (ActorContext) session.getAttribute(ACTOR_KEY);
						if (actorContext != null && actorContext.isValid())
							xch.putAttachment(SESSION_KEY, s = actorContext);
						else
							xch.putAttachment(SESSION_KEY, s = new DefaultActorContextImpl() {
									private ActorImpl<? extends WebMessage> actor;

									@Override
									public ActorImpl<? extends WebMessage> getActor() {
										if (actor != null)
											return actor;
										else
											return (actor = autoCreateActor(xch));
									}

									@SuppressWarnings("unchecked")
									private ActorImpl<? extends WebMessage> autoCreateActor(HttpServerExchange xch) {
										registerActorClasses();
										final String uri = xch.getRequestURI();
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
														} catch (IOException e) {
															UndertowLogger.REQUEST_IO_LOGGER.ioException(e);
														} catch (ClassNotFoundException e) {
															UndertowLogger.ROOT_LOGGER.fatalf(e, "Class not found");
															throw new RuntimeException(e);
														}
													}

													private void registerWebActor(Class<?> c) {
														actorClasses.add(c);
													}
												});
											} catch (IOException e) {
												UndertowLogger.REQUEST_IO_LOGGER.ioException(e);
											}
										}
									}
								}
							);
					}
				}
				return s;
			}

			private Session getSession(HttpServerExchange xch) {
				final SessionManager sm = xch.getAttachment(SessionManager.ATTACHMENT_KEY);
				final SessionConfig sessionConfig = xch.getAttachment(SessionConfig.ATTACHMENT_KEY);
				final Session session = sm.getSession(xch, sessionConfig);
				if (session == null)
					return sm.createSession(xch, sessionConfig);
				return session;
			}
		});
	}
}
