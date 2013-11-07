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

import co.paralleluniverse.actors.ActorRef;
import co.paralleluniverse.comsat.webactors.HttpRequest;
import javax.servlet.http.HttpSession;
import javax.websocket.EndpointConfig;
import javax.websocket.Session;

public class ServletWebActors {
    public static void attachWebActor(HttpSession session, ActorRef<Object> actor) {
        WebActorServlet.attachWebActor(session, actor);
    }

    public static boolean isWebActorAttached(HttpSession session) {
        return WebActorServlet.isWebActorAttached(session);
    }

    public static ActorRef<? super HttpRequest> getWebActor(HttpSession session) {
        return WebActorServlet.getWebActor(session);
    }

    public static void attachWebActor(Session session, EndpointConfig config, ActorRef<Object> actor) {
        WebActorEndpoint.attachWebActor(session, config, actor);
    }
}
