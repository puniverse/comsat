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
package org.springframework.web.servlet.config.annotation;

import java.util.List;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.context.request.async.CallableProcessingInterceptor;
import org.springframework.web.context.request.async.DeferredResultProcessingInterceptor;
import org.springframework.web.servlet.handler.AbstractHandlerMapping;

// TODO generate dynamically

/**
 * Same-package proxy for several functions used in comsat-spring-web.
 *
 * @author circlespainter
 */
public class ServletConfigAnnotationProtectedProxy {
    /**
     * @see InterceptorRegistry#getInterceptors()
     */
    public static List<Object> getInterceptors(InterceptorRegistry ir) {
        return ir.getInterceptors();
    }

    /**
     * @see ContentNegotiationConfigurer#getContentNegotiationManager()
     */
    public static ContentNegotiationManager getContentNegotiationManager(ContentNegotiationConfigurer configurer) throws Exception {
        return configurer.getContentNegotiationManager();
    }

    /**
     * @see ViewControllerRegistry#getHandlerMapping()
     */
    public static AbstractHandlerMapping getHandlerMapping(ViewControllerRegistry registry) {
        return registry.getHandlerMapping();
    }

    /**
     * @see ResourceHandlerRegistry#getHandlerMapping()
     */
    public static AbstractHandlerMapping getHandlerMappingResource(ResourceHandlerRegistry registry) {
        return registry.getHandlerMapping();
    }

    /**
     * @see DefaultServletHandlerConfigurer#getHandlerMapping()
     */
    public static AbstractHandlerMapping getHandlerMappingDefaultServlet(DefaultServletHandlerConfigurer configurer) {
        return configurer.getHandlerMapping();
    }

    /**
     * @see AsyncSupportConfigurer#getTaskExecutor()
     */
    public static AsyncTaskExecutor getTaskExecutor(AsyncSupportConfigurer configurer) {
        return configurer.getTaskExecutor();
    }

    /**
     * @see AsyncSupportConfigurer#getTaskExecutor()
     */
    public static Long getTimeout(AsyncSupportConfigurer configurer) {
        return configurer.getTimeout();
    }

    /**
     * @see AsyncSupportConfigurer#getDeferredResultInterceptors()
     */
    public static List<DeferredResultProcessingInterceptor> getDeferredResultInterceptors(AsyncSupportConfigurer configurer) {
        return configurer.getDeferredResultInterceptors();
    }

    /**
     * @see AsyncSupportConfigurer#getCallableInterceptors()
     */
    public static List<CallableProcessingInterceptor> getCallableInterceptors(AsyncSupportConfigurer configurer) {
        return configurer.getCallableInterceptors();
    }
}
