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

// TODO circlespainter: I don't like "patching" the original API's packages like this but it's the only way to delegate to protected members in Java. Maybe better generating this class at runtime.

import co.paralleluniverse.fibers.SuspendExecution;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.logging.Log;
import org.springframework.context.ApplicationContext;
import org.springframework.context.i18n.LocaleContext;
import org.springframework.web.multipart.MultipartException;
 
/**
 * {@link DispatcherServlet} wrapper in the same package, publishing access to its protected members for call proxying by the Comsat integration module
 * 
 * @author circlespainter
 */
public final class FiberDispatcherServletProtectedWrapper {
    private final DispatcherServlet dispatcherServlet;

    /**
     * Wrapping constructor
     * 
     * @param dispatcherServlet 
     */
    public FiberDispatcherServletProtectedWrapper(DispatcherServlet dispatcherServlet) {
        this.dispatcherServlet = dispatcherServlet;
    }

    /** @see DispatcherServlet#pageNotFoundLogger */
    public static final Log pageNotFoundLogger = DispatcherServlet.pageNotFoundLogger;

    /** @see DispatcherServlet#DispatcherServlet(org.springframework.web.context.WebApplicationContext) */
    public void onRefresh(ApplicationContext context) {
        dispatcherServlet.onRefresh(context);
    }

    /** @see DispatcherServlet#getDefaultStrategies(org.springframework.context.ApplicationContext, java.lang.Class) */
    public <T> T getDefaultStrategy(ApplicationContext context, Class<T> strategyInterface) {
        return dispatcherServlet.getDefaultStrategy(context, strategyInterface);
    }

    /** @see DispatcherServlet#getDefaultStrategies(org.springframework.context.ApplicationContext, java.lang.Class) */
    public <T> List<T> getDefaultStrategies(ApplicationContext context, Class<T> strategyInterface) {
        return dispatcherServlet.getDefaultStrategies(context, strategyInterface);
    }
    
    /** @see DispatcherServlet#createDefaultStrategy(org.springframework.context.ApplicationContext, java.lang.Class) */
    public Object createDefaultStrategy(ApplicationContext context, Class<?> clazz) {
        return dispatcherServlet.createDefaultStrategy(context, clazz);
    }

    /** @see DispatcherServlet#buildLocaleContext(javax.servlet.http.HttpServletRequest) */
    public LocaleContext buildLocaleContext(HttpServletRequest request) {
        return dispatcherServlet.buildLocaleContext(request);
    }

    /** @see DispatcherServlet#getHandler(javax.servlet.http.HttpServletRequest) */
    public HandlerExecutionChain getHandler(HttpServletRequest request) throws Exception {
        return dispatcherServlet.getHandler(request);
    }

    /** @see DispatcherServlet#noHandlerFound(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse) */
    public void noHandlerFound(HttpServletRequest request, HttpServletResponse response) throws Exception {
        dispatcherServlet.noHandlerFound(request, response);
    }

    /** @see DispatcherServlet#getHandlerAdapter(java.lang.Object) */
    public HandlerAdapter getHandlerAdapter(Object handler) throws ServletException {
        return dispatcherServlet.getHandlerAdapter(handler);
    }

    /** @see DispatcherServlet#processHandlerException(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, java.lang.Object, java.lang.Exception) */
    public ModelAndView processHandlerException(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        return dispatcherServlet.processHandlerException(request, response, handler, ex);
    }

    /** @see DispatcherServlet#render(org.springframework.web.servlet.ModelAndView, javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse) */
    public void render(ModelAndView mv, HttpServletRequest request, HttpServletResponse response) throws Exception {
        dispatcherServlet.render(mv, request, response);
    }
    
    /** @see DispatcherServlet#getDefaultViewName(javax.servlet.http.HttpServletRequest) */
    public String getDefaultViewName(HttpServletRequest request) throws Exception {
        return dispatcherServlet.getDefaultViewName(request);
    }

    /** @see DispatcherServlet#resolveViewName(java.lang.String, java.util.Map, java.util.Locale, javax.servlet.http.HttpServletRequest) */
    public View resolveViewName(String viewName, Map<String, Object> model, Locale locale, HttpServletRequest request) throws Exception {
        return dispatcherServlet.resolveViewName(viewName, model, locale, request);
    }

    /** @see DispatcherServlet#initStrategies(org.springframework.context.ApplicationContext) */
    public void initStrategies(ApplicationContext context) {
        dispatcherServlet.initStrategies(context);
    }

    /** @see DispatcherServlet#doService(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse) */
    // Proxying is fine, the target method has been added to `suspendables`
    public void doService(HttpServletRequest request, HttpServletResponse response) throws Exception, SuspendExecution {
        dispatcherServlet.doService(request, response);
    }

    /** @see DispatcherServlet#doDispatch(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse) */
    // Proxying is fine, the target method has been added to `suspendables`
    public void doDispatch(HttpServletRequest request, HttpServletResponse response) throws Exception, SuspendExecution {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    /** @see DispatcherServlet#checkMultipart(javax.servlet.http.HttpServletRequest) */
    public HttpServletRequest checkMultipart(HttpServletRequest request) throws MultipartException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    /** @see DispatcherServlet#cleanupMultipart(javax.servlet.http.HttpServletRequest) */
    public void cleanupMultipart(HttpServletRequest request) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}