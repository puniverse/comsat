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
package org.springframework.web.servlet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.logging.Log;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.i18n.LocaleContext;
import org.springframework.web.context.ConfigurableWebApplicationContext;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.ServletRequestAttributes;

// TODO circlespainter: I don't like "patching" the original API's packages like this but it's the only way to delegate to protected members in Java. Maybe better generating this class at runtime.

/**
 * {@link FrameworkServlet} wrapper in the same package, publishing access to its protected members for call proxying by the Comsat integration module
 * 
 * @author circlespainter
 */
public class FiberFrameworkServletProtectedWrapper {
    private final FrameworkServlet frameworkServlet;
    
    /**
     * Wrapping constructor
     * 
     * @param frameworkServlet 
     */
    public FiberFrameworkServletProtectedWrapper(FrameworkServlet frameworkServlet) {
        this.frameworkServlet = frameworkServlet;
    }

    /** @see FrameworkServlet#configureAndRefreshWebApplicationContext(org.springframework.web.context.ConfigurableWebApplicationContext) */
    public void configureAndRefreshWebApplicationContext(ConfigurableWebApplicationContext wac) {
        frameworkServlet.configureAndRefreshWebApplicationContext(wac);
    }

    /** @see FrameworkServlet#applyInitializers(org.springframework.context.ConfigurableApplicationContext) */
    public void applyInitializers(ConfigurableApplicationContext wac) {
        frameworkServlet.applyInitializers(wac);
    }
    
    /** @see FrameworkServlet#buildLocaleContext(javax.servlet.http.HttpServletRequest)} */
    public LocaleContext buildLocaleContext(HttpServletRequest request) {
        return frameworkServlet.buildLocaleContext(request);
    }

    /** @see FrameworkServlet#buildRequestAttributes(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, org.springframework.web.context.request.RequestAttributes)} */
    public ServletRequestAttributes buildRequestAttributes(HttpServletRequest request, HttpServletResponse response, RequestAttributes previousAttributes) {
        return frameworkServlet.buildRequestAttributes(request, response, previousAttributes);
    }

    /** @see FrameworkServlet#findWebApplicationContext()} */
    public WebApplicationContext findWebApplicationContext() {
        return frameworkServlet.findWebApplicationContext();
    }

    /** @see FrameworkServlet#getUsernameForRequest(javax.servlet.http.HttpServletRequest) */
    public String getUsernameForRequest(HttpServletRequest request) {
        return frameworkServlet.getUsernameForRequest(request);
    }

    /** @see FrameworkServlet#initServletBean()  */
    public void initServletBean() throws ServletException {
        frameworkServlet.initServletBean();
    }

    /** @see FrameworkServlet#initWebApplicationContext() */
    public WebApplicationContext initWebApplicationContext() {
        return frameworkServlet.initWebApplicationContext();
    }

    /** @see FrameworkServlet#createWebApplicationContext(org.springframework.context.ApplicationContext) */
    public WebApplicationContext createWebApplicationContext(ApplicationContext parent) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}