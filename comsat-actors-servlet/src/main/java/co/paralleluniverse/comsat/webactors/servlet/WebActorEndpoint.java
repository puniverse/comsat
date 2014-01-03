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
import co.paralleluniverse.actors.ExitMessage;
import co.paralleluniverse.actors.FakeActor;
import co.paralleluniverse.actors.LifecycleMessage;
import co.paralleluniverse.comsat.webactors.WebDataMessage;
import co.paralleluniverse.comsat.webactors.WebMessage;
import co.paralleluniverse.comsat.webactors.WebSocketOpened;
import static co.paralleluniverse.comsat.webactors.servlet.WebActorServlet.ACTOR_KEY;
import co.paralleluniverse.comsat.webactors.servlet.WebActorServlet.HttpActorRef;
import co.paralleluniverse.fibers.FiberUtil;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.SuspendableRunnable;
import co.paralleluniverse.strands.Timeout;
import co.paralleluniverse.strands.channels.SendPort;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import javax.servlet.http.HttpSession;
import javax.websocket.CloseReason;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import javax.websocket.Session;

/**
 * A WebSocket endpoint that forwards requests to a web actor.
 */
public class WebActorEndpoint extends Endpoint {
    private volatile EndpointConfig config;

    @Override
    public void onOpen(Session session, EndpointConfig config) {
        if (this.config == null)
            this.config = config;
        ActorRef webActor = getHttpSessionActor(config).getWebActor();
        if (webActor != null) {
            WebSocketActorRef wsa = attachWebActor(session, config, webActor);
            wsa.onOpen();
        } else {
            try {
                session.close(new CloseReason(CloseReason.CloseCodes.CANNOT_ACCEPT, "session actor not found"));
            } catch (IOException ex) {
                getHttpSession(config).getServletContext().log("IOException", ex);
            }
        }
    }

    static WebSocketActorRef attachWebActor(Session session, EndpointConfig config, ActorRef<? super WebMessage> actor) {
        return attachWebActor(session, getHttpSession(config), actor);
    }

    static WebSocketActorRef attachWebActor(Session session, HttpSession httpSession, ActorRef<? super WebMessage> actor) {
        if (session.getUserProperties().containsKey(ACTOR_KEY))
            throw new RuntimeException("Session is already attached to an actor.");
        WebSocketActorRef wsa = new WebSocketActorRef(session, httpSession, actor);
        session.getUserProperties().put(ACTOR_KEY, wsa);
        return wsa;
    }

    @Override
    public void onError(Session session, Throwable t) {
        getSessionActor(session).onError(t);
    }

    @Override
    public void onClose(Session session, CloseReason closeReason) {
        getSessionActor(session).onClose(closeReason);
    }

    private static WebSocketActorRef getSessionActor(Session session) {
        return (WebSocketActorRef) session.getUserProperties().get(ACTOR_KEY);
    }

    private static HttpActorRef getHttpSessionActor(EndpointConfig config) {
        HttpSession httpSession = getHttpSession(config);
        if (httpSession == null)
            throw new RuntimeException("HttpSession hasn't been embedded by the EndPoint Configurator.");
        return (HttpActorRef) httpSession.getAttribute(ACTOR_KEY);
    }

    private static HttpSession getHttpSession(EndpointConfig config) {
        return (HttpSession) config.getUserProperties().get(HttpSession.class.getName());
    }

    static class WebSocketActorRef extends FakeActor<WebDataMessage> {
        private final Session session;
        private final HttpSession httpSession;
        private final ActorRef<? super WebMessage> webActor;

        public WebSocketActorRef(Session session, EndpointConfig config, ActorRef<? super WebMessage> webActor) {
            this(session, getHttpSession(config), webActor);
        }

        public WebSocketActorRef(Session session, HttpSession httpSession, ActorRef<? super WebMessage> webActor) {
            super(session.toString(), new WebSocketChannel(session, httpSession));
            this.session = session;
            this.httpSession = httpSession;
            this.webActor = webActor;
            watch(webActor);

            session.addMessageHandler(new MessageHandler.Whole<ByteBuffer>() {
                @Override
                public void onMessage(final ByteBuffer message) {
                    try {
                        WebSocketActorRef.this.webActor.send(new WebDataMessage(WebSocketActorRef.this, message));
                    } catch (SuspendExecution ex) {
                        throw new AssertionError(ex);
                    }
                }
            });
            session.addMessageHandler(new MessageHandler.Whole<String>() {
                @Override
                public void onMessage(final String message) {
                    try {
                        WebSocketActorRef.this.webActor.send(new WebDataMessage(WebSocketActorRef.this, message));
                    } catch (SuspendExecution ex) {
                        throw new AssertionError(ex);
                    }
                }
            });
        }

        void onOpen() {
            try {
                FiberUtil.runInFiber(new SuspendableRunnable() {
                    @Override
                    public void run() throws SuspendExecution, InterruptedException {
                        webActor.send(new WebSocketOpened(WebSocketActorRef.this));
                    }
                });
            } catch (ExecutionException e) {
                log("Exception in onOpen", e.getCause());
            } catch (InterruptedException e) {
                throw new RuntimeException();
            }
        }

        void onClose(CloseReason closeReason) {
            die(closeReason.getCloseCode() == CloseReason.CloseCodes.NORMAL_CLOSURE ? null : new RuntimeException(closeReason.toString()));
        }

        void onError(Throwable t) {
            log("onError", t);
        }

        ActorRef<? super WebDataMessage> getWebActor() {
            return webActor;
        }

        @Override
        protected WebDataMessage handleLifecycleMessage(LifecycleMessage m) {
            if (m instanceof ExitMessage) {
                ExitMessage em = (ExitMessage) m;
                if (em.getActor() != null && em.getActor().equals(webActor))
                    die(em.getCause());
            }
            return null;
        }

        @Override
        protected void throwIn(RuntimeException e) {
            die(e);
        }

        @Override
        public void interrupt() {
            die(new InterruptedException());
        }

        @Override
        protected void die(Throwable cause) {
            super.die(cause);
            try {
                session.close(new CloseReason(CloseReason.CloseCodes.GOING_AWAY, cause != null ? (cause.getClass() + ": " + cause.getMessage()) : ""));
            } catch (IOException ex) {
                log("IOException on interrupt", ex);
            }
        }

        private void log(String message) {
            httpSession.getServletContext().log(message);
        }

        private void log(String message, Throwable t) {
            httpSession.getServletContext().log(message, t);
        }
    }

    private static class WebSocketChannel implements SendPort<WebDataMessage> {
        private final Session session;
        private final HttpSession httpSession;

        public WebSocketChannel(Session session, HttpSession httpSession) {
            this.session = session;
            this.httpSession = httpSession;
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
        public boolean send(WebDataMessage message, Timeout timeout) throws SuspendExecution, InterruptedException {
            return send(message, timeout.nanosLeft(), TimeUnit.NANOSECONDS);
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
                httpSession.getServletContext().log("IOException on close", ex);
            }
        }
    }
}
