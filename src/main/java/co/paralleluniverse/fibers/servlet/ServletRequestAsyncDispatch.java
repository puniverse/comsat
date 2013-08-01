/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.fibers.servlet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;
import javax.servlet.AsyncContext;
import javax.servlet.DispatcherType;
import javax.servlet.ServletContext;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

/**
 *
 * @author eitan
 */
public class ServletRequestAsyncDispatch implements ServletRequest {
    private final ServletRequest req;

    public ServletRequestAsyncDispatch(ServletRequest req) {
        this.req = req;
    }

    public RequestDispatcerAsyncDispatch getRequestDispatcher(String path) {
        return new RequestDispatcerAsyncDispatch(path, req.getAsyncContext());
//        return super.getRequestDispatcher(path);
    }

    javax.servlet.ServletRequest getReq() {
        return req;
    }

    // Delegations
    public Object getAttribute(String name) {
        return req.getAttribute(name);
    }

    public Enumeration<String> getAttributeNames() {
        return req.getAttributeNames();
    }

    public String getCharacterEncoding() {
        return req.getCharacterEncoding();
    }

    public void setCharacterEncoding(String env) throws UnsupportedEncodingException {
        req.setCharacterEncoding(env);
    }

    public int getContentLength() {
        return req.getContentLength();
    }

    public String getContentType() {
        return req.getContentType();
    }

    public ServletInputStream getInputStream() throws IOException {
        return req.getInputStream();
    }

    public String getParameter(String name) {
        return req.getParameter(name);
    }

    public Enumeration<String> getParameterNames() {
        return req.getParameterNames();
    }

    public String[] getParameterValues(String name) {
        return req.getParameterValues(name);
    }

    public Map<String, String[]> getParameterMap() {
        return req.getParameterMap();
    }

    public String getProtocol() {
        return req.getProtocol();
    }

    public String getScheme() {
        return req.getScheme();
    }

    public String getServerName() {
        return req.getServerName();
    }

    public int getServerPort() {
        return req.getServerPort();
    }

    public BufferedReader getReader() throws IOException {
        return req.getReader();
    }

    public String getRemoteAddr() {
        return req.getRemoteAddr();
    }

    public String getRemoteHost() {
        return req.getRemoteHost();
    }

    public void setAttribute(String name, Object o) {
        req.setAttribute(name, o);
    }

    public void removeAttribute(String name) {
        req.removeAttribute(name);
    }

    public Locale getLocale() {
        return req.getLocale();
    }

    public Enumeration<Locale> getLocales() {
        return req.getLocales();
    }

    public boolean isSecure() {
        return req.isSecure();
    }

    public String getRealPath(String path) {
        return req.getRealPath(path);
    }

    public int getRemotePort() {
        return req.getRemotePort();
    }

    public String getLocalName() {
        return req.getLocalName();
    }

    public String getLocalAddr() {
        return req.getLocalAddr();
    }

    public int getLocalPort() {
        return req.getLocalPort();
    }

    public ServletContext getServletContext() {
        return req.getServletContext();
    }

    public AsyncContext startAsync() throws IllegalStateException {
        throw new UnsupportedOperationException("you can't use startAsync mechanism from fiber.servlet");
    }

    public AsyncContext startAsync(javax.servlet.ServletRequest servletRequest, ServletResponse servletResponse) throws IllegalStateException {
        throw new UnsupportedOperationException("you can't use startAsync mechanism from fiber.servlet");
    }

    public boolean isAsyncStarted() {
        return req.isAsyncStarted();
    }

    public boolean isAsyncSupported() {
        return false;
    }

    public AsyncContext getAsyncContext() {
        return null;
    }

    public DispatcherType getDispatcherType() {
        return req.getDispatcherType();
    }

    public int hashCode() {
        return req.hashCode();
    }

    public boolean equals(Object obj) {
        return req.equals(obj);
    }

    public String toString() {
        return req.toString();
    }

}
