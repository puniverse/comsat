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
 * Based on org.springframework.web.servlet.FrameworkServlet in Spring Framework Web MVC
 * Copyright the original authors Rod Johnson and Juergen Hoeller
 * Released under the ASF 2.0 license.
 */
package co.paralleluniverse.fibers.springframework.web.servlet;

import co.paralleluniverse.fibers.SuspendExecution;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeansException;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.EnvironmentCapable;
import org.springframework.web.servlet.HttpServletBean;
import org.springframework.web.servlet.FiberHttpServletBeanProtectedWrapper;

import co.paralleluniverse.fibers.servlet.FiberHttpServlet;
import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.logging.Log;


/**
 * Mirror of {@link HttpServletBean} implementing the extended {@link FiberHttpServlet} fiber-blocking servlet interface 
 *
 * @author circlespainter
 */
public abstract class FiberHttpServletBean extends FiberHttpServlet implements EnvironmentCapable, EnvironmentAware {
    // References to original instance and a `protected`-opening proxy of it
    private final HttpServletBean httpServletBean;
    private final FiberHttpServletBeanProtectedWrapper httpServletBeanProtectedWrapper;

    /**
     * Wrapping constructor
     * 
     * @param httpServletBean The wrapped instance
     */
    // Constructor rule 1: wrapping constructor only as this class is abstract and not a full replica
    public FiberHttpServletBean(HttpServletBean httpServletBean) {
        this.httpServletBean = httpServletBean;
        this.httpServletBeanProtectedWrapper = new FiberHttpServletBeanProtectedWrapper(httpServletBean);
        this.logger = httpServletBeanProtectedWrapper.logger;
    }

    ////////////////////////////////////////////
    // Proxying public features below this point
    ////////////////////////////////////////////

    /** @see HttpServletBean#getEnvironment() */
    @Override
    public ConfigurableEnvironment getEnvironment() {
        return httpServletBean.getEnvironment();
    }

    /** @see HttpServletBean#setEnvironment(org.springframework.core.env.Environment) */
    @Override
    public void setEnvironment(Environment environment) {
        httpServletBean.setEnvironment(environment);
    }
    
    @Override
    public final void init() throws ServletException {
        httpServletBean.init();
    }
    
    ///////////////////////////////////////////////
    // Proxying protected features below this point
    ///////////////////////////////////////////////

    /** @see HttpServletBean#logger */
    // TODO circlespainter: logger should really be re-implemented but avoiding doing so also avoids a lot of other re-implementations
    protected final Log logger;
    
    /** @see HttpServletBean#createEnvironment() */
    protected ConfigurableEnvironment createEnvironment() {
        return httpServletBeanProtectedWrapper.createEnvironment();
    }

    //////////////////////////////////////////////////////////////////////
    // Re-implementing public features below this point;
    // derived from HttpServletBean, relevant copyright and licences apply
    //////////////////////////////////////////////////////////////////////

    /** Equivalent of {@link HttpServletBean#getServletContext()}, only returning fiber-enabled context */
    // Rule 1: re-implementing in order to mirror logic but add same time letting the fiber-enabled context be returned
    @Override
    public final ServletContext getServletContext() {
        return (getServletConfig() != null ? getServletConfig().getServletContext() : null);
    }

    /** Equivalent of {@link HttpServletBean#getServletName()} */
    // Rule 1: re-implementing in order to mirror logic but add same time letting this very servlet name from this very fiber-enabled servlet config be returned
    @Override
    public final String getServletName() {
	return (getServletConfig() != null ? getServletConfig().getServletName() : null);
    }

    //////////////////////////////////////////////////////////////////////
    // Re-implementing protected features below this point;
    // derived from HttpServletBean, relevant copyright and licences apply
    //////////////////////////////////////////////////////////////////////

    /** @see HttpServletBean#initServletBean(java.lang.String) */
    // Rule 4: re-implementing just because it's less code than proxying (empty original method)
    protected void initServletBean() throws ServletException {}

    /** @see HttpServletBean#initBeanWrapper(org.springframework.beans.BeanWrapper)} */
    // Rule 4: re-implementing just because it's less code than proxying (empty original method)
    protected void initBeanWrapper(BeanWrapper bw) throws BeansException {}
    
    // TODO circlespainter: comment & JavaDocs (opening access to `DispatcherServletForwarder`)
    @Override
    public void doHead(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException, SuspendExecution {
        super.doHead(req, resp); //To change body of generated methods, choose Tools | Templates.
    }

    // TODO circlespainter: comment & JavaDocs (opening access to `DispatcherServletForwarder`)
    @Override
    public long getLastModified(HttpServletRequest req) {
        return super.getLastModified(req); //To change body of generated methods, choose Tools | Templates.
    }
}