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
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.text.MessageFormat;
import java.util.Enumeration;
import java.util.ResourceBundle;
import java.util.concurrent.ForkJoinPool;
import javax.servlet.*;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

/**
 * Fiber-blocking HttpServlet base class.
 * 
 * @author eithan
 * @author circlespainter
 */
public class FiberHttpServlet extends HttpServlet {
    private static final String METHOD_DELETE = "DELETE";
    private static final String METHOD_HEAD = "HEAD";
    private static final String METHOD_GET = "GET";
    private static final String METHOD_OPTIONS = "OPTIONS";
    private static final String METHOD_POST = "POST";
    private static final String METHOD_PUT = "PUT";
    private static final String METHOD_TRACE = "TRACE";

    private static final String HEADER_IFMODSINCE = "If-Modified-Since";
    private static final String HEADER_LASTMOD = "Last-Modified";

    private static final String LSTRING_FILE =
        "javax.servlet.http.LocalStrings";
    private static ResourceBundle lStrings =
        ResourceBundle.getBundle(LSTRING_FILE);

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
                try {
                    ac.complete();
                } catch (final IllegalStateException ignored) {}
            }
        }
    }

    @Suspendable
    private void exec0(FiberHttpServlet servlet, AsyncContext ac, HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // TODO: check if ac has expired
        if (!disableSyncForward)
            servlet.currentAsyncContext.set(ac);
        servlet.service(request, response);
        try {
            ac.complete();
        } catch (final IllegalStateException ignored) {}
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

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    // The rest is copied from HttpServlet as we don't instrument by default "java" and "javax" packages
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    @Suspendable
    protected void service(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException
    {
        final String method = req.getMethod();

        //noinspection IfCanBeSwitch
        if (method.equals(METHOD_GET)) {
            long lastModified = getLastModified(req);
            if (lastModified == -1) {
                // servlet doesn't support if-modified-since, no reason
                // to go through further expensive logic
                doGet(req, resp);
            } else {
                long ifModifiedSince = req.getDateHeader(HEADER_IFMODSINCE);
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

        } else if (method.equals(METHOD_HEAD)) {
            final long lastModified = getLastModified(req);
            maybeSetLastModified(resp, lastModified);
            doHead(req, resp);

        } else if (method.equals(METHOD_POST)) {
            doPost(req, resp);

        } else if (method.equals(METHOD_PUT)) {
            doPut(req, resp);

        } else if (method.equals(METHOD_DELETE)) {
            doDelete(req, resp);

        } else if (method.equals(METHOD_OPTIONS)) {
            doOptions(req,resp);

        } else if (method.equals(METHOD_TRACE)) {
            doTrace(req,resp);

        } else {
            //
            // Note that this means NO servlet supports whatever
            // method was requested, anywhere on this server.
            //

            String errMsg = lStrings.getString("http.method_not_implemented");
            final Object[] errArgs = new Object[1];
            errArgs[0] = method;
            errMsg = MessageFormat.format(errMsg, errArgs);

            resp.sendError(HttpServletResponse.SC_NOT_IMPLEMENTED, errMsg);
        }
    }

    @Override
    @Suspendable
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException
    {
        final String protocol = req.getProtocol();
        final String msg = lStrings.getString("http.method_get_not_supported");
        if (protocol.endsWith("1.1")) {
            resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, msg);
        } else {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, msg);
        }
    }

    @Override
    @Suspendable
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException
    {
        final String protocol = req.getProtocol();
        final String msg = lStrings.getString("http.method_post_not_supported");
        if (protocol.endsWith("1.1")) {
            resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, msg);
        } else {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, msg);
        }
    }

    @Override
    @Suspendable
    protected void doPut(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException
    {
        final String protocol = req.getProtocol();
        final String msg = lStrings.getString("http.method_put_not_supported");
        if (protocol.endsWith("1.1")) {
            resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, msg);
        } else {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, msg);
        }
    }

    @Override
    @Suspendable
    protected void doDelete(HttpServletRequest req,
                            HttpServletResponse resp)
        throws ServletException, IOException
    {
        final String protocol = req.getProtocol();
        final String msg = lStrings.getString("http.method_delete_not_supported");
        if (protocol.endsWith("1.1")) {
            resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, msg);
        } else {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, msg);
        }
    }

    @Override
    @Suspendable
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException
    {
        final Method[] methods = getAllDeclaredMethods(this.getClass());

        boolean ALLOW_GET = false;
        boolean ALLOW_HEAD = false;
        boolean ALLOW_POST = false;
        boolean ALLOW_PUT = false;
        boolean ALLOW_DELETE = false;
        final boolean ALLOW_TRACE = true;
        final boolean ALLOW_OPTIONS = true;

        for (final Method m : methods) {
            if (m.getName().equals("doGet")) {
                ALLOW_GET = true;
                ALLOW_HEAD = true;
            }
            if (m.getName().equals("doPost"))
                ALLOW_POST = true;
            if (m.getName().equals("doPut"))
                ALLOW_PUT = true;
            if (m.getName().equals("doDelete"))
                ALLOW_DELETE = true;

        }

        String allow = null;
        if (ALLOW_GET)
            allow=METHOD_GET;
        if (ALLOW_HEAD)
            if (allow==null) allow=METHOD_HEAD;
            else allow += ", " + METHOD_HEAD;
        if (ALLOW_POST)
            if (allow==null) allow=METHOD_POST;
            else allow += ", " + METHOD_POST;
        if (ALLOW_PUT)
            if (allow==null) allow=METHOD_PUT;
            else allow += ", " + METHOD_PUT;
        if (ALLOW_DELETE)
            if (allow==null) allow=METHOD_DELETE;
            else allow += ", " + METHOD_DELETE;
        if (ALLOW_TRACE)
            if (allow==null) allow=METHOD_TRACE;
            else allow += ", " + METHOD_TRACE;
        if (ALLOW_OPTIONS)
            if (allow==null) allow=METHOD_OPTIONS;
            else allow += ", " + METHOD_OPTIONS;

        resp.setHeader("Allow", allow);
    }

    private Method[] getAllDeclaredMethods(Class<?> c) {

        if (c.equals(javax.servlet.http.HttpServlet.class)) {
            return null;
        }

        final Method[] parentMethods = getAllDeclaredMethods(c.getSuperclass());
        Method[] thisMethods = c.getDeclaredMethods();

        if ((parentMethods != null) && (parentMethods.length > 0)) {
            final Method[] allMethods =
                new Method[parentMethods.length + thisMethods.length];
            System.arraycopy(parentMethods, 0, allMethods, 0,
                parentMethods.length);
            System.arraycopy(thisMethods, 0, allMethods, parentMethods.length,
                thisMethods.length);

            thisMethods = allMethods;
        }

        return thisMethods;
    }

    @Override
    @Suspendable
    protected void doTrace(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException
    {

        int responseLength;

        final String CRLF = "\r\n";
        final StringBuilder buffer = new StringBuilder("TRACE ").append(req.getRequestURI())
            .append(" ").append(req.getProtocol());

        final Enumeration<String> reqHeaderEnum = req.getHeaderNames();

        while (reqHeaderEnum.hasMoreElements()) {
            final String headerName = reqHeaderEnum.nextElement();
            buffer.append(CRLF).append(headerName).append(": ")
                .append(req.getHeader(headerName));
        }

        buffer.append(CRLF);

        responseLength = buffer.length();

        resp.setContentType("message/http");
        resp.setContentLength(responseLength);
        final ServletOutputStream out = resp.getOutputStream();
        out.print(buffer.toString());
    }

    @Override
    @Suspendable
    protected void doHead(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException
    {
        final NoBodyResponse response = new NoBodyResponse(resp);

        doGet(req, response);
        response.setContentLength();
    }

    private void maybeSetLastModified(HttpServletResponse resp,
                                      long lastModified) {
        if (resp.containsHeader(HEADER_LASTMOD))
            return;
        if (lastModified >= 0)
            resp.setDateHeader(HEADER_LASTMOD, lastModified);
    }
}

final class NoBodyResponse extends HttpServletResponseWrapper {
    private static final ResourceBundle lStrings
        = ResourceBundle.getBundle("javax.servlet.http.LocalStrings");

    private NoBodyOutputStream noBody;
    private PrintWriter writer;
    private boolean didSetContentLength;
    private boolean usingOutputStream;

    NoBodyResponse(HttpServletResponse r) {
        super(r);
        noBody = new NoBodyOutputStream();
    }

    final void setContentLength() {
        if (!didSetContentLength) {
            if (writer != null) {
                writer.flush();
            }
            setContentLength(noBody.getContentLength());
        }
    }

    @Override
    public final void setContentLength(int len) {
        super.setContentLength(len);
        didSetContentLength = true;
    }

    @Override
    public final ServletOutputStream getOutputStream() throws IOException {

        if (writer != null) {
            throw new IllegalStateException(
                lStrings.getString("err.ise.getOutputStream"));
        }
        usingOutputStream = true;

        return noBody;
    }

    @Override
    public final PrintWriter getWriter() throws UnsupportedEncodingException {

        if (usingOutputStream) {
            throw new IllegalStateException(
                lStrings.getString("err.ise.getWriter"));
        }

        if (writer == null) {
            final OutputStreamWriter w = new OutputStreamWriter(
                noBody, getCharacterEncoding());
            writer = new PrintWriter(w);
        }

        return writer;
    }
}

final class NoBodyOutputStream extends ServletOutputStream {

    private static final String LSTRING_FILE =
        "javax.servlet.http.LocalStrings";
    private static ResourceBundle lStrings =
        ResourceBundle.getBundle(LSTRING_FILE);

    private int contentLength = 0;

    NoBodyOutputStream() {}

    final int getContentLength() {
        return contentLength;
    }

    @Override
    public final void write(int b) {
        contentLength++;
    }

    @Override
    public final void write(byte buf[], int offset, int len)
        throws IOException
    {
        if (len >= 0) {
            contentLength += len;
        } else {
            // This should have thrown an IllegalArgumentException, but
            // changing this would break backwards compatibility
            throw new IOException(lStrings.getString("err.io.negativelength"));
        }
    }
}
