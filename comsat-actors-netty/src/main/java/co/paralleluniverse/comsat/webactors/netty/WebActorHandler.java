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
import co.paralleluniverse.comsat.webactors.*;
import co.paralleluniverse.comsat.webactors.Cookie;
import co.paralleluniverse.comsat.webactors.HttpRequest;
import co.paralleluniverse.comsat.webactors.HttpResponse;
import co.paralleluniverse.fibers.FiberUtil;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.SuspendableRunnable;
import co.paralleluniverse.strands.Timeout;
import co.paralleluniverse.strands.channels.SendPort;
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

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author circlespainter
 */
public final class WebActorHandler extends SimpleChannelInboundHandler<Object> {

    public interface Session {
        boolean isValid();
        void invalidate();
        ActorImpl<? extends WebMessage> getActor();
        Map<String, Object> getAttachments();
    }

    public static abstract class DefaultSessionImpl implements Session {
        final Map<String, Object> attachments = new ConcurrentHashMap<>();
        private boolean valid = true;

        @Override
        public final void invalidate() {
            attachments.clear();
            valid = false;
        }

        @Override
        public final boolean isValid() {
            return valid;
        }

        @Override
        public final Map<String, Object> getAttachments() {
            return attachments;
        }
    }

    public interface SessionSelector {
        Session select(FullHttpRequest req);
    }

    private static final String ACTOR_KEY = "co.paralleluniverse.actor";

    private WebSocketServerHandshaker handshaker;
    private WebSocketActorAdapter webSocketActor;
    private final SessionSelector selector;

    public WebActorHandler(SessionSelector selector) {
        this.selector = selector;
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        ctx.close();
        cause.printStackTrace(System.err);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof FullHttpRequest) {
            handleHttpRequest(ctx, (FullHttpRequest) msg);
        } else if (msg instanceof WebSocketFrame) {
            handleWebSocketFrame(ctx, (WebSocketFrame) msg);
        }
    }

    private boolean handlesWithHttp(String uri, Class<?> actorClass) {
        final WebActor w = actorClass.getAnnotation(WebActor.class);
        if (w != null) {
            for (String httpPattern : w.httpUrlPatterns()) {
                if (servletMatch(httpPattern, uri))
                    return true;
            }
        }
        return false;
    }

    private boolean handlesWithWebSocket(String uri, Class<?> actorClass) {
        final WebActor w = actorClass.getAnnotation(WebActor.class);
        if (w != null) {
            for (String webSocketPattern : w.webSocketUrlPatterns()) {
                if (servletMatch(webSocketPattern, uri))
                    return true;
            }
        }
        return false;
    }

    private boolean servletMatch(String pattern, String uri) {
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

        final Session session = selector.select(req);
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
            // TODO Fix to test first the most specific one
            if (handlesWithWebSocket(uri, session.getActor().getClass())) {
                if (internalActor == null || !(internalActor instanceof WebSocketActorAdapter)) {
                    //noinspection unchecked
                    this.webSocketActor = new WebSocketActorAdapter(ctx, (ActorRef<? super WebMessage>) userActorRef);
                    session.getAttachments().put(ACTOR_KEY, this.webSocketActor);
                }
                // Handshake
                final WebSocketServerHandshakerFactory wsFactory = new WebSocketServerHandshakerFactory(getWebSocketLocation(uri), null, true);
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
                    internalActor = new HttpActorAdapter(session, (ActorRef<HttpRequest>) userActorRef);
                    session.getAttachments().put(ACTOR_KEY, internalActor);
                }
                ((HttpActorAdapter) internalActor).service(ctx, req);
                return;
            }
        }

        sendHttpResponse(ctx, req, new DefaultFullHttpResponse(req.getProtocolVersion(), NOT_FOUND));
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

        public HttpActorAdapter(Session session, ActorRef<? super HttpRequest> webActor) {
            super(webActor.getName(), new HttpChannelAdapter());

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

            sendHttpResponse(ctx, req, res, !message.shouldStartActor());

            if (message.shouldStartActor()) {
                try {
                    message.getFrom().send(new HttpStreamOpened(new HttpStreamActorAdapter(ctx, req).ref(), message));
                } catch (SuspendExecution e) {
                    throw new AssertionError(e);
                }
            }

            return true;
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

    private static class HttpStreamActorAdapter extends FakeActor<WebDataMessage> {
        private volatile boolean dead;

        public HttpStreamActorAdapter(final ChannelHandlerContext ctx, final FullHttpRequest req) {
            super(req.toString(), new HttpStreamChannelAdapter(ctx));
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
        HttpStreamActorAdapter actor;
        final ChannelHandlerContext ctx;

        public HttpStreamChannelAdapter(ChannelHandlerContext ctx) {
            this.ctx = ctx;
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
            if (res.getByteBufferBody() != null)
                buf = Unpooled.wrappedBuffer(res.getByteBufferBody());
            else
                buf = Unpooled.wrappedBuffer(res.getStringBody().getBytes());
            buf.release();
            ctx.writeAndFlush(buf);
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

    private static String getWebSocketLocation(String uri) {
        return uri.replace("http://", "ws://").replace("https://", "wss://");
    }
}
