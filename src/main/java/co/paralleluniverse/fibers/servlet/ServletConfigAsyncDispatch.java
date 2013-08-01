/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.fibers.servlet;

import java.util.Enumeration;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

/**
 *
 * @author eitan
 */
public class ServletConfigAsyncDispatch implements ServletConfig {
    private final ServletConfig scon;
    private final ServletContext sc;

    public ServletConfigAsyncDispatch(ServletConfig scon, ServletContext sc) {
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
