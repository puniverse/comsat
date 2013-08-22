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
package co.paralleluniverse.fibers.servlet;

import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.SuspendableRunnable;
import java.io.IOException;
import javax.servlet.AsyncContext;
import javax.servlet.GenericServlet;
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

public abstract class FiberGenericServlet extends GenericServlet {
    private static final long serialVersionUID = 1L;
    private transient FiberServletConfig configAD;
    private transient FiberServletContext contextAD;
    private final ThreadLocal<AsyncContext> currentAsyncContext = new ThreadLocal<AsyncContext>();

    @Override
    public final ServletConfig getServletConfig() {
        return configAD;
    }

    @Override
    public final ServletContext getServletContext() {
        return contextAD;
    }

    @Override
    public final void init(ServletConfig config) throws ServletException {
        this.contextAD = new FiberServletContext(config.getServletContext(), currentAsyncContext);
        this.configAD = new FiberServletConfig(config, contextAD);
        this.init();
    }

    @Override
    public final void service(final ServletRequest req, final ServletResponse res) {
        final long st = System.nanoTime();
        req.setAttribute("org.apache.catalina.ASYNC_SUPPORTED", true);
        final AsyncContext ac = req.startAsync();
        final FiberServletRequest srad = wrapRequest(req);
        new Fiber(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                long rt = System.nanoTime();
                try {
                    currentAsyncContext.set(ac);
                    suspendableService(srad, res);
                } catch (Exception ex) {
                    log("Exception in fiber servlet", ex);
                } finally {
                    if (req.isAsyncStarted())
                        ac.complete();
                    currentAsyncContext.set(null);
                    final long ct = System.nanoTime();
                    System.out.println("FGS complete " + ((double)(ct-rt))/1e6 +" "+((double)(rt-st))/1e6);
                }
            }
        }).start();
    }

    /**
     * Called by the servlet container to allow the servlet to respond to
     * a request. See {@link Servlet#service}. This methods may call
     * suspendable functions since it runs in Fiber context
     *
     * <p>This method is declared abstract so subclasses, such as
     * <code>HttpServlet</code>, must override it.
     *
     * @param req the <code>ServletRequest</code> object
     * that contains the client's request
     *
     * @param res the <code>ServletResponse</code> object
     * that will contain the servlet's response
     *
     * @exception ServletException if an exception occurs that
     * interferes with the servlet's
     * normal operation occurred
     *
     * @exception IOException if an input or output
     * exception occurs
     */
    protected abstract void suspendableService(ServletRequest req, ServletResponse res)
            throws ServletException, IOException, SuspendExecution;

    FiberServletRequest wrapRequest(final ServletRequest req) {
        return new FiberServletRequest(req);
    }
}