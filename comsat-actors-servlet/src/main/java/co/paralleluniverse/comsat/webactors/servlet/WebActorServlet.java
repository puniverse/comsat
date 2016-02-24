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

import co.paralleluniverse.actors.*;
import co.paralleluniverse.common.util.Pair;
import co.paralleluniverse.common.util.SystemProperties;
import co.paralleluniverse.comsat.webactors.Cookie;
import co.paralleluniverse.comsat.webactors.*;
import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.Suspendable;
import co.paralleluniverse.strands.SuspendableRunnable;
import co.paralleluniverse.strands.Timeout;
import co.paralleluniverse.strands.channels.Channel;
import co.paralleluniverse.strands.channels.Channels;
import co.paralleluniverse.strands.channels.SendPort;
import co.paralleluniverse.strands.concurrent.ReentrantLock;

import javax.servlet.*;
import javax.servlet.annotation.WebListener;
import javax.servlet.http.*;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A servlet that forwards requests to a web actor.
 */
@WebListener
public final class WebActorServlet extends HttpServlet implements HttpSessionListener {
    static final String ACTOR_CLASS_PARAM = "actor";
    static final String ACTOR_PARAM_PREFIX = "actorParam";

    static final String ACTOR_SESSION_KEY = "co.paralleluniverse.actor";

    private static final String HTTP_SERVLET_FAKE_ACTOR_SESSION_KEY = WebActorServlet.class.getName() + ".httpFakeActor";

    private static final String DISPATCH_INTERNAL_ERROR_EXCEPTION_REQUEST_KEY = WebActorServlet.class.getName() + ".reqAttr.internalErrorException";
    private static final String DISPATCH_ERROR_STATUS_REQUEST_KEY = WebActorServlet.class.getName() + ".reqAttr.errorStatus";
    private static final String DISPATCH_ERROR_MESSAGE_REQUEST_KEY = WebActorServlet.class.getName() + ".reqAttr.errorMessage";

    public static final String PROP_DISABLE_SYNC_ERRORS = WebActorServlet.class.getName() + ".disableSyncErrors";
    static final boolean disableSyncErrorsGlobal = SystemProperties.isEmptyOrTrue(PROP_DISABLE_SYNC_ERRORS);

    public static final String PROP_ACTOR_END_ACTION = WebActorServlet.class.getName() + ".actorEndAction";
    public static final String PROP_ACTOR_END_ACTION_VALUE_DIE_IF_ERROR_ELSE_RESTART = "dieIfErrorElseRestart";
    public static final String PROP_ACTOR_END_ACTION_VALUE_RESTART = "restart";
//    public static final String PROP_ACTOR_END_ACTION_VALUE_DIE = "die";
    static final String actorEndActionGlobal = System.getProperty(PROP_ACTOR_END_ACTION, PROP_ACTOR_END_ACTION_VALUE_DIE_IF_ERROR_ELSE_RESTART);

    public static final String PROP_ASYNC_TIMEOUT = HttpActor.class.getName() + ".asyncTimeout";

    private final AtomicReference<ActorSpec> specAR = new AtomicReference<>();

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

    @Suspendable
    static <T> Pair<HttpActor, ActorRef<T>> attachWebActor(final HttpSession session, final ActorRef<T> actor, ActorSpec spec) {
        ReentrantLock l = null;
        try {
            final HttpActor oldHttpActor = (HttpActor) session.getAttribute(HTTP_SERVLET_FAKE_ACTOR_SESSION_KEY);
            final HttpActor httpActor;
            if (oldHttpActor == null)
                httpActor = new HttpActor(spec);
            else
                httpActor = oldHttpActor;
            l = httpActor.singleReqInProgressPerSessionLock;
            l.lock();
            if (oldHttpActor == null)
                session.setAttribute(HTTP_SERVLET_FAKE_ACTOR_SESSION_KEY, httpActor);
            final Object oldActor = session.getAttribute(ACTOR_SESSION_KEY);
            if (oldActor != null)
                //noinspection unchecked
                return new Pair<>(httpActor, (ActorRef<T>) oldActor);
            //noinspection unchecked
            session.setAttribute(ACTOR_SESSION_KEY, actor);
            return new Pair<>(httpActor, actor);
        } catch (final IllegalStateException e) { // Invalidated session
            return null;
        } finally {
            if (l != null)
                l.unlock();
        }
    }

    static boolean isWebActorAttached(final HttpSession session) {
        ReentrantLock l = null;
        try {
            final Object httpActor = session.getAttribute(HTTP_SERVLET_FAKE_ACTOR_SESSION_KEY);
            if (httpActor == null || !(httpActor instanceof HttpActor))
                return false;
            l = ((HttpActor) httpActor).singleReqInProgressPerSessionLock;
            l.lock();
            return (session.getAttribute(ACTOR_SESSION_KEY) != null);
        } catch (final IllegalStateException ignored) { // Invalidated
            return false;
        } finally {
            if (l != null)
                l.unlock();
        }
    }

    static HttpActor getHttpActor(final HttpSession session) {
        try {
            final Object httpActor = session.getAttribute(HTTP_SERVLET_FAKE_ACTOR_SESSION_KEY);
            if (httpActor == null || !(httpActor instanceof HttpActor))
                return null;
            return (HttpActor) httpActor;
        } catch (final IllegalStateException ignored) {
        } // Invalidated
        return null;
    }

    static ActorRef<? super HttpRequest> getWebActor(final HttpSession session) {
        ReentrantLock l = null;
        try {
            final Object httpActor = session.getAttribute(HTTP_SERVLET_FAKE_ACTOR_SESSION_KEY);
            if (httpActor == null || !(httpActor instanceof HttpActor))
                return null;
            l = ((HttpActor) httpActor).singleReqInProgressPerSessionLock;
            l.lock();
            Object userActor = session.getAttribute(ACTOR_SESSION_KEY);
            if (userActor == null || !(userActor instanceof ActorRef<?>))
                return null;
            //noinspection unchecked
            return ((ActorRef<? super HttpRequest>) userActor);
        } catch (final IllegalStateException ignored) { // Invalidated
            return null;
        } finally {
            if (l != null)
                l.unlock();
        }
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
    protected final void service(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
        if (!disableSyncErrorsGlobal && DispatcherType.ASYNC.equals(req.getDispatcherType())) {
            final Throwable ex = (Throwable) req.getAttribute(DISPATCH_INTERNAL_ERROR_EXCEPTION_REQUEST_KEY);
            if (ex != null)
                throw new ServletException(ex);
            final Integer status = (Integer) req.getAttribute(DISPATCH_ERROR_STATUS_REQUEST_KEY);
            if (status != null) {
                resp.sendError(status, (String) req.getAttribute(DISPATCH_ERROR_MESSAGE_REQUEST_KEY));
                return;
            }
        }

        if (isTomcat(req.getClass()))
            req.setAttribute("org.apache.catalina.ASYNC_SUPPORTED", true);
        final AsyncContext ctx = req.startAsync();

        new Fiber(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                try {
                    try {
                        //noinspection unchecked
                        specAR.compareAndSet(null, new ActorSpec(Class.forName(actorClassName), actorParams));
                    } catch (final ClassNotFoundException ex) {
                        internalError(req, ctx, resp, new RuntimeException("Unable to load actorClass: " + ex.getMessage()));
                        return;
                    }
                    final ActorSpec spec = specAR.get();
                    HttpActor ha = getHttpActor(req.getSession());
                    if (ha == null) {
                        if (actorClassName != null) {
                            //noinspection unchecked
                            final ActorRef<WebMessage> actor = (ActorRef<WebMessage>) Actor.newActor(spec).spawn();
                            final Pair<HttpActor, ActorRef<WebMessage>> ret = attachWebActor(req.getSession(), actor, spec);
                            if (ret == null) {
                                internalError(req, ctx, resp, new RuntimeException("Session invalidated during request execution"));
                                return;
                            }
                            ha = ret.getFirst();
                        } else if (redirectPath != null) {
                            redirect(ctx, resp, redirectPath);
                            return;
                        } else {
                            internalError(req, ctx, resp, new RuntimeException("Actor not found"));
                            return;
                        }
                    }

                    assert ha != null;

                    ha.service(ctx, req, resp);
                } catch (final ServletException | IOException e) {
                    // TODO review handling
                    e.printStackTrace(System.err);
                    throw new RuntimeException(e);
                }
            }
        }).start();
    }

    static void redirect(AsyncContext ac, HttpServletResponse res, String path) throws IOException {
        res.sendRedirect(path);
        if (isTomcat(ac.getClass())) // Seems required only by Tomcat
            ac.complete();
    }


    static void internalError(HttpServletRequest req, AsyncContext ac, HttpServletResponse resp, Throwable t) throws IOException {
        if (disableSyncErrorsGlobal) {
            resp.sendError(500, t.getMessage());
            if (isTomcat(ac.getClass())) // Seems required only by Tomcat
                ac.complete();
        } else {
            req.setAttribute(DISPATCH_INTERNAL_ERROR_EXCEPTION_REQUEST_KEY, t);
            ac.dispatch();
        }
    }

    static void error(HttpServletRequest req, AsyncContext ac, HttpServletResponse resp, int status, String message) throws IOException {
        if (disableSyncErrorsGlobal || isTomcat(ac.getClass())) { // Tomcat doesn't seem to support dispatching error responses
            resp.sendError(status, message);
            if (isTomcat(ac.getClass())) // Seems required only by Tomcat
                ac.complete();
        } else {
            req.setAttribute(DISPATCH_ERROR_STATUS_REQUEST_KEY, status);
            req.setAttribute(DISPATCH_ERROR_MESSAGE_REQUEST_KEY, message);
            ac.dispatch();
        }
    }

    static final class HttpActor extends FakeActor<HttpResponse> {
        static final long reqTimeoutMS;

        static {
            reqTimeoutMS = getLong(PROP_ASYNC_TIMEOUT, 120_000L);
        }

        static final class ReqInProgress {
            AsyncContext asyncCtx;
            HttpServletRequest req;
            HttpSession session;
            HttpServletResponse resp;
            ActorRef<? super HttpRequest> userActor;
            Object watchToken;
        }

        private final ReentrantLock singleReqInProgressPerSessionLock = new ReentrantLock();
        private final ActorSpec userActorSpec;

        final Channel<Object> done = Channels.newChannel(1);

        private AtomicReference<ReqInProgress>
            reqInProgressAR = new AtomicReference<>(),
            lastReqInProgressAR = new AtomicReference<>();

        private final HttpChannel channel;

        private volatile boolean dead;

        public HttpActor(ActorSpec spec) {
            super("HttpActor-" + UUID.randomUUID(), new HttpChannel());

            userActorSpec = spec;

            channel = ((HttpChannel) (SendPort) getMailbox());
            channel.actor = this;
        }

        // For webactors
        final ActorRef<? super HttpRequest> getUserActor() {
            final ReqInProgress r = lastReqInProgressAR.get();
            return r != null ? r.userActor : null;
        }

        final void service(final AsyncContext ctx, final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException, SuspendExecution, InterruptedException {
            singleReqInProgressPerSessionLock.lock(); // Else bad things can happen to the (thread-unsafe...) session

            try {
                startReq(ctx, req, resp);
                final ReqInProgress r = reqInProgressAR.get();
                if (r == null) {
                    if (resp != null) // Happened == null with Jetty
                        internalError(req, ctx, resp, new RuntimeException("Session invalidated during request execution"));
                    singleReqInProgressPerSessionLock.unlock();
                    return;
                }

                r.userActor.send(new ServletHttpRequest(ref(), req, resp));
                Object token = done.receive(reqTimeoutMS, TimeUnit.MILLISECONDS);
                if (token == null) // Timeout
                    internalError(req, ctx, resp, new RuntimeException("The request timed out (timeout after " + reqTimeoutMS + "ms"));
            } finally {
                unwatch();
                singleReqInProgressPerSessionLock.unlock();
            }
        }

        private void startReq(AsyncContext ctx, HttpServletRequest req, HttpServletResponse resp) {
            if (req == null || resp == null)
                return;

            ReqInProgress r = new ReqInProgress();

            r.req = req;
            r.resp = resp;
            r.asyncCtx = ctx;

            r.session = req.getSession();
            if (!watch(r, r.session))
                return;

            reqInProgressAR.set(r);
            lastReqInProgressAR.set(r);
        }

        private boolean watch(ReqInProgress r, HttpSession s) {
            r.userActor = s != null ? getWebActor0(s) : null;
            if (r.userActor == null || r.session == null)
                return false;

            r.watchToken = watch(r.userActor);
            return true;
        }

        private ActorRef<? super HttpRequest> getWebActor0(HttpSession session) {
            try {
                Object userActor = session.getAttribute(ACTOR_SESSION_KEY);
                if (userActor == null || !(userActor instanceof ActorRef<?>))
                    return null;
                //noinspection unchecked
                return ((ActorRef<? super HttpRequest>) userActor);
            } catch (final IllegalStateException ignored) { // Invalidated
                return null;
            }
        }

        final void unwatch() {
            final ReqInProgress r = reqInProgressAR.getAndSet(null);
            if (r != null) {
                unwatch(r.userActor, r.watchToken);
            }
        }

        private void abortReqInProgress(final Throwable cause) throws IOException {
            final ReqInProgress r = reqInProgressAR.get();
            if (r != null) { // Not unwatched
                if (cause != null)
                    internalError(r.req, r.asyncCtx, r.resp, new RuntimeException("Actor is dead because of " + cause.getMessage()));
                else
                    internalError(r.req, r.asyncCtx, r.resp, new RuntimeException("Actor has terminated."));
            }

            unwatch();
        }

        @Override
        protected final HttpResponse handleLifecycleMessage(LifecycleMessage m) {
            if (m instanceof ExitMessage) {
                final ExitMessage em = (ExitMessage) m;
                final ReqInProgress r = reqInProgressAR.get();
                if (em.getActor() != null && r != null && em.getActor().equals(r.userActor)) {
                    //noinspection ThrowableResultOfMethodCallIgnored
                    if (userActorSpec != null &&
                            (actorEndActionGlobal.equals(PROP_ACTOR_END_ACTION_VALUE_RESTART) ||
                             actorEndActionGlobal.equals(PROP_ACTOR_END_ACTION_VALUE_DIE_IF_ERROR_ELSE_RESTART) &&
                                 em.getCause() == null)) {
                        unwatch();
                        attachWebActor(r.session, userActorSpec.build().spawn(), userActorSpec);
                    } else {
                        die(em.getCause());
                    }
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
                abortReqInProgress(cause);
            } catch (final IOException e) {
                throw new RuntimeException(e);
            }

            final ReqInProgress r = reqInProgressAR.get();
            final HttpSession s = r != null ? r.session : null;
            if (r != null) {
                try {
                    s.invalidate();
                } catch (final IllegalStateException ignored) {
                } // Invalidated
            }
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
        @Suspendable
        public final boolean trySend(HttpResponse msg) {
            HttpServletRequest request;
            try {
                request = ((ServletHttpRequest) msg.getRequest()).request;

                final AsyncContext ctx = request.getAsyncContext();
                final HttpServletResponse response = (HttpServletResponse) ctx.getResponse();
                try {
                    if (msg.getCookies() != null) {
                        for (final Cookie wc : msg.getCookies())
                            response.addCookie(getServletCookie(wc));
                    }
                    if (msg.getHeaders() != null) {
                        for (final Map.Entry<String, String> h : msg.getHeaders().entries())
                            response.addHeader(h.getKey(), h.getValue());
                    }

                    if (msg.getStatus() >= 400 && msg.getStatus() < 600) {
                        //noinspection ThrowableResultOfMethodCallIgnored
                        error(request, ctx, response, msg.getStatus(), msg.getError() != null ? msg.getError().toString() : null);
                        return true;
                    }

                    if (msg.getRedirectPath() != null) {
                        redirect(ctx, response, msg.getRedirectPath());
                        return true;
                    }

                    response.setStatus(msg.getStatus());

                    if (msg.getContentType() != null)
                        response.setContentType(msg.getContentType());
                    if (msg.getCharacterEncoding() != null)
                        response.setCharacterEncoding(msg.getCharacterEncoding().name());
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
                try {
                    if (actor != null)
                        actor.done.send(this); // Unlock serving fiber in fake actor so next can come
                } catch (final SuspendExecution | InterruptedException e) {
                    e.printStackTrace(System.err); // TODO handle
                }
            }
        }

        @Override
        public final void close() {
            if (actor != null)
                actor.die(null);
        }

        @Override
        public final void close(Throwable t) {
            t.printStackTrace(System.err); // TODO: handle
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
                if (response != null) {
                    ServletOutputStream os = writeBody(message, response, false);
                    os.flush();
                } else {
                    return false;
                }
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
                if (response != null) {
                    try {
                        ServletOutputStream os = response.getOutputStream();
                        os.close();
                    } catch (final IOException ignored) {
                        //ctx.getRequest().getServletContext().log("error", e);
                    }
                    ctx.complete();
                }
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

    static boolean isTomcat(Class c) {
        return c.getName().startsWith("org.apache.");
    }

    static long getLong(String propName, long def) {
        final String longS = System.getProperty(propName);

        if (longS == null)
            return def;

        long l;
        try {
            l = Long.parseLong(longS);
        } catch (final NumberFormatException ignored) {
            l = def;
        }
        return l;
    }
}
