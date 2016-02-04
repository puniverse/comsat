/*
 * COMSAT
 * Copyright (c) 2013-2016, Parallel Universe Software Co. All rights reserved.
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

import java.io.IOException;
import java.security.Principal;
import java.util.Collection;
import java.util.Enumeration;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.Part;

/**
 *
 * @author eitan
 */
public class FiberHttpServletRequest extends FiberServletRequest implements HttpServletRequest {
    private final String contextPath;
    private final String servletPath;
    private final String pathInfo;

    public FiberHttpServletRequest(FiberHttpServlet servlet, javax.servlet.http.HttpServletRequest req) {
        super(servlet, req);
        // Jetty (what about other containers?) nullifies the following values in the request
        // when the service method returns. If we want to access them in an async context (in
        // the fiber), we need to capture them.
        pathInfo = req.getPathInfo();
        servletPath = req.getServletPath();
        contextPath = req.getContextPath();
    }

    @Override
    javax.servlet.http.HttpServletRequest getReq() {
        return (javax.servlet.http.HttpServletRequest) super.getReq();
    }

    // Delegations 
    @Override
    public String getAuthType() {
        return getReq().getAuthType();
    }

    @Override
    public Cookie[] getCookies() {
        return getReq().getCookies();
    }

    @Override
    public long getDateHeader(String name) {
        return getReq().getDateHeader(name);
    }

    @Override
    public String getHeader(String name) {
        return getReq().getHeader(name);
    }

    @Override
    public Enumeration<String> getHeaders(String name) {
        return getReq().getHeaders(name);
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        return getReq().getHeaderNames();
    }

    @Override
    public int getIntHeader(String name) {
        return getReq().getIntHeader(name);
    }

    @Override
    public String getMethod() {
        return getReq().getMethod();
    }

    @Override
    public String getPathInfo() {
        return pathInfo;
    }

    @Override
    public String getPathTranslated() {
        return getReq().getPathTranslated();
    }

    @Override
    public String getContextPath() {
        return contextPath;
    }

    @Override
    public String getQueryString() {
        return getReq().getQueryString();
    }

    @Override
    public String getRemoteUser() {
        return getReq().getRemoteUser();
    }

    @Override
    public boolean isUserInRole(String role) {
        return getReq().isUserInRole(role);
    }

    @Override
    public Principal getUserPrincipal() {
        return getReq().getUserPrincipal();
    }

    @Override
    public String getRequestedSessionId() {
        return getReq().getRequestedSessionId();
    }

    @Override
    public String getRequestURI() {
        return getReq().getRequestURI();
    }

    @Override
    public StringBuffer getRequestURL() {
        return getReq().getRequestURL();
    }

    @Override
    public String getServletPath() {
        return servletPath;
    }

    @Override
    public HttpSession getSession(boolean create) {
        return getReq().getSession(create);
    }

    @Override
    public HttpSession getSession() {
        return getReq().getSession();
    }

    @Override
    public boolean isRequestedSessionIdValid() {
        return getReq().isRequestedSessionIdValid();
    }

    @Override
    public boolean isRequestedSessionIdFromCookie() {
        return getReq().isRequestedSessionIdFromCookie();
    }

    @Override
    public boolean isRequestedSessionIdFromURL() {
        return getReq().isRequestedSessionIdFromURL();
    }

    @Override
    @Deprecated
    @SuppressWarnings("deprecation")
    public boolean isRequestedSessionIdFromUrl() {
        return getReq().isRequestedSessionIdFromUrl();
    }

    @Override
    public boolean authenticate(HttpServletResponse response) throws IOException, ServletException {
        return getReq().authenticate(response);
    }

    @Override
    public void login(String username, String password) throws ServletException {
        getReq().login(username, password);
    }

    @Override
    public void logout() throws ServletException {
        getReq().logout();
    }

    @Override
    public Collection<Part> getParts() throws IOException, ServletException {
        return getReq().getParts();
    }

    @Override
    public Part getPart(String name) throws IOException, ServletException {
        return getReq().getPart(name);
    }
}
