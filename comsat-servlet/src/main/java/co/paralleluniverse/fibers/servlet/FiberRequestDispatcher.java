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

import java.io.IOException;
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
class FiberRequestDispatcher implements RequestDispatcher {
    public static final String SLASH = "/";
    private final String path;
    private final AsyncContext ac;

    public FiberRequestDispatcher(String path, AsyncContext ac) {
        this.path = path;
        this.ac = ac;
    }

    @Override
    public void forward(ServletRequest request, final ServletResponse response) throws ServletException, IOException {
        if (ac == null)
            throw new UnsupportedOperationException("Sync forward emulation is disabled");
        final ServletRequest baseReq = ((FiberServletRequest) request).getReq();
        if (baseReq != ac.getRequest() || response != ac.getResponse())
            throw new UnsupportedOperationException("Changing the request or the response in forward is not yet supported from fiber servlet");
        response.reset();
        if (ac.getRequest() instanceof HttpServletRequest)
            ac.dispatch(relToAbs((HttpServletRequest) ac.getRequest(), path));
        else
            ac.dispatch(path); // TODO: think about relative forward in GenericServlet
    }

    @Override
    public void include(ServletRequest request, final ServletResponse response) throws ServletException {
        throw new UnsupportedOperationException("Include calls are not yet supported by fiber servlet");
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
