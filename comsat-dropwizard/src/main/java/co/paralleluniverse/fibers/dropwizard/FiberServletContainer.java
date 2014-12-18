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
package co.paralleluniverse.fibers.dropwizard;

import co.paralleluniverse.fibers.Suspendable;
import co.paralleluniverse.fibers.servlet.FiberHttpServlet;
import com.sun.jersey.api.core.ResourceConfig;
import java.io.IOException;
import java.util.Enumeration;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * A Servlet for deploying root resource classes that assign a fiber per request.
 *
 * @see org.glassfish.jersey.servlet.ServletContainer
 * @author eitan
 */
public class FiberServletContainer extends FiberHttpServlet implements Filter {
    private static final int DEFAULT_STACK_SIZE = 120;
    private final com.sun.jersey.spi.container.servlet.ServletContainer jerseySC;

    /**
     * Create Jersey Servlet container.
     */
    public FiberServletContainer() {
        this.jerseySC = new com.sun.jersey.spi.container.servlet.ServletContainer();
    }

    /**
     * Create Jersey Servlet container.
     *
     * @param resourceConfig container configuration.
     */
    public FiberServletContainer(ResourceConfig resourceConfig) {
        this.jerseySC = new com.sun.jersey.spi.container.servlet.ServletContainer(resourceConfig);
    }

    /**
     * Wraps a Jerset Servlet container.
     *
     * @param sc the servlet container
     */
    public FiberServletContainer(com.sun.jersey.spi.container.servlet.ServletContainer sc) {
        this.jerseySC = sc;
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        if (getStackSize() <= 0)
            setStackSize(DEFAULT_STACK_SIZE);
    }

    /**
     * Get the servlet context for the servlet or filter, depending on
     * how this class is registered.
     *
     * @return the servlet context for the servlet or filter.
     */
    @Override
    public ServletContext getServletContext() {
        return jerseySC.getServletContext();
    }

    @Override
    @Suspendable
    public void service(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        jerseySC.service(req, res);
    }

    // Delegations
    @Override
    public void destroy() {
        jerseySC.destroy();
    }

    @Override
    public void init() throws ServletException {
        jerseySC.init(getServletConfig());
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        jerseySC.init(filterConfig);
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        jerseySC.doFilter(servletRequest, servletResponse, filterChain);
    }

    public void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
        jerseySC.doFilter(request, response, chain);
    }

    @Override
    public String getInitParameter(String name) {
        return jerseySC.getInitParameter(name);
    }

    @Override
    public Enumeration<String> getInitParameterNames() {
        return jerseySC.getInitParameterNames();
    }

    @Override
    public String getServletInfo() {
        return jerseySC.getServletInfo();
    }

    @Override
    public void log(String msg) {
        jerseySC.log(msg);
    }

    @Override
    public void log(String message, Throwable t) {
        jerseySC.log(message, t);
    }

    @Override
    public String getServletName() {
        return jerseySC.getServletName();
    }

    @Override
    public int hashCode() {
        return jerseySC.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return jerseySC.equals(obj);
    }

    @Override
    public String toString() {
        return jerseySC.toString();
    }
}