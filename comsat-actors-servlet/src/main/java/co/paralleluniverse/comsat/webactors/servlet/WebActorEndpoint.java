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
import static co.paralleluniverse.comsat.webactors.servlet.WebActorServlet.ACTOR_KEY;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.channels.SendPort;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;
import javax.servlet.http.HttpSession;
import javax.websocket.CloseReason;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import javax.websocket.Session;

public class WebActorEndpoint extends Endpoint {
    private volatile EndpointConfig config;

    static void attachWebSocket(final Session session, EndpointConfig config, final ActorRef<Object> actor) {
        if (session.getUserProperties().containsKey(ACTOR_KEY))
            throw new RuntimeException("Session is already attached to an actor.");
        session.getUserProperties().put(ACTOR_KEY, actor);
        final SendPort<WebDataMessage> sp = new WebSocketChannel(session, config);
        session.addMessageHandler(new MessageHandler.Whole<ByteBuffer>() {
            @Override
            public void onMessage(final ByteBuffer message) {
                try {
                    actor.send(new WebDataMessage(sp, message));
                } catch (SuspendExecution ex) {
                    throw new AssertionError(ex);
                }
            }
        });
        session.addMessageHandler(new MessageHandler.Whole<String>() {
            @Override
            public void onMessage(final String message) {
                try {
                    actor.send(new WebDataMessage(sp, message));
                } catch (SuspendExecution ex) {
                    throw new AssertionError(ex);
                }
            }
        });
    }

    @Override
    public void onOpen(final Session session, EndpointConfig config) {
        if (this.config == null)
            this.config = config;
        ActorRef<Object> actor = getHttpSessionActor(config);
        if (actor != null) {
            attachWebSocket(session, config, actor);
        } else {
            try {
                session.close(new CloseReason(CloseReason.CloseCodes.CANNOT_ACCEPT, "session actor not found"));
            } catch (IOException ex) {
                getHttpSession(config).getServletContext().log("IOException", ex);
            }
        }
    }

    @Override
    public void onError(Session session, Throwable t) {
        getHttpSession(config).getServletContext().log("onError", t);
    }

    @Override
    public void onClose(Session session, CloseReason closeReason) {
    }

    static ActorRef<Object> getHttpSessionActor(EndpointConfig config) {
        HttpSession httpSession = getHttpSession(config);
        if (httpSession == null)
            throw new RuntimeException("HttpSession hasn't been embedded by the EndPoint Configurator.");
        return (ActorRef<Object>) httpSession.getAttribute(ACTOR_KEY);
    }

    static HttpSession getHttpSession(EndpointConfig config) {
        return (HttpSession) config.getUserProperties().get(HttpSession.class.getName());
    }

    private static class WebSocketChannel implements SendPort<WebDataMessage> {
        private final Session session;
        private final EndpointConfig config;

        public WebSocketChannel(Session session, EndpointConfig config) {
            this.session = session;
            this.config = config;
        }

        @Override
        public void send(WebDataMessage message) throws SuspendExecution, InterruptedException {
            trySend(message);
        }

        @Override
        public boolean send(WebDataMessage message, long timeout, TimeUnit unit) throws SuspendExecution, InterruptedException {
            return trySend(message);
        }

        @Override
        public boolean trySend(WebDataMessage message) {
            if (!session.isOpen())
                return false;
            if (!message.isBinary())
                session.getAsyncRemote().sendText(message.getStringBody()); // TODO: use fiber async instead of servlet Async ?
            else
                session.getAsyncRemote().sendBinary(message.getByteBufferBody());
            return true;
        }

        @Override
        public void close() {
            try {
                session.close();
            } catch (IOException ex) {
                WebActorEndpoint.getHttpSession(config).getServletContext().log("IOException on close", ex);
            }
        }
    }
}
