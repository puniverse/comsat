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
package co.paralleluniverse.fibers.springframework.web.servlet;

import org.springframework.context.ApplicationContextAware;
import org.springframework.web.servlet.FrameworkServlet;

// TODO circlespainter: complete

/**
 * Rewriting of {@link FrameworkServlet} implementing the extended {@link FiberHttpServletBean} fiber-blocking servlet interface 
 * 
 * @author circlespainter
 */
public abstract class FiberFrameworkServlet extends FiberHttpServletBean implements ApplicationContextAware {
    // References to original instance and a `protected`-opening proxy of it
    private final FrameworkServlet frameworkServlet;

    /**
     * Rule a: wrapping constructor only as this class is abstract and not a full replica
     * 
     * @param frameworkServlet The wrapped instance
     */
    public FiberFrameworkServlet(FrameworkServlet frameworkServlet) {
        super(frameworkServlet);
        this.frameworkServlet = frameworkServlet;
    }
    
    ////////////////////////////////////////////
    // Proxying public features below this point
    ////////////////////////////////////////////

    /**
     * Proxy for {@link FrameworkServlet#DEFAULT_NAMESPACE_SUFFIX} 
     */
    public static final String DEFAULT_NAMESPACE_SUFFIX = FrameworkServlet.DEFAULT_NAMESPACE_SUFFIX;
    /**
     * Proxy for {@link FrameworkServlet#DEFAULT_CONTEXT_CLASS} 
     */
    public static final Class<?> DEFAULT_CONTEXT_CLASS = FrameworkServlet.DEFAULT_CONTEXT_CLASS;

    /**
     * Proxy for {@link FrameworkServlet#setContextAttribute(java.lang.String)}
     */
    public void setContextAttribute(String contextAttribute) {
        frameworkServlet.setContextAttribute(contextAttribute);
    }

        
    
    ///////////////////////////////////////////////
    // Proxying protected features below this point
    ///////////////////////////////////////////////

    ///////////////////////////////////////////////////////////
    // Rewriting public features below this point;
    // adapted from HttpServletBean, © and Apache License apply
    ///////////////////////////////////////////////////////////

    /**
     * Exact replica of {@link FrameworkServlet#SERVLET_CONTEXT_PREFIX}
     */
    // Rule 1: rewriting because it needs to reference this very class, not the original one
    public static final String SERVLET_CONTEXT_PREFIX = FiberFrameworkServlet.class.getName() + ".CONTEXT.";

    ///////////////////////////////////////////////////////////
    // Rewriting protected features below this point;
    // adapted from HttpServletBean, © and Apache License apply
    ///////////////////////////////////////////////////////////

    ///////////////////////////////////////////////////////////
    // Rewriting private features below this point;
    // adapted from HttpServletBean, © and Apache License apply
    ///////////////////////////////////////////////////////////

    /////////////////////////////
    // Untouched private features
    /////////////////////////////
    
    // private static final String INIT_PARAM_DELIMITERS = ",; \t\n";
    // private static final boolean responseGetStatusAvailable = ClassUtils.hasMethod(HttpServletResponse.class, "getStatus");
    // private String contextAttribute;
    // private Class<?> contextClass = DEFAULT_CONTEXT_CLASS;
    // private String contextId;
    // private String namespace;
    // private String contextConfigLocation;
    // private final ArrayList<ApplicationContextInitializer<ConfigurableApplicationContext>> contextInitializers = new ArrayList<ApplicationContextInitializer<ConfigurableApplicationContext>>();
    // private String contextInitializerClasses;
    // private boolean publishContext = true;
    // private boolean publishEvents = true;
    // private boolean threadContextInheritable = false;
    // private boolean dispatchOptionsRequest = false;
    // private boolean dispatchTraceRequest = false;
    // private WebApplicationContext webApplicationContext;
    // private boolean webApplicationContextInjected = false;
    // private boolean refreshEventReceived = false;
}