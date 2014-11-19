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
 * Based on org.springframework.boot.autoconfigure.web.DispatcherServletAutoConfiguration in Spring Boot
 * Copyright the original authors Phillip Webb and Dave Syer.
 * Released under the ASF 2.0 license.
 */
package co.paralleluniverse.fibers.springframework.boot.autoconfigure.web;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.web.DispatcherServletAutoConfiguration;
import org.springframework.boot.autoconfigure.web.EmbeddedServletContainerAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import co.paralleluniverse.fibers.springframework.web.servlet.FiberDispatcherServlet;
import javax.servlet.MultipartConfigElement;
import javax.servlet.ServletRegistration;
import org.springframework.beans.factory.annotation.Autowired;
import static org.springframework.boot.autoconfigure.web.DispatcherServletAutoConfiguration.DEFAULT_DISPATCHER_SERVLET_BEAN_NAME;
import static org.springframework.boot.autoconfigure.web.DispatcherServletAutoConfiguration.DEFAULT_DISPATCHER_SERVLET_REGISTRATION_BEAN_NAME;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.context.embedded.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;


/**
 * Spring Web MVC FiberDispatcherServlet auto-configuration (whichenables fiber-blocking controllers);
 * higher priority than normal Spring Web MVC {@link org.springframework.web.servlet.DispatcherServlet}.
 * 
 * @see WebMvcAutoConfiguration
 * 
 * @author circlespainter
 */
@Configuration
@ConditionalOnWebApplication
@ConditionalOnClass(FiberDispatcherServlet.class)
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
@AutoConfigureAfter(EmbeddedServletContainerAutoConfiguration.class)
public class FiberDispatcherServletAutoConfiguration extends DispatcherServletAutoConfiguration {

    //////////////////////////////////////////////////////////////////////
    // Re-implementing protected features below this point;
    // derived from DispatcherServletAutoConfiguration, relevant copyright
    // and licences apply
    //////////////////////////////////////////////////////////////////////
    
    /** @see DispatcherServletAutoConfiguration.FiberDispatcherServletConfiguration */
    // Rule 1: needs to return `FiberDispatcherServlet` instead of `DispatcherServlet`
    @Configuration
    @ConditionalOnClass(ServletRegistration.class)
    protected static class FiberDispatcherServletConfiguration {
        @Autowired
        private ServerProperties server;

        @Autowired(required = false)
        private MultipartConfigElement multipartConfig;

        @Bean(name = DEFAULT_DISPATCHER_SERVLET_BEAN_NAME)
        public FiberDispatcherServlet dispatcherServlet() {
            return new FiberDispatcherServlet();
        }

        @Bean(name = DEFAULT_DISPATCHER_SERVLET_REGISTRATION_BEAN_NAME)
        public ServletRegistrationBean dispatcherServletRegistration() {
            ServletRegistrationBean registration = new ServletRegistrationBean(
                    dispatcherServlet(), this.server.getServletMapping());
            registration.setName(DEFAULT_DISPATCHER_SERVLET_BEAN_NAME);
            if (this.multipartConfig != null) {
                registration.setMultipartConfig(this.multipartConfig);
            }
            return registration;
        }
    }
}