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

import co.paralleluniverse.fibers.FiberUtil;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.SuspendableRunnable;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import javax.servlet.AsyncContext;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

/**
 *
 * @author eitan
 */
public class RequestDispatcerAsyncDispatch implements RequestDispatcher {
    public static final String SLASH = "/";
    private final String path;
    private final AsyncContext ac;

    public RequestDispatcerAsyncDispatch(String path, AsyncContext ac) {
        this.path = path;
        this.ac = ac;
    }

    @Override
    public void forward(ServletRequest request, final ServletResponse response) throws ServletException, IOException {
        ServletRequest baseReq = request instanceof HttpServletRequestAsyncDispatch ? ((HttpServletRequestAsyncDispatch) request).getReq() : request;
        if (baseReq != ac.getRequest() || response != ac.getResponse())
//            currAc = ac.getRequest().startAsync(request, response);
            throw new UnsupportedOperationException("Changing the request or the response in forward is not yet supported from fiber servlet");
        try {
            FiberUtil.runInFiberChecked(new SuspendableRunnable() {
                @Override
                public void run() throws SuspendExecution, InterruptedException {
                    response.reset();
                    ac.dispatch(relToAbs((HttpServletRequest) ac.getRequest(), path));

//                    try {
//                        new AsyncDispatcherCB() {
//                            @Override
//                            protected Void requestAsync(Fiber current, AsyncListener callback) {
//                                currAc.addListener(callback);
//                                response.reset();
//                                currAc.dispatch(path);
//                                return null;
//                            }
//                        }.run();
//                    } catch (ServletException e) {
//                        throw new RuntimeException(e);
//                    }
                }
            }, ServletException.class);
        } catch (InterruptedException e) {
            throw new AssertionError(e); // REMOVE AFTER SYNC TO NEW QUASAR VERSION
        }
    }

    @Override
    public void include(ServletRequest request, final ServletResponse response) throws ServletException {
        throw new UnsupportedOperationException("Include calls are not yet supported by fiber servlet");
//        try {
//            FiberUtil.runInFiberChecked(new SuspendableRunnable() {
//                @Override
//                public void run() throws SuspendExecution, InterruptedException {
//                    try {
//                        new AsyncDispatcherCB() {
//                            @Override
//                            protected Void requestAsync(Fiber current, AsyncListener callback) {
//                                ac.addListener(callback);
//                                Log.getLogger(RequestDispatcerAsyncDispatch.class).info("beforeDispatch");
//                                ac.dispatch(path);
//                                Log.getLogger(RequestDispatcerAsyncDispatch.class).info("afterDispatch");
//                                return null;
//                            }
//                        }.run();
//                    } catch (ServletException e) {
//                        throw new RuntimeException(e);
//                    }
//                }
//            }, ServletException.class);
//        } catch (ExecutionException e) {
//            throw new AssertionError(e); // REMOVE AFTER SYNC TO NEW QUASAR VERSION
//        } catch (InterruptedException e) {
//            throw new AssertionError(e);
//        }
    }

    public static String relToAbs(HttpServletRequest req, String rel) {
        if (!rel.startsWith("/")) {
            String relTo = addPaths(req.getServletPath(), req.getPathInfo());
            int slash = relTo.lastIndexOf("/");
            if (slash > 1)
                relTo = relTo.substring(0, slash + 1);
            else
                relTo = "/";
            rel = addPaths(relTo, rel);
        }
        return rel;
    }

    public static String addPaths(String p1, String p2) {
        if (p1 == null || p1.length() == 0) {
            if (p1 != null && p2 == null)
                return p1;
            return p2;
        }
        if (p2 == null || p2.length() == 0)
            return p1;

        int split = p1.indexOf(';');
        if (split < 0)
            split = p1.indexOf('?');
        if (split == 0)
            return p2 + p1;
        if (split < 0)
            split = p1.length();

        StringBuilder buf = new StringBuilder(p1.length() + p2.length() + 2);
        buf.append(p1);

        if (buf.charAt(split - 1) == '/') {
            if (p2.startsWith(SLASH)) {
                buf.deleteCharAt(split - 1);
                buf.insert(split - 1, p2);
            } else
                buf.insert(split, p2);
        } else {
            if (p2.startsWith(SLASH))
                buf.insert(split, p2);
            else {
                buf.insert(split, SLASH);
                buf.insert(split + 1, p2);
            }
        }

        return buf.toString();
    }
}
