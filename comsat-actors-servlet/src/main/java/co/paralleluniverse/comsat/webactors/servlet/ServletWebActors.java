/*
 * COMSAT
 * Copyright (c) 2013-2016, Parallel Universe Software Co. All rights reserved.
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

import co.paralleluniverse.actors.ActorRef;
import co.paralleluniverse.comsat.webactors.HttpRequest;
import co.paralleluniverse.comsat.webactors.WebMessage;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.Callable;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.websocket.DeploymentException;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.Session;
import javax.websocket.server.ServerContainer;
import javax.websocket.server.ServerEndpointConfig;

/**
 * Static methods to attach web actors with HTTP and WebSocket sessions
 */
public final class ServletWebActors {
    /**
     * Attaches the given web actor to the given {@link HttpSession}.
     *
     * @param session the session
     * @param actor   the web actor
     * @return {@code actor}
     */
    public static <T> ActorRef<T> attachWebActor(HttpSession session, final ActorRef<T> actor) {
        //noinspection unchecked,ConstantConditions
        return WebActorServlet.getOrAttachSessionActor(session, new Callable<ActorRef>() {
            @Override
            public ActorRef call() throws Exception {
                return actor;
            }
        }).httpSessionActor.userWebActor;
    }

    /**
     * Checks whether a web actor is attached to an {@link HttpSession}.
     */
    public static boolean isWebActorAttached(HttpSession session) {
        return WebActorServlet.isWebActorAttached(session);
    }

    /**
     * Returns the web actor attached to the given {@link HttpSession}.
     *
     * @param session the session
     * @return the web actor attached to the session, or {@code null} if none.
     */
    public static ActorRef<? super HttpRequest> getWebActor(HttpSession session) {
        return WebActorServlet.getWebActor(session);
    }

    /**
     * Attaches the given web actor to the given WebSocket session
     *
     * @param session     the WebSocket {@link Session}
     * @param httpSession the WebSocket's associated HTTP session
     * @param actor       the web actor
     * @return {@code actor}
     */
    public static <T> ActorRef<T> attachWebActor(Session session, HttpSession httpSession, ActorRef<T> actor) {
        WebActorEndpoint.attachWebActor(session, httpSession, (ActorRef<WebMessage>) actor);
        return actor;
    }

    /**
     * Attaches the given web actor to the given WebSocket session
     *
     * @param session the WebSocket {@link Session}
     * @param config  the WebSocket endpoint's configuration
     * @param actor   the web actor
     * @return {@code actor}
     */
    public static <T> ActorRef<T> attachWebActor(Session session, EndpointConfig config, ActorRef<T> actor) {
        WebActorEndpoint.attachWebActor(session, config, (ActorRef<WebMessage>) actor);
        return actor;
    }

    /**
     * Attaches the given web actor to web socket sessions that will be opened at the given URI (or URI pattern).
     * This method installs a new WebSocket {@link Endpoint}.
     *
     * @param sc           the {@link ServletContext}
     * @param webSocketURI the WebSocket's URI (or URI pattern)
     * @param actor        the web actor
     * @return {@code actor}
     */
    public static <T> ActorRef<T> attachWebActor(ServletContext sc, String webSocketURI, ActorRef<T> actor) {
        return attachWebActor(sc, Collections.singleton(webSocketURI), actor);
    }

    /**
     * Attaches the given web actor to web socket sessions that will be opened at the given URIs (or URI patterns).
     * This method installs new WebSocket {@link Endpoint}s.
     *
     * @param sc            the {@link ServletContext}
     * @param webSocketURIs the WebSockets' URI (or URI pattern)
     * @param actor         the web actor
     * @return {@code actor}
     */
    public static <T> ActorRef<T> attachWebActor(ServletContext sc, Collection<String> webSocketURIs, ActorRef<T> actor) {
        final ServerContainer scon = (ServerContainer) sc.getAttribute("javax.websocket.server.ServerContainer");
        for (String webSocketURI : webSocketURIs) {
            try {
                scon.addEndpoint(ServerEndpointConfig.Builder.create(WebActorEndpoint.class, webSocketURI).configurator(new EmbedHttpSessionWsConfigurator()).build());
            } catch (DeploymentException ex) {
                sc.log("Unable to deploy endpoint", ex);
            }
        }
        return actor;
    }

    private ServletWebActors() {
    }
}
