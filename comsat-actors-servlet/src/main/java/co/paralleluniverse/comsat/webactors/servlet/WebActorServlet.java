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

import co.paralleluniverse.actors.Actor;
import co.paralleluniverse.actors.ActorRef;
import co.paralleluniverse.actors.ActorSpec;
import co.paralleluniverse.actors.ExitMessage;
import co.paralleluniverse.actors.FakeActor;
import co.paralleluniverse.actors.LifecycleMessage;
import co.paralleluniverse.actors.ShutdownMessage;
import co.paralleluniverse.comsat.webactors.Cookie;
import co.paralleluniverse.comsat.webactors.HttpRequest;
import co.paralleluniverse.comsat.webactors.HttpResponse;
import co.paralleluniverse.comsat.webactors.HttpStreamOpened;
import co.paralleluniverse.comsat.webactors.WebDataMessage;
import co.paralleluniverse.comsat.webactors.WebMessage;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.Timeout;
import co.paralleluniverse.strands.channels.SendPort;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.TimeUnit;
import javax.servlet.AsyncContext;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.WebListener;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

/**
 * A servlet that forwards requests to a web actor.
 */
@WebListener
public final class WebActorServlet extends HttpServlet implements HttpSessionListener {
    static final String ACTOR_KEY = "co.paralleluniverse.actor";
    static final String ACTOR_CLASS_PARAM = "actor";
    static final String ACTOR_PARAM_PREFIX = "actorParam";

    private String redirectPath = null;
    private String actorClassName = null;
    private String[] actorParams = null;

    @Override
    public final void init(ServletConfig config) throws ServletException {
        super.init(config);
        final Enumeration<String> initParameterNames = config.getInitParameterNames();
        final SortedMap<Integer, String> map = new TreeMap<>();
        while (initParameterNames.hasMoreElements()) {
            final String name = initParameterNames.nextElement();
            if (name.equals("redirectNoSessionPath"))
                setRedirectNoSessionPath(config.getInitParameter(name));
            else if (name.equals(ACTOR_CLASS_PARAM))
                setActorClassName(config.getInitParameter(name));
            else if (name.startsWith(ACTOR_PARAM_PREFIX)) {
                try {
                    final int num = Integer.parseInt(name.substring("actorParam".length()));
                    map.put(num, config.getInitParameter(name));
                } catch (final NumberFormatException nfe) {
                    getServletContext().log("Wrong actor parameter number: ", nfe);
                }

            }
        }
        final Collection<String> values = map.values();
        actorParams = values.toArray(new String[values.size()]);
    }

    public final WebActorServlet setRedirectNoSessionPath(String path) {
        this.redirectPath = path;
        return this;
    }

    public final WebActorServlet setActorClassName(String className) {
        this.actorClassName = className;
        return this;
    }

    static <T> ActorRef<T> attachWebActor(HttpSession session, ActorRef<T> actor) {
        //noinspection SynchronizationOnLocalVariableOrMethodParameter
        synchronized (session) {
            Object oldActor;
            if ((oldActor = session.getAttribute(ACTOR_KEY)) != null)
                //noinspection unchecked
                return (ActorRef<T>) oldActor;
            //noinspection unchecked
            session.setAttribute(ACTOR_KEY, new HttpActor(session, (ActorRef<HttpRequest>) actor));
            return actor;
        }
    }

    static boolean isWebActorAttached(HttpSession session) {
        //noinspection SynchronizationOnLocalVariableOrMethodParameter
        synchronized (session) {
            return (session.getAttribute(ACTOR_KEY) != null);
        }
    }

    static HttpActor getHttpActor(HttpSession session) {
        final Object actor = session.getAttribute(ACTOR_KEY);
        if ((actor != null) && (actor instanceof HttpActor))
            return (HttpActor) actor;
        return null;
    }

    static ActorRef<? super HttpRequest> getWebActor(HttpSession session) {
        final HttpActor har = getHttpActor(session);
        return har != null ? har.userActor : null;
    }

    @Override
    public final void sessionCreated(HttpSessionEvent se) {
    }

    @Override
    public final void sessionDestroyed(HttpSessionEvent se) {
        final HttpActor ha = getHttpActor(se.getSession());
        if (ha != null)
            ha.die(null);
    }

    @Override
    protected final void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpActor ha = getHttpActor(req.getSession());

        if (ha == null) {
            if (actorClassName != null) {
                try {
                    //noinspection unchecked
                    final ActorRef<WebMessage> actor = (ActorRef<WebMessage>) Actor.newActor(new ActorSpec(Class.forName(actorClassName), actorParams)).spawn();
                    attachWebActor(req.getSession(), actor);
                    ha = getHttpActor(req.getSession());
                } catch (final ClassNotFoundException ex) {
                    req.getServletContext().log("Unable to load actorClass: ", ex);
                    return;
                }
            } else if (redirectPath != null) {
                resp.sendRedirect(redirectPath);
                return;
            } else {
                resp.sendError(500, "Actor not found");
                return;
            }
        }

        assert ha != null;
        ha.service(req, resp);
    }

    static final class HttpActor extends FakeActor<HttpResponse> {
        private HttpSession session;
        private ActorRef<? super HttpRequest> userActor;

        private volatile boolean dead;
        private volatile AsyncContext asyncCtx;
        private volatile HttpServletResponse resp;
        private volatile Object watchToken;

        public HttpActor(HttpSession session, ActorRef<? super HttpRequest> userActor) {
            super(session.toString(), new HttpChannel());

            ((HttpChannel) (SendPort) getMailbox()).actor = this;

            this.session = session;
            this.userActor = userActor;
        }

        ActorRef<? super HttpRequest> getUserActor() {
            return userActor;
        }

        final void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            watchToken = watch(userActor);

            this.resp = resp;

            if (isDone()) {
                req.getSession().removeAttribute(ACTOR_KEY);
                handleDeath(getDeathCause());
                return;
            }

            req.setAttribute("org.apache.catalina.ASYNC_SUPPORTED", true);
            this.asyncCtx = req.startAsync();
            try {
                userActor.send(new ServletHttpRequest(ref(), req, resp));
            } catch (final SuspendExecution ex) {
                req.getServletContext().log("Exception: ", ex);
            }
        }

        private void unwatch() {
            if (watchToken != null && userActor != null)
                unwatch(userActor, watchToken);
        }

        private void handleDeath(Throwable cause) throws IOException {
            if (cause != null)
                resp.sendError(500, "Actor is dead because of " + cause.getMessage());
            else
                resp.sendError(500, "Actor has terminated.");
            if (asyncCtx != null)
                asyncCtx.complete(); // Seems to be required only by Tomcat, TODO: perform only on Tomcat
        }

        @Override
        protected final HttpResponse handleLifecycleMessage(LifecycleMessage m) {
            if (m instanceof ExitMessage) {
                final ExitMessage em = (ExitMessage) m;
                if (em.getActor() != null && em.getActor().equals(userActor)) {
                    try {
                        handleDeath(em.getCause());
                    } catch (final IOException e) {
                        throw new RuntimeException(e);
                    }
                    die(em.getCause());
                }
            }
            return null;
        }

        @Override
        protected final void throwIn(RuntimeException e) {
            die(e);
        }

        @Override
        protected final void interrupt() {
            die(new InterruptedException());
        }

        @Override
        protected void die(Throwable cause) {
            if (dead)
                return;
            dead = true;
            super.die(cause);
            try {
                session.invalidate();
            } catch (final Exception ignored) {}

            // Ensure to release references to server objects
            unwatch(userActor, watchToken);
            userActor = null;
            session = null;
            asyncCtx = null;
            resp = null;
        }

        @Override
        public final String toString() {
            return "ServletHttpActor{" + "session=" + session + ", userActor=" + userActor + '}';
        }
    }

    private static final class HttpChannel implements SendPort<HttpResponse> {
        private HttpActor actor;

        HttpChannel() {}

        @Override
        public final void send(HttpResponse message) throws SuspendExecution, InterruptedException {
            trySend(message);
        }

        @Override
        public final boolean send(HttpResponse message, long timeout, TimeUnit unit) throws SuspendExecution, InterruptedException {
            send(message);
            return true;
        }

        @Override
        public final boolean send(HttpResponse message, Timeout timeout) throws SuspendExecution, InterruptedException {
            return send(message, timeout.nanosLeft(), TimeUnit.NANOSECONDS);
        }

        @Override
        public final boolean trySend(HttpResponse msg) {
            try {
                final HttpServletRequest request = ((ServletHttpRequest) msg.getRequest()).request;
                if (!request.isAsyncStarted())
                    return false;

                final AsyncContext ctx = request.getAsyncContext();
                final HttpServletResponse response = (HttpServletResponse) ctx.getResponse();
                try {
                    if (!response.isCommitted()) {
                        if (msg.getCookies() != null) {
                            for (final Cookie wc : msg.getCookies())
                                response.addCookie(getServletCookie(wc));
                        }
                        if (msg.getHeaders() != null) {
                            for (final Map.Entry<String, String> h : msg.getHeaders().entries())
                                response.addHeader(h.getKey(), h.getValue());
                        }
                    }

                    if (msg.getStatus() >= 400 && msg.getStatus() < 600) {
                        //noinspection ThrowableResultOfMethodCallIgnored
                        response.sendError(msg.getStatus(), msg.getError() != null ? msg.getError().toString() : null);
                        ctx.complete(); // Seems to be required only by Tomcat, TODO: perform only on Tomcat
                        return true;
                    }

                    if (msg.getRedirectPath() != null) {
                        response.sendRedirect(msg.getRedirectPath());
                        ctx.complete(); // Seems to be required only by Tomcat, TODO: perform only on Tomcat
                        return true;
                    }

                    if (!response.isCommitted()) {
                        response.setStatus(msg.getStatus());

                        if (msg.getContentType() != null)
                            response.setContentType(msg.getContentType());
                        if (msg.getCharacterEncoding() != null)
                            response.setCharacterEncoding(msg.getCharacterEncoding().name());
                    }
                    final ServletOutputStream out = writeBody(msg, response, !msg.shouldStartActor());
                    out.flush(); // commits the response

                    if (msg.shouldStartActor()) {
                        try {
                            msg.getFrom().send(new HttpStreamOpened(new HttpStreamActor(request, response).ref(), msg));
                        } catch (final SuspendExecution e) {
                            throw new AssertionError(e);
                        }
                    } else {
                        out.close();
                        ctx.complete();
                    }
                    return true;
                } catch (final IOException ex) {
                    if (request.getServletContext() != null)
                        request.getServletContext().log("IOException", ex);
                    ctx.complete();
                    return false;
                }
            } finally {
                if (actor != null)
                    actor.unwatch();
            }
        }

        @Override
        public final void close() {
            if (actor != null)
                actor.die(null);
        }

        @Override
        public final void close(Throwable t) {
            t.printStackTrace(System.err); // TODO: log
            if (actor != null)
                actor.die(t);
        }
    }

    private static final class HttpStreamActor extends FakeActor<WebDataMessage> {
        private volatile boolean dead;

        public HttpStreamActor(final HttpServletRequest request, final HttpServletResponse response) {
            super(request.toString(), new HttpStreamChannel(request.getAsyncContext(), response));
            ((HttpStreamChannel) (Object) mailbox()).actor = this;
        }

        @Override
        protected final WebDataMessage handleLifecycleMessage(LifecycleMessage m) {
            if (m instanceof ShutdownMessage)
                die(null);
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
            if (dead)
                return;
            dead = true;
            mailbox().close();
            super.die(cause);
        }

        @Override
        public final String toString() {
            return "HttpStreamActor{request + " + getName() + "}";
        }
    }

    private static final class HttpStreamChannel implements SendPort<WebDataMessage> {
        HttpStreamActor actor;

        AsyncContext ctx;
        HttpServletResponse response;

        public HttpStreamChannel(AsyncContext ctx, HttpServletResponse response) {
            this.ctx = ctx;
            this.response = response;
        }

        @Override
        public void send(WebDataMessage message) throws SuspendExecution, InterruptedException {
            trySend(message);
        }

        @Override
        public boolean send(WebDataMessage message, long timeout, TimeUnit unit) throws SuspendExecution, InterruptedException {
            send(message);
            return true;
        }

        @Override
        public boolean send(WebDataMessage message, Timeout timeout) throws SuspendExecution, InterruptedException {
            return send(message, timeout.nanosLeft(), TimeUnit.NANOSECONDS);
        }

        @Override
        public boolean trySend(WebDataMessage message) {
            try {
                ServletOutputStream os = writeBody(message, response, false);
                os.flush();
            } catch (final IOException ex) {
                if (actor != null)
                    actor.die(ex);
                return false;
            }
            return true;
        }

        @Override
        public void close() {
            try {
                try {
                    ServletOutputStream os = response.getOutputStream();
                    os.close();
                } catch (IOException e) {
                    //ctx.getRequest().getServletContext().log("error", e);
                }
                ctx.complete();
            } catch (final Exception ignored) {
            } finally {
                if (actor != null)
                    actor.die(null);
                actor = null;
                ctx = null;
                response = null;
            }
        }

        @Override
        public void close(Throwable t) {
            if (actor != null)
                actor.die(t);
            close();
        }
    }

    static ServletOutputStream writeBody(WebMessage message, HttpServletResponse response, boolean shouldClose) throws IOException {
        final byte[] arr;
        final int offset;
        final int length;
        ByteBuffer bb;
        String sb;
        if ((bb = message.getByteBufferBody()) != null) {
            if (bb.hasArray()) {
                arr = bb.array();
                offset = bb.arrayOffset() + bb.position();
                length = bb.remaining();
                bb.position(bb.limit());
            } else {
                arr = new byte[bb.remaining()];
                bb.get(arr);
                offset = 0;
                length = arr.length;
            }
        } else if ((sb = message.getStringBody()) != null) {
            arr = sb.getBytes(response.getCharacterEncoding());
            offset = 0;
            length = arr.length;
        } else {
            arr = null;
            offset = 0;
            length = 0;
        }

        if (!response.isCommitted() && shouldClose)
            response.setContentLength(length);

        final ServletOutputStream out = response.getOutputStream();
        if (arr != null)
            out.write(arr, offset, length);
        return out;
    }

    private static javax.servlet.http.Cookie getServletCookie(Cookie wc) {
        javax.servlet.http.Cookie c = new javax.servlet.http.Cookie(wc.getName(), wc.getValue());
        c.setComment(wc.getComment());
        if (wc.getDomain() != null)
            c.setDomain(wc.getDomain());
        c.setMaxAge(wc.getMaxAge());
        c.setPath(wc.getPath());
        c.setSecure(wc.isSecure());
        c.setHttpOnly(wc.isHttpOnly());
        c.setVersion(wc.getVersion());
        return c;
    }
}
