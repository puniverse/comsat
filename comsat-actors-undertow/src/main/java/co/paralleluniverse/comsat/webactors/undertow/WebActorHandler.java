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
import co.paralleluniverse.fibers.FiberUtil;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.Suspendable;
import co.paralleluniverse.strands.SuspendableRunnable;
import co.paralleluniverse.strands.Timeout;
import co.paralleluniverse.strands.channels.SendPort;
import co.paralleluniverse.strands.concurrent.CountDownLatch;
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
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author circlespainter
 */
public class WebActorHandler implements HttpHandler {
    protected static final String ACTOR_KEY = "co.paralleluniverse.comsat.webactors.sessionActor";

    protected static final ExecutorService es = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    protected static final ScheduledExecutorService ts = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors());

    // @FunctionalInterface
    public interface ContextProvider {
        Context get(HttpServerExchange xch);
    }

    public interface Context {
        String getId();
        ReentrantLock getLock();

        boolean isValid() throws SuspendExecution, InterruptedException;
        void invalidate() throws SuspendExecution, InterruptedException;

        boolean renew();
        void restart(HttpServerExchange xch);

        ActorRef<? extends WebMessage> getWebActor();
        boolean handlesWithHttp(String uri);
        boolean handlesWithWebSocket(String uri);

        enum WatchPolicy { DONT_WATCH, DIE, DIE_IF_EXCEPTION_ELSE_RESTART, RESTART }
        WatchPolicy watch();

        Map<String, Object> getAttachments();
    }

    public static abstract class DefaultContextImpl implements Context {
        private final static String durationProp = System.getProperty(DefaultContextImpl.class.getName() + ".durationMillis");
        private final static long DURATION = durationProp != null ? Long.parseLong(durationProp) : 60_000L;
        private final ReentrantLock lock = new ReentrantLock();
        @SuppressWarnings("unused")
        private final long created;
        final Map<String, Object> attachments = new HashMap<>();

        protected long renewed;
        private Long validityMS;

        private boolean valid = true;

        public DefaultContextImpl() {
            renewed = created = new Date().getTime();
        }

        @Override
        public final void invalidate() throws SuspendExecution, InterruptedException {
            final HttpActorAdapter actor = (HttpActorAdapter) attachments.get(ACTOR_KEY);
            if (actor != null)
//                actor.ch.send(HttpActorAdapter.EXIT);
                actor.die(null);
            attachments.clear();
            valid = false;
        }

        @Override
        public final boolean isValid() throws SuspendExecution, InterruptedException {
            final boolean ret = valid && (new Date().getTime() - renewed) <= getValidityMS();
            if (!ret)
                invalidate();
            return ret;
        }

        @Override
        public final boolean renew() {
            if (!valid)
                return false;

            renewed = new Date().getTime();
            return true;
        }

        @Override
        public final Map<String, Object> getAttachments() {
            return attachments;
        }

        @Override
        public final ReentrantLock getLock() {
            return lock;
        }

        public void setValidityMS(long validityMS) {
            this.validityMS = validityMS;
        }

        public final long getValidityMS() {
            return validityMS != null ? validityMS : DURATION;
        }
    }

    static final WeakHashMap<Class<?>, List<Pair<String, String>>> classToUrlPatterns = new WeakHashMap<>();

    protected ContextProvider contextProvider;
    private HttpHandler fallbackHttpHandler = null;

    public WebActorHandler(ContextProvider contextProvider) {
        this.contextProvider = contextProvider;
    }

    public final void setFallbackHttpHandler(HttpHandler httpHandler) {
        this.fallbackHttpHandler = httpHandler;
    }

    @Override
    public final void handleRequest(final HttpServerExchange xch) throws Exception {

        if (contextProvider == null) {
            initContextProvider();
        }

        final Context context = contextProvider.get(xch);
        if (context == null) {
            handlingComplete(xch);
            return;
        }

        final ReentrantLock lock = context.getLock();
        assert lock != null;

        lock.lock();

        try {
            final ActorRef<? extends WebMessage> userActorRef = context.getWebActor();
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

                    // xch.dispatch(); // Start async

                    // Handle with websocket
                    Handlers.websocket(new WebSocketConnectionCallback() {
                        @Override
                        public final void onConnect(WebSocketHttpExchange exchange, WebSocketChannel channel) {
                            webSocketActor.setChannel(channel);

                            channel.getReceiveSetter().set(new AbstractReceiveListener() {
                                @Override
                                protected final void onFullBinaryMessage(WebSocketChannel channel, BufferedBinaryMessage message) throws IOException {
                                    webSocketActor.onMessage(message);
                                }

                                @Override
                                protected final void onFullTextMessage(WebSocketChannel channel, BufferedTextMessage message) throws IOException {
                                    webSocketActor.onMessage(message);
                                }
                            });

                            channel.resumeReceives();

                            try {
                                FiberUtil.runInFiber(new SuspendableRunnable() {
                                    @Override
                                    public final void run() throws SuspendExecution, InterruptedException {
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
                    final HttpActorAdapter adapter;
                    if (internalActor == null || !(internalActor instanceof HttpActorAdapter)) {
                        //noinspection unchecked
                        adapter = new HttpActorAdapter((ActorRef<? super HttpRequest>) userActorRef, context);
                        addActorToContextAndUnlock(context, internalActor, lock);
                    } else {
                        adapter = (HttpActorAdapter) internalActor;
                    }

                    // Free IO thread and move to worker thread.
                    // Doing only here as it seems to cause problems to web socket handling
                    xch.dispatch(new Runnable() {
                        @Override
                        @Suspendable
                        public void run() {
//                            xch.dispatch(); // Start async: detach response sending from worker thread's return
//                            // Free worker thread by starting available I/O in a fiber
//                            new Fiber(new SuspendableRunnable() {
//                                @Override
//                                public void run() throws SuspendExecution, InterruptedException {
                                    new ByteArrayReadChannelListener(xch.getConnection().getByteBufferPool()) {
                                        @Override
                                        @Suspendable
                                        protected final void byteArrayDone(final byte[] ba) {
                                            try {
                                                // adapter.ch.send(new HttpRequestWrapper(adapter.ref(), xch, ByteBuffer.wrap(ba)));
                                                adapter.handleRequest(new UndertowHttpRequest(adapter.ref(), xch, ByteBuffer.wrap(ba)));
                                            } catch (final SuspendExecution e) {
                                                throw new AssertionError(e);
                                            } catch (final InterruptedException e) {
                                                throw new RuntimeException(e);
                                            }
                                        }

                                        @Override
                                        protected final void error(IOException e) {
                                            UndertowLogger.ROOT_LOGGER.error("Exception while reading HTTP request", e);
                                            throw new RuntimeException(e);
                                        }
                                    }.setup(xch.getRequestChannel());
                                }
//                            }).start();
//                        }
                    });
                    return;
                }
            }

            handlingComplete(xch);

        } finally {
            if (lock.isHeldByCurrentStrand() && lock.isLocked())
                lock.unlock();
        }
    }

    protected void initContextProvider() {
    }

    private void handlingComplete(HttpServerExchange xch) throws Exception {
        if (fallbackHttpHandler != null)
            fallbackHttpHandler.handleRequest(xch);
        else
            sendHttpResponse(xch, StatusCodes.NOT_FOUND);
    }

    static void addActorToContextAndUnlock(Context context, ActorImpl actor, ReentrantLock lock) {
        context.getAttachments().put(ACTOR_KEY, actor);
        lock.unlock();
    }

    private static final class WebSocketActorAdapter extends FakeActor<WebDataMessage> {
        ActorRef<? super WebMessage> userActor;

        private WebSocketChannelAdapter adapter;

        private WebSocketChannel channel;

        public WebSocketActorAdapter(ActorRef<? super WebMessage> userActor) {
            super(userActor.getName(), new WebSocketChannelAdapter());
            adapter = (WebSocketChannelAdapter) (SendPort) mailbox();
            adapter.actor = this;
            this.userActor = userActor;
            watch(userActor);
        }

        final void setChannel(WebSocketChannel channel) {
            this.channel = channel;
            this.adapter.channel = channel;
        }

        final void onMessage(BufferedBinaryMessage message) {
            try {
                userActor.send(new WebDataMessage(ref(), toBuffer(message.getData().getResource()).duplicate()));
            } catch (final SuspendExecution ex) {
                throw new AssertionError(ex);
            }
        }

        final void onMessage(BufferedTextMessage message) {
            try {
                userActor.send(new WebDataMessage(ref(), message.getData()));
            } catch (final SuspendExecution ex) {
                throw new AssertionError(ex);
            }
        }

        @Override
        protected final WebDataMessage handleLifecycleMessage(LifecycleMessage m) {
            if (m instanceof ExitMessage) {
                final ExitMessage em = (ExitMessage) m;
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
            } finally {
                // Ensure to release server references
                adapter = null;
                userActor = null;
                channel = null;
            }
        }

        @Override
        public final String toString() {
            return "WebSocketActor{" + "userActor=" + userActor + '}';
        }
    }

    private static final class WebSocketChannelAdapter implements SendPort<WebDataMessage> {
        WebSocketChannel channel;

        WebSocketActorAdapter actor;

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

    private static final class HttpActorAdapter extends FakeActor<HttpResponse> {
        private final static String replyTimeoutProp = System.getProperty(HttpActorAdapter.class.getName() + ".replyTimeout");
        private static final long REPLY_TIMEOUT = replyTimeoutProp != null ? Long.parseLong(replyTimeoutProp) : 120_000L;

        private final AtomicReference<CountDownLatch> gate = new AtomicReference<>();

        private volatile ActorRef<? super HttpRequest> userActor;
        private volatile Context context;

        private volatile HttpServerExchange xch;
        private volatile boolean needsRestart;

        private volatile boolean dead;
        private volatile Object watchToken;

        private volatile ScheduledFuture<?> cancelTask;

        HttpActorAdapter(ActorRef<? super HttpRequest> userActor, Context actorContext) {
            super("HttpActorAdapter", new HttpChannelAdapter());

            ((HttpChannelAdapter) (SendPort) getMailbox()).actor = this;
            if (actorContext.watch() != Context.WatchPolicy.DONT_WATCH) {
                if (userActor != null)
                    watchToken = watch(userActor);
            }

            this.userActor = userActor;
            this.context = actorContext;
        }

        @Override
        public final String toString() {
            return "HttpActorAdapter{" + userActor + "}";
        }


        @Override
        @Suspendable
        protected final HttpResponse handleLifecycleMessage(LifecycleMessage m) {
            handleLifecycle(m);
            return null;
        }

        @Override
        @Suspendable
        protected final void die(Throwable cause) {
            handleDie(cause);
        }

        @Override
        @Suspendable
        protected final void throwIn(RuntimeException e) {
            die(e);
        }

        @Override
        @Suspendable
        protected final void interrupt() {
            die(new InterruptedException());
        }

        @Suspendable
        final void handleRequest(UndertowHttpRequest s) throws SuspendExecution, InterruptedException {
            blockSessionRequests();
            xch = s.xch;
            if (needsRestart) {
                context.restart(xch);
                context.getLock().lock();
                addActorToContextAndUnlock(context, HttpActorAdapter.this, context.getLock());
                needsRestart = false;
            }
            userActor.send(s);
        }

        final void handleReply(final HttpResponse message) throws InterruptedException {
            try {
                final UndertowHttpRequest undertowRequest = (UndertowHttpRequest) message.getRequest();
                final HttpServerExchange xch = undertowRequest.xch;

                final int status = message.getStatus();

                if (status >= 400 && status < 600) {
                    // Sending a reply directly from a fiber produces a thread-local related leak due to unfreed buffers
                    es.submit(new Runnable() {
                        @Override
                        public void run() {
                            sendHttpResponse(xch, status);
                        }
                    });
                    return;
                }

                if (message.getRedirectPath() != null) {
                    // Sending a reply directly from a fiber produces a thread-local related leak due to unfreed buffers
                    es.submit(new Runnable() {
                        @Override
                        public void run() {
                            sendHttpRedirect(xch, message.getRedirectPath());
                        }
                    });
                    return;
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
                        final HttpStreamActorAdapter httpStreamActorAdapter = new HttpStreamActorAdapter(xch);

                        if (!sink.flush()) {
                            sink.getWriteSetter().set(ChannelListeners.flushingChannelListener(new ChannelListener<StreamSinkChannel>() {
                                @Override
                                public final void handleEvent(final StreamSinkChannel channel) {
                                    try {
                                        FiberUtil.runInFiber(new SuspendableRunnable() {
                                            @Override
                                            public void run() throws SuspendExecution, InterruptedException {
                                                notifySSEStarted(httpStreamActorAdapter, message, channel);
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
                            notifySSEStarted(httpStreamActorAdapter, message, sink);
                        }
                    } catch (final Exception e) {
                        UndertowLogger.ROOT_LOGGER.error("Exception while sending SSE start response", e);
                        throw new RuntimeException(e);
                    }
                } else {
                    // Sending a reply directly from a fiber produces a thread-local related leak due to unfreed buffers
                    es.submit(new Runnable() {
                        @Override
                        public void run() {
                            if (message.getStringBody() != null)
                                sendHttpResponse(xch, status, message.getStringBody());
                            else if (message.getByteBufferBody() != null)
                                sendHttpResponse(xch, status, message.getByteBufferBody());
                            else
                                sendHttpResponse(xch, status);
                        }
                    });
                }
            } finally {
                unblockSessionRequests();
            }
        }

        final boolean handleLifecycle(LifecycleMessage l) {
            if (l instanceof ExitMessage) {
                final ExitMessage em = (ExitMessage) l;
                if (em.getActor() != null && em.getActor().equals(userActor)) {
                    final Context.WatchPolicy wp = context.watch();
                    //noinspection ThrowableResultOfMethodCallIgnored
                    if (wp == Context.WatchPolicy.RESTART ||
                        wp == Context.WatchPolicy.DIE_IF_EXCEPTION_ELSE_RESTART && em.getCause() == null) {
                        needsRestart = true;
                    } else {
                        handleDie(em.getCause());
                        return true;
                    }
                }
            }
            return false;
        }

        @Suspendable
        final void handleDie(final Throwable cause) {
            try {
                possiblyReplyDeadAndUnblock(cause);

                if (dead)
                    return;
                dead = true;
                HttpActorAdapter.super.die(cause);
                try {
                    context.invalidate();
                } catch (final Exception ignored) {
                }

                // Ensure to release references
                if (userActor != null && watchToken != null)
                    unwatch(userActor, watchToken);
            } finally {
                unblockSessionRequests();
            }

            userActor = null;
            watchToken = null;
            context = null;
            xch = null;
//            f = null;
//            ch = null;
        }

        private void possiblyReplyDeadAndUnblock(final Throwable cause) {
            if (isRequestInProgress()) {
                try {
                    final HttpServerExchange xch1 = xch;
                    // Sending a reply directly from a fiber produces a thread-local related leak due to unfreed buffers
                    es.submit(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                if (cause != null) {
                                    sendHttpResponse(xch1, StatusCodes.INTERNAL_SERVER_ERROR, "Actor is dead because of " + cause.getMessage());
                                } else {
                                    sendHttpResponse(xch1, StatusCodes.INTERNAL_SERVER_ERROR, "Actor has terminated.");
                                }
                            } finally {
                                unblockSessionRequests();
                            }
                        }
                    });
                } finally {
                    unblockSessionRequests();
                }
            }
        }

        private void notifySSEStarted(HttpStreamActorAdapter httpStreamActorAdapter, HttpResponse message, StreamSinkChannel channel) throws SuspendExecution {
            httpStreamActorAdapter.setChannel(channel);
            message.getFrom().send(new HttpStreamOpened(httpStreamActorAdapter.ref(), message));
        }

        @Suspendable
        private void blockSessionRequests() throws InterruptedException {
            while (!gate.compareAndSet(null, new CountDownLatch(1))) {
                final CountDownLatch l = gate.get();
                if (l != null)
                    l.await();
            }
            final HttpServerExchange xch1 = xch;
            cancelTask = ts.schedule(new Runnable() {
                @Override
                public void run() {
                    sendHttpResponse(xch1, StatusCodes.INTERNAL_SERVER_ERROR, "Timeout while waiting for user actor to reply.");
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
    }

    private static final class HttpChannelAdapter implements SendPort<HttpResponse> {
        private HttpActorAdapter actor;

        HttpChannelAdapter() {}

        @Override
        @Suspendable
        public final void send(HttpResponse message) throws SuspendExecution, InterruptedException {
            trySend(message);
        }

        @Override
        @Suspendable
        public final boolean send(HttpResponse message, long timeout, TimeUnit unit) throws SuspendExecution, InterruptedException {
            send(message);
            return true;
        }

        @Override
        @Suspendable
        public final boolean send(HttpResponse message, Timeout timeout) throws SuspendExecution, InterruptedException {
            return send(message, timeout.nanosLeft(), TimeUnit.NANOSECONDS);
        }

        @Override
        @Suspendable
        public final boolean trySend(final HttpResponse m) {
            try {
                actor.handleReply(m);
            } catch (final InterruptedException e) {
                throw new RuntimeException(e);
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
            UndertowLogger.ROOT_LOGGER.error("Exception while closing HTTP adapter", t);
            if (actor != null)
                actor.die(t);
        }
    }

    private static final class HttpStreamActorAdapter extends FakeActor<WebDataMessage> {
        private HttpStreamChannelAdapter adapter;

        private volatile boolean dead;

        HttpStreamActorAdapter(HttpServerExchange xch) {
            super(xch.toString(), new HttpStreamChannelAdapter(xch));
            adapter = (HttpStreamChannelAdapter) (SendPort) mailbox();
            adapter.actor = this;
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

            adapter = null;
        }

        @Override
        public final String toString() {
            return "HttpStreamActorAdapter{request + " + getName() + "}";
        }

        public final void setChannel(StreamSinkChannel channel) {
            adapter.setChannel(channel);
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
        final int size = (int) Buffers.remaining(payload);
        if (size == 0) {
            return Buffers.EMPTY_BYTE_BUFFER;
        }
        final ByteBuffer buffer = ByteBuffer.allocate(size);
        for (ByteBuffer buf : payload) {
            buffer.put(buf);
        }
        buffer.flip();
        return buffer;
    }
}
