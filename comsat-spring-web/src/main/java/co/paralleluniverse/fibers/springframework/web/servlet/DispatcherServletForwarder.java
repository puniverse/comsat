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

import java.io.IOException;
import java.util.Enumeration;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.DispatcherServlet;

import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.Suspendable;


// TODO circlespainter: generate dynamically

/**
 * Extends concrete {@link DispatcherServlet} in order to forward {@link javax.servlet.http.HttpServlet} method calls to the new
 * {@link co.paralleluniverse.fibers.servlet.FiberHttpServlet} hierarchy. Like {@link DispatcherServlet} it needs to provide a
 * default constructor.
 * 
 * @author circlespainter
 */
public class DispatcherServletForwarder extends DispatcherServlet {
    private FiberDispatcherServlet fiberDispatcherServlet;

    /**
     * Setter for the {@link FiberDispatcherServlet} target of forwarded calls
     * 
     * @param fiberDispatchingServlet 
     */
    protected void setForwardingTarget(FiberDispatcherServlet fiberDispatchingServlet) {
        this.fiberDispatcherServlet = fiberDispatchingServlet;
    }

    /** @see javax.servlet.GenericServlet#service(javax.servlet.ServletRequest, javax.servlet.ServletResponse) */
    @Override
    public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
        fiberDispatcherServlet.service(req, res); //To change body of generated methods, choose Tools | Templates.
    }

    /** @see javax.servlet.http.HttpServlet#doHead(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse) */
    @Override
    @Suspendable
    protected void doHead(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            fiberDispatcherServlet.doHead(req, resp); //To change body of generated methods, choose Tools | Templates.
        } catch (SuspendExecution ex) {
            throw new AssertionError(ex);
        }
    }

    /** @see javax.servlet.http.HttpServlet#getLastModified(javax.servlet.http.HttpServletRequest) */
    @Override
    protected long getLastModified(HttpServletRequest req) {
        return fiberDispatcherServlet.getLastModified(req);
    }

    /** @see javax.servlet.GenericServlet#log(java.lang.String, java.lang.Throwable) */
    @Override
    public void log(String message, Throwable t) {
        fiberDispatcherServlet.log(message, t);
    }

    /** @see javax.servlet.GenericServlet#log(java.lang.String) */
    @Override
    public void log(String msg) {
        fiberDispatcherServlet.log(msg);
    }

    /** @see javax.servlet.GenericServlet#init(javax.servlet.ServletConfig) */
    @Override
    public void init(ServletConfig config) throws ServletException {
        fiberDispatcherServlet.init(config);
    }

    /** @see javax.servlet.GenericServlet#getServletInfo() */
    @Override
    public String getServletInfo() {
        return fiberDispatcherServlet.getServletInfo();
    }

    /** @see javax.servlet.GenericServlet#getServletConfig() */
    @Override
    public ServletConfig getServletConfig() {
        return fiberDispatcherServlet.getServletConfig();
    }

    /** @see javax.servlet.GenericServlet#getInitParameterNames() */
    @Override
    public Enumeration<String> getInitParameterNames() {
        return fiberDispatcherServlet.getInitParameterNames();
    }

    /** @see javax.servlet.GenericServlet#getInitParameter(java.lang.String) */
    @Override
    public String getInitParameter(String name) {
        return fiberDispatcherServlet.getInitParameter(name);
    }
}