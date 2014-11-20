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

import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.Suspendable;
import java.io.IOException;
import java.util.Enumeration;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.DispatcherServlet;

// TODO circlespainter: comment & JavaDocs

/**
 * @author circlespainter
 */
public class DispatcherServletForwarder extends DispatcherServlet {
    private FiberDispatcherServlet fiberDispatcherServlet;

    protected void setForwardingTarget(FiberDispatcherServlet fiberDispatchingServlet) {
        this.fiberDispatcherServlet = fiberDispatchingServlet;
    }

    @Override
    public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
        fiberDispatcherServlet.service(req, res); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    @Suspendable
    protected void doHead(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            fiberDispatcherServlet.doHead(req, resp); //To change body of generated methods, choose Tools | Templates.
        } catch (SuspendExecution ex) {
            throw new AssertionError(ex);
        }
    }

    @Override
    protected long getLastModified(HttpServletRequest req) {
        return fiberDispatcherServlet.getLastModified(req);
    }

    @Override
    public void log(String message, Throwable t) {
        fiberDispatcherServlet.log(message, t);
    }

    @Override
    public void log(String msg) {
        fiberDispatcherServlet.log(msg);
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        fiberDispatcherServlet.init(config);
    }

    @Override
    public String getServletInfo() {
        return fiberDispatcherServlet.getServletInfo();
    }

    @Override
    public ServletConfig getServletConfig() {
        return fiberDispatcherServlet.getServletConfig();
    }

    @Override
    public Enumeration<String> getInitParameterNames() {
        return fiberDispatcherServlet.getInitParameterNames();
    }

    @Override
    public String getInitParameter(String name) {
        return fiberDispatcherServlet.getInitParameter(name);
    }
}
