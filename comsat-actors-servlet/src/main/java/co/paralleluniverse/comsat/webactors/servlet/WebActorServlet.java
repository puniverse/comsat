/*
 * COMSAT
 * Copyright (c) 2013-2016, Parallel Universe Sotware Co. All rights reserved.
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
import co.paralleluniverse.actors.behaviors.FromMessage;
import co.paralleluniverse.common.util.SystemProperties;
import co.paralleluniverse.comsat.webactors.Cookie;
import co.paralleluniverse.comsat.webactors.*;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.Suspendable;
import co.paralleluniverse.strands.Timeout;
import co.paralleluniverse.strands.channels.SendPort;
import co.paralleluniverse.strands.concurrent.ReentrantLock;

import javax.servlet.*;
import javax.servlet.annotation.WebListener;
import javax.servlet.http.*;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A servlet that forwards requests to a web actor.
 */
@WebListener
public final class WebActorServlet extends HttpServlet implements HttpSessionListener {
    static final String ACTOR_CLASS_PARAM = "actor";
    static final String ACTOR_PARAM_PREFIX = "actorParam";

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

    public static final String PROP_ASYNC_TIMEOUT = WebActorServlet.class.getName() + ".asyncTimeout";
    static final long reqTimeoutMS;

    static {
        reqTimeoutMS = getLong(PROP_ASYNC_TIMEOUT, 120_000_000L);
    }

    static final class SessionData {
        HttpSessionActor httpSessionActor;
        ActorRef httpSessionActorRef;

        public SessionData(Callable<ActorRef> userWebActorBuilder, HttpSession session) {
            this.httpSessionActor = new HttpSessionActor(userWebActorBuilder, session);
            this.httpSessionActorRef = httpSessionActor.spawn();
        }
    }

    static final ConcurrentHashMap<String, SessionData> sessionToWebActor = new ConcurrentHashMap<>();
    private static final ReentrantLock sessionToWebActorLock = new ReentrantLock();

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

    @Override
    public final void sessionCreated(HttpSessionEvent se) {
        // Simply forbid override for implementation extension control purposes
    }

    @Override
    public final void sessionDestroyed(HttpSessionEvent se) {
        cleanup(se.getSession().getId());
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
    static SessionData getOrAttachSessionActor
        (final HttpSession session, final Callable<ActorRef> actorRefBuilder) {
        sessionToWebActorLock.lock();
        try {
            releaseInvalidSessions(); // It looks like the servlet container won't always do it
            final String id = session.getId();
            SessionData sd = sessionToWebActor.get(id);
            if (sd == null)
                sessionToWebActor.put(id, sd = new SessionData(actorRefBuilder, session));
            return sd;
        } catch (final Exception e) { // Should never happen
            e.printStackTrace();
            throw new RuntimeException(e);
        } finally {
            sessionToWebActorLock.unlock();
        }
    }

    @Suspendable
    private static void releaseInvalidSessions() {
        sessionToWebActorLock.lock();
        try {
            // TODO less often
            final List<String> toBeCleared = new ArrayList<>(64);
            for (final Map.Entry<String, SessionData> e : sessionToWebActor.entrySet()) {
                if (!isValid(e.getValue().httpSessionActor.session))
                    toBeCleared.add(e.getKey());
            }
            for (final String id : toBeCleared)
                cleanup(id);
        } finally {
            sessionToWebActorLock.unlock();
        }
    }

    @Suspendable
    private static void cleanup(String id) {
        sessionToWebActorLock.lock();
        try {
            final SessionData sd = sessionToWebActor.remove(id);
            if (sd != null) {
                try {
                    //noinspection unchecked
                    sd.httpSessionActorRef.send(HttpSessionActor.EXIT);
                } catch (final SuspendExecution e) {
                    throw new AssertionError(e);
                }
                sd.httpSessionActor = null;
                sd.httpSessionActorRef = null;
            }
        } finally {
            sessionToWebActorLock.unlock();
        }
    }

    private static boolean isValid(HttpSession session) {
        boolean result = true;
        try {
            session.getAttribute("Doesn't matter");
        } catch (final IllegalStateException ignored) {
            result = false;
        }
        return result;
    }

    static boolean isWebActorAttached(final HttpSession session) {
        return sessionToWebActor.get(session.getId()) != null;
    }

    static <T> ActorRef<T> getWebActor(final HttpSession session) {
        final SessionData sd = sessionToWebActor.get(session.getId());
        //noinspection unchecked
        return sd != null ? (ActorRef<T>) sd.httpSessionActor.userWebActor : null;
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

        if (fixTomcat(req.getClass()))
            req.setAttribute("org.apache.catalina.ASYNC_SUPPORTED", true);
        final AsyncContext ctx = req.startAsync();

        try {
            //noinspection unchecked
            specAR.compareAndSet(null, new ActorSpec(Class.forName(actorClassName), actorParams));
        } catch (final ClassNotFoundException ex) {
            internalError(req, ctx, resp, new RuntimeException("Unable to load actorClass: " + ex.getMessage()));
            return;
        }

        try {
            if (actorClassName != null) {
                //noinspection unchecked
                final Callable<ActorRef> actorRefBuilder = new Callable<ActorRef>() {
                    @Override
                    public ActorRef call() throws Exception {
                        //noinspection unchecked
                        return Actor.newActor(specAR.get()).spawn();
                    }
                };
                //noinspection unchecked
                new HttpRequestActor(actorRefBuilder).service(ctx, req, resp);
            } else if (redirectPath != null) {
                redirect(ctx, resp, redirectPath);
            } else {
                internalError(req, ctx, resp, new RuntimeException("Actor not found"));
            }
        } catch (final ServletException | IOException e) {
            // TODO review handling
            e.printStackTrace(System.err);
            throw new RuntimeException(e);
        }
    }

    static void redirect(AsyncContext ac, HttpServletResponse res, String path) throws IOException {
        res.sendRedirect(path);
        if (fixTomcat(ac.getClass())) // Seems required only by Tomcat
            ac.complete();
    }

    static void internalError(HttpServletRequest req, AsyncContext ac, HttpServletResponse resp, Throwable t) throws IOException {
        if (disableSyncErrorsGlobal) {
            resp.sendError(500, t.getMessage());
            if (fixTomcat(ac.getClass())) // Seems required only by Tomcat
                ac.complete();
        } else {
            req.setAttribute(DISPATCH_INTERNAL_ERROR_EXCEPTION_REQUEST_KEY, t);
            ac.dispatch();
        }
    }

    static void error(HttpServletRequest req, AsyncContext ac, HttpServletResponse resp, int status, String message) throws IOException {
        if (disableSyncErrorsGlobal || fixTomcat(ac.getClass())) { // Tomcat doesn't seem to support dispatching error responses
            resp.sendError(status, message);
            if (fixTomcat(ac.getClass())) // Seems required only by Tomcat
                ac.complete();
        } else {
            req.setAttribute(DISPATCH_ERROR_STATUS_REQUEST_KEY, status);
            req.setAttribute(DISPATCH_ERROR_MESSAGE_REQUEST_KEY, message);
            ac.dispatch();
        }
    }

    static final class HttpRequestActor extends FakeActor<HttpResponse> {
        private final HttpRequestChannel channel;
        private final Callable<ActorRef> actorRefBuilder;

        private AsyncContext asyncCtx;
        private HttpServletRequest req;
        private HttpServletResponse resp;

        private ActorRef sessionActorRef;
        private Object watchToken;

        private volatile boolean dead;

        public HttpRequestActor(Callable<ActorRef> actorRefBuilder) {
            super("HttpRequestActor-" + UUID.randomUUID(), new HttpRequestChannel());

            this.actorRefBuilder = actorRefBuilder;

            channel = ((HttpRequestChannel) (SendPort) getMailbox());
            channel.actor = this;
        }

        final void service(final AsyncContext ctx, final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
            startReq(ctx, req, resp);
            if (watchToken == null) {
                internalError(req, ctx, resp, new RuntimeException("Session invalidated during request execution"));
                return;
            }

            try {
                //noinspection unchecked
                sessionActorRef.send(new HttpSessionActor.Req(ref(), new ServletHttpRequest(sessionActorRef, req, resp)));
            } catch (final SuspendExecution se) {
                unwatch();
                internalError(req, ctx, resp, new AssertionError(se));
            }
        }

        private void startReq(AsyncContext ctx, HttpServletRequest req, HttpServletResponse resp) {
            final HttpSession s;
            if (req == null || resp == null || (s = req.getSession()) == null)
                return;

            this.req = req;
            this.resp = resp;
            asyncCtx = ctx;

            final SessionData sd = getOrAttachSessionActor(s, actorRefBuilder);
            sessionActorRef = sd != null ? sd.httpSessionActorRef : null;
            if (sessionActorRef != null)
                watchToken = watch(sessionActorRef);
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
        protected HttpResponse handleLifecycleMessage(LifecycleMessage m) {
            if (m instanceof ExitMessage) {
                final ExitMessage em = (ExitMessage) m;
                if (em.getActor() != null && em.getActor().equals(sessionActorRef)) {
                    die(em.getCause());
                }
            }
            return null;
        }

        void unwatch() {
            if (sessionActorRef != null && watchToken != null)
                unwatch(sessionActorRef, watchToken);
        }

        private void abortReqInProgress(final Throwable cause) throws IOException {
            if (watchToken != null) {
                if (cause != null) {
                    cause.printStackTrace(System.err);
                    internalError(req, asyncCtx, resp, new RuntimeException("Http request actor is dead", cause));
                } else {
                    internalError(req, asyncCtx, resp, new RuntimeException("Http request actor has terminated."));
                }
            }
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
            unwatch();
        }
    }

    private static final class HttpRequestChannel implements SendPort<HttpResponse> {
        private HttpRequestActor actor;

        HttpRequestChannel() {}

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
                            msg.getFrom().send(new HttpStreamOpened(new HttpStreamActor(request, response, ctx).ref(), msg));
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
            t.printStackTrace(System.err); // TODO: handle
            if (actor != null)
                actor.die(t);
        }
    }

    static final class HttpSessionActor extends BasicActor {
        static final class Req implements FromMessage {
            final FromMessage req;
            final ActorRef<HttpResponse> from;

            public Req(ActorRef<HttpResponse> from, FromMessage req) {
                this.from = from;
                this.req = req;
            }

            @Override
            public ActorRef<?> getFrom() {
                return from;
            }
        }

        static final Object EXIT = new Object();

        ActorRef userWebActor;

        private Callable<ActorRef> userWebActorBuilder;
        private HttpSession session;
        private Object watchToken;

        public HttpSessionActor(Callable<ActorRef> userWebActorBuilder, HttpSession session) {
            this.userWebActorBuilder = userWebActorBuilder;
            this.session = session;
        }

        @Override
        protected Void doRun() throws InterruptedException, SuspendExecution {
            try {
                this.userWebActor = userWebActorBuilder.call();
            } catch (final Exception e) {
                throw new RuntimeException(e);
            }
            this.watchToken = watch(userWebActor);
            //noinspection InfiniteLoopStatement
            for(;;) {
                final Object m = receive();
                if (EXIT.equals(m)) {
                    unwatch(userWebActor, watchToken);
                    userWebActorBuilder = null;
                    session = null;
                    userWebActor = null;
                    watchToken = null;
                    return null;
                } else if (m instanceof Req) {
                    final Req r = (Req) m;
                    final ActorRef caller = r.getFrom();
                    //noinspection unchecked
                    userWebActor.send(r.req);
                    try {
                        //noinspection unchecked
                        final Object reply = receive(reqTimeoutMS, TimeUnit.MILLISECONDS, new MessageProcessor<Object, Object>() {
                            @Override
                            public Object process(Object m) throws SuspendExecution, InterruptedException {
                                if (m instanceof FromMessage) {
                                    final FromMessage fm = (FromMessage) m;
                                    final ActorRef a = fm.getFrom();
                                    if (a == null || a.equals(userWebActor))
                                        return m;
                                }
                                return null; // Skip
                            }
                        });
                        //noinspection unchecked
                        caller.send(reply);
                    } catch (final TimeoutException ignored) { // Let the caller handle the timeout
                    }
                }
            }
        }

        @Override
        protected final Object handleLifecycleMessage(LifecycleMessage m) {
            if (m instanceof ExitMessage) {
                final ExitMessage em = (ExitMessage) m;
                if (em.getActor() != null && em.getActor().equals(userWebActor)) {
                    //noinspection ThrowableResultOfMethodCallIgnored
                    if (actorEndActionGlobal.equals(PROP_ACTOR_END_ACTION_VALUE_RESTART) ||
                        actorEndActionGlobal.equals(PROP_ACTOR_END_ACTION_VALUE_DIE_IF_ERROR_ELSE_RESTART) && em.getCause() == null) {
                        unwatch(userWebActor, watchToken);
                        userWebActor = null;
                        watchToken = null;
                        try {
                            userWebActor = userWebActorBuilder.call(); // New
                            watchToken = watch(userWebActor);
                        } catch (final Exception e) { // Should not happen
                            e.printStackTrace();
                            throw new RuntimeException(e);
                        }
                    } else {
                        throw new LifecycleException(m);
                    }
                }
            }
            return null;
        }
    }

    private static final class HttpStreamActor extends FakeActor<WebDataMessage> {
        private volatile boolean dead;

        public HttpStreamActor(final HttpServletRequest request, final HttpServletResponse response, final AsyncContext ac) {
            super(request.toString(), new HttpStreamChannel(ac, response));
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
                    } catch (final IOException ignored) { // TODO handle
                        //ctx.getRequest().getServletContext().log("error", e);
                    }
                    ctx.complete();
                }
            } catch (final Exception ignored) { // TODO handle
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

    static boolean fixTomcat(Class c) {
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
