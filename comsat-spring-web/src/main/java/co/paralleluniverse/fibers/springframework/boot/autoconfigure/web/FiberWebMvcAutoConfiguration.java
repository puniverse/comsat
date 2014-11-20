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
 * Based on org.springframework.boot.autoconfigure.web.WebMvcAutoConfiguration in Spring Boot
 * Copyright the original authors Phillip Webb and Dave Syer.
 * Released under the ASF 2.0 license.
 */
package co.paralleluniverse.fibers.springframework.boot.autoconfigure.web;

import co.paralleluniverse.fibers.springframework.web.servlet.FiberDispatcherServlet;
import javax.servlet.Servlet;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.web.WebMvcAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

/**
 * Spring Web MVC auto-configuration enabling fiber-blocking controllers; higher priority than normal Spring one
 * 
 * @see WebMvcAutoConfiguration
 * 
 * @author circlespainter
 */
@Configuration
@ComponentScan(basePackages = "co.paralleluniverse.fibers.springframework.boot.autoconfigure.web")
@ConditionalOnWebApplication
@ConditionalOnClass({ Servlet.class, FiberDispatcherServlet.class, WebMvcConfigurerAdapter.class })
@ConditionalOnMissingBean(WebMvcConfigurationSupport.class)
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
@AutoConfigureAfter(FiberDispatcherServletAutoConfiguration.class)
public class FiberWebMvcAutoConfiguration {
    // TODO circlespainter: finish, comment and JavaDocs
    
    /*
    
    @Autowired
    private WebMvcConfigurationSupport webMvcConfigurationSupport;
    
    @Bean
    public FiberRequestMappingHandlerAdapter fiberRequestMappingHandlerAdapter() {
        WebMvcConfigurationSupportProtectedProxy webMvcConfigurationSupportProtectedProxy = new WebMvcConfigurationSupportProtectedProxy(webMvcConfigurationSupport);
        
        List<HandlerMethodArgumentResolver> argumentResolvers = new ArrayList<HandlerMethodArgumentResolver>();
        webMvcConfigurationSupportProtectedProxy.addArgumentResolvers(argumentResolvers);

        List<HandlerMethodReturnValueHandler> returnValueHandlers = new ArrayList<HandlerMethodReturnValueHandler>();
        webMvcConfigurationSupportProtectedProxy.addReturnValueHandlers(returnValueHandlers);

        FiberRequestMappingHandlerAdapter adapter = new FiberRequestMappingHandlerAdapter();
        adapter.setContentNegotiationManager(webMvcConfigurationSupport.mvcContentNegotiationManager());
        adapter.setMessageConverters(webMvcConfigurationSupportProtectedProxy.getMessageConverters());
        adapter.setWebBindingInitializer(webMvcConfigurationSupportProtectedProxy.getConfigurableWebBindingInitializer());
        adapter.setCustomArgumentResolvers(argumentResolvers);
        adapter.setCustomReturnValueHandlers(returnValueHandlers);

        AsyncSupportConfigurer configurer = new AsyncSupportConfigurer();
        AsyncSupportConfigurerProtectedProxy configurerProtectedProxy = new AsyncSupportConfigurerProtectedProxy(configurer);
        webMvcConfigurationSupport.configureAsyncSupport(configurer);

        if (configurerProtectedProxy.getTaskExecutor() != null) {
            adapter.setTaskExecutor(configurerProtectedProxy.getTaskExecutor());
        }
        if (configurerProtectedProxy.getTimeout() != null) {
            adapter.setAsyncRequestTimeout(configurerProtectedProxy.getTimeout());
        }
        adapter.setCallableInterceptors(configurerProtectedProxy.getCallableInterceptors());
        adapter.setDeferredResultInterceptors(configurerProtectedProxy.getDeferredResultInterceptors());

        return adapter;
    }
    
    */
}