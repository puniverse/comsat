/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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
    public HttpServletRequestAsyncDispatch(javax.servlet.http.HttpServletRequest req) {
        super(req);
    }

    @Override
    javax.servlet.http.HttpServletRequest getReq() {
        return (javax.servlet.http.HttpServletRequest) super.getReq();
    }

    // Delegations 
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
        return getReq().getPathInfo();
    }

    public String getPathTranslated() {
        return getReq().getPathTranslated();
    }

    public String getContextPath() {
        return getReq().getContextPath();
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
        return getReq().getServletPath();
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
