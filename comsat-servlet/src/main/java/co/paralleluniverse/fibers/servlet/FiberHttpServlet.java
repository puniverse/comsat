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

import co.paralleluniverse.common.util.SystemProperties;
import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.Suspendable;
import co.paralleluniverse.strands.SuspendableRunnable;
import java.io.IOException;
import java.util.concurrent.ForkJoinPool;
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
    static final Long timeout = timeoutS != null ? Long.parseLong(timeoutS) : 120000L;

    static final boolean debugBypassToRegularJFP = SystemProperties.isEmptyOrTrue(FiberHttpServlet.class.getName() + ".debug.bypassToRegularFJP");

    static final boolean disableSyncExceptionsEmulation = SystemProperties.isEmptyOrTrue(FiberHttpServlet.class.getName() + ".disableSyncExceptionsEmulation");
    static final boolean disableSyncForwardEmulation = SystemProperties.isEmptyOrTrue(FiberHttpServlet.class.getName() + ".disableSyncForwardEmulation");
    static final boolean disableJettyAsyncFixes = SystemProperties.isEmptyOrTrue(FiberHttpServlet.class.getName() + ".disableJettyAsyncFixes");
    static final boolean disableTomcatAsyncFixes = SystemProperties.isEmptyOrTrue(FiberHttpServlet.class.getName() + ".disableTomcatAsyncFixes");

    private static final long serialVersionUID = 1L;

    private static final String FIBER_ASYNC_REQUEST_EXCEPTION = "co.paralleluniverse.fibers.servlet.exception";

    private final ThreadLocal<AsyncContext> currentAsyncContext = new ThreadLocal<>();

    private int stackSize = -1;

    private transient FiberServletContext contextAD;

    private ForkJoinPool fjp = new ForkJoinPool();

    public FiberHttpServlet() {
        System.err.println("Async timeout: " + timeout);
        System.err.println("Debug mode, bypass to regular JFP: " + debugBypassToRegularJFP);
        System.err.println("Disable sync exceptions emulation: " + disableSyncExceptionsEmulation);
        System.err.println("Disable sync redirects emulation: " + disableSyncForwardEmulation);
        System.err.println("Disable Jetty async fixes: " + disableJettyAsyncFixes);
    }

    /**
     * @inheritDoc
     *
     * @return Wrapped version of the ServletContext initiated by {@link #init(javax.servlet.ServletConfig) }
     */
    @Override
    public ServletContext getServletContext() {
        return
            !FiberHttpServlet.disableSyncForwardEmulation ?
                contextAD : super.getServletContext();
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        this.contextAD = new FiberServletContext(config.getServletContext(), currentAsyncContext);

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

    @Suspendable
    static void exec(FiberHttpServlet servlet, AsyncContext ac, HttpServletRequest request, HttpServletResponse response) {
        if (!disableSyncExceptionsEmulation) {
            try {
                exec0(servlet, ac, request, response);
            } catch (final ServletException | IOException ex) {
                // Multi-catch above seems to break ASM during instrumentation in some circumstances
                // seemingly tied to structured class-loading, as in standalone servlet containers
                servlet.log("Exception in servlet's fiber, dispatching to container", ex);
                request.setAttribute(FIBER_ASYNC_REQUEST_EXCEPTION, ex);
                if (!disableSyncForwardEmulation)
                    servlet.currentAsyncContext.set(null);
                ac.dispatch();
            }
        } else {
            try {
                exec0(servlet, ac, request, response);
            } catch (final Throwable t) {
                servlet.log("Error during pool-based execution", t);
                ((HttpServletResponse) ac.getResponse()).setStatus(500);
                ac.complete();
            }
        }
    }

    private static void exec0(FiberHttpServlet servlet, AsyncContext ac, HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // TODO: check if ac has expired
        if (!disableSyncForwardEmulation)
            servlet.currentAsyncContext.set(ac);
        servlet.service(request, response);
        ac.complete();
    }

    @Override
    @Suspendable
    final public void service(final ServletRequest req, ServletResponse res) throws ServletException, IOException {
        if (!disableSyncExceptionsEmulation && DispatcherType.ASYNC.equals(req.getDispatcherType())) {
            final Throwable ex = (Throwable) req.getAttribute(FIBER_ASYNC_REQUEST_EXCEPTION);
            if (ex != null)
                throw new ServletException(ex);
        }

        final HttpServletRequest request;
        final HttpServletResponse response;
        try {
             request = (HttpServletRequest) req;
             response = (HttpServletResponse) res;
        } catch (final ClassCastException cce) {
            throw new ServletException("Unsupported non-HTTP request or response detected");
        }

        if (!disableTomcatAsyncFixes)
            req.setAttribute("org.apache.catalina.ASYNC_SUPPORTED", true);

        final AsyncContext ac = req.startAsync();

        if (timeout != null)
            ac.setTimeout(timeout); // TODO: config

        final HttpServletRequest r =
            !disableJettyAsyncFixes ?
                new FiberHttpServletRequest(request) :
                request;
        if (debugBypassToRegularJFP)
            fjp.execute(new ServletRunnable(this, ac, r, response));
        else
            new Fiber(null, stackSize, new ServletSuspendableRunnable(this, ac, r, response)).start();
    }

    private final static class ServletSuspendableRunnable implements SuspendableRunnable {
        private final AsyncContext ac;
        private final HttpServletRequest request;
        private final HttpServletResponse response;
        private final FiberHttpServlet servlet;

        public ServletSuspendableRunnable(FiberHttpServlet servlet, AsyncContext ac, HttpServletRequest request, HttpServletResponse response) {
            this.servlet = servlet;
            this.ac = ac;
            this.request = request;
            this.response = response;
        }

        @Override
        public final void run() throws SuspendExecution, InterruptedException {
            exec(servlet, ac, request, response);
        }
    }

    private static class ServletRunnable implements Runnable {
        private final FiberHttpServlet servlet;
        private final AsyncContext ac;
        private final HttpServletRequest request;
        private final HttpServletResponse response;

        public ServletRunnable(FiberHttpServlet servlet, AsyncContext ac, HttpServletRequest request, HttpServletResponse response) {
            this.servlet = servlet;
            this.ac = ac;
            this.request = request;
            this.response = response;
        }

        @Override
        public final void run() {
            exec(servlet, ac, request, response);
        }
    }
}
