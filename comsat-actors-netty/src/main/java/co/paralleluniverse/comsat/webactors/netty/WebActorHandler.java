/*
 * COMSAT
 * Copyright (c) 2015, Parallel Universe Software Co. All rights reserved.
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
package co.paralleluniverse.comsat.webactors.netty;

import co.paralleluniverse.actors.*;
import co.paralleluniverse.common.util.Pair;
import co.paralleluniverse.common.util.SystemProperties;
import co.paralleluniverse.comsat.webactors.*;
import co.paralleluniverse.comsat.webactors.Cookie;
import co.paralleluniverse.comsat.webactors.HttpRequest;
import co.paralleluniverse.comsat.webactors.HttpResponse;
import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.FiberUtil;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.SuspendableRunnable;
import co.paralleluniverse.strands.Timeout;
import co.paralleluniverse.strands.channels.SendPort;
import co.paralleluniverse.strands.concurrent.ReentrantLock;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;

import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.http.HttpHeaders.Names.*;

import io.netty.handler.codec.http.cookie.ServerCookieEncoder;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.util.CharsetUtil;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 *
 * @author circlespainter
 */
public class WebActorHandler extends SimpleChannelInboundHandler<Object> {
    final static String SESSION_COOKIE_KEY = "JSESSIONID";
    final static Map<String, Session> sessions = new ConcurrentHashMap<>();

    private static AtomicReference<Fiber> cleanupFiber = new AtomicReference<>();

    private static final WeakHashMap<Class<?>, List<Pair<String, String>>> classToUrlPatterns = new WeakHashMap<>();
    private static final InternalLogger log = InternalLoggerFactory.getInstance(AutoWebActorHandler.class);

    public interface Session {
        boolean isValid();
        void invalidate();
        ActorImpl<? extends WebMessage> getActor();
        ReentrantLock getLock();
        Map<String, Object> getAttachments();
    }

    public static abstract class DefaultSessionImpl implements Session {
        private final static String durationProp = System.getProperty(DefaultSessionImpl.class.getName() + ".durationMs");
        private final static long DURATION = durationProp != null ? Long.parseLong(durationProp) : 60_000l;

        private final ReentrantLock lock = new ReentrantLock();

        private final long created;

        final Map<String, Object> attachments = new HashMap<>();

        private boolean valid = true;

        public DefaultSessionImpl() {
            this.created = new Date().getTime();
        }

        @Override
        public final void invalidate() {
            attachments.clear();
            valid = false;
        }

        @Override
        public final boolean isValid() {
            final boolean ret = valid && (new Date().getTime() - created) <= DURATION;
            if (!ret)
                invalidate();
            return ret;
        }

        @Override
        public final Map<String, Object> getAttachments() {
            return attachments;
        }

        @Override
        public final ReentrantLock getLock() {
            return lock;
        }
    }

    // @FunctionalInterface
    public interface SessionSelector {
        Session select(ChannelHandlerContext ctx, FullHttpRequest req);
    }

    private static final String ACTOR_KEY = "co.paralleluniverse.actor";

    private final SessionSelector selector;
    private final String httpResponseEncoderName;

    private WebSocketServerHandshaker handshaker;
    private WebSocketActorAdapter webSocketActor;

    public WebActorHandler(SessionSelector selector, String httpResponseEncoderName) {
        this.selector = selector;
        this.httpResponseEncoderName = httpResponseEncoderName;
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        ctx.close();
        log.error("Exception caught", cause);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof FullHttpRequest) {
            handleHttpRequest(ctx, (FullHttpRequest) msg);
        } else if (msg instanceof WebSocketFrame) {
            handleWebSocketFrame(ctx, (WebSocketFrame) msg);
        }
    }

    private void handleWebSocketFrame(ChannelHandlerContext ctx, WebSocketFrame frame) {
        // Check for closing frame
        if (frame instanceof CloseWebSocketFrame) {
            handshaker.close(ctx.channel(), (CloseWebSocketFrame) frame.retain());
            return;
        }

        if (frame instanceof PingWebSocketFrame) {
            ctx.channel().write(new PongWebSocketFrame(frame.content().retain()));
            return;
        }

        if (frame instanceof ContinuationWebSocketFrame)
            return;

        if (frame instanceof TextWebSocketFrame)
            webSocketActor.onMessage(((TextWebSocketFrame) frame).text());
        else
            webSocketActor.onMessage(frame.content().nioBuffer());
    }

    private void handleHttpRequest(ChannelHandlerContext ctx, FullHttpRequest req) throws SuspendExecution {
        // Handle 100 CONTINUE expectation.
        if (HttpHeaders.is100ContinueExpected(req)) {
            ctx.write(new DefaultFullHttpResponse(req.getProtocolVersion(), CONTINUE));
        }

        // Handle a bad request.
        if (!req.getDecoderResult().isSuccess()) {
            sendHttpResponse(ctx, req, new DefaultFullHttpResponse(req.getProtocolVersion(), BAD_REQUEST));
            return;
        }

        final Session session = selector.select(ctx, req);
        assert session != null;

        final ReentrantLock lock = session.getLock();
        assert lock != null;

        lock.lock();

        try {
            ActorImpl<? extends WebMessage> userActor;
            ActorRef<? extends WebMessage> userActorRef = null;
            Class userActorClass = null;
            ActorImpl internalActor = null;
            if (session.isValid() && session.getActor() != null) {
                internalActor = (ActorImpl) session.getAttachments().get(ACTOR_KEY);
                userActor = session.getActor();
                userActorRef = userActor.ref();
                userActorClass = userActor.getClass();
            }

            final String uri = req.getUri();
            if (userActorRef != null) {
                if (handlesWithWebSocket(uri, session.getActor().getClass())) {
                    if (internalActor == null || !(internalActor instanceof WebSocketActorAdapter)) {
                        //noinspection unchecked
                        this.webSocketActor = new WebSocketActorAdapter(ctx, (ActorRef<? super WebMessage>) userActorRef);
                        addActorToSessionAndUnlock(session, webSocketActor, lock);
                    }
                    // Handshake
                    final WebSocketServerHandshakerFactory wsFactory = new WebSocketServerHandshakerFactory(uri, null, true);
                    handshaker = wsFactory.newHandshaker(req);
                    if (handshaker == null) {
                        WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
                    } else {
                        @SuppressWarnings("unchecked") final ActorRef<WebMessage> userActorRef0 = (ActorRef<WebMessage>) webSocketActor.webActor;
                        handshaker.handshake(ctx.channel(), req).addListener(new GenericFutureListener<ChannelFuture>() {
                            @Override
                            public void operationComplete(ChannelFuture future) throws Exception {
                                FiberUtil.runInFiber(new SuspendableRunnable() {
                                    @Override
                                    public void run() throws SuspendExecution, InterruptedException {
                                        userActorRef0.send(new WebSocketOpened(WebActorHandler.this.webSocketActor.ref()));
                                    }
                                });
                            }
                        });
                    }
                    return;
                } else if (handlesWithHttp(uri, userActorClass)) {
                    if (internalActor == null) {
                        //noinspection unchecked
                        internalActor = new HttpActorAdapter(session, (ActorRef<HttpRequest>) userActorRef, httpResponseEncoderName);
                        addActorToSessionAndUnlock(session, internalActor, lock);
                    }
                    ((HttpActorAdapter) internalActor).service(ctx, req);
                    return;
                }
            }
        } finally {
            if (lock.isHeldByCurrentStrand() && lock.isLocked())
                lock.unlock();
        }

        sendHttpResponse(ctx, req, new DefaultFullHttpResponse(req.getProtocolVersion(), NOT_FOUND));
    }

    private void addActorToSessionAndUnlock(Session session, ActorImpl actor, ReentrantLock lock) {
        session.getAttachments().put(ACTOR_KEY, actor);
        lock.unlock();
    }

    private static class WebSocketActorAdapter extends FakeActor<WebDataMessage> {
        final ActorRef<? super WebMessage> webActor;
        private final ChannelHandlerContext ctx;

        public WebSocketActorAdapter(ChannelHandlerContext ctx, ActorRef<? super WebMessage> webActor) {
            super(webActor.getName(), new WebSocketChannelAdapter(ctx));
            this.ctx = ctx;
            this.webActor = webActor;
            watch(webActor);
        }

        void onMessage(final ByteBuffer message) {
            try {
                webActor.send(new WebDataMessage(ref(), message));
            } catch (SuspendExecution ex) {
                throw new AssertionError(ex);
            }
        }

        void onMessage(final String message) {
            try {
                webActor.send(new WebDataMessage(ref(), message));
            } catch (SuspendExecution ex) {
                throw new AssertionError(ex);
            }
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
            ctx.channel().close();
        }

        @Override
        public String toString() {
            return "WebSocketActor{" + "webActor=" + webActor + '}';
        }
    }

    private static class WebSocketChannelAdapter implements SendPort<WebDataMessage> {
        private final ChannelHandlerContext ctx;

        public WebSocketChannelAdapter(ChannelHandlerContext ctx) {
            this.ctx = ctx;
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
            if (!message.isBinary())
                ctx.writeAndFlush(new TextWebSocketFrame(message.getStringBody()));
            else
                ctx.writeAndFlush(new BinaryWebSocketFrame(Unpooled.wrappedBuffer(message.getByteBufferBody())));
            return true;
        }

        @Override
        public void close() {
            ctx.channel().close();
        }

        @Override
        public void close(Throwable t) {
            close();
        }
    }

    private static class HttpActorAdapter extends FakeActor<HttpResponse> {
        final ActorRef<? super HttpRequest> webActor;
        private final Session session;
        private volatile boolean dead;

        public HttpActorAdapter(Session session, ActorRef<? super HttpRequest> webActor, String httpResponseEncoderName) {
            super(webActor.getName(), new HttpChannelAdapter(session, httpResponseEncoderName));

            this.session = session;
            this.webActor = webActor;
            watch(webActor);
        }

        void service(ChannelHandlerContext ctx, FullHttpRequest req) throws SuspendExecution {
            if (isDone()) {
                @SuppressWarnings("ThrowableResultOfMethodCallIgnored") Throwable deathCause = getDeathCause();
                if (deathCause != null)
                    sendHttpResponse(ctx, req, new DefaultFullHttpResponse(req.getProtocolVersion(), INTERNAL_SERVER_ERROR, Unpooled.wrappedBuffer(("Actor is dead because of " + deathCause.getMessage()).getBytes())));
                else
                    sendHttpResponse(ctx, req, new DefaultFullHttpResponse(req.getProtocolVersion(), INTERNAL_SERVER_ERROR, Unpooled.wrappedBuffer(("Actor has finished.").getBytes())));
                return;
            }

            webActor.send(new HttpRequestWrapper(ref(), ctx, req));
        }

        @Override
        protected HttpResponse handleLifecycleMessage(LifecycleMessage m) {
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
        protected void interrupt() {
            die(new InterruptedException());
        }

        @Override
        protected void die(Throwable cause) {
            if (dead)
                return;
            this.dead = true;
            super.die(cause);
            session.invalidate();
        }

        @Override
        public String toString() {
            return "ServletHttpActor{" + "webActor=" + webActor + '}';
        }
    }

    private static class HttpChannelAdapter implements SendPort<HttpResponse> {
        private final static boolean startSessionOnlyForSSE = SystemProperties.isEmptyOrTrue(HttpChannelAdapter.class.getName() + ".startSessionOnlyForSSE");

        private final String httpResponseEncoderName;
        private final Session session;

        public HttpChannelAdapter(Session session, String httpResponseEncoderName) {
            this.session = session;
            this.httpResponseEncoderName = httpResponseEncoderName;
        }

        @Override
        public void send(HttpResponse message) throws SuspendExecution, InterruptedException {
            trySend(message);
        }

        @Override
        public boolean send(HttpResponse message, long timeout, TimeUnit unit) throws SuspendExecution, InterruptedException {
            send(message);
            return true;
        }

        @Override
        public boolean send(HttpResponse message, Timeout timeout) throws SuspendExecution, InterruptedException {
            return send(message, timeout.nanosLeft(), TimeUnit.NANOSECONDS);
        }

        @Override
        public boolean trySend(HttpResponse message) {
            final HttpRequestWrapper nettyRequest = (HttpRequestWrapper) message.getRequest();
            final FullHttpRequest req = nettyRequest.req;
            final ChannelHandlerContext ctx = nettyRequest.ctx;

            final HttpResponseStatus status = HttpResponseStatus.valueOf(message.getStatus());

            if (message.getStatus() >= 400 && message.getStatus() < 600) {
                sendHttpResponse(ctx, req, new DefaultFullHttpResponse(req.getProtocolVersion(), status));
                close();
                return true;
            }

            if (message.getRedirectPath() != null) {
                sendHttpRedirect(ctx, req, message.getRedirectPath());
                close();
                return true;
            }

            FullHttpResponse res;
            if (message.getStringBody() != null)
                res = new DefaultFullHttpResponse(req.getProtocolVersion(), status, Unpooled.wrappedBuffer(message.getStringBody().getBytes()));
            else if (message.getByteBufferBody() != null)
                res = new DefaultFullHttpResponse(req.getProtocolVersion(), status, Unpooled.wrappedBuffer(message.getByteBufferBody()));
            else
                res = new DefaultFullHttpResponse(req.getProtocolVersion(), status);

            if (message.getCookies() != null) {
                final ServerCookieEncoder enc = ServerCookieEncoder.STRICT;
                for (Cookie c : message.getCookies())
                    HttpHeaders.setHeader(res, COOKIE, enc.encode(getNettyCookie(c)));
            }
            if (message.getHeaders() != null) {
                for (Map.Entry<String, String> h : message.getHeaders().entries())
                    HttpHeaders.setHeader(res, h.getKey(), h.getValue());
            }

            if (message.getContentType() != null)
                HttpHeaders.setHeader(res, CONTENT_TYPE, message.getContentType());
            if (message.getCharacterEncoding() != null)
                HttpHeaders.setHeader(res, CONTENT_ENCODING, message.getCharacterEncoding());

            // This will copy the request content, which must still be referenceable, doing before the request handler
            // unallocates it (unfortunately it is explicitly reference-counted in Netty)
            final HttpStreamActorAdapter httpStreamActorAdapter = new HttpStreamActorAdapter(ctx, req);

            final boolean sseStarted = message.shouldStartActor();
            if (sseStarted || !startSessionOnlyForSSE) {
                final String sessionId = UUID.randomUUID().toString();
                res.headers().add(SET_COOKIE, ServerCookieEncoder.STRICT.encode(SESSION_COOKIE_KEY, sessionId));
                startSession(sessionId, session);
            }
            sendHttpResponse(ctx, req, res, !sseStarted);

            if (sseStarted) {
                ctx.pipeline().remove(httpResponseEncoderName);
                try {
                    message.getFrom().send(new HttpStreamOpened(httpStreamActorAdapter.ref(), message));
                } catch (SuspendExecution e) {
                    throw new AssertionError(e);
                }
            }

            return true;
        }

        private static void startSession(String sessionId, Session session) {
            sessions.put(sessionId, session);

            if (cleanupFiber.get() == null) {
                cleanupFiber.set(new Fiber<Void>() {
                    @Override
                    public Void run() throws SuspendExecution, InterruptedException {
                        for (final String sessionId : sessions.keySet()) {
                            final Session s = sessions.get(sessionId);
                            if (!s.isValid())
                                sessions.remove(sessionId);
                        }
                        cleanupFiber.set(null);
                        return null;
                    }
                }.start());
            }
        }

        private io.netty.handler.codec.http.cookie.Cookie getNettyCookie(Cookie c) {
            io.netty.handler.codec.http.cookie.Cookie ret = new io.netty.handler.codec.http.cookie.DefaultCookie(c.getName(), c.getValue());
            ret.setDomain(c.getDomain());
            ret.setHttpOnly(c.isHttpOnly());
            ret.setMaxAge(c.getMaxAge());
            ret.setPath(c.getPath());
            ret.setSecure(c.isSecure());
            return ret;
        }

        @Override
        public void close() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void close(Throwable t) {
            throw new UnsupportedOperationException();
        }
    }

    static boolean handlesWithHttp(String uri, Class<?> actorClass) {
        return match(uri, actorClass).equals("http");
    }

    static boolean handlesWithWebSocket(String uri, Class<?> actorClass) {
        return match(uri, actorClass).equals("ws");
    }

    private static class HttpStreamActorAdapter extends FakeActor<WebDataMessage> {
        private volatile boolean dead;

        public HttpStreamActorAdapter(final ChannelHandlerContext ctx, final FullHttpRequest req) {
            super(req.toString(), new HttpStreamChannelAdapter(ctx, req));
            ((HttpStreamChannelAdapter) (Object) mailbox()).actor = this;
        }

        @Override
        protected WebDataMessage handleLifecycleMessage(LifecycleMessage m) {
            if (m instanceof ShutdownMessage) {
                die(null);
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
            if (dead)
                return;
            this.dead = true;
            mailbox().close();
            super.die(cause);
        }

        @Override
        public String toString() {
            return "NettyHttpStreamActor{request + " + getName() + "}";
        }
    }

    private static class HttpStreamChannelAdapter implements SendPort<WebDataMessage> {
        private final Charset encoding;
        HttpStreamActorAdapter actor;
        final ChannelHandlerContext ctx;

        public HttpStreamChannelAdapter(ChannelHandlerContext ctx, FullHttpRequest req) {
            this.ctx = ctx;
            this.encoding = HttpRequestWrapper.extractCharacterEncodingOrDefault(req.headers());
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
        public boolean trySend(WebDataMessage res) {
            final ByteBuf buf;
            final String stringBody = res.getStringBody();
            if (stringBody != null) {
                byte[] bs = stringBody.getBytes(encoding);
                buf = Unpooled.wrappedBuffer(bs);
            } else {
                buf = Unpooled.wrappedBuffer(res.getByteBufferBody());
            }
            ChannelFuture channelFuture = ctx.writeAndFlush(buf);
            channelFuture.syncUninterruptibly();
            System.out.println(channelFuture.isSuccess());
            return true;
        }

        @Override
        public void close() {
            ctx.channel().close();
        }

        @Override
        public void close(Throwable t) {
            close();
        }
    }

    private static void sendHttpResponse(ChannelHandlerContext ctx, FullHttpRequest req, FullHttpResponse res) {
        sendHttpResponse(ctx, req, res, null);
    }

    private static void sendHttpResponse(ChannelHandlerContext ctx, FullHttpRequest req, FullHttpResponse res, Boolean close) {
        // Generate an error page if response getStatus code is not OK (200).
        if (res.getStatus().code() != 200) {
            final ByteBuf buf = Unpooled.copiedBuffer(res.getStatus().toString(), CharsetUtil.UTF_8);
            res.content().writeBytes(buf);
            buf.release();
            HttpHeaders.setContentLength(res, res.content().readableBytes());
        }
        writeHttpResponse(ctx, req, res, close);
    }

    private static void sendHttpRedirect(ChannelHandlerContext ctx, FullHttpRequest req, String newUri) {
        final FullHttpResponse res = new DefaultFullHttpResponse(req.getProtocolVersion(), FOUND);
        HttpHeaders.setHeader(res, LOCATION, newUri);
        writeHttpResponse(ctx, req, res, true);
    }

    private static void writeHttpResponse(ChannelHandlerContext ctx, FullHttpRequest req, FullHttpResponse res, Boolean close) {
        // TODO Understand what's the issue with the keepalive
        // Send the response and close the connection if necessary.
//        if (!HttpHeaders.isKeepAlive(req) || res.getStatus().code() != 200) {
        if (close == null || close)
            ctx.writeAndFlush(res).addListener(ChannelFutureListener.CLOSE);
        else
            ctx.writeAndFlush(res);
//        } else {
//            res.headers().set(CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
//            ctx.writeAndFlush(res);
//        }
    }

    private static String match(String uri, Class<?> actorClass) {
        if (uri != null && actorClass != null) {
            for (final Pair<String, String> e : lookupOrInsert(actorClass)) {
                if (servletMatch(e.getFirst(), uri))
                    return e.getSecond();
            }
        }
        return "";
    }

    private static List<Pair<String, String>> lookupOrInsert(Class<?> actorClass) {
        if (actorClass != null) {
            final List<Pair<String, String>> lookup = classToUrlPatterns.get(actorClass);
            if (lookup != null)
                return lookup;
            return insert(actorClass);
        }
        return null;
    }

    private static List<Pair<String, String>> insert(Class<?> actorClass) {
        if (actorClass != null) {
            final WebActor wa = actorClass.getAnnotation(WebActor.class);
            final List<Pair<String, String>> ret = new ArrayList<>(4);
            for (String httpP : wa.httpUrlPatterns())
                addPattern(ret, httpP, "http");
            for (String wsP : wa.webSocketUrlPatterns())
                addPattern(ret, wsP, "ws");
            classToUrlPatterns.put(actorClass, ret);
            return ret;
        }
        return null;
    }

    private static void addPattern(List<Pair<String, String>> ret, String p, String type) {
        if (p != null) {
            @SuppressWarnings("MismatchedQueryAndUpdateOfCollection") final Pair<String, String> entry = new Pair<>(p, type);
            if (p.endsWith("*") || p.startsWith("*.") || p.equals("/")) // Wildcard -> end
                ret.add(entry);
            else // Exact -> beginning
                ret.add(0, entry);
        }
    }

    private static boolean servletMatch(String pattern, String uri) {
        // As per servlet spec
        if (pattern != null && uri != null) {
            if (pattern.startsWith("/") && pattern.endsWith("*"))
                return uri.startsWith(pattern.substring(0, pattern.length() - 1));
            if (pattern.startsWith("*."))
                return uri.endsWith(pattern.substring(2));
            if (pattern.isEmpty())
                return uri.equals("/");
            return pattern.equals("/") || pattern.equals(uri);
        }
        return false;
    }
}
