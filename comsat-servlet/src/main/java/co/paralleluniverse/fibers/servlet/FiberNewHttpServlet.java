package co.paralleluniverse.fibers.servlet;

import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.Suspendable;
import co.paralleluniverse.strands.SuspendableRunnable;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ResourceBundle;
import javax.servlet.AsyncContext;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class FiberNewHttpServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private transient FiberServletConfig configAD;
    private transient FiberServletContext contextAD;
    private final ThreadLocal<AsyncContext> currentAsyncContext = new ThreadLocal<>();
    private int stackSize = -1;

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

        String sss = config.getInitParameter("stack-size");
        if (sss != null)
            stackSize = Integer.parseInt(sss);

        this.init();
    }

    protected void setStackSize(int stackSize) {
        this.stackSize = stackSize;
    }

    protected int getStackSize() {
        return stackSize;
    }

    @Override
    @Suspendable
    public final void service(final ServletRequest req, ServletResponse res) throws ServletException, IOException {
        final HttpServletRequest request;
        final HttpServletResponse response;

        if (!(req instanceof HttpServletRequest
                && res instanceof HttpServletResponse)) {
            throw new ServletException("non-HTTP request or response");
        }

        request = (HttpServletRequest) req;
        response = (HttpServletResponse) res;
        req.setAttribute("org.apache.catalina.ASYNC_SUPPORTED", true);
        final AsyncContext ac = req.startAsync();
        ac.setTimeout(120000);
        final FiberHttpServletRequest srad = new FiberHttpServletRequest(request);
        new Fiber(null, stackSize, new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                try {
                    // TODO: check if ac has expired
                    currentAsyncContext.set(ac);
                    service(srad, response); //To change body of generated methods, choose Tools | Templates.
//                    suspendableService(srad, res);
                } catch (Exception ex) {
                    log("Exception in fiber servlet", ex);
                } finally {
                    if (req.isAsyncStarted())
                        ac.complete();
                    currentAsyncContext.set(null);
                }
            }
        }).start();
    }

    private static final String METHOD_DELETE = "DELETE";
    private static final String METHOD_HEAD = "HEAD";
    private static final String METHOD_GET = "GET";
    private static final String METHOD_OPTIONS = "OPTIONS";
    private static final String METHOD_POST = "POST";
    private static final String METHOD_PUT = "PUT";
    private static final String METHOD_TRACE = "TRACE";

    private static final String HEADER_IFMODSINCE = "If-Modified-Since";
    private static final String HEADER_LASTMOD = "Last-Modified";

    private static final String LSTRING_FILE
            = "javax.servlet.http.LocalStrings";
    private static ResourceBundle lStrings
            = ResourceBundle.getBundle(LSTRING_FILE);

    @Suspendable
    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String method = req.getMethod();

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
            long lastModified = getLastModified(req);
            maybeSetLastModified(resp, lastModified);
            doHead(req, resp);

        } else if (method.equals(METHOD_POST)) {
            doPost(req, resp);

        } else if (method.equals(METHOD_PUT)) {
            doPut(req, resp);

        } else if (method.equals(METHOD_DELETE)) {
            doDelete(req, resp);

        } else if (method.equals(METHOD_OPTIONS)) {
            doOptions(req, resp);

        } else if (method.equals(METHOD_TRACE)) {
            doTrace(req, resp);

        } else {
            //
            // Note that this means NO servlet supports whatever
            // method was requested, anywhere on this server.
            //

            String errMsg = lStrings.getString("http.method_not_implemented");
            Object[] errArgs = new Object[1];
            errArgs[0] = method;
            errMsg = MessageFormat.format(errMsg, errArgs);

            resp.sendError(HttpServletResponse.SC_NOT_IMPLEMENTED, errMsg);
        }
    }

    private void maybeSetLastModified(HttpServletResponse resp,
            long lastModified) {
        if (resp.containsHeader(HEADER_LASTMOD))
            return;
        if (lastModified >= 0)
            resp.setDateHeader(HEADER_LASTMOD, lastModified);
    }

    @Suspendable
    @Override
    protected void doTrace(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        super.doTrace(req, resp); //To change body of generated methods, choose Tools | Templates.
    }

    @Suspendable
    @Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        super.doOptions(req, resp); //To change body of generated methods, choose Tools | Templates.
    }

    @Suspendable
    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        super.doDelete(req, resp); //To change body of generated methods, choose Tools | Templates.
    }

    @Suspendable
    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        super.doPut(req, resp); //To change body of generated methods, choose Tools | Templates.
    }

    @Suspendable
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        super.doPost(req, resp); //To change body of generated methods, choose Tools | Templates.
    }

    @Suspendable
    @Override
    protected void doHead(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        super.doHead(req, resp); //To change body of generated methods, choose Tools | Templates.
    }

    @Suspendable
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        super.doGet(req, resp); //To change body of generated methods, choose Tools | Templates.
    }
}
