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

import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.Suspendable;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.i18n.LocaleContext;
import org.springframework.web.context.ConfigurableWebApplicationContext;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.ServletRequestAttributes;


// TODO circlespainter: generate dynamically

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
        return frameworkServlet.createWebApplicationContext(parent);
    }

    /** @see FrameworkServlet#service(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse) */
    // Proxying is fine, the target method has been added to `suspendables`
    public void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException, SuspendExecution {
        frameworkServlet.service(request, response);
    }

    /** @see FrameworkServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse) */
    // Proxying is fine, the target method has been added to `suspendables`
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException, SuspendExecution {
        frameworkServlet.doGet(request, response);
    }

    /** @see FrameworkServlet#doPost(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse) */
    // Proxying is fine, the target method has been added to `suspendables`
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException, SuspendExecution {
        frameworkServlet.doPost(request, response);
    }

    /** @see FrameworkServlet#doPut(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse) */
    // Proxying is fine, the target method has been added to `suspendables`
    public void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException, SuspendExecution {
        frameworkServlet.doPut(request, response);
    }

    /** @see FrameworkServlet#doDelete(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse) */
    // Proxying is fine, the target method has been added to `suspendables`
    public void doDelete(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException, SuspendExecution {
        frameworkServlet.doDelete(request, response);
    }

    /** @see FrameworkServlet#doOptions(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse) */
    // Proxying is fine, the target method has been added to `suspendables`
    public void doOptions(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException, SuspendExecution {
        frameworkServlet.doOptions(request, response);
    }

    /** @see FrameworkServlet#doTrace(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse) */
    // Proxying is fine, the target method has been added to `suspendables`
    @Suspendable
    public void doTrace(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        frameworkServlet.doTrace(request, response);
    }

    /** @see FrameworkServlet#processRequest(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse) */
    // Proxying is fine, the target method has been added to `suspendables`
    public void processRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException, SuspendExecution {
        frameworkServlet.processRequest(request, response);
    }
}