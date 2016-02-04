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
    private static final String PROP_ASYNC_TIMEOUT = FiberHttpServlet.class.getName() + ".asyncTimeout";
    static final Long asyncTimeout;

    public static final String PROP_DEBUG_BYPASS_TO_REGULAR_FJP = FiberHttpServlet.class.getName() + ".debug.bypassToRegularFJP";
    static final boolean debugBypassToRegularJFPGlobal = SystemProperties.isEmptyOrTrue(PROP_DEBUG_BYPASS_TO_REGULAR_FJP);

    public static final String PROP_DISABLE_SYNC_EXCEPTIONS = FiberHttpServlet.class.getName() + ".disableSyncExceptions";
    static final boolean disableSyncExceptionsGlobal = SystemProperties.isEmptyOrTrue(PROP_DISABLE_SYNC_EXCEPTIONS);
    public static final String PROP_DISABLE_SYNC_FORWARD = FiberHttpServlet.class.getName() + ".disableSyncForward";
    static final boolean disableSyncForwardGlobal = SystemProperties.isEmptyOrTrue(PROP_DISABLE_SYNC_FORWARD);

    public static final String PROP_DISABLE_JETTY_ASYNC_FIXES = FiberHttpServlet.class.getName() + ".disableJettyAsyncFixes";
    static final Boolean disableJettyAsyncFixesGlobal;
    public static final String PROP_DISABLE_TOMCAT_ASYNC_FIXES = FiberHttpServlet.class.getName() + ".disableTomcatAsyncFixes";
    static final Boolean disableTomcatAsyncFixesGlobal;

    static {
        asyncTimeout = getLong(PROP_ASYNC_TIMEOUT);
        disableJettyAsyncFixesGlobal = getBoolean(PROP_DISABLE_JETTY_ASYNC_FIXES);
        disableTomcatAsyncFixesGlobal = getBoolean(PROP_DISABLE_TOMCAT_ASYNC_FIXES);
    }

    private static final long serialVersionUID = 1L;

    private static final String FIBER_ASYNC_REQUEST_EXCEPTION = "co.paralleluniverse.fibers.servlet.exception";

    private final ThreadLocal<AsyncContext> currentAsyncContext = new ThreadLocal<>();

    private int stackSize = -1;

    private transient FiberServletContext contextAD;

    private ForkJoinPool fjp = new ForkJoinPool();

    boolean debugBypassToRegularJFP, disableSyncExceptions, disableSyncForward, disableJettyAsyncFixes, disableTomcatAsyncFixes;

    /**
     * @return Wrapped version of the ServletContext initiated by {@link #init(javax.servlet.ServletConfig) }
     * @inheritDoc
     */
    @Override
    public ServletContext getServletContext() {
        return disableSyncForward ? super.getServletContext() : contextAD;
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);

        this.contextAD = new FiberServletContext(config.getServletContext(), currentAsyncContext);

        final String ss = config.getInitParameter("stack-size");
        if (ss != null)
            stackSize = Integer.parseInt(ss);

        final String debugByPassToJFP = config.getInitParameter(PROP_DEBUG_BYPASS_TO_REGULAR_FJP);
        if (debugByPassToJFP != null)
            debugBypassToRegularJFP = debugBypassToRegularJFPGlobal;

        final String disableSE = config.getInitParameter(PROP_DISABLE_SYNC_EXCEPTIONS);
        if (disableSE != null)
            disableSyncExceptions = disableSyncExceptionsGlobal;

        final String disableSF = config.getInitParameter(PROP_DISABLE_SYNC_FORWARD);
        if (disableSF != null)
            disableSyncForward = disableSyncForwardGlobal;

        final String disableJF = config.getInitParameter(PROP_DISABLE_JETTY_ASYNC_FIXES);
        if (disableJF != null)
            disableJettyAsyncFixes = disableJettyAsyncFixesGlobal != null ? disableJettyAsyncFixesGlobal : !isJetty(config);

        final String disableTF = config.getInitParameter(PROP_DISABLE_TOMCAT_ASYNC_FIXES);
        if (disableTF != null)
            disableTomcatAsyncFixes = disableTomcatAsyncFixesGlobal != null ? disableTomcatAsyncFixesGlobal : !isTomcat(config);
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
        if (!disableSyncExceptions && DispatcherType.ASYNC.equals(req.getDispatcherType())) {
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

        if (asyncTimeout != null)
            ac.setTimeout(asyncTimeout);

        final HttpServletRequest r =
            !disableJettyAsyncFixes ?
                new FiberHttpServletRequest(this, request) :
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
            servlet.exec(servlet, ac, request, response);
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
            servlet.exec(servlet, ac, request, response);
        }
    }

    @Suspendable
    final void exec(FiberHttpServlet servlet, AsyncContext ac, HttpServletRequest request, HttpServletResponse response) {
        if (!disableSyncExceptions) {
            try {
                exec0(servlet, ac, request, response);
            } catch (final ServletException | IOException ex) {
                // Multi-catch above seems to break ASM during instrumentation in some circumstances
                // seemingly tied to structured class-loading, as in standalone servlet containers
                servlet.log("Exception in servlet's fiber, dispatching to container", ex);
                request.setAttribute(FIBER_ASYNC_REQUEST_EXCEPTION, ex);
                if (!disableSyncForward)
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

    private void exec0(FiberHttpServlet servlet, AsyncContext ac, HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // TODO: check if ac has expired
        if (!disableSyncForward)
            servlet.currentAsyncContext.set(ac);
        servlet.service(request, response);
        ac.complete();
    }

    private static boolean isJetty(ServletConfig config) {
        return config.getClass().getName().startsWith("org.eclipse.jetty.");
    }

    private static boolean isTomcat(ServletConfig config) {
        return config.getClass().getName().startsWith("org.apache.tomcat.");
    }

    private static Long getLong(String propName) {
        final String asyncTimeoutS = System.getProperty(propName);
        if (asyncTimeoutS != null) {
            Long l = null;
            try {
                l = Long.parseLong(asyncTimeoutS);
            } catch (final NumberFormatException ignored) {
            }
            return l;
        } else {
            return null;
        }
    }

    private static Boolean getBoolean(String propName) {
        final String disableJettyAsyncFixesGlobalS = System.getProperty(propName);
        if (disableJettyAsyncFixesGlobalS != null) {
            Boolean b = null;
            if (Boolean.TRUE.toString().equals(disableJettyAsyncFixesGlobalS))
                b = true;
            else if (Boolean.FALSE.toString().equals(disableJettyAsyncFixesGlobalS))
                b = false;
            return b;
        } else {
            return null;
        }
    }
}
