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
import co.paralleluniverse.comsat.webactors.WebDataMessage;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.channels.SendPort;
import java.nio.ByteBuffer;
import javax.servlet.http.HttpSession;
import javax.websocket.MessageHandler;
import javax.websocket.Session;

public class ServletWebActors {
    static final String ACTOR_KEY = "co.paralleluniverse.actor";

    public static void attachHttpSession(HttpSession session, ActorRef<Object> actor) {
        session.setAttribute(ACTOR_KEY, actor);
    }

    public static void attachWebSocket(final Session session, final ActorRef<Object> actor) {
        if (session.getUserProperties().containsKey(ACTOR_KEY))
            throw new RuntimeException("Session is already attached to an actor.");
        session.getUserProperties().put(ACTOR_KEY, actor);
        // TODO: register the handler in order to enable detach
        final SendPort<WebDataMessage> sp = new WebSocketSendPort(session);
        session.addMessageHandler(new MessageHandler.Whole<ByteBuffer>() {
            @Override
            public void onMessage(final ByteBuffer message) {
                try {
                    actor.send(new WebDataMessage(message) {
                        @Override
                        public SendPort<WebDataMessage> sender() {
                            return sp;
                        }
                    });
                } catch (SuspendExecution ex) {
                    throw new AssertionError(ex);
                }
            }
        });
        session.addMessageHandler(new MessageHandler.Whole<String>() {
            @Override
            public void onMessage(final String message) {
                try {
                    actor.send(new WebDataMessage(message) {
                        @Override
                        public SendPort<WebDataMessage> sender() {
                            return sp;
                        }
                    });
                } catch (SuspendExecution ex) {
                    throw new AssertionError(ex);
                }
            }
        });

    }

    public static void detachWebSocket(final Session session) {
        ActorRef<?> get = (ActorRef<?>) session.getUserProperties().get(ACTOR_KEY);
        if (get != null) {
            session.getUserProperties().remove(ACTOR_KEY);
//            session.removeMessageHandler(null);
        }
    }

    public static boolean isHttpAttached(HttpSession session) {
        return (session.getAttribute(ACTOR_KEY) != null);
    }
}
