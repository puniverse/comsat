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

import java.io.IOException;
import java.security.Principal;
import java.util.Collection;
import java.util.Enumeration;
import javax.servlet.AsyncContext;
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
public class HttpServletRequestAsyncDispatch extends ServletRequestAsyncDispatch implements HttpServletRequest{
    private final String contextPath;
    private final String servletPath;
    private final String pathInfo;

    public HttpServletRequestAsyncDispatch(javax.servlet.http.HttpServletRequest req) {
        super(req);
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

    public Cookie[] getCookies() {
        return getReq().getCookies();
    }

    public long getDateHeader(String name) {
        return getReq().getDateHeader(name);
    }

    public String getHeader(String name) {
        return getReq().getHeader(name);
    }

    public Enumeration<String> getHeaders(String name) {
        return getReq().getHeaders(name);
    }

    public Enumeration<String> getHeaderNames() {
        return getReq().getHeaderNames();
    }

    public int getIntHeader(String name) {
        return getReq().getIntHeader(name);
    }

    public String getMethod() {
        return getReq().getMethod();
    }

    public String getPathInfo() {
        return pathInfo;
    }

    public String getPathTranslated() {
        return getReq().getPathTranslated();
    }

    public String getContextPath() {
        return contextPath;
    }

    public String getQueryString() {
        return getReq().getQueryString();
    }

    public String getRemoteUser() {
        return getReq().getRemoteUser();
    }

    public boolean isUserInRole(String role) {
        return getReq().isUserInRole(role);
    }

    public Principal getUserPrincipal() {
        return getReq().getUserPrincipal();
    }

    public String getRequestedSessionId() {
        return getReq().getRequestedSessionId();
    }

    public String getRequestURI() {
        return getReq().getRequestURI();
    }

    public StringBuffer getRequestURL() {
        return getReq().getRequestURL();
    }

    public String getServletPath() {
        return servletPath;
    }

    public HttpSession getSession(boolean create) {
        return getReq().getSession(create);
    }

    public HttpSession getSession() {
        return getReq().getSession();
    }

    public boolean isRequestedSessionIdValid() {
        return getReq().isRequestedSessionIdValid();
    }

    public boolean isRequestedSessionIdFromCookie() {
        return getReq().isRequestedSessionIdFromCookie();
    }

    public boolean isRequestedSessionIdFromURL() {
        return getReq().isRequestedSessionIdFromURL();
    }

    public boolean isRequestedSessionIdFromUrl() {
        return getReq().isRequestedSessionIdFromUrl();
    }

    public boolean authenticate(HttpServletResponse response) throws IOException, ServletException {
        return getReq().authenticate(response);
    }

    public void login(String username, String password) throws ServletException {
        getReq().login(username, password);
    }

    public void logout() throws ServletException {
        getReq().logout();
    }

    public Collection<Part> getParts() throws IOException, ServletException {
        return getReq().getParts();
    }

    public Part getPart(String name) throws IOException, ServletException {
        return getReq().getPart(name);
    }
}
