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
import java.io.IOException;
import java.net.URI;
import java.util.Enumeration;
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

/**
 *
 * @author eitan
 */
public class ServletContainer extends co.paralleluniverse.fibers.servlet.HttpServlet {
    private final org.glassfish.jersey.servlet.ServletContainer jerseySC;

    public ServletContainer() {
        this.jerseySC = new org.glassfish.jersey.servlet.ServletContainer();
    }

    public ServletContainer(ResourceConfig rc) {
        this.jerseySC = new org.glassfish.jersey.servlet.ServletContainer(rc);
    }

    public ServletContainer(org.glassfish.jersey.servlet.ServletContainer sc) {
        this.jerseySC = sc;
    }

    //TODO - can't override final method
    public void _init(ServletConfig config) throws ServletException {
        System.out.println("1");
        super.init(config);
    }

    //TODO - can't override final method
    public ServletContext _getServletContext() {
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

    public Value<Integer> service(URI baseUri, URI requestUri, HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        return jerseySC.service(baseUri, requestUri, request, response);
    }

    public void init(FilterConfig filterConfig) throws ServletException {
        jerseySC.init(filterConfig);
    }

    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        jerseySC.doFilter(servletRequest, servletResponse, filterChain);
    }

    public void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
        jerseySC.doFilter(request, response, chain);
    }

    public ResourceConfig getConfiguration() {
        return jerseySC.getConfiguration();
    }

    public void reload() {
        jerseySC.reload();
    }

    public void reload(ResourceConfig configuration) {
        jerseySC.reload(configuration);
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
}
