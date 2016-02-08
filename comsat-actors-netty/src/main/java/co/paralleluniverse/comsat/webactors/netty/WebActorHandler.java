/*
/*
 * COMSAT
 * Copyright (c) 2015-2016, Parallel Universe Software Co. All rights reserved.
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
import co.paralleluniverse.fibers.FiberUtil;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.SuspendableRunnable;
import co.paralleluniverse.strands.Timeout;
import co.paralleluniverse.strands.channels.SendPort;
import co.paralleluniverse.strands.concurrent.ReentrantLock;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;

import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.http.HttpHeaders.Names.*;

import io.netty.handler.codec.http.cookie.ServerCookieEncoder;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * @author circlespainter
 */
public class WebActorHandler extends SimpleChannelInboundHandler<Object> {
    // @FunctionalInterface
    public interface WebActorContextProvider {
        Context get(ChannelHandlerContext ctx, FullHttpRequest req);
    }

    public interface Context {
        boolean isValid();
        void invalidate();

        ActorRef<? extends WebMessage> getRef();

        ReentrantLock getLock();

        Map<String, Object> getAttachments();

        boolean handlesWithHttp(String uri);
        boolean handlesWithWebSocket(String uri);

        boolean watch();
    }

    public static abstract class DefaultContextImpl implements Context {
        private final static String durationProp = System.getProperty(DefaultContextImpl.class.getName() + ".durationMillis");
        private final static long DURATION = durationProp != null ? Long.parseLong(durationProp) : 60_000L;
        private final ReentrantLock lock = new ReentrantLock();
        private final long created;
        private final Map<String, Object> attachments = new HashMap<>();

        private boolean valid = true;

        public DefaultContextImpl() {
            created = new Date().getTime();
        }

        @Override
        public void invalidate() {
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

        @Override
        public boolean watch() {
            return true;
        }
    }

    public WebActorHandler(WebActorContextProvider selector) {
        this(selector, null);
    }

    public WebActorHandler(WebActorContextProvider selector, String httpResponseEncoderName) {
        this.selector = selector;
        this.httpResponseEncoderName = httpResponseEncoderName;
    }

    @Override
    public final void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    public final void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if (ctx.channel().isOpen())
            ctx.close();
        log.error("Exception caught", cause);
    }

    @Override
    protected final void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof FullHttpRequest) {
            handleHttpRequest(ctx, (FullHttpRequest) msg);
        } else if (msg instanceof WebSocketFrame) {
            handleWebSocketFrame(ctx, (WebSocketFrame) msg);
        } else {
            throw new AssertionError("Unexpected message " + msg);
        }
    }

    protected static boolean sessionsEnabled() {
        return "always".equals(trackSession) || "sse".equals(trackSession);
    }

    protected final static String SESSION_COOKIE_KEY = "JSESSIONID";
    protected final static Map<String, Context> sessions = Collections.synchronizedMap(new WeakHashMap<String, Context>());
    protected final static String TRACK_SESSION_PROP = HttpChannelAdapter.class.getName() + ".trackSession";
    protected final static String trackSession = System.getProperty(TRACK_SESSION_PROP, "sse");

    protected final static String OMIT_DATE_HEADER_PROP = HttpChannelAdapter.class.getName() + ".omitDateHeader";
    protected final static Boolean omitDateHeader = SystemProperties.isEmptyOrTrue(OMIT_DATE_HEADER_PROP);

    private static final String ACTOR_KEY = "co.paralleluniverse.comsat.webactors.sessionActor";

    private static final WeakHashMap<Class<?>, List<Pair<String, String>>> classToUrlPatterns = new WeakHashMap<>();
    private static final InternalLogger log = InternalLoggerFactory.getInstance(AutoWebActorHandler.class);

    private final WebActorContextProvider selector;
    private final String httpResponseEncoderName;

    private WebSocketServerHandshaker handshaker;
    private WebSocketActorAdapter webSocketActor;

    private void handleWebSocketFrame(ChannelHandlerContext ctx, WebSocketFrame frame) {
        // Check for closing frame
        if (frame instanceof CloseWebSocketFrame) {
            handshaker.close(ctx.channel(), (CloseWebSocketFrame) frame.retain());
            return;
        }

        if (frame instanceof PingWebSocketFrame) {
            ctx.channel().writeAndFlush(new PongWebSocketFrame(frame.content().retain()));
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
        // Handle a bad request.
        if (!req.getDecoderResult().isSuccess()) {
            sendHttpResponse(ctx, req, new DefaultFullHttpResponse(req.getProtocolVersion(), BAD_REQUEST), false);
            return;
        }

        final String uri = req.getUri();

        final Context actorCtx = selector.get(ctx, req);
        assert actorCtx != null;

        final ReentrantLock lock = actorCtx.getLock();
        assert lock != null;

        lock.lock();

        try {
            final ActorRef<? extends WebMessage> userActorRef = actorCtx.getRef();
            ActorImpl internalActor = (ActorImpl) actorCtx.getAttachments().get(ACTOR_KEY);

            if (userActorRef != null) {
                if (actorCtx.handlesWithWebSocket(uri)) {
                    if (internalActor == null || !(internalActor instanceof WebSocketActorAdapter)) {
                        //noinspection unchecked
                        webSocketActor = new WebSocketActorAdapter(ctx, (ActorRef<? super WebMessage>) userActorRef);
                        addActorToContextAndUnlock(actorCtx, webSocketActor, lock);
                    }
                    // Handshake
                    final WebSocketServerHandshakerFactory wsFactory = new WebSocketServerHandshakerFactory(uri, null, true);
                    handshaker = wsFactory.newHandshaker(req);
                    if (handshaker == null) {
                        WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
                    } else {
                        @SuppressWarnings("unchecked") final ActorRef<WebMessage> userActorRef0 = (ActorRef<WebMessage>) webSocketActor.userActor;
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
                } else if (actorCtx.handlesWithHttp(uri)) {
                    if (internalActor == null || !(internalActor instanceof HttpActorAdapter)) {
                        //noinspection unchecked
                        internalActor = new HttpActorAdapter((ActorRef<HttpRequest>) userActorRef, actorCtx, httpResponseEncoderName);
                        addActorToContextAndUnlock(actorCtx, internalActor, lock);
                    }
                    //noinspection unchecked
                    ((HttpActorAdapter) internalActor).service(ctx, req);
                    return;
                }
            }
        } finally {
            if (lock.isHeldByCurrentStrand() && lock.isLocked())
                lock.unlock();
        }

        sendHttpResponse(ctx, req, new DefaultFullHttpResponse(req.getProtocolVersion(), NOT_FOUND), false);
    }

    private void addActorToContextAndUnlock(Context actorContext, ActorImpl actor, ReentrantLock lock) {
        actorContext.getAttachments().put(ACTOR_KEY, actor);
        lock.unlock();
    }

    private static final class WebSocketActorAdapter extends FakeActor<WebDataMessage> {
        ActorRef<? super WebMessage> userActor;

        private ChannelHandlerContext ctx;

        public WebSocketActorAdapter(ChannelHandlerContext ctx, ActorRef<? super WebMessage> userActor) {
            super(userActor.getName(), new WebSocketChannelAdapter(ctx));
            ((WebSocketChannelAdapter) (SendPort) getMailbox()).actor = this;
            this.ctx = ctx;
            this.userActor = userActor;
            watch(userActor);
        }

        @Override
        public final void interrupt() {
            die(new InterruptedException());
        }

        @Override
        public final String toString() {
            return "WebSocketActorAdapter{" + "userActor=" + userActor + '}';
        }

        private void onMessage(final ByteBuffer message) {
            try {
                userActor.send(new WebDataMessage(ref(), message));
            } catch (SuspendExecution ex) {
                throw new AssertionError(ex);
            }
        }

        private void onMessage(final String message) {
            try {
                userActor.send(new WebDataMessage(ref(), message));
            } catch (SuspendExecution ex) {
                throw new AssertionError(ex);
            }
        }

        @Override
        protected final WebDataMessage handleLifecycleMessage(LifecycleMessage m) {
            if (m instanceof ExitMessage) {
                ExitMessage em = (ExitMessage) m;
                if (em.getActor() != null && em.getActor().equals(userActor))
                    die(em.getCause());
            }
            return null;
        }

        @Override
        protected final void throwIn(RuntimeException e) {
            die(e);
        }

        @Override
        protected final void die(Throwable cause) {
            super.die(cause);
            if (ctx.channel().isOpen())
                ctx.close();

            // Ensure to release server references
            userActor = null;
            ctx = null;
        }
    }

    private static final class WebSocketChannelAdapter implements SendPort<WebDataMessage> {
        private final ChannelHandlerContext ctx;

        WebSocketActorAdapter actor;

        public WebSocketChannelAdapter(ChannelHandlerContext ctx) {
            this.ctx = ctx;
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
            if (!message.isBinary())
                ctx.writeAndFlush(new TextWebSocketFrame(message.getStringBody()));
            else
                ctx.writeAndFlush(new BinaryWebSocketFrame(Unpooled.wrappedBuffer(message.getByteBufferBody())));
            return true;
        }

        @Override
        public final void close() {
            if (ctx.channel().isOpen())
                ctx.close();
            if (actor != null)
                actor.die(null);
        }

        @Override
        public final void close(Throwable t) {
            if (actor != null)
                actor.die(t);
            close();
        }
    }

    private static final class HttpActorAdapter extends FakeActor<HttpResponse> {
        private ActorRef<? super HttpRequest> userActor;
        private Context context;

        private volatile boolean dead;

        private volatile ChannelHandlerContext ctx;
        private volatile FullHttpRequest req;
        private volatile Object watchToken;

        HttpActorAdapter(ActorRef<? super HttpRequest> userActor, Context actorContext, String httpResponseEncoderName) {
            super("HttpActorAdapter", new HttpChannelAdapter(actorContext, httpResponseEncoderName));

            if (actorContext.watch())
                ((HttpChannelAdapter) (SendPort) getMailbox()).actor = this;

            this.userActor = userActor;
            this.context = actorContext;
        }

        final void service(ChannelHandlerContext ctx, FullHttpRequest req) throws SuspendExecution {
            if (context.watch())
                watchToken = watch(userActor);

            this.ctx = ctx;
            this.req = req;

            if (isDone()) {
                handleDeath(getDeathCause());
                return;
            }

            userActor.send(new HttpRequestWrapper(ref(), ctx, req));
        }

        final void unwatch() {
            if (watchToken != null && userActor != null) {
                unwatch(userActor, watchToken);
                watchToken = null;
            }
        }

        private void handleDeath(Throwable cause) {
            if (cause != null)
                sendHttpResponse(ctx, req, new DefaultFullHttpResponse(req.getProtocolVersion(), INTERNAL_SERVER_ERROR, Unpooled.wrappedBuffer(("Actor is dead because of " + cause.getMessage()).getBytes())), false);
            else
                sendHttpResponse(ctx, req, new DefaultFullHttpResponse(req.getProtocolVersion(), INTERNAL_SERVER_ERROR, Unpooled.wrappedBuffer(("Actor has terminated.").getBytes())), false);
        }

        @Override
        protected final HttpResponse handleLifecycleMessage(LifecycleMessage m) {
            if (m instanceof ExitMessage) {
                final ExitMessage em = (ExitMessage) m;
                if (em.getActor() != null && em.getActor().equals(userActor)) {
                    handleDeath(em.getCause());
                    die(em.getCause());
                }
            }
            return null;
        }

        @Override
        protected final void die(Throwable cause) {
            if (dead)
                return;
            dead = true;
            super.die(cause);
            try {
                context.invalidate();
            } catch (final Exception ignored) {}

            // Ensure to release references to server objects
            unwatch();
            userActor = null;
            watchToken = null;
            context = null;
            ctx = null;
            req = null;
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
        public final String toString() {
            return "HttpActorAdapter{" + userActor + "}";
        }
    }

    private static final class HttpChannelAdapter implements SendPort<HttpResponse> {
        HttpActorAdapter actor;

        private final String httpResponseEncoderName;

        private Context actorContext;

        public HttpChannelAdapter(Context actorContext, String httpResponseEncoderName) {
            this.actorContext = actorContext;
            this.httpResponseEncoderName = httpResponseEncoderName;
        }

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
        public final boolean trySend(HttpResponse message) {
            try {
                final HttpRequestWrapper nettyRequest = (HttpRequestWrapper) message.getRequest();
                final FullHttpRequest req = nettyRequest.req;
                final ChannelHandlerContext ctx = nettyRequest.ctx;

                final HttpResponseStatus status = HttpResponseStatus.valueOf(message.getStatus());

                if (message.getStatus() >= 400 && message.getStatus() < 600) {
                    sendHttpResponse(ctx, req, new DefaultFullHttpResponse(req.getProtocolVersion(), status), false);
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
                    for (final Cookie c : message.getCookies())
                        HttpHeaders.setHeader(res, COOKIE, enc.encode(getNettyCookie(c)));
                }
                if (message.getHeaders() != null) {
                    for (final Map.Entry<String, String> h : message.getHeaders().entries())
                        HttpHeaders.setHeader(res, h.getKey(), h.getValue());
                }

                if (message.getContentType() != null) {
                    String ct = message.getContentType();
                    if (message.getCharacterEncoding() != null)
                        ct = ct + "; charset=" + message.getCharacterEncoding().name();
                    HttpHeaders.setHeader(res, CONTENT_TYPE, ct);
                }

                final boolean sseStarted = message.shouldStartActor();
                if (trackSession(sseStarted)) {
                    final String sessionId = UUID.randomUUID().toString();
                    res.headers().add(SET_COOKIE, ServerCookieEncoder.STRICT.encode(SESSION_COOKIE_KEY, sessionId));
                    startSession(sessionId, actorContext);
                }
                if (!sseStarted) {
                    final String stringBody = message.getStringBody();
                    long contentLength = 0L;
                    if (stringBody != null)
                        contentLength = stringBody.getBytes().length;
                    else {
                        final ByteBuffer byteBufferBody = message.getByteBufferBody();
                        if (byteBufferBody != null)
                            contentLength = byteBufferBody.remaining();
                    }
                    res.headers().add(CONTENT_LENGTH, contentLength);
                }

                final HttpStreamActorAdapter httpStreamActorAdapter;
                if (sseStarted)
                    // This will copy the request content, which must still be referenceable, doing before the request handler
                    // unallocates it (unfortunately it is explicitly reference-counted in Netty)
                    httpStreamActorAdapter = new HttpStreamActorAdapter(ctx, req);
                else
                    httpStreamActorAdapter = null;

                sendHttpResponse(ctx, req, res, false);

                if (sseStarted) {
                    if (httpResponseEncoderName != null) {
                        ctx.pipeline().remove(httpResponseEncoderName);
                    } else {
                        final ChannelPipeline pl = ctx.pipeline();
                        final List<String> handlerKeysToBeRemoved = new ArrayList<>();
                        for (final Map.Entry<String, ChannelHandler> e : pl) {
                            if (e.getValue() instanceof HttpResponseEncoder)
                                handlerKeysToBeRemoved.add(e.getKey());
                        }
                        for (final String k : handlerKeysToBeRemoved)
                            pl.remove(k);
                    }

                    try {
                        message.getFrom().send(new HttpStreamOpened(httpStreamActorAdapter.ref(), message));
                    } catch (SuspendExecution e) {
                        throw new AssertionError(e);
                    }
                }

                return true;
            } finally {
                actor.unwatch();
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
        public final void close() {
            if (actor != null)
                actor.die(null);
            actorContext = null;
        }

        @Override
        public final void close(Throwable t) {
            log.error("Exception while closing HTTP adapter", t);
            if (actor != null)
                actor.die(t);
        }
    }

    protected static boolean trackSession(boolean sseStarted) {
        return
            trackSession != null && (
                "always".equals(trackSession) ||
                sseStarted && "sse".equals(trackSession)
        );
    }

    protected static boolean handlesWithHttp(String uri, Class<?> actorClass) {
        return match(uri, actorClass).equals("websocket");
    }

    protected static boolean handlesWithWebSocket(String uri, Class<?> actorClass) {
        return match(uri, actorClass).equals("ws");
    }

    private static final class HttpStreamActorAdapter extends FakeActor<WebDataMessage> {
        private volatile boolean dead;

        public HttpStreamActorAdapter(final ChannelHandlerContext ctx, final FullHttpRequest req) {
            super(req.toString(), new HttpStreamChannelAdapter(ctx, req));
            ((HttpStreamChannelAdapter) (SendPort) getMailbox()).actor = this;
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
            return "HttpStreamActorAdapter{request + " + getName() + "}";
        }
    }

    private static final class HttpStreamChannelAdapter implements SendPort<WebDataMessage> {
        private final Charset encoding;
        private final ChannelHandlerContext ctx;

        HttpStreamActorAdapter actor;

        public HttpStreamChannelAdapter(ChannelHandlerContext ctx, FullHttpRequest req) {
            this.ctx = ctx;
            this.encoding = HttpRequestWrapper.extractCharacterEncodingOrDefault(req.headers());
        }

        @Override
        public final void send(WebDataMessage message) throws SuspendExecution, InterruptedException {
            trySend(message);
        }

        @Override
        public final boolean send(WebDataMessage message, long timeout, TimeUnit unit) throws SuspendExecution, InterruptedException {
            send(message);
            return true;
        }

        @Override
        public final boolean send(WebDataMessage message, Timeout timeout) throws SuspendExecution, InterruptedException {
            return send(message, timeout.nanosLeft(), TimeUnit.NANOSECONDS);
        }

        @Override
        public final boolean trySend(WebDataMessage res) {
            final ByteBuf buf;
            final String stringBody = res.getStringBody();
            if (stringBody != null) {
                byte[] bs = stringBody.getBytes(encoding);
                buf = Unpooled.wrappedBuffer(bs);
            } else {
                buf = Unpooled.wrappedBuffer(res.getByteBufferBody());
            }
            ctx.writeAndFlush(buf);
            return true;
        }

        @Override
        public final void close() {
            if (ctx.channel().isOpen())
                ctx.close();
            if (actor != null)
                actor.die(null);
        }

        @Override
        public final void close(Throwable t) {
            if (actor != null)
                actor.die(t);
            close();
        }
    }

    private static void sendHttpResponse(ChannelHandlerContext ctx, FullHttpRequest req, FullHttpResponse res, Boolean close) {
        writeHttpResponse(ctx, req, res, close);
    }

    private static void sendHttpRedirect(ChannelHandlerContext ctx, FullHttpRequest req, String newUri) {
        final FullHttpResponse res = new DefaultFullHttpResponse(req.getProtocolVersion(), FOUND);
        HttpHeaders.setHeader(res, LOCATION, newUri);
        writeHttpResponse(ctx, req, res, true);
    }

    private static void writeHttpResponse(ChannelHandlerContext ctx, FullHttpRequest req, FullHttpResponse res, Boolean close) {
        if (!omitDateHeader && !res.headers().contains(DefaultHttpHeaders.Names.DATE))
            DefaultHttpHeaders.addDateHeader(res, DefaultHttpHeaders.Names.DATE, new Date());

        // Send the response and close the connection if necessary.
        if (!HttpHeaders.isKeepAlive(req) || res.getStatus().code() != 200 || close == null || close) {
            res.headers().set(CONNECTION, HttpHeaders.Values.CLOSE);
            ctx.writeAndFlush(res).addListener(ChannelFutureListener.CLOSE);
        } else {
            res.headers().set(CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
            write(ctx, res);
        }
    }

    private static ChannelFuture write(ChannelHandlerContext ctx, Object res) {
        return ctx.writeAndFlush(res);
        // : ctx.write(res);
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
                addPattern(ret, httpP, "websocket");
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

    private static void startSession(String sessionId, Context actorContext) {
        sessions.put(sessionId, actorContext);
    }
}
