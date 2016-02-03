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
package co.paralleluniverse.fibers.servlet;

import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.Suspendable;
import co.paralleluniverse.strands.SuspendableRunnable;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ResourceBundle;
import javax.servlet.AsyncContext;
import javax.servlet.DispatcherType;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Fiber-blocking HttpServlet base class.
 * 
 * @author eithan
 * @author circlespainter
 */
public class FiberHttpServlet extends HttpServlet {
    private static final String timeoutS = System.getProperty(FiberHttpServlet.class.getName() + ".asyncTimeout");
    private static final Long timeout = timeoutS != null ? Long.parseLong(timeoutS) : 120000L;

    private static final long serialVersionUID = 1L;

    private static final String FIBER_ASYNC_REQUEST_EXCEPTION = "co.paralleluniverse.fibers.servlet.exception";

    private final ThreadLocal<AsyncContext> currentAsyncContext = new ThreadLocal<>();

    private int stackSize = -1;

    private transient FiberServletConfig configAD;
    private transient FiberServletContext contextAD;

    /**
     * @inheritDoc
     *
     * @return Wrapped version of the ServletConfig initiated by {@link #init(javax.servlet.ServletConfig) }
     */
    @Override
    public final ServletConfig getServletConfig() {
        return configAD;
    }

    /**
     * @inheritDoc
     *
     * @return Wrapped version of the ServletContext initiated by {@link #init(javax.servlet.ServletConfig) }
     */
    @Override
    public ServletContext getServletContext() {
        return contextAD;
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        this.contextAD = new FiberServletContext(config.getServletContext(), currentAsyncContext);
        this.configAD = new FiberServletConfig(config, contextAD);

        final String sss = config.getInitParameter("stack-size");
        if (sss != null)
            stackSize = Integer.parseInt(sss);

        this.init();
    }

    protected final void setStackSize(int stackSize) {
        this.stackSize = stackSize;
    }

    protected final int getStackSize() {
        return stackSize;
    }

    @Override
    @Suspendable
    final public void service(final ServletRequest req, ServletResponse res) throws ServletException, IOException {
        if (DispatcherType.ASYNC.equals(req.getDispatcherType())) {
            final Throwable ex = (Throwable) req.getAttribute(FIBER_ASYNC_REQUEST_EXCEPTION);
            if (ex != null) {
                log("Being dispatched exception produced in fiber; now in container's thread, wrapping in ServletException and throwing", ex);
                throw new ServletException(ex);
            }
        }

        if (!(req instanceof HttpServletRequest
            && res instanceof HttpServletResponse)) {
            throw new ServletException("Only HTTP is supported, but detected non-HTTP request or response");
        }

        final HttpServletRequest request = (HttpServletRequest) req;
        final HttpServletResponse response = (HttpServletResponse) res;

        req.setAttribute("org.apache.catalina.ASYNC_SUPPORTED", true);
        final AsyncContext ac = req.startAsync();
        if (timeout != null)
            ac.setTimeout(timeout); // TODO: config

        final FiberHttpServletRequest srad = new FiberHttpServletRequest(request);
        new Fiber(null, stackSize, new ServletSuspendableRunnable(this, ac, srad, response, req, request)).start();
    }

    private static final ResourceBundle lStrings = ResourceBundle.getBundle("javax.servlet.http.LocalStrings");
    private static final String errMsg = lStrings.getString("http.method_not_implemented");

    @Suspendable
    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        final String method = req.getMethod();

        if ("GET".equals(method)) {
            long lastModified = getLastModified(req);
            if (lastModified == -1) {
                // servlet doesn't support if-modified-since, no reason
                // to go through further expensive logic
                doGet(req, resp);
            } else {
                long ifModifiedSince = req.getDateHeader("If-Modified-Since");
                if (ifModifiedSince < lastModified) {
                    // If the servlet mod time is later, call doGet()
                    // Round down to the nearest second for a proper compare
                    // A ifModifiedSince of -1 will always be less
                    maybeSetLastModified(resp, lastModified);
                    doGet(req, resp);
                } else {
                    resp.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
                }
            }
        } else if ("HEAD".equals(method)) {
            long lastModified = getLastModified(req);
            maybeSetLastModified(resp, lastModified);
            doHead(req, resp);
        } else if ("POST".equals(method)) {
            doPost(req, resp);
        } else if ("PUT".equals(method)) {
            doPut(req, resp);
        } else if ("DELETE".equals(method)) {
            doDelete(req, resp);
        } else if ("OPTIONS".equals(method)) {
            doOptions(req, resp);
        } else if ("TRACE".equals(method)) {
            doTrace(req, resp);
        } else {//
            // Note that this means NO servlet supports whatever
            // method was requested, anywhere on this server.
            final Object[] errArgs = new Object[1];
            errArgs[0] = method;
            final String err = MessageFormat.format(errMsg, errArgs);
            resp.sendError(HttpServletResponse.SC_NOT_IMPLEMENTED, err);
        }
    }

    private static final String HEADER_LASTMOD = "Last-Modified";

    private void maybeSetLastModified(HttpServletResponse resp, long lastModified) {
        if (resp.containsHeader(HEADER_LASTMOD))
            return;
        if (lastModified >= 0)
            resp.setDateHeader(HEADER_LASTMOD, lastModified);
    }

    private final static class ServletSuspendableRunnable implements SuspendableRunnable {
        private final AsyncContext ac;
        private final FiberHttpServletRequest srad;
        private final HttpServletResponse response;
        private final ServletRequest req;
        private final HttpServletRequest request;
        private final FiberHttpServlet servlet;

        public ServletSuspendableRunnable(FiberHttpServlet servlet, AsyncContext ac, FiberHttpServletRequest srad, HttpServletResponse response, ServletRequest req, HttpServletRequest request) {
            this.servlet = servlet;
            this.ac = ac;
            this.srad = srad;
            this.response = response;
            this.req = req;
            this.request = request;
        }

        @Override
        public final void run() throws SuspendExecution, InterruptedException {
            try {
                // TODO: check if ac has expired
                servlet.currentAsyncContext.set(ac);
                servlet.service(srad, response);
                if (req.isAsyncStarted())
                    ac.complete();
            } catch (final ServletException | IOException ex) {
                // Multi-catch above seems to break ASM during instrumentation in some circumstances
                // seemingly tied to structured class-loading, as in standalone servlet containers
                servlet.log("Exception in servlet's fiber, dispatching to container", ex);
                request.setAttribute(FIBER_ASYNC_REQUEST_EXCEPTION, ex);
                servlet.currentAsyncContext.set(null);
                if (req.isAsyncStarted())
                    ac.dispatch();
            }
        }
    }
}
