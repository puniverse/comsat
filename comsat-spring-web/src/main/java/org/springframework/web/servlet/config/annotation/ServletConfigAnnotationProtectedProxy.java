/*
 * Copyright 2002-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
