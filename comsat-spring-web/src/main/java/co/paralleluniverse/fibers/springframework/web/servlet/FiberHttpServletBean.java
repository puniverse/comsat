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

// TODO circlespainter: finish replacement according to general scheme

/**
 * Rewriting of {@link HttpServletBean} implementing the extended {@link FiberHttpServlet} fiber-blocking servlet interface 
 *
 * @author circlespainter
 */
public abstract class FiberHttpServletBean extends FiberHttpServlet implements EnvironmentCapable, EnvironmentAware {
    // References to original instance and a `protected`-opening proxy of it
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

    ////////////////////////////////////////////
    // Proxying public features below this point
    ////////////////////////////////////////////

    /**
     * See {@link HttpServletBean#getEnvironment()}
     */
    @Override
    public ConfigurableEnvironment getEnvironment() {
        return httpServletBean.getEnvironment();
    }

    /**
     * See {@link HttpServletBean#setEnvironment(org.springframework.core.env.Environment)}
     */
    @Override
    public void setEnvironment(Environment environment) {
        httpServletBean.setEnvironment(environment);
    }
    
    ///////////////////////////////////////////////
    // Proxying protected features below this point
    ///////////////////////////////////////////////

    /**
     * See {@link HttpServletBean#createEnvironment()}
     */
    protected ConfigurableEnvironment createEnvironment() {
        return httpServletBeanProtectedWrapper.createEnvironment();
    }

    ///////////////////////////////////////////////////////////
    // Rewriting public interface below this point;
    // adapted from HttpServletBean, © and Apache License apply
    ///////////////////////////////////////////////////////////

    /**
     * Overridden method that simply returns {@code null} when no ServletConfig set yet.
     * 
     * @see #getServletConfig()
     */
    // Rewriting in order to mirror logic but add same time letting the Fiber-enabled context be returned (rule 1)
    @Override
    public final ServletContext getServletContext() {
        return (getServletConfig() != null ? getServletConfig().getServletContext() : null);
    }

    /**
     * Overridden method that simply returns {@code null} when no ServletConfig set yet.
     *
     * @see #getServletConfig()
     */
    // Rewriting in order to mirror logic but add same time letting this very servlet name from this very fiber-enabled servlet config be returned  (rule 1)
    @Override
    public final String getServletName() {
	return (getServletConfig() != null ? getServletConfig().getServletName() : null);
    }

    ///////////////////////////////////////////////////////////
    // Rewriting protected features below this point;
    // adapted from HttpServletBean, © and Apache License apply
    ///////////////////////////////////////////////////////////

    /** Logger available to subclasses */
    // Rewriting because logging must cite this class, not the original one (rule 1)
    protected final Log logger = LogFactory.getLog(getClass());

    /**
     * Map config parameters onto bean properties of this servlet, and invoke subclass initialization.
     *
     * @throws ServletException if bean properties are invalid (or required properties are missing), or if subclass initialization fails.
     */
    // Rewriting because it depends on reimplemented `logger` (rule 2)
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
     * Proxying access to protected {@link FiberHttpServletBean#addRequiredPropertyProxy}
     */
    // Rewriting because it depends on rewritten `requiredProperties` (rule 2)
    protected final void addRequiredProperty(String property) {
        this.requiredProperties.add(property);
    }

    /**
     * Subclasses may override this to perform custom initialization.
     * All bean properties of this servlet will have been set before this
     * method is invoked.
     * <p>
     * This default implementation is empty.
     *
     * @throws ServletException if subclass initialization fails
     */
    // Rewriting just because it's less code than proxying (empty original method, rule 0)
    protected void initServletBean() throws ServletException {}

    /**
     * Initialize the BeanWrapper for this FiberHttpServletBean,
     * possibly with custom editors.
     * <p>
     * This default implementation is empty.
     *
     * @param bw the BeanWrapper to initialize
     * @throws BeansException if thrown by BeanWrapper methods
     * @see org.springframework.beans.BeanWrapper#registerCustomEditor
     */
    // Rewriting just because it's less code than proxying (empty original method, rule 0)
    protected void initBeanWrapper(BeanWrapper bw) throws BeansException {}

    ///////////////////////////////////////////////////////////
    // Rewriting private features below this point;
    // adapted from HttpServletBean, © and Apache License apply
    ///////////////////////////////////////////////////////////
    
    // Rewriting because rewritten `init` depends on it (rule 3)
    private final Set<String> requiredProperties = new HashSet<>();

    // Rewriting because rewritten `init` depends on it (rule 3)
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