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
package co.paralleluniverse.fibers.jersey;

import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.servlet.FiberHttpServlet;
import java.io.IOException;
import java.net.URI;
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
import org.glassfish.jersey.internal.util.collection.Value;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.spi.Container;

/**
 *
 * @author eitan
 */
public class ServletContainer extends FiberHttpServlet implements Filter, Container {
    private final org.glassfish.jersey.servlet.ServletContainer jerseySC;

    /**
     * Create Jersey Servlet container.
     */
    public ServletContainer() {
        this.jerseySC = new org.glassfish.jersey.servlet.ServletContainer();
    }

    /**
     * Create Jersey Servlet container.
     *
     * @param resourceConfig container configuration.
     */
    public ServletContainer(ResourceConfig rc) {
        this.jerseySC = new org.glassfish.jersey.servlet.ServletContainer(rc);
    }

    /**
     * Wraps a Jerset Servlet container.
     *
     * @param sc the servlet container
     */
    public ServletContainer(org.glassfish.jersey.servlet.ServletContainer sc) {
        this.jerseySC = sc;
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
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
    public void suspendableService(ServletRequest req, ServletResponse res) throws ServletException, IOException, SuspendExecution {
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

    /**
     * Dispatch client requests to a resource class.
     *
     * @param baseUri the base URI of the request.
     * @param requestUri the URI of the request.
     * @param request the {@link javax.servlet.http.HttpServletRequest} object that contains the request the client made to the Web component.
     * @param response the {@link javax.servlet.http.HttpServletResponse} object that contains the response the Web component returns to the client.
     * @return lazily initialized response status code {@link Value value provider}.
     * @throws IOException if an input or output error occurs while the Web component is handling the HTTP request.
     * @throws ServletException if the HTTP request cannot be handled.
     */
    public Value<Integer> service(URI baseUri, URI requestUri, HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        return jerseySC.service(baseUri, requestUri, request, response);
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
    public ResourceConfig getConfiguration() {
        return jerseySC.getConfiguration();
    }

    @Override
    public void reload() {
        jerseySC.reload();
    }

    @Override
    public void reload(ResourceConfig configuration) {
        jerseySC.reload(configuration);
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
