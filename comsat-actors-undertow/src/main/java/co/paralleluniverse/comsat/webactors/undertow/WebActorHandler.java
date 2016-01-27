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
import co.paralleluniverse.common.util.SystemProperties;
import co.paralleluniverse.comsat.webactors.*;
import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.FiberUtil;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.Suspendable;
import co.paralleluniverse.strands.SuspendableRunnable;
import co.paralleluniverse.strands.Timeout;
import co.paralleluniverse.strands.channels.SendPort;
import co.paralleluniverse.strands.concurrent.ReentrantLock;
import com.google.common.base.Charsets;
import io.undertow.Handlers;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.CookieImpl;
import io.undertow.server.session.*;
import io.undertow.util.Headers;
import io.undertow.util.HttpString;
import io.undertow.util.StatusCodes;
import io.undertow.util.StringReadChannelListener;
import io.undertow.websockets.WebSocketConnectionCallback;
import io.undertow.websockets.core.*;
import io.undertow.websockets.spi.WebSocketHttpExchange;
import org.xnio.Buffers;
import org.xnio.ChannelListener;
import org.xnio.ChannelListeners;
import org.xnio.channels.StreamSinkChannel;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
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

		void invalidate();

		ActorRef<? extends WebMessage> getRef();
		Class<? extends ActorImpl<? extends WebMessage>> getWebActorClass();

		ReentrantLock getLock();

		Map<String, Object> getAttachments();
	}


	public static abstract class DefaultContextImpl implements Context {
		private final static String durationProp = System.getProperty(DefaultContextImpl.class.getName() + ".durationMillis");
		private final static long DURATION = durationProp != null ? Long.parseLong(durationProp) : 60_000l;
		final Map<String, Object> attachments = new HashMap<>();
		private final ReentrantLock lock = new ReentrantLock();
		private final long created;
		private boolean valid = true;

		public DefaultContextImpl() {
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

	private static final WeakHashMap<Class<?>, List<Pair<String, String>>> classToUrlPatterns = new WeakHashMap<>();

	private final SessionAttachmentHandler sessionHandler;
	private final ContextProvider selector;

	public WebActorHandler(ContextProvider selector) {
		this.selector = selector;
		// this.continueHandler = Handlers.httpContinueRead(null);
		SessionManager sessionManager = new InMemorySessionManager("SESSION_MANAGER");
		SessionCookieConfig sessionConfig = new SessionCookieConfig();
		this.sessionHandler = new SessionAttachmentHandler(sessionManager, sessionConfig);
	}

	@Override
	public void handleRequest(final HttpServerExchange xch) throws Exception {
		// continueHandler.handleRequest(xch);
		sessionHandler.handleRequest(xch);

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
				if (handlesWithWebSocket(uri, context.getWebActorClass())) {
					if (internalActor == null || !(internalActor instanceof WebSocketActorAdapter)) {

						@SuppressWarnings("unchecked") final ActorRef<WebMessage> userActorRef0 = (ActorRef<WebMessage>) userActorRef;
						internalActor = new WebSocketActorAdapter(userActorRef0);

						//noinspection unchecked
						addActorToContextAndUnlock(context, internalActor, lock);
					}

					final WebSocketActorAdapter webSocketActor = (WebSocketActorAdapter) internalActor;
					final ActorRef userActorRef0 = userActorRef;

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
										userActorRef0.send(new WebSocketOpened(webSocketActor.ref()));
									}
								});
							} catch (InterruptedException | ExecutionException e) {
								throw new RuntimeException(e);
							}
						}
					}).handleRequest(xch);

					return;
				} else if (handlesWithHttp(uri, context.getWebActorClass())) {
					xch.dispatch(); // Start async

					//noinspection ConstantConditions
					if (internalActor == null || !(internalActor instanceof HttpActorAdapter)) {
						//noinspection unchecked
						internalActor = new HttpActorAdapter(context, (ActorRef<HttpRequest>) userActorRef);
						addActorToContextAndUnlock(context, internalActor, lock);
					}

					//noinspection ConstantConditions
					((HttpActorAdapter) internalActor).service(xch);
					return;
				}
			}

			sendHttpResponse(xch, StatusCodes.NOT_FOUND);
		} finally {
			if (lock.isHeldByCurrentStrand() && lock.isLocked())
				lock.unlock();
		}
	}

	private void addActorToContextAndUnlock(Context context, ActorImpl actor, ReentrantLock lock) {
		context.getAttachments().put(ACTOR_KEY, actor);
		lock.unlock();
	}

	private static class WebSocketActorAdapter extends FakeActor<WebDataMessage> {
		final ActorRef<? super WebMessage> webActor;
		private final WebSocketChannelAdapter channelAdapter;

		private WebSocketChannel channel;

		public WebSocketActorAdapter(ActorRef<? super WebMessage> webActor) {
			super(webActor.getName(), new WebSocketChannelAdapter());
			this.channelAdapter = (WebSocketChannelAdapter) (SendPort) mailbox();
			this.webActor = webActor;
			watch(webActor);
		}

		void setChannel(WebSocketChannel channel) {
			this.channel = channel;
			this.channelAdapter.channel = channel;
		}

		void onMessage(BufferedBinaryMessage message) {
			try {
				webActor.send(new WebDataMessage(ref(), toBuffer(message.getData().getResource()).duplicate()));
			} catch (SuspendExecution ex) {
				throw new AssertionError(ex);
			}
		}

		void onMessage(BufferedTextMessage message) {
			try {
				webActor.send(new WebDataMessage(ref(), message.getData()));
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
			try {
				channel.sendClose();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public String toString() {
			return "WebSocketActor{" + "webActor=" + webActor + '}';
		}
	}

	private static class WebSocketChannelAdapter implements SendPort<WebDataMessage> {
		WebSocketChannel channel;

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
				WebSockets.sendText(message.getStringBody(), channel, null);
			else
				WebSockets.sendBinary(message.getByteBufferBody(), channel, null);
			return true;
		}

		@Override
		public void close() {
			try {
				channel.sendClose();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public void close(Throwable t) {
			close();
		}
	}

	private static class HttpActorAdapter extends FakeActor<HttpResponse> {
		final ActorRef<? super HttpRequest> webActor;
		private final Context context;
		private volatile boolean dead;

		public HttpActorAdapter(Context context, ActorRef<? super HttpRequest> webActor) {
			super(webActor.getName(), new HttpChannelAdapter(context));

			this.context = context;
			this.webActor = webActor;
			watch(webActor);
		}

		void service(final HttpServerExchange xch) throws SuspendExecution {
			if (isDone()) {
				@SuppressWarnings("ThrowableResultOfMethodCallIgnored") final Throwable deathCause = getDeathCause();
				if (deathCause != null)
					sendHttpResponse(xch, StatusCodes.INTERNAL_SERVER_ERROR, "Actor is dead because of " + deathCause.getMessage());
				else
					sendHttpResponse(xch, StatusCodes.INTERNAL_SERVER_ERROR, "Actor has finished");
				return;
			}

			final String charset = xch.getRequestCharset();
			final StringReadChannelListener l = new StringReadChannelListener(xch.getConnection().getBufferPool()) {
				@Override
				protected void stringDone(final String s) {
					new Fiber(new SuspendableRunnable() {
						@Override
						public void run() throws SuspendExecution, InterruptedException {
							try {
								webActor.send(new HttpRequestWrapper(ref(), xch, ByteBuffer.wrap(charset != null ? s.getBytes(charset) : s.getBytes(Charsets.ISO_8859_1.name()))));
							} catch (final UnsupportedEncodingException ex) {
								throw new RuntimeException(ex);
							}
						}
					}).start();
				}

				@Override
				protected void error(IOException e) {
					throw new RuntimeException(e);
				}
			};
			l.setup(xch.getRequestChannel());
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
			context.invalidate();
		}

		@Override
		public String toString() {
			return "HttpActorAdapter{" + "webActor=" + webActor + '}';
		}
	}

	private static class HttpChannelAdapter implements SendPort<HttpResponse> {
		private final static boolean trackSessionOnlyForSSE = SystemProperties.isEmptyOrTrue(HttpChannelAdapter.class.getName() + ".trackSessionOnlyForSSE");

		private final Context context;

		public HttpChannelAdapter(Context context) {
			this.context = context;
		}

		private static void startSession(HttpServerExchange xch, Context context) {
			SessionManager sm = xch.getAttachment(SessionManager.ATTACHMENT_KEY);
			SessionConfig sessionConfig = xch.getAttachment(SessionConfig.ATTACHMENT_KEY);
			Session session = sm.getSession(xch, sessionConfig);
			if (session == null)
				session = sm.createSession(xch, sessionConfig);
			session.setAttribute(ACTOR_KEY, context);
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
		public boolean trySend(final HttpResponse message) {
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
				for (Cookie c : message.getCookies())
					xch.setResponseCookie(newUndertowCookie(c));
			}
			if (message.getHeaders() != null) {
				for (Map.Entry<String, String> h : message.getHeaders().entries())
					xch.getResponseHeaders().add(new HttpString(h.getKey()), h.getValue());
			}

			if (message.getContentType() != null) {
				String ct = message.getContentType();
				if (message.getCharacterEncoding() != null)
					ct = ct + "; charset=" + message.getCharacterEncoding().name();
				xch.getResponseHeaders().add(new HttpString("Content-Type"), ct);
			}

			// This will copy the request content, which must still be referenceable, doing before the request handler
			// unallocates it (unfortunately it is explicitly reference-counted in Netty)
			final HttpStreamActorAdapter httpStreamActorAdapter = new HttpStreamActorAdapter(xch);

			final boolean sseStarted = message.shouldStartActor();
			if (sseStarted || !trackSessionOnlyForSSE)
				startSession(xch, context);

			if (sseStarted) {
				try {
					xch.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/event-stream; charset=UTF-8");
					xch.setPersistent(false);

					final StreamSinkChannel sink = xch.getResponseChannel();
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
								} catch (InterruptedException | ExecutionException e) {
									throw new RuntimeException(e);
								}
							}
						}, null));
						sink.resumeWrites();
					} else {
						handleSSEStart(httpStreamActorAdapter, message, sink);
					}
				} catch (Exception e) {
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
		public void close() {
			throw new UnsupportedOperationException();
		}

		@Override
		public void close(Throwable t) {
			throw new UnsupportedOperationException();
		}
	}

	private static class HttpStreamActorAdapter extends FakeActor<WebDataMessage> {
		private final HttpStreamChannelAdapter channelAdapter;

		private volatile boolean dead;

		public HttpStreamActorAdapter(HttpServerExchange xch) {
			super(xch.toString(), new HttpStreamChannelAdapter(xch));
			((HttpStreamChannelAdapter) (SendPort) mailbox()).actor = this;
			this.channelAdapter = (HttpStreamChannelAdapter) (SendPort) mailbox();
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

		public void setChannel(StreamSinkChannel channel) {
			this.channelAdapter.channel = channel;
		}
	}

	private static class HttpStreamChannelAdapter implements SendPort<WebDataMessage> {
		final HttpServerExchange xch;

		HttpStreamActorAdapter actor;
		StreamSinkChannel channel;

		public HttpStreamChannelAdapter(HttpServerExchange xch) {
			this.xch = xch;
		}

		@Override
		@Suspendable
		public void send(WebDataMessage message) throws SuspendExecution, InterruptedException {
			trySend(message);
		}

		@Override
		@Suspendable
		public boolean send(WebDataMessage message, long timeout, TimeUnit unit) throws SuspendExecution, InterruptedException {
			send(message);
			return true;
		}

		@Override
		@Suspendable
		public boolean send(WebDataMessage message, Timeout timeout) throws SuspendExecution, InterruptedException {
			return send(message, timeout.nanosLeft(), TimeUnit.NANOSECONDS);
		}

		@Override
		@Suspendable
		public boolean trySend(final WebDataMessage res) {
			try {
				final String stringBody = res.getStringBody();
				final String charset = xch.getRequestCharset();
				try {
					if (stringBody != null) {
						if (charset != null)
							new FiberWriteChannelListener(stringBody, Charset.forName(charset), channel).run();
						else
							new FiberWriteChannelListener(stringBody, channel).run();
					} else {
						new FiberWriteChannelListener(res.getByteBufferBody(), channel).run();
					}
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			} catch (SuspendExecution e) {
				throw new AssertionError(e);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
			return true;
		}

		@Override
		public void close() {
			xch.endExchange();
		}

		@Override
		public void close(Throwable t) {
			close();
		}
	}

	protected static boolean handlesWithHttp(String uri, Class<?> actorClass) {
		return match(uri, actorClass).equals("http");
	}

	protected static boolean handlesWithWebSocket(String uri, Class<?> actorClass) {
		return match(uri, actorClass).equals("ws");
	}

	private static void sendHttpResponse(HttpServerExchange xch, int statusCode) {
		sendHttpResponse(xch, statusCode, (String) null);
	}

	private static void sendHttpResponse(HttpServerExchange xch, int statusCode, String body) {
		xch.setResponseCode(statusCode);
		if (body != null)
			xch.getResponseSender().send(body);
		xch.endExchange();
	}

	private static void sendHttpResponse(HttpServerExchange xch, int statusCode, ByteBuffer body) {
		xch.setResponseCode(statusCode);
		if (body != null)
			xch.getResponseSender().send(body);
		xch.endExchange();
	}

	private static void sendHttpRedirect(HttpServerExchange xch, String path) {
		xch.setResponseCode(StatusCodes.FOUND);
		xch.getResponseHeaders().add(Headers.LOCATION, xch.getProtocol() + "://" + xch.getHostAndPort() + path);
		xch.endExchange();
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
