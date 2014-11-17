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
package org.springframework.web.servlet;

// TODO circlespainter: I don't like "patching" the original API's packages like this but it's the only way to delegate to protected members in Java. Maybe better generating this class at runtime.
 
import org.apache.commons.logging.Log;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * {@link HttpServletBean} wrapper in the same package, publishing access to its protected members for call proxying by the Comsat integration module
 * 
 * @author circlespainter
 */
public final class FiberHttpServletBeanProtectedWrapper {
    private final HttpServletBean httpServletBean;

    /**
     * Wrapping constructor
     * 
     * @param httpServletBean 
     */
    public FiberHttpServletBeanProtectedWrapper(HttpServletBean httpServletBean) {
        this.httpServletBean = httpServletBean;
        this.logger = httpServletBean.logger;
    }

    /** @see FrameworkServlet#logger */
    public final Log logger;

    /** @see HttpServletBean#addRequiredProperty(java.lang.String) */
    public final void addRequiredProperty(String property) {
        httpServletBean.addRequiredProperty(property);
    }
    
    /** @see HttpServletBean#createEnvironment()} */
    public final ConfigurableEnvironment createEnvironment() {
        return httpServletBean.createEnvironment();
    }
}