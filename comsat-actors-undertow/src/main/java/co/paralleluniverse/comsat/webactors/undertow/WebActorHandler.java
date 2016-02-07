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
package co.paralleluniverse.comsat.webactors.undertow;

import co.paralleluniverse.actors.*;
import co.paralleluniverse.common.util.Pair;
import co.paralleluniverse.comsat.webactors.*;
import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.FiberUtil;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.SuspendableRunnable;
import co.paralleluniverse.strands.Timeout;
import co.paralleluniverse.strands.channels.SendPort;
import co.paralleluniverse.strands.concurrent.ReentrantLock;
import io.undertow.Handlers;
import io.undertow.UndertowLogger;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.CookieImpl;
import io.undertow.util.*;
import io.undertow.websockets.WebSocketConnectionCallback;
import io.undertow.websockets.core.*;
import io.undertow.websockets.spi.WebSocketHttpExchange;
import org.xnio.Buffers;
import org.xnio.ChannelListener;
import org.xnio.ChannelListeners;
import org.xnio.channels.StreamSinkChannel;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * @author circlespainter
 */
public class WebActorHandler implements HttpHandler {
    protected static final String ACTOR_KEY = "co.paralleluniverse.comsat.webactors.sessionActor";

    // @FunctionalInterface
    public interface ContextProvider {
        Context get(HttpServerExchange xch);
    }

    public interface Context {
        boolean isValid();

        ActorRef<? extends WebMessage> getRef();

        ReentrantLock getLock();

        Map<String, Object> getAttachments();

        boolean handlesWithWebSocket(String uri);
        boolean handlesWithHttp(String uri);
    }

    public static abstract class DefaultContextImpl implements Context {
        private final static String durationProp = System.getProperty(DefaultContextImpl.class.getName() + ".durationMillis");
        private final static long DURATION = durationProp != null ? Long.parseLong(durationProp) : 60_000L;
        final Map<String, Object> attachments = new HashMap<>();
        private final ReentrantLock lock = new ReentrantLock();
        private final long created;
        private boolean valid = true;

        public DefaultContextImpl() {
            this.created = new Date().getTime();
        }

        private void invalidate() {
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

    static final WeakHashMap<Class<?>, List<Pair<String, String>>> classToUrlPatterns = new WeakHashMap<>();

    private final ContextProvider selector;

    public WebActorHandler(ContextProvider selector) {
        this.selector = selector;
        // this.continueHandler = Handlers.httpContinueRead(null);
    }

    @Override
    public final void handleRequest(final HttpServerExchange xch) throws Exception {
        // continueHandler.handleRequest(xch);

        final Context context = selector.get(xch);
        assert context != null;

        final ReentrantLock lock = context.getLock();
        assert lock != null;

        lock.lock();

        try {
            final ActorRef<? extends WebMessage> userActorRef = context.getRef();
            ActorImpl internalActor = (ActorImpl) context.getAttachments().get(ACTOR_KEY);

            final String uri = xch.getRequestURI();
            if (userActorRef != null) {
                if (context.handlesWithWebSocket(uri)) {
                    if (internalActor == null || !(internalActor instanceof WebSocketActorAdapter)) {

                        @SuppressWarnings("unchecked") final ActorRef<WebMessage> userActorRef0 = (ActorRef<WebMessage>) userActorRef;
                        internalActor = new WebSocketActorAdapter(userActorRef0);

                        //noinspection unchecked
                        addActorToContextAndUnlock(context, internalActor, lock);
                    }

                    final WebSocketActorAdapter webSocketActor = (WebSocketActorAdapter) internalActor;

                    xch.dispatch(); // Start async

                    // Handle with websocket
                    Handlers.websocket(new WebSocketConnectionCallback() {
                        @Override
                        public void onConnect(WebSocketHttpExchange exchange, WebSocketChannel channel) {
                            webSocketActor.setChannel(channel);

                            channel.getReceiveSetter().set(new AbstractReceiveListener() {
                                @Override
                                protected void onFullBinaryMessage(WebSocketChannel channel, BufferedBinaryMessage message) throws IOException {
                                    webSocketActor.onMessage(message);
                                }

                                @Override
                                protected void onFullTextMessage(WebSocketChannel channel, BufferedTextMessage message) throws IOException {
                                    webSocketActor.onMessage(message);
                                }
                            });

                            channel.resumeReceives();

                            try {
                                FiberUtil.runInFiber(new SuspendableRunnable() {
                                    @Override
                                    public void run() throws SuspendExecution, InterruptedException {
                                        //noinspection unchecked
                                        ((ActorRef) userActorRef).send(new WebSocketOpened(webSocketActor.ref()));
                                    }
                                });
                            } catch (final InterruptedException | ExecutionException e) {
                                UndertowLogger.ROOT_LOGGER.error("Exception while sending `WebSocketOpened` message to actor", e);

                                throw new RuntimeException(e);
                            }
                        }
                    }).handleRequest(xch);

                    return;
                } else if (context.handlesWithHttp(uri)) {
                    //noinspection ConstantConditions
                    if (internalActor == null || !(internalActor instanceof HttpActorAdapter)) {
                        //noinspection unchecked
                        internalActor = HttpActorAdapter.INSTANCE;
                        addActorToContextAndUnlock(context, internalActor, lock);
                    }

                    //noinspection unchecked
                    ((HttpActorAdapter) internalActor).service(xch, (ActorRef<? super HttpRequest>) userActorRef);
                    return;
                }
            }

            sendHttpResponse(xch, StatusCodes.NOT_FOUND);
        } finally {
            if (lock.isHeldByCurrentStrand() && lock.isLocked())
                lock.unlock();
        }
    }

    static void addActorToContextAndUnlock(Context context, ActorImpl actor, ReentrantLock lock) {
        context.getAttachments().put(ACTOR_KEY, actor);
        lock.unlock();
    }

    private static final class WebSocketActorAdapter extends FakeActor<WebDataMessage> {
        final ActorRef<? super WebMessage> webActor;
        private final WebSocketChannelAdapter channelAdapter;

        private WebSocketChannel channel;

        public WebSocketActorAdapter(ActorRef<? super WebMessage> webActor) {
            super(webActor.getName(), new WebSocketChannelAdapter());
            this.channelAdapter = (WebSocketChannelAdapter) (SendPort) mailbox();
            this.webActor = webActor;
        }

        final void setChannel(WebSocketChannel channel) {
            this.channel = channel;
            this.channelAdapter.channel = channel;
        }

        final void onMessage(BufferedBinaryMessage message) {
            try {
                webActor.send(new WebDataMessage(ref(), toBuffer(message.getData().getResource()).duplicate()));
            } catch (final SuspendExecution ex) {
                throw new AssertionError(ex);
            }
        }

        final void onMessage(BufferedTextMessage message) {
            try {
                webActor.send(new WebDataMessage(ref(), message.getData()));
            } catch (final SuspendExecution ex) {
                throw new AssertionError(ex);
            }
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
                channel.sendClose();
            } catch (final IOException e) {
                UndertowLogger.ROOT_LOGGER.error("Exception while closing websocket channel during actor death", e);
                throw new RuntimeException(e);
            }
        }

        @Override
        public final String toString() {
            return "WebSocketActor{" + "userActor=" + webActor + '}';
        }
    }

    private static final class WebSocketChannelAdapter implements SendPort<WebDataMessage> {
        WebSocketChannel channel;

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
                WebSockets.sendText(message.getStringBody(), channel, null);
            else
                WebSockets.sendBinary(message.getByteBufferBody(), channel, null);
            return true;
        }

        @Override
        public final void close() {
            try {
                channel.sendClose();
            } catch (final IOException e) {
                UndertowLogger.ROOT_LOGGER.error("Exception while closing websocket channel", e);

                throw new RuntimeException(e);
            }
        }

        @Override
        public final void close(Throwable t) {
            close();
        }
    }

    private static final class HttpActorAdapter extends FakeActor<HttpResponse> {
        static final HttpActorAdapter INSTANCE = new HttpActorAdapter();

        private HttpActorAdapter() {
            super("HttpActorAdapter", HttpChannelAdapter.INSTANCE);
        }

        final void service(final HttpServerExchange xch, final ActorRef<? super HttpRequest> userActor) throws SuspendExecution {
            if (isDone()) {
                @SuppressWarnings("ThrowableResultOfMethodCallIgnored") final Throwable deathCause = getDeathCause();
                if (deathCause != null)
                    sendHttpResponse(xch, StatusCodes.INTERNAL_SERVER_ERROR, "Actor is dead because of " + deathCause.getMessage());
                else
                    sendHttpResponse(xch, StatusCodes.INTERNAL_SERVER_ERROR, "Actor has finished");
                return;
            }

            new HttpByteArrayReadChannelListener(ref(), xch, userActor).setup(xch.getRequestChannel());
        }

        @Override
        protected final HttpResponse handleLifecycleMessage(LifecycleMessage m) {
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
        public final String toString() {
            return "HttpActorAdapter";
        }

        private static final class RequestSending implements SuspendableRunnable {
            private final ActorRef from;
            private final ActorRef<? super HttpRequest> userActor;
            private final HttpServerExchange xch;
            private final byte[] ba;

            public RequestSending(ActorRef from, ActorRef<? super HttpRequest> userActor, HttpServerExchange xch, byte[] ba) {
                this.from = from;
                this.userActor = userActor;
                this.xch = xch;
                this.ba = ba;
            }

            @Override
            public final void run() throws SuspendExecution, InterruptedException {
                //noinspection unchecked
                userActor.send(new HttpRequestWrapper(from, xch, ByteBuffer.wrap(ba)));
            }
        }

        private static final class HttpByteArrayReadChannelListener extends ByteArrayReadChannelListener {
            private final HttpServerExchange xch;
            private final ActorRef<? super HttpRequest> userActor;
            private final ActorRef<HttpResponse> from;

            public HttpByteArrayReadChannelListener(ActorRef<HttpResponse> from, HttpServerExchange xch, ActorRef<? super HttpRequest> userActor) {
                super(xch.getConnection().getByteBufferPool());
                this.xch = xch;
                this.userActor = userActor;
                this.from = from;
            }

            @Override
            protected final void byteArrayDone(final byte[] ba) {
                xch.dispatch(); // Start async

                new Fiber(new RequestSending(from, userActor, xch, ba)).start();
            }

            @Override
            protected final void error(IOException e) {
                UndertowLogger.ROOT_LOGGER.error("Exception while reading HTTP request", e);
                throw new RuntimeException(e);
            }
        }
    }

    private static final class HttpChannelAdapter implements SendPort<HttpResponse> {
        static final HttpChannelAdapter INSTANCE = new HttpChannelAdapter();

        private HttpChannelAdapter() {
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
        public final boolean trySend(final HttpResponse message) {
            final HttpRequestWrapper undertowRequest = (HttpRequestWrapper) message.getRequest();
            final HttpServerExchange xch = undertowRequest.xch;

            final int status = message.getStatus();

            if (status >= 400 && status < 600) {
                sendHttpResponse(xch, status);
                close();
                return true;
            }

            if (message.getRedirectPath() != null) {
                sendHttpRedirect(xch, message.getRedirectPath());
                close();
                return true;
            }

            if (message.getCookies() != null) {
                for (final Cookie c : message.getCookies())
                    xch.setResponseCookie(newUndertowCookie(c));
            }
            final HeaderMap heads = xch.getResponseHeaders();
            if (message.getHeaders() != null) {
                for (final String k : message.getHeaders().keys())
                    heads.putAll(new HttpString(k), message.getHeaderValues(k));
            }

            if (message.getContentType() != null) {
                String ct = message.getContentType();
                if (message.getCharacterEncoding() != null)
                    ct = ct + "; charset=" + message.getCharacterEncoding().name();
                xch.getResponseHeaders().add(Headers.CONTENT_TYPE, ct);
            }

            final boolean sseStarted = message.shouldStartActor();
            if (sseStarted) {
                try {
                    xch.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/event-stream; charset=UTF-8");
                    xch.setPersistent(false);

                    final StreamSinkChannel sink = xch.getResponseChannel();
                    // This will copy the request content, which must still be referenceable, doing before the request handler
                    // unallocates it (unfortunately it is explicitly reference-counted in Netty)
                    final HttpStreamActorAdapter httpStreamActorAdapter = new HttpStreamActorAdapter(xch);

                    if (!sink.flush()) {
                        sink.getWriteSetter().set(ChannelListeners.flushingChannelListener(new ChannelListener<StreamSinkChannel>() {
                            @Override
                            public void handleEvent(final StreamSinkChannel channel) {
                                try {
                                    FiberUtil.runInFiber(new SuspendableRunnable() {
                                        @Override
                                        public void run() throws SuspendExecution, InterruptedException {
                                            handleSSEStart(httpStreamActorAdapter, message, channel);
                                        }
                                    });
                                } catch (final InterruptedException | ExecutionException e) {
                                    UndertowLogger.ROOT_LOGGER.error("Exception while handling SSE start response event", e);
                                    throw new RuntimeException(e);
                                }
                            }
                        }, null));
                        sink.resumeWrites();
                    } else {
                        handleSSEStart(httpStreamActorAdapter, message, sink);
                    }
                } catch (final Exception e) {
                    UndertowLogger.ROOT_LOGGER.error("Exception while sending SSE start response", e);
                    throw new RuntimeException(e);
                }
            } else {
                if (message.getStringBody() != null)
                    sendHttpResponse(xch, status, message.getStringBody());
                else if (message.getByteBufferBody() != null)
                    sendHttpResponse(xch, status, message.getByteBufferBody());
                else
                    sendHttpResponse(xch, status);
            }

            return true;
        }

        private void handleSSEStart(HttpStreamActorAdapter httpStreamActorAdapter, HttpResponse message, StreamSinkChannel channel) throws SuspendExecution {
            httpStreamActorAdapter.setChannel(channel);
            message.getFrom().send(new HttpStreamOpened(httpStreamActorAdapter.ref(), message));
        }

        private io.undertow.server.handlers.Cookie newUndertowCookie(Cookie c) {
            io.undertow.server.handlers.Cookie ret = new CookieImpl(c.getName(), c.getValue());
            ret.setComment(c.getComment());
            ret.setDomain(c.getDomain());
            ret.setHttpOnly(c.isHttpOnly());
            ret.setMaxAge(c.getMaxAge());
            ret.setPath(c.getPath());
            ret.setVersion(c.getVersion());
            ret.setSecure(c.isSecure());
            return ret;
        }

        @Override
        public final void close() {
            // Stateless, single-instance HTTP write port, nothing to do
        }

        @Override
        public final void close(Throwable t) {
            UndertowLogger.ROOT_LOGGER.error("Exception while closing HTTP adapter", t);
        }
    }

    private static final class HttpStreamActorAdapter extends FakeActor<WebDataMessage> {
        private final HttpStreamChannelAdapter channelAdapter;

        private volatile boolean dead;

        HttpStreamActorAdapter(HttpServerExchange xch) {
            super(xch.toString(), new HttpStreamChannelAdapter(xch));
            ((HttpStreamChannelAdapter) (SendPort) mailbox()).actor = this;
            this.channelAdapter = (HttpStreamChannelAdapter) (SendPort) mailbox();
        }

        @Override
        protected final WebDataMessage handleLifecycleMessage(LifecycleMessage m) {
            if (m instanceof ShutdownMessage) {
                die(null);
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
            if (dead)
                return;
            this.dead = true;
            mailbox().close();
            super.die(cause);
        }

        @Override
        public final String toString() {
            return "HttpStreamActorAdapter{request + " + getName() + "}";
        }

        public final void setChannel(StreamSinkChannel channel) {
            this.channelAdapter.setChannel(channel);
        }
    }

    private static final class HttpStreamChannelAdapter implements SendPort<WebDataMessage> {
        private final HttpServerExchange xch;

        private StreamSinkChannel channel;

        HttpStreamActorAdapter actor;

        HttpStreamChannelAdapter(HttpServerExchange xch) {
            this.xch = xch;
        }

        public final void setChannel(StreamSinkChannel channel) {
            this.channel = channel;
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
        public final boolean trySend(final WebDataMessage res) {
            final String stringBody = res.getStringBody();
            final String charset = xch.getRequestCharset();
            final StringWriteChannelListener l;
            if (stringBody != null) {
                if (charset != null)
                    l = new StringWriteChannelListener(stringBody, Charset.forName(charset));
                else
                    l = new StringWriteChannelListener(stringBody);
            } else {
                l = new StringWriteChannelListener(res.getByteBufferBody());
            }
            l.setup(channel);
            return true;
        }

        @Override
        public final void close() {
            xch.endExchange();
        }

        @Override
        public final void close(Throwable t) {
            close();
        }
    }

    static void sendHttpResponse(HttpServerExchange xch, int statusCode) {
        sendHttpResponse(xch, statusCode, (String) null);
    }

    static void sendHttpResponse(HttpServerExchange xch, int statusCode, String body) {
        xch.setStatusCode(statusCode);
        if (body != null)
            xch.getResponseSender().send(body);
        xch.endExchange();
    }

    static void sendHttpResponse(HttpServerExchange xch, int statusCode, ByteBuffer body) {
        xch.setStatusCode(statusCode);
        if (body != null)
            xch.getResponseSender().send(body);
        xch.endExchange();
    }

    static void sendHttpRedirect(HttpServerExchange xch, String path) {
        xch.setStatusCode(StatusCodes.FOUND);
        xch.getResponseHeaders().add(Headers.LOCATION, xch.getProtocol() + "://" + xch.getHostAndPort() + path);
        xch.endExchange();
    }

    static boolean handlesWithHttp(String uri, Class<?> actorClass) {
        return match(uri, actorClass).equals("http");
    }

    static boolean handlesWithWebSocket(String uri, Class<?> actorClass) {
        return match(uri, actorClass).equals("ws");
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

    private static ByteBuffer toBuffer(ByteBuffer... payload) {
        if (payload.length == 1) {
            return payload[0];
        }
        int size = (int) Buffers.remaining(payload);
        if (size == 0) {
            return Buffers.EMPTY_BYTE_BUFFER;
        }
        ByteBuffer buffer = ByteBuffer.allocate(size);
        for (ByteBuffer buf : payload) {
            buffer.put(buf);
        }
        buffer.flip();
        return buffer;
    }
}
