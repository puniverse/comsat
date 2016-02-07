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
import co.paralleluniverse.actors.ExitMessage;
import co.paralleluniverse.actors.FakeActor;
import co.paralleluniverse.actors.LifecycleMessage;
import co.paralleluniverse.comsat.webactors.WebDataMessage;
import co.paralleluniverse.comsat.webactors.WebMessage;
import co.paralleluniverse.comsat.webactors.WebSocketOpened;
import static co.paralleluniverse.comsat.webactors.servlet.WebActorServlet.ACTOR_KEY;
import co.paralleluniverse.comsat.webactors.servlet.WebActorServlet.HttpActor;
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
public final class WebActorEndpoint extends Endpoint {
    private volatile EndpointConfig config;

    @Override
    public final void onOpen(Session session, EndpointConfig config) {
        if (this.config == null)
            this.config = config;
        final ActorRef webActor = getHttpSessionActor(config).getUserActor();
        if (webActor != null) {
            //noinspection unchecked
            final WebSocketActor wsa = attachWebActor(session, config, webActor);
            wsa.onOpen();
        } else {
            try {
                session.close(new CloseReason(CloseReason.CloseCodes.CANNOT_ACCEPT, "session actor not found"));
            } catch (final IOException ex) {
                getHttpSession(config).getServletContext().log("IOException", ex);
            }
        }
    }

    static WebSocketActor attachWebActor(Session session, EndpointConfig config, ActorRef<? super WebMessage> actor) {
        return attachWebActor(session, getHttpSession(config), actor);
    }

    static WebSocketActor attachWebActor(Session session, HttpSession httpSession, ActorRef<? super WebMessage> actor) {
        if (session.getUserProperties().containsKey(ACTOR_KEY))
            throw new RuntimeException("Session is already attached to an actor.");
        final WebSocketActor wsa = new WebSocketActor(session, httpSession, actor);
        session.getUserProperties().put(ACTOR_KEY, wsa);
        return wsa;
    }

    @Override
    public final void onError(Session session, Throwable t) {
        getSessionActor(session).onError(t);
    }

    @Override
    public final void onClose(Session session, CloseReason closeReason) {
        getSessionActor(session).onClose(closeReason);
    }

    private static WebSocketActor getSessionActor(Session session) {
        return (WebSocketActor) session.getUserProperties().get(ACTOR_KEY);
    }

    private static HttpActor getHttpSessionActor(EndpointConfig config) {
        final HttpSession httpSession = getHttpSession(config);
        if (httpSession == null)
            throw new RuntimeException("HttpSession hasn't been embedded by the EndPoint Configurator.");
        return (HttpActor) httpSession.getAttribute(ACTOR_KEY);
    }

    private static HttpSession getHttpSession(EndpointConfig config) {
        return (HttpSession) config.getUserProperties().get(HttpSession.class.getName());
    }

    static final class WebSocketActor extends FakeActor<WebDataMessage> {
        private final Session session;
        private final HttpSession httpSession;
        private final ActorRef<? super WebMessage> webActor;

        public WebSocketActor(Session session, HttpSession httpSession, ActorRef<? super WebMessage> webActor) {
            super(session.toString(), new WebSocketChannel(session, httpSession));

            ((WebSocketChannel) (SendPort) getMailbox()).actor = this;

            this.session = session;
            this.httpSession = httpSession;
            this.webActor = webActor;
            watch(webActor);

            session.addMessageHandler(new MessageHandler.Whole<ByteBuffer>() {
                @Override
                public final void onMessage(final ByteBuffer message) {
                    try {
                        WebSocketActor.this.webActor.send(new WebDataMessage(WebSocketActor.this.ref(), message));
                    } catch (final SuspendExecution ex) {
                        throw new AssertionError(ex);
                    }
                }
            });
            session.addMessageHandler(new MessageHandler.Whole<String>() {
                @Override
                public final void onMessage(final String message) {
                    try {
                        WebSocketActor.this.webActor.send(new WebDataMessage(WebSocketActor.this.ref(), message));
                    } catch (final SuspendExecution ex) {
                        throw new AssertionError(ex);
                    }
                }
            });
        }

        final void onOpen() {
            try {
                FiberUtil.runInFiber(new SuspendableRunnable() {
                    @Override
                    public final void run() throws SuspendExecution, InterruptedException {
                        webActor.send(new WebSocketOpened(WebSocketActor.this.ref()));
                    }
                });
            } catch (final ExecutionException e) {
                log("Exception in onOpen", e.getCause());
            } catch (final InterruptedException e) {
                throw new RuntimeException();
            }
        }

        final void onClose(CloseReason closeReason) {
            // don't call die because the session is already closed.
            super.die(closeReason.getCloseCode() == CloseReason.CloseCodes.NORMAL_CLOSURE ? null : new RuntimeException(closeReason.toString()));
        }

        final void onError(Throwable t) {
            log("onError", t);
        }

        @Override
        protected final WebDataMessage handleLifecycleMessage(LifecycleMessage m) {
            if (m instanceof ExitMessage) {
                final ExitMessage em = (ExitMessage) m;
                if (em.getActor() != null && em.getActor().equals(webActor))
                    die(em.getCause());
            }
            return null;
        }

        @Override
        protected final void throwIn(RuntimeException e) {
            die(e);
        }

        @Override
        public final void interrupt() {
            die(new InterruptedException());
        }

        @Override
        protected final void die(Throwable cause) {
            super.die(cause);
            try {
                session.close(new CloseReason(CloseReason.CloseCodes.GOING_AWAY, cause != null ? (cause.getClass() + ": " + cause.getMessage()) : ""));
            } catch (final IOException ex) {
                log("IOException on interrupt", ex);
            }
        }

        private void log(String message, Throwable t) {
            httpSession.getServletContext().log(message, t);
        }

        @Override
        public String toString() {
            return "WebSocketActor{" + "session=" + session + ", webActor=" + webActor + '}';
        }
    }

    private static final class WebSocketChannel implements SendPort<WebDataMessage> {
        private final Session session;
        private final HttpSession httpSession;

        WebSocketActor actor;

        public WebSocketChannel(Session session, HttpSession httpSession) {
            this.session = session;
            this.httpSession = httpSession;
        }

        @Override
        public final void send(WebDataMessage message) throws SuspendExecution, InterruptedException {
            trySend(message);
        }

        @Override
        public final boolean send(WebDataMessage message, long timeout, TimeUnit unit) throws SuspendExecution, InterruptedException {
            return trySend(message);
        }

        @Override
        public final boolean send(WebDataMessage message, Timeout timeout) throws SuspendExecution, InterruptedException {
            return send(message, timeout.nanosLeft(), TimeUnit.NANOSECONDS);
        }

        @Override
        public final boolean trySend(WebDataMessage message) {
            if (!session.isOpen())
                return false;
            // TODO: use fiber async instead of servlet Async ?
            if (!message.isBinary())
                session.getAsyncRemote().sendText(message.getStringBody());
            else
                session.getAsyncRemote().sendBinary(message.getByteBufferBody());
            return true;
        }

        @Override
        public final void close() {
            try {
                session.close();
            } catch (final IOException ex) {
                httpSession.getServletContext().log("IOException on close", ex);
            } finally {
                if (actor != null)
                    actor.die(null);
            }
        }

        @Override
        public final void close(Throwable t) {
            if (actor != null)
                actor.die(t);
            close();
        }
    }
}
