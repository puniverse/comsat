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
package co.paralleluniverse.fibers.servlet;

import java.util.Enumeration;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

/**
 *
 * @author eitan
 */
class FiberServletConfig implements ServletConfig {
    private final ServletConfig scon;
    private final FiberServletContext sc;

    public FiberServletConfig(ServletConfig scon, FiberServletContext sc) {
        this.scon = scon;
        this.sc = sc;
    }

    @Override
    public ServletContext getServletContext() {
        return sc;
    }

    // Delegations
    @Override
    public String getServletName() {
        return scon.getServletName();
    }

    @Override
    public String getInitParameter(String name) {
        return scon.getInitParameter(name);
    }

    @Override
    public Enumeration<String> getInitParameterNames() {
        return scon.getInitParameterNames();
    }

    @Override
    public int hashCode() {
        return scon.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return scon.equals(obj);
    }

    @Override
    public String toString() {
        return scon.toString();
    }
}
