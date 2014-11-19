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
package co.paralleluniverse.fibers.springframework.boot.autoconfigure.web;

import co.paralleluniverse.fibers.springframework.web.servlet.FiberDispatcherServlet;
import javax.servlet.Servlet;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.web.WebMvcAutoConfiguration;
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
@ConditionalOnWebApplication
@ConditionalOnClass({ Servlet.class, FiberDispatcherServlet.class, WebMvcConfigurerAdapter.class })
@ConditionalOnMissingBean(WebMvcConfigurationSupport.class)
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
@AutoConfigureAfter(FiberDispatcherServletAutoConfiguration.class) // Luckily this annotation is not inherited
public class FiberWebMvcAutoConfiguration extends WebMvcAutoConfiguration {}