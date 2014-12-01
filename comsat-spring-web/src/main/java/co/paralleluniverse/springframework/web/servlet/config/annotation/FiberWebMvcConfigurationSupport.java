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
/*
 * Based on org.springframework.web.servlet.config.annotation.FiberWebMvcConfigurationSupport
 * in Spring Framework Web MVC.
 * Copyright the original authors Rossen Stoyanchev, Brian Clozel and Sebastien Deleuze.
 * Released under the ASF 2.0 license.
 */
package co.paralleluniverse.springframework.web.servlet.config.annotation;

import co.paralleluniverses.pringframework.web.servlet.mvc.method.annotation.FiberRequestMappingHandlerAdapter;
import java.util.ArrayList;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.DelegatingWebMvcConfiguration;
import org.springframework.web.servlet.config.annotation.ServletConfigAnnotationProtectedProxy;

/**
 * @author circlespainter
 * 
 * @see EnableWebMvc
 * @see WebMvcConfigurer
 * @see WebMvcConfigurerAdapter
 */
@Configuration
public class FiberWebMvcConfigurationSupport extends DelegatingWebMvcConfiguration {
    @Bean
    public FiberRequestMappingHandlerAdapter fiberRequestMappingHandlerAdapter() {
        List<HandlerMethodArgumentResolver> argumentResolvers = new ArrayList<>();
        addArgumentResolvers(argumentResolvers);

        List<HandlerMethodReturnValueHandler> returnValueHandlers = new ArrayList<>();
        addReturnValueHandlers(returnValueHandlers);

        FiberRequestMappingHandlerAdapter adapter = new FiberRequestMappingHandlerAdapter();
        adapter.setContentNegotiationManager(mvcContentNegotiationManager());
        adapter.setMessageConverters(getMessageConverters());
        adapter.setWebBindingInitializer(getConfigurableWebBindingInitializer());
        adapter.setCustomArgumentResolvers(argumentResolvers);
        adapter.setCustomReturnValueHandlers(returnValueHandlers);

        AsyncSupportConfigurer configurer = new AsyncSupportConfigurer();
        configureAsyncSupport(configurer);

        if (ServletConfigAnnotationProtectedProxy.getTaskExecutor(configurer) != null) {
            adapter.setTaskExecutor(ServletConfigAnnotationProtectedProxy.getTaskExecutor(configurer));
        }
        if (ServletConfigAnnotationProtectedProxy.getTimeout(configurer) != null) {
            adapter.setAsyncRequestTimeout(ServletConfigAnnotationProtectedProxy.getTimeout(configurer));
        }
        adapter.setCallableInterceptors(ServletConfigAnnotationProtectedProxy.getCallableInterceptors(configurer));
        adapter.setDeferredResultInterceptors(ServletConfigAnnotationProtectedProxy.getDeferredResultInterceptors(configurer));

        return adapter;
    }
}
