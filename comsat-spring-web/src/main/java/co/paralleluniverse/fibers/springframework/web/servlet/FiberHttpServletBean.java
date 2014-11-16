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
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import javax.servlet.ServletConfig;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.PropertyValues;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceEditor;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.StringUtils;
import org.springframework.web.context.support.ServletContextResourceLoader;

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
     * Constructor rule 1: wrapping constructor only as this class is abstract and not a full replica
     * 
     * @param httpServletBean The wrapped instance
     */
    public FiberHttpServletBean(HttpServletBean httpServletBean) {
        this.httpServletBean = httpServletBean;
        this.httpServletBeanProtectedWrapper = new FiberHttpServletBeanProtectedWrapper(httpServletBean);
    }

    ////////////////////////////////////////////
    // Proxying public features below this point
    ////////////////////////////////////////////

    /**
     * Proxy for {@link HttpServletBean#getEnvironment()}
     */
    @Override
    public ConfigurableEnvironment getEnvironment() {
        return httpServletBean.getEnvironment();
    }

    /**
     * Proxy for {@link HttpServletBean#setEnvironment(org.springframework.core.env.Environment)}
     */
    @Override
    public void setEnvironment(Environment environment) {
        httpServletBean.setEnvironment(environment);
    }
    
    ///////////////////////////////////////////////
    // Proxying protected features below this point
    ///////////////////////////////////////////////

    /**
     * Proxy for {@link HttpServletBean#createEnvironment()}
     */
    protected ConfigurableEnvironment createEnvironment() {
        return httpServletBeanProtectedWrapper.createEnvironment();
    }

    ///////////////////////////////////////////////////////////
    // Rewriting public features below this point;
    // adapted from HttpServletBean, © and Apache License apply
    ///////////////////////////////////////////////////////////

    /**
     * Exact replica of {@link HttpServletBean#getServletContext()}
     */
    // Rule 1: rewriting in order to mirror logic but add same time letting the Fiber-enabled context be returned
    @Override
    public final ServletContext getServletContext() {
        return (getServletConfig() != null ? getServletConfig().getServletContext() : null);
    }

    /**
     * Exact replica of {@link HttpServletBean#getServletName()}
     */
    // Rule 1: rewriting in order to mirror logic but add same time letting this very servlet name from this very fiber-enabled servlet config be returned
    @Override
    public final String getServletName() {
	return (getServletConfig() != null ? getServletConfig().getServletName() : null);
    }

    ///////////////////////////////////////////////////////////
    // Rewriting protected features below this point;
    // adapted from HttpServletBean, © and Apache License apply
    ///////////////////////////////////////////////////////////

    /** Exact replica of {@link HttpServletBean#logger} */
    // Rule 1: rewriting because logging must cite this class, not the original one
    protected final Log logger = LogFactory.getLog(getClass());

    /**
     * Exact replica of {@link HttpServletBean#init()}
     */
    // Rule 2: rewriting because it depends on reimplemented `logger`
    @Override
    public final void init() throws ServletException {
	if (logger.isDebugEnabled()) {
            logger.debug("Initializing servlet '" + getServletName() + "'");
	}

	// Set bean properties from init parameters.
	try {
		PropertyValues pvs = new ServletConfigPropertyValues(getServletConfig(), this.requiredProperties);
		BeanWrapper bw = PropertyAccessorFactory.forBeanPropertyAccess(this);
		ResourceLoader resourceLoader = new ServletContextResourceLoader(getServletContext());
		bw.registerCustomEditor(Resource.class, new ResourceEditor(resourceLoader, getEnvironment()));
		initBeanWrapper(bw);
		bw.setPropertyValues(pvs, true);
	}
	catch (BeansException ex) {
		logger.error("Failed to set bean properties on servlet '" + getServletName() + "'", ex);
		throw ex;
	}

	// Let subclasses do whatever initialization they like.
	initServletBean();

        if (logger.isDebugEnabled()) {
		logger.debug("Servlet '" + getServletName() + "' configured successfully");
	}
    }

    /**
     * Exact replica of {@link HttpServletBean#addRequiredProperty(java.lang.String)}
     */
    // Rule 2: rewriting because it depends on rewritten `requiredProperties`
    protected final void addRequiredProperty(String property) {
        this.requiredProperties.add(property);
    }

    /**
     * Exact replica of {@link HttpServletBean#addRequiredProperty(java.lang.String)}
     */
    // Rule 4: rewriting just because it's less code than proxying (empty original method)
    protected void initServletBean() throws ServletException {}

    /**
     * Exact replica of {@link HttpServletBean#initBeanWrapper(org.springframework.beans.BeanWrapper)}
     */
    // Rule 4: rewriting just because it's less code than proxying (empty original method)
    protected void initBeanWrapper(BeanWrapper bw) throws BeansException {}

    ///////////////////////////////////////////////////////////
    // Rewriting private features below this point;
    // adapted from HttpServletBean, © and Apache License apply
    ///////////////////////////////////////////////////////////
    
    /**
     * Exact replica of {@link HttpServletBean#requiredProperties}
     */
    // Rule 3: rewriting because rewritten `init` depends on it
    private final Set<String> requiredProperties = new HashSet<>();

    /**
     * Exact replica of {@link HttpServletBean.ServletConfigPropertyValues}
     */
    // Rule 3: rewriting because rewritten `init` depends on it
    private static class ServletConfigPropertyValues extends MutablePropertyValues {
        public ServletConfigPropertyValues(ServletConfig config, Set<String> requiredProperties)
                throws ServletException {

            Set<String> missingProps = (requiredProperties != null && !requiredProperties.isEmpty())
                    ? new HashSet<>(requiredProperties) : null;

            Enumeration<String> en = config.getInitParameterNames();
            while (en.hasMoreElements()) {
                String property = en.nextElement();
                Object value = config.getInitParameter(property);
                addPropertyValue(new PropertyValue(property, value));
                if (missingProps != null) {
                    missingProps.remove(property);
                }
            }

            if (missingProps != null && missingProps.size() > 0) {
                throw new ServletException(
                        "Initialization from ServletConfig for servlet '" + config.getServletName()
                        + "' failed; the following required properties were missing: "
                        + StringUtils.collectionToDelimitedString(missingProps, ", "));
            }
        }
    }

}