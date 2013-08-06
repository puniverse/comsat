/*
 * COMSAT
 * Copyright (C) 2013, Parallel Universe Software Co. All rights reserved.
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
public class ServletContextAsyncDispatch implements javax.servlet.ServletContext {
    private final javax.servlet.ServletContext sc;
    private final ThreadLocal<AsyncContext> ac;

    public ServletContextAsyncDispatch(javax.servlet.ServletContext sc, ThreadLocal<AsyncContext> ac) {
        this.sc = sc;
        this.ac = ac;
    }

    public RequestDispatcerAsyncDispatch getRequestDispatcher(String path) {
        return new RequestDispatcerAsyncDispatch(path, ac.get());
//        return super.getRequestDispatcher(path);
    }

    public ServletContextAsyncDispatch getContext(String uripath) {
        return new ServletContextAsyncDispatch(sc.getContext(uripath), ac);
    }

    public RequestDispatcher getNamedDispatcher(String name) {
        throw new UnsupportedOperationException("Not yet implemented");
//        return new RequestDispatcher(sc.getNamedDispatcher(name).;
    }

    public String getContextPath() {
        return sc.getContextPath();
    }

    public int getMajorVersion() {
        return sc.getMajorVersion();
    }

    public int getMinorVersion() {
        return sc.getMinorVersion();
    }

    public int getEffectiveMajorVersion() {
        return sc.getEffectiveMajorVersion();
    }

    public int getEffectiveMinorVersion() {
        return sc.getEffectiveMinorVersion();
    }

    public String getMimeType(String file) {
        return sc.getMimeType(file);
    }

    public Set<String> getResourcePaths(String path) {
        return sc.getResourcePaths(path);
    }

    public URL getResource(String path) throws MalformedURLException {
        return sc.getResource(path);
    }

    public InputStream getResourceAsStream(String path) {
        return sc.getResourceAsStream(path);
    }

    public Servlet getServlet(String name) throws ServletException {
        return sc.getServlet(name);
    }

    public Enumeration<Servlet> getServlets() {
        return sc.getServlets();
    }

    public Enumeration<String> getServletNames() {
        return sc.getServletNames();
    }

    public void log(String msg) {
        sc.log(msg);
    }

    public void log(Exception exception, String msg) {
        sc.log(exception, msg);
    }

    public void log(String message, Throwable throwable) {
        sc.log(message, throwable);
    }

    public String getRealPath(String path) {
        return sc.getRealPath(path);
    }

    public String getServerInfo() {
        return sc.getServerInfo();
    }

    public String getInitParameter(String name) {
        return sc.getInitParameter(name);
    }

    public Enumeration<String> getInitParameterNames() {
        return sc.getInitParameterNames();
    }

    public boolean setInitParameter(String name, String value) {
        return sc.setInitParameter(name, value);
    }

    public Object getAttribute(String name) {
        return sc.getAttribute(name);
    }

    public Enumeration<String> getAttributeNames() {
        return sc.getAttributeNames();
    }

    public void setAttribute(String name, Object object) {
        sc.setAttribute(name, object);
    }

    public void removeAttribute(String name) {
        sc.removeAttribute(name);
    }

    public String getServletContextName() {
        return sc.getServletContextName();
    }

    public Dynamic addServlet(String servletName, String className) {
        return sc.addServlet(servletName, className);
    }

    public Dynamic addServlet(String servletName, Servlet servlet) {
        return sc.addServlet(servletName, servlet);
    }

    public Dynamic addServlet(String servletName, Class<? extends Servlet> servletClass) {
        return sc.addServlet(servletName, servletClass);
    }

    public <T extends Servlet> T createServlet(Class<T> clazz) throws ServletException {
        return sc.createServlet(clazz);
    }

    public ServletRegistration getServletRegistration(String servletName) {
        return sc.getServletRegistration(servletName);
    }

    public Map<String, ? extends ServletRegistration> getServletRegistrations() {
        return sc.getServletRegistrations();
    }

    public FilterRegistration.Dynamic addFilter(String filterName, String className) {
        return sc.addFilter(filterName, className);
    }

    public FilterRegistration.Dynamic addFilter(String filterName, Filter filter) {
        return sc.addFilter(filterName, filter);
    }

    public FilterRegistration.Dynamic addFilter(String filterName, Class<? extends Filter> filterClass) {
        return sc.addFilter(filterName, filterClass);
    }

    public <T extends Filter> T createFilter(Class<T> clazz) throws ServletException {
        return sc.createFilter(clazz);
    }

    public FilterRegistration getFilterRegistration(String filterName) {
        return sc.getFilterRegistration(filterName);
    }

    public Map<String, ? extends FilterRegistration> getFilterRegistrations() {
        return sc.getFilterRegistrations();
    }

    public SessionCookieConfig getSessionCookieConfig() {
        return sc.getSessionCookieConfig();
    }

    public void setSessionTrackingModes(Set<SessionTrackingMode> sessionTrackingModes) {
        sc.setSessionTrackingModes(sessionTrackingModes);
    }

    public Set<SessionTrackingMode> getDefaultSessionTrackingModes() {
        return sc.getDefaultSessionTrackingModes();
    }

    public Set<SessionTrackingMode> getEffectiveSessionTrackingModes() {
        return sc.getEffectiveSessionTrackingModes();
    }

    public void addListener(String className) {
        sc.addListener(className);
    }

    public <T extends EventListener> void addListener(T t) {
        sc.addListener(t);
    }

    public void addListener(Class<? extends EventListener> listenerClass) {
        sc.addListener(listenerClass);
    }

    public <T extends EventListener> T createListener(Class<T> clazz) throws ServletException {
        return sc.createListener(clazz);
    }

    public JspConfigDescriptor getJspConfigDescriptor() {
        return sc.getJspConfigDescriptor();
    }

    public ClassLoader getClassLoader() {
        return sc.getClassLoader();
    }

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
}
