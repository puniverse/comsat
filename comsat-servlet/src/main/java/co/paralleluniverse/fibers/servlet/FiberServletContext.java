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

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.EventListener;
import java.util.Map;
import java.util.Set;
import javax.servlet.AsyncContext;
import javax.servlet.Filter;
import javax.servlet.FilterRegistration;
import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;
import javax.servlet.ServletRegistration.Dynamic;
import javax.servlet.SessionCookieConfig;
import javax.servlet.SessionTrackingMode;
import javax.servlet.descriptor.JspConfigDescriptor;

/**
 *
 * @author eitan
 */
class FiberServletContext implements ServletContext {
    private final ServletContext sc;
    private final ThreadLocal<AsyncContext> ac;

    public FiberServletContext(ServletContext sc, ThreadLocal<AsyncContext> ac) {
        this.sc = sc;
        this.ac = ac;
    }

    @Override
    public FiberRequestDispatcer getRequestDispatcher(String path) {
        return new FiberRequestDispatcer(path, ac.get());
    }

    @Override
    public FiberServletContext getContext(String uripath) {
        return new FiberServletContext(sc.getContext(uripath), ac);
    }

    @Override
    public RequestDispatcher getNamedDispatcher(String name) {
        throw new UnsupportedOperationException("Not yet implemented");
//        return new RequestDispatcher(sc.getNamedDispatcher(name).;
    }

    @Override
    public String getContextPath() {
        return sc.getContextPath();
    }

    @Override
    public int getMajorVersion() {
        return sc.getMajorVersion();
    }

    @Override
    public int getMinorVersion() {
        return sc.getMinorVersion();
    }

    @Override
    public int getEffectiveMajorVersion() {
        return sc.getEffectiveMajorVersion();
    }

    @Override
    public int getEffectiveMinorVersion() {
        return sc.getEffectiveMinorVersion();
    }

    @Override
    public String getMimeType(String file) {
        return sc.getMimeType(file);
    }

    @Override
    public Set<String> getResourcePaths(String path) {
        return sc.getResourcePaths(path);
    }

    @Override
    public URL getResource(String path) throws MalformedURLException {
        return sc.getResource(path);
    }

    @Override
    public InputStream getResourceAsStream(String path) {
        return sc.getResourceAsStream(path);
    }

    @Override
    public Servlet getServlet(String name) throws ServletException {
        return sc.getServlet(name);
    }

    @Override
    public Enumeration<Servlet> getServlets() {
        return sc.getServlets();
    }

    @Override
    public Enumeration<String> getServletNames() {
        return sc.getServletNames();
    }

    @Override
    public void log(String msg) {
        sc.log(msg);
    }

    @Override
    public void log(Exception exception, String msg) {
        sc.log(exception, msg);
    }

    @Override
    public void log(String message, Throwable throwable) {
        sc.log(message, throwable);
    }

    @Override
    public String getRealPath(String path) {
        return sc.getRealPath(path);
    }

    @Override
    public String getServerInfo() {
        return sc.getServerInfo();
    }

    @Override
    public String getInitParameter(String name) {
        return sc.getInitParameter(name);
    }

    @Override
    public Enumeration<String> getInitParameterNames() {
        return sc.getInitParameterNames();
    }

    @Override
    public boolean setInitParameter(String name, String value) {
        return sc.setInitParameter(name, value);
    }

    @Override
    public Object getAttribute(String name) {
        return sc.getAttribute(name);
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        return sc.getAttributeNames();
    }

    @Override
    public void setAttribute(String name, Object object) {
        sc.setAttribute(name, object);
    }

    @Override
    public void removeAttribute(String name) {
        sc.removeAttribute(name);
    }

    @Override
    public String getServletContextName() {
        return sc.getServletContextName();
    }

    @Override
    public Dynamic addServlet(String servletName, String className) {
        return sc.addServlet(servletName, className);
    }

    @Override
    public Dynamic addServlet(String servletName, Servlet servlet) {
        return sc.addServlet(servletName, servlet);
    }

    @Override
    public Dynamic addServlet(String servletName, Class<? extends Servlet> servletClass) {
        return sc.addServlet(servletName, servletClass);
    }

    @Override
    public <T extends Servlet> T createServlet(Class<T> clazz) throws ServletException {
        return sc.createServlet(clazz);
    }

    @Override
    public ServletRegistration getServletRegistration(String servletName) {
        return sc.getServletRegistration(servletName);
    }

    @Override
    public Map<String, ? extends ServletRegistration> getServletRegistrations() {
        return sc.getServletRegistrations();
    }

    @Override
    public FilterRegistration.Dynamic addFilter(String filterName, String className) {
        return sc.addFilter(filterName, className);
    }

    @Override
    public FilterRegistration.Dynamic addFilter(String filterName, Filter filter) {
        return sc.addFilter(filterName, filter);
    }

    @Override
    public FilterRegistration.Dynamic addFilter(String filterName, Class<? extends Filter> filterClass) {
        return sc.addFilter(filterName, filterClass);
    }

    @Override
    public <T extends Filter> T createFilter(Class<T> clazz) throws ServletException {
        return sc.createFilter(clazz);
    }

    @Override
    public FilterRegistration getFilterRegistration(String filterName) {
        return sc.getFilterRegistration(filterName);
    }

    @Override
    public Map<String, ? extends FilterRegistration> getFilterRegistrations() {
        return sc.getFilterRegistrations();
    }

    @Override
    public SessionCookieConfig getSessionCookieConfig() {
        return sc.getSessionCookieConfig();
    }

    @Override
    public void setSessionTrackingModes(Set<SessionTrackingMode> sessionTrackingModes) {
        sc.setSessionTrackingModes(sessionTrackingModes);
    }

    @Override
    public Set<SessionTrackingMode> getDefaultSessionTrackingModes() {
        return sc.getDefaultSessionTrackingModes();
    }

    @Override
    public Set<SessionTrackingMode> getEffectiveSessionTrackingModes() {
        return sc.getEffectiveSessionTrackingModes();
    }

    @Override
    public void addListener(String className) {
        sc.addListener(className);
    }

    @Override
    public <T extends EventListener> void addListener(T t) {
        sc.addListener(t);
    }

    @Override
    public void addListener(Class<? extends EventListener> listenerClass) {
        sc.addListener(listenerClass);
    }

    @Override
    public <T extends EventListener> T createListener(Class<T> clazz) throws ServletException {
        return sc.createListener(clazz);
    }

    @Override
    public JspConfigDescriptor getJspConfigDescriptor() {
        return sc.getJspConfigDescriptor();
    }

    @Override
    public ClassLoader getClassLoader() {
        return sc.getClassLoader();
    }

    @Override
    public void declareRoles(String... roleNames) {
        sc.declareRoles(roleNames);
    }

    @Override
    public int hashCode() {
        return sc.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return sc.equals(obj);
    }

    @Override
    public String toString() {
        return sc.toString();
    }

    @Override
    public String getVirtualServerName() {
        return sc.getVirtualServerName();
    }
}
