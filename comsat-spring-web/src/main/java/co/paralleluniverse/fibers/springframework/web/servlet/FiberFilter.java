/*
 * COMSAT
 * Copyright (c) 2013-2014, Parallel Universe Software Co. All rights reserved.
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
package co.paralleluniverse.fibers.springframework.web.servlet;

import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.servlet.FiberServletRequest;
import co.paralleluniverse.strands.SuspendableRunnable;
import java.io.IOException;
import javax.servlet.AsyncContext;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

/**
 *
 * @author circlespainter
 */
public class FiberFilter implements javax.servlet.Filter {
    static private final int TIMEOUT = 120000;
    
    private final ThreadLocal<AsyncContext> currentAsyncContext = new ThreadLocal<AsyncContext>();
    private int stackSize = -1;

    @Override
    public void init(FilterConfig config) throws ServletException {
        String sss = config.getInitParameter("stack-size");
        if(sss != null)
            stackSize = Integer.parseInt(sss);
    }

    @Override
    public void doFilter(final ServletRequest req, final ServletResponse res, final FilterChain chain) throws IOException, ServletException {
        req.setAttribute("org.apache.catalina.ASYNC_SUPPORTED", true);
        final AsyncContext ac = req.startAsync();
        ac.setTimeout(TIMEOUT);
        final FiberServletRequest srad = new FiberServletRequest(req) {

            @Override
            public AsyncContext startAsync(ServletRequest servletRequest, ServletResponse servletResponse) throws IllegalStateException {
                return startAsync();
            }

            @Override
            public AsyncContext startAsync() throws IllegalStateException {
                // Don't create a new one
                return req.getAsyncContext();
            }
            
        };
        new Fiber(null, stackSize, new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                try {
                    // TODO: check if ac has expired
                    currentAsyncContext.set(ac);
                    // Go on with the filter chain
                    chain.doFilter(req, res);
                } catch (Exception ex) {
                    // TODO log somehow
                    // log("Exception in fiber servlet", ex);
                } finally {
                    if (req.isAsyncStarted())
                        ac.complete();
                    currentAsyncContext.set(null);
                }
            }
        }).start();
    }

    @Override
    public void destroy() {}
}
