/*
 * COMSAT
 * Copyright (C) 2013, Parallel Universe Software Co. All rights reserved.
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
import co.paralleluniverse.comsat.webactors.Cookie;
import co.paralleluniverse.comsat.webactors.HttpRequest;
import co.paralleluniverse.comsat.webactors.HttpResponse;
import co.paralleluniverse.comsat.webactors.WebDataMessage;
import co.paralleluniverse.comsat.webactors.WebMessage;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.Timeout;
import co.paralleluniverse.strands.channels.SendPort;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Enumeration;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
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
public class WebActorServlet extends HttpServlet implements HttpSessionListener {
    static final String ACTOR_KEY = "co.paralleluniverse.actor";
    static final String ACTOR_CLASS_PARAM = "actor";
    static final String ACTOR_PARAM_PREFIX = "actorParam";
    private String redirectPath = null;
    private String actorClassName = null;
    private String[] actorParams = null;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        Enumeration<String> initParameterNames = config.getInitParameterNames();
        SortedMap<Integer, String> map = new TreeMap<>();
        while (initParameterNames.hasMoreElements()) {
            String name = initParameterNames.nextElement();
            if (name.equals("redirectNoSessionPath"))
                setRedirectNoSessionPath(config.getInitParameter(name));
            else if (name.equals(ACTOR_CLASS_PARAM))
                setActorClassName(config.getInitParameter(name));
            else if (name.startsWith(ACTOR_PARAM_PREFIX)) {
                try {
                    int num = Integer.parseInt(name.substring("actorParam".length()));
                    map.put(num, config.getInitParameter(name));
                } catch (NumberFormatException nfe) {
                    getServletContext().log("Wrong actor parameter number: ", nfe);
                }

            }
        }
        actorParams = map.values().toArray(new String[0]);
    }

    public WebActorServlet setRedirectNoSessionPath(String path) {
        this.redirectPath = path;
        return this;
    }

    public WebActorServlet setActorClassName(String className) {
        this.actorClassName = className;
        return this;
    }

    static <T> ActorRef<T> attachWebActor(HttpSession session, ActorRef<T> actor) {
        synchronized (session) {
            Object oldActor;
            if ((oldActor = session.getAttribute(ACTOR_KEY)) != null)
                return (ActorRef<T>) oldActor;
            session.setAttribute(ACTOR_KEY, new HttpActorRef(session, (ActorRef<HttpRequest>) actor));
            return actor;
        }
    }

    static boolean isWebActorAttached(HttpSession session) {
        synchronized (session) {
            return (session.getAttribute(ACTOR_KEY) != null);
        }
    }

    static HttpActorRef getHttpActorRef(HttpSession session) {
        Object actor = session.getAttribute(ACTOR_KEY);
        if ((actor != null) && (actor instanceof ActorRef))
            return (HttpActorRef) actor;
        return null;
    }

    static ActorRef<? super HttpRequest> getWebActor(HttpSession session) {
        HttpActorRef har = getHttpActorRef(session);
        return har != null ? har.webActor : null;
    }

    @Override
    public void sessionCreated(HttpSessionEvent se) {
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent se) {
        HttpActorRef ha = getHttpActorRef(se.getSession());
        ha.die(null);
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpActorRef ha = getHttpActorRef(req.getSession());

        if (ha == null) {
            if (actorClassName != null) {
                try {
                    ActorRef<WebMessage> actor = (ActorRef<WebMessage>) Actor.newActor(new ActorSpec(Class.forName(actorClassName), actorParams)).spawn();
                    attachWebActor(req.getSession(), actor);
                    ha = getHttpActorRef(req.getSession());
                } catch (ClassNotFoundException ex) {
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

        ha.service(req, resp);
    }

    static class HttpActorRef extends FakeActor<HttpResponse> {
        private final HttpSession session;
        private final ActorRef<? super HttpRequest> webActor;
        private volatile boolean dead;

        public HttpActorRef(HttpSession session, ActorRef<? super HttpRequest> webActor) {
            super(session.toString(), new HttpChannel());

            this.session = session;
            this.webActor = webActor;
            watch(webActor);
        }

        void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            if (isDone()) {
                Throwable deathCause = getDeathCause();
                req.getSession().removeAttribute(ACTOR_KEY);
                if (deathCause != null)
                    resp.sendError(500, "Actor is dead because of " + deathCause.getMessage());
                else
                    resp.sendError(500, "Actor is finised.");
                return;
            }

            req.setAttribute("org.apache.catalina.ASYNC_SUPPORTED", true);
            req.startAsync();
            try {
                webActor.send(new ServletHttpRequest(this, req, resp));
            } catch (SuspendExecution ex) {
                req.getServletContext().log("Exception: ", ex);
            }
        }

        ActorRef<? super HttpRequest> getWebActor() {
            return webActor;
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
        public void interrupt() {
            die(new InterruptedException());
        }

        @Override
        protected void die(Throwable cause) {
            if (dead)
                return;
            this.dead = true;
            super.die(cause);
            try {
                session.invalidate();
            } catch (Exception e) {
            }
        }

        private void log(String message) {
            session.getServletContext().log(message);
        }

        private void log(String message, Throwable t) {
            session.getServletContext().log(message, t);
        }
    }

    private static class HttpChannel implements SendPort<HttpResponse> {
        private Throwable exception;

        HttpChannel() {
        }

        @Override
        public void send(HttpResponse message) throws SuspendExecution, InterruptedException {
            if (!trySend(message)) {
//                if (exception == null)
//                    throw new ChannelClosedException(this, exception);
//                throw Exceptions.rethrow(exception);
            }
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
            final ServletHttpRequest req = (ServletHttpRequest) message.getRequest();
            final HttpServletRequest request = req.request;
            if (!request.isAsyncStarted())
                return false;

            final AsyncContext ctx = request.getAsyncContext();
            final HttpServletResponse response = (HttpServletResponse) ctx.getResponse();
            try {
                if (message instanceof HttpResponse) {
                    final HttpResponse msg = (HttpResponse) message;
                    if (!response.isCommitted()) {
                        if (msg.getCookies() != null) {
                            for (Cookie wc : msg.getCookies())
                                response.addCookie(getServletCookie(wc));
                        }
                        if (msg.getHeaders() != null) {
                            for (Map.Entry<String, String> h : msg.getHeaders().entries())
                                response.addHeader(h.getKey(), h.getValue());
                        }
                    }

                    if (msg.getStatus() >= 400 && msg.getStatus() < 600) {
                        response.sendError(msg.getStatus(), msg.getError() != null ? msg.getError().toString() : null);
                        close();
                        return true;
                    }

                    if (msg.getRedirectPath() != null) {
                        response.sendRedirect(msg.getRedirectPath());
                        close();
                        return true;
                    }

                    if (!response.isCommitted()) {
                        response.setStatus(msg.getStatus());

                        if (msg.getContentType() != null)
                            response.setContentType(msg.getContentType());
                        if (msg.getCharacterEncoding() != null)
                            response.setCharacterEncoding(msg.getCharacterEncoding().name());
                    }
                }
                ServletOutputStream out = writeBody(message, response, req.shouldClose());
                out.flush(); // commits the response

                if (req.shouldClose()) {
                    out.close();
                    ctx.complete();
                }
                return true;
            } catch (IOException ex) {
                request.getServletContext().log("IOException", ex);
                ctx.complete();
                this.exception = ex;
                return false;
            }
        }

        @Override
        public void close() {
            throw new UnsupportedOperationException();
        }
    }

    static SendPort<WebDataMessage> openChannel(final HttpServletRequest request, final HttpServletResponse response) {
        return new SendPort<WebDataMessage>() {
            private final AsyncContext ctx = request.getAsyncContext();

            @Override
            public void send(WebDataMessage message) throws SuspendExecution, InterruptedException {
                if (!trySend(message)) {
//                if (exception == null)
//                    throw new ChannelClosedException(this, exception);
//                throw Exceptions.rethrow(exception);
                }
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
                } catch (IOException ex) {
                    // may be caused when client closed the connection
                    // TODO: what about other cases ? who will close the connection ?
                    return false;
                }
                return true;
            }

            @Override
            public void close() {
                try {
                    ServletOutputStream os = response.getOutputStream();
                        os.close();
                } catch (IOException e) {
                    ctx.getRequest().getServletContext().log("error", e);
                }
                ctx.complete();
            }
        };
    }

    static ServletOutputStream writeBody(WebMessage message, HttpServletResponse response, boolean shouldClose) throws IOException {
        final byte[] arr;
        final int offset;
        final int length;
        ByteBuffer bb;
        String sb;
        if ((bb = message.getByteBufferBody()) != null) {
//                        WritableByteChannel wc = Channels.newChannel(out);
//                        wc.write(bb);
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
