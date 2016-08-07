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
import co.paralleluniverse.common.util.SystemProperties;
import co.paralleluniverse.comsat.webactors.Cookie;
import co.paralleluniverse.comsat.webactors.*;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.Suspendable;
import co.paralleluniverse.strands.Timeout;
import co.paralleluniverse.strands.channels.SendPort;
import co.paralleluniverse.strands.concurrent.CountDownLatch;

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
    protected static final String SESSION_KEY_ACTOR = WebActorServlet.class.getName() + ".actor";

    protected static final ExecutorService es = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    protected static final ScheduledExecutorService ts = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors());

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

    static HttpActorAdapter getOrAttachSessionActor(final HttpSession session, Callable<HttpActorAdapter> sessionActorProducer) {
        if (session.getAttribute(SESSION_KEY_ACTOR) != null)
            return (HttpActorAdapter) session.getAttribute(SESSION_KEY_ACTOR);

        final HttpActorAdapter actor;
        try {
            actor = sessionActorProducer.call();
        } catch (final Exception e) {
            throw new AssertionError(e); // Will not happen
        }
        session.setAttribute(SESSION_KEY_ACTOR, actor);
        return actor;
    }

    @Override
    public final void sessionCreated(HttpSessionEvent se) {
        // Simply forbid override for implementation extension control purposes
    }

    @Override
    public final void sessionDestroyed(HttpSessionEvent se) {
        se.getSession().removeAttribute(SESSION_KEY_ACTOR);
    }

    static boolean isWebActorAttached(final HttpSession session) {
        return session.getAttribute(SESSION_KEY_ACTOR) != null;
    }

    static <T> ActorRef<T> getWebActor(final HttpSession session) {
        final HttpActorAdapter a = (HttpActorAdapter) session.getAttribute(SESSION_KEY_ACTOR);
        //noinspection unchecked
        return a != null ? (ActorRef<T>) a.userWebActorRef : null;
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
                getOrAttachSessionActor (
                    req.getSession(), new Callable<HttpActorAdapter>() {
                        @Override
                        public HttpActorAdapter call() throws Exception {
                            return new HttpActorAdapter(actorRefBuilder, req.getSession());
                        }
                    }
                ).handleRequest(ctx, req, resp);
            } else if (redirectPath != null) {
                redirect(ctx, resp, redirectPath);
            } else {
                internalError(req, ctx, resp, new RuntimeException("Actor not found"));
            }
        } catch (final ServletException | IOException | InterruptedException | SuspendExecution e) {
            internalError(req, ctx, resp, e);
        }
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

    static final class HttpActorAdapter extends FakeActor<HttpResponse> {
        volatile HttpSession session;
        volatile ActorRef userWebActorRef;

        private final static String replyTimeoutProp = System.getProperty(HttpActorAdapter.class.getName() + ".replyTimeout");
        private static final long REPLY_TIMEOUT = replyTimeoutProp != null ? Long.parseLong(replyTimeoutProp) : 120_000L;

        private final AtomicReference<co.paralleluniverse.strands.concurrent.CountDownLatch> gate = new AtomicReference<>();

        private volatile Callable<ActorRef> userWebActorRefBuilder;

        private volatile AsyncContext asyncCtx;
        private volatile HttpServletRequest req;
        private volatile HttpServletResponse res;

        private volatile boolean needsRestart;

        private volatile Object watchToken;
        private volatile boolean dead;

        private volatile ScheduledFuture<?> cancelTask;

        public HttpActorAdapter(Callable<ActorRef> userWebActorRefBuilder, HttpSession session) {
            super("HttpActorAdapter-" + UUID.randomUUID(), new HttpRequestChannel());

            this.userWebActorRefBuilder = userWebActorRefBuilder;
            this.session = session;

            ((HttpRequestChannel) (SendPort) getMailbox()).actor = this;

            try {
                this.userWebActorRef = userWebActorRefBuilder.call();
            } catch (final Exception e) {
                throw new RuntimeException(e);
            }
            this.watchToken = watch(userWebActorRef);
        }

        final void handleRequest(final AsyncContext asyncCtx, final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException, SuspendExecution, InterruptedException {
            blockSessionRequests();

            if (needsRestart) {
                unwatch(userWebActorRef, watchToken);
                userWebActorRef = null;
                watchToken = null;
                try {
                    userWebActorRef = userWebActorRefBuilder.call(); // New
                    watchToken = watch(userWebActorRef);
                } catch (final Exception e) { // Should not happen
                    e.printStackTrace();
                    throw new RuntimeException(e);
                }
                needsRestart = false;
            }

            this.asyncCtx = asyncCtx;
            this.req = req;
            this.res = resp;

            //noinspection unchecked
            userWebActorRef.send(new HttpRequestWrapper(ref(), req, resp));
        }

        @Suspendable
        private void blockSessionRequests() throws InterruptedException {
            while (!gate.compareAndSet(null, new CountDownLatch(1))) {
                final CountDownLatch l = gate.get();
                if (l != null)
                    l.await();
            }
            final HttpServletRequest req = this.req;
            final HttpServletResponse res = this.res;
            final AsyncContext ctx = this.asyncCtx;
            cancelTask = ts.schedule(new Runnable() {
                @Override
                public void run() {
                    try {
                        internalError(req, ctx, res, new RuntimeException("Timeout while waiting for user actor to reply."));
                    } catch (final IOException e) {
                        // TODO Handle
                        e.printStackTrace();
                    }
                }
            }, REPLY_TIMEOUT, TimeUnit.MILLISECONDS);
        }

        @Suspendable
        private void unblockSessionRequests() {
            if (cancelTask != null)
                cancelTask.cancel(true);
            final CountDownLatch l = gate.getAndSet(null);
            if (l != null)
                l.countDown();
        }

        private boolean isRequestInProgress() {
            return gate.get() != null;
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
        protected final HttpResponse handleLifecycleMessage(LifecycleMessage m) {
            handleLifecycle(m);
            return null;
        }

        private boolean handleLifecycle(LifecycleMessage m) {
            if (m instanceof ExitMessage) {
                final ExitMessage em = (ExitMessage) m;
                if (em.getActor() != null && em.getActor().equals(userWebActorRef)) {
                    //noinspection ThrowableResultOfMethodCallIgnored
                    if (actorEndActionGlobal.equals(PROP_ACTOR_END_ACTION_VALUE_RESTART) ||
                        actorEndActionGlobal.equals(PROP_ACTOR_END_ACTION_VALUE_DIE_IF_ERROR_ELSE_RESTART) && em.getCause() == null) {
                        needsRestart = true;
                    } else {
                        handleDie(em.getCause());
                        return true;
                    }
                }
            }
            return false;
        }

        @Override
        protected final void die(Throwable cause) {
            handleDie(cause);
        }

        private void handleDie(Throwable cause) {
            try {
                possiblyReplyDeadAndUnblock(cause);

                if (dead)
                    return;
                dead = true;

                super.die(cause);

                try {
                    session.invalidate();
                } catch (final Throwable ignored) {}

                // Ensure to release references
                if (userWebActorRef != null && watchToken != null)
                    unwatch(userWebActorRef, watchToken);

            } catch (final IOException e) {
                throw new RuntimeException(e);
            } finally {
                unblockSessionRequests();
            }

            asyncCtx = null;
            req = null;
            session = null;
            res = null;

            userWebActorRefBuilder = null;
            userWebActorRef = null;
            watchToken = null;
        }

        private void possiblyReplyDeadAndUnblock(final Throwable cause) throws IOException {
            if (isRequestInProgress()) {
                try {
                    if (cause != null) {
                        cause.printStackTrace(System.err);
                        internalError(req, asyncCtx, res, new RuntimeException("Actor is dead", cause));
                    } else {
                        internalError(req, asyncCtx, res, new RuntimeException("Actor has terminated"));
                    }
                } finally {
                    unblockSessionRequests();
                }
            }
        }

        final void handleReply(final HttpResponse msg) throws SuspendExecution {
            HttpServletRequest request;
            try {
                request = ((HttpRequestWrapper) msg.getRequest()).request;

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
                    }

                    if (msg.getRedirectPath() != null) {
                        redirect(ctx, response, msg.getRedirectPath());
                    }

                    final boolean sse = msg.shouldStartActor();
                    if (msg.shouldStartActor())
                        msg.getFrom().send(new HttpStreamOpened(new HttpStreamActor(request, response, ctx).ref(), msg));

                    if (isUndertow(request.getClass()))
                        // In Undertow, sending a reply directly from a fiber produces a thread-local related leak due to unfreed buffers
                        es.submit(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    writeResponse(ctx, msg, response, sse);
                                } catch (final IOException e1) {
                                    try {
                                        internalError(req, ctx, response, new RuntimeException(e1));
                                    } catch (final IOException e2) {
                                        // TODO handle
                                        throw new RuntimeException(e2);
                                    }
                                }
                            }
                        });
                    else
                        writeResponse(ctx, msg, response, sse);
                } catch (final IOException ex) {
                    if (request.getServletContext() != null)
                        request.getServletContext().log("IOException", ex);
                    ctx.complete();
                }
            } finally {
                unblockSessionRequests();
            }
        }

        private void writeResponse(AsyncContext ctx, HttpResponse msg, HttpServletResponse response, boolean sse) throws IOException {
            response.setStatus(msg.getStatus());

            if (msg.getContentType() != null)
                response.setContentType(msg.getContentType());
            if (msg.getCharacterEncoding() != null)
                response.setCharacterEncoding(msg.getCharacterEncoding().name());
            final ServletOutputStream out = writeBody(msg, response, !msg.shouldStartActor());
            out.flush(); // commits the response

            if (!sse) {
                out.close();
                ctx.complete();
            }
        }
    }

    private static final class HttpRequestChannel implements SendPort<HttpResponse> {
        private HttpActorAdapter actor;

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
            try {
                actor.handleReply(msg);
            } catch (final SuspendExecution e) {
                throw new AssertionError(e);
            }
            return true;
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

    static boolean isTomcat(Class c) {
        return c.getName().startsWith("org.apache.");
    }

    static boolean isUndertow(Class c) {
        return c.getName().startsWith("io.undertow.servlet.");
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
