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

import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeansException;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.EnvironmentCapable;
import org.springframework.web.servlet.HttpServletBean;
import org.springframework.web.servlet.FiberHttpServletBeanProtectedWrapper;

import co.paralleluniverse.fibers.servlet.FiberHttpServlet;

// TODO circlespainter: finish replacement according to general scheme

/**
 * Dual to {@link HttpServletBean} implementing the extended {@link FiberHttpServlet} fiber-blocking servlet interface 
 *
 * @author circlespainter
 */
public abstract class FiberHttpServletBean extends FiberHttpServlet
    implements EnvironmentCapable, EnvironmentAware
{
    private final HttpServletBean httpServletBean;
    private final FiberHttpServletBeanProtectedWrapper httpServletBeanProtectedWrapper;

    /**
     * Wrapping constructor.
     * 
     * @param httpServletBean The wrapped instance. Runtime type will need to be a concrete subclass overriding all non-final methods
     *                         that have been redefined here in order to forward the call.
     */
    public FiberHttpServletBean(HttpServletBean httpServletBean) {
        this.httpServletBean = httpServletBean;
        this.httpServletBeanProtectedWrapper = new FiberHttpServletBeanProtectedWrapper(httpServletBean);
    }

    //////////////////////////////////////////////////
    // Forwarding of public interface below this point
    //////////////////////////////////////////////////

    @Override
    public ConfigurableEnvironment getEnvironment() {
        return httpServletBean.getEnvironment();
    }

    @Override
    public void setEnvironment(Environment environment) {
        httpServletBean.setEnvironment(environment);
    }
    
    @Override
    public final void init() throws ServletException {
        httpServletBean.init();
    }

    /////////////////////////////////////////////////////
    // Forwarding of protected interface below this point
    /////////////////////////////////////////////////////

    protected final void addRequiredProperty(String property) {
        httpServletBeanProtectedWrapper.addRequiredPropertyProxy(property);
    }
    
    protected ConfigurableEnvironment createEnvironment() {
        return httpServletBeanProtectedWrapper.createEnvironment();
    }

    ////////////////////////////////////////////////////////
    // Reimplementation of public interface below this point
    ////////////////////////////////////////////////////////

    /**
     * See {@link HttpServletBean#getServletContext()}
     */
    // Reimplementing to mirror logic but add same time letting the Fiber-enabled context be returned
    @Override
    public final ServletContext getServletContext() {
        return (getServletConfig() != null ? getServletConfig().getServletContext() : null);
    }

    /**
     * See {@link HttpServletBean#getServletContext()}
     */
    // Reimplementing to mirror logic but add same time letting this very servlet name from this very fiber-enabled servlet config be returned 
    @Override
    public final String getServletName() {
	return (getServletConfig() != null ? getServletConfig().getServletName() : null);
    }

    ///////////////////////////////////////////////////////////
    // Reimplementation of protected interface below this point
    ///////////////////////////////////////////////////////////

    protected final Log logger = LogFactory.getLog(getClass());
    
    /**
     * See {@link HttpServletBean#initServletBean()}
     */
    protected void initServletBean() throws ServletException {}

    /**
     * See {@link HttpServletBean#initBeanWrapper()}
     */
    protected void initBeanWrapper(BeanWrapper bw) throws BeansException {}
}