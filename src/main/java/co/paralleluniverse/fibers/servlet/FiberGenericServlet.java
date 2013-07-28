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
/*
 * Based on javax.servlet.http.HttpServlet in Tomcat
 * Copyright the Apache Software Foundation
 * Released under the ASF 2.0 license.
 */
package co.paralleluniverse.fibers.servlet;

import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.SuspendableRunnable;
import java.io.IOException;
import java.util.Enumeration;
import javax.servlet.AsyncContext;
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

/**
 * Defines a generic, protocol-independent servlet. To write an HTTP servlet for
 * use on the Web, extend {@link javax.servlet.http.HttpServlet} instead.
 * <p>
 * <code>FiberGenericServlet</code> implements the
 * <code>Servlet</code> and
 * <code>ServletConfig</code> interfaces.
 * <code>FiberGenericServlet</code> may be
 * directly extended by a servlet, although it's more common to extend a
 * protocol-specific subclass such as
 * <code>HttpServlet</code>.
 * <p>
 * <code>FiberGenericServlet</code> makes writing servlets easier. It provides simple
 * versions of the lifecycle methods
 * <code>init</code> and
 * <code>destroy</code>
 * and of the methods in the
 * <code>ServletConfig</code> interface.
 * <code>FiberGenericServlet</code> also implements the
 * <code>log</code> method,
 * declared in the
 * <code>ServletContext</code> interface.
 * <p>
 * To write a generic servlet, you need only override the abstract
 * <code>service</code> method.
 *
 * @version $Version$
 */
public abstract class FiberGenericServlet implements Servlet, ServletConfig,
        java.io.Serializable {
    private static final long serialVersionUID = 1L;
    private transient ServletConfig config;

    /**
     * Does nothing. All of the servlet initialization is done by one of the
     * <code>init</code> methods.
     */
    public FiberGenericServlet() {
        // NOOP
    }

    /**
     * Called by the servlet container to indicate to a servlet that the servlet
     * is being taken out of service. See {@link Servlet#destroy}.
     */
    @Override
    public void destroy() {
        // NOOP by default
    }

    /**
     * Returns a
     * <code>String</code> containing the value of the named
     * initialization parameter, or
     * <code>null</code> if the parameter does not
     * exist. See {@link ServletConfig#getInitParameter}.
     * <p>
     * This method is supplied for convenience. It gets the value of the named
     * parameter from the servlet's
     * <code>ServletConfig</code> object.
     *
     * @param name
     * a <code>String</code> specifying the name of the
     * initialization parameter
     * @return String a <code>String</code> containing the value of the
     * initialization parameter
     */
    @Override
    public String getInitParameter(String name) {
        return getServletConfig().getInitParameter(name);
    }

    /**
     * Returns the names of the servlet's initialization parameters as an
     * <code>Enumeration</code> of
     * <code>String</code> objects, or an empty
     * <code>Enumeration</code> if the servlet has no initialization parameters.
     * See {@link ServletConfig#getInitParameterNames}.
     * <p>
     * This method is supplied for convenience. It gets the parameter names from
     * the servlet's
     * <code>ServletConfig</code> object.
     *
     * @return Enumeration an enumeration of <code>String</code> objects
     * containing the names of the servlet's initialization parameters
     */
    @Override
    public Enumeration<String> getInitParameterNames() {
        return getServletConfig().getInitParameterNames();
    }

    /**
     * Returns this servlet's {@link ServletConfig} object.
     *
     * @return ServletConfig the <code>ServletConfig</code> object that
     * initialized this servlet
     */
    @Override
    public ServletConfig getServletConfig() {
        return config;
    }

    /**
     * Returns a reference to the {@link ServletContext} in which this servlet
     * is running. See {@link ServletConfig#getServletContext}.
     * <p>
     * This method is supplied for convenience. It gets the context from the
     * servlet's
     * <code>ServletConfig</code> object.
     *
     * @return ServletContext the <code>ServletContext</code> object passed to
     * this servlet by the <code>init</code> method
     */
    @Override
    public ServletContext getServletContext() {
        return getServletConfig().getServletContext();
    }

    /**
     * Returns information about the servlet, such as author, version, and
     * copyright. By default, this method returns an empty string. Override this
     * method to have it return a meaningful value. See
     * {@link Servlet#getServletInfo}.
     *
     * @return String information about this servlet, by default an empty string
     */
    @Override
    public String getServletInfo() {
        return "";
    }

    /**
     * Called by the servlet container to indicate to a servlet that the servlet
     * is being placed into service. See {@link Servlet#init}.
     * <p>
     * This implementation stores the {@link ServletConfig} object it receives
     * from the servlet container for later use. When overriding this form of
     * the method, call
     * <code>super.init(config)</code>.
     *
     * @param config
     * the <code>ServletConfig</code> object that contains
     * configuration information for this servlet
     * @exception ServletException
     * if an exception occurs that interrupts the servlet's
     * normal operation
     * @see UnavailableException
     */
    @Override
    public void init(ServletConfig config) throws ServletException {
        this.config = config;
        this.init();
    }

    /**
     * A convenience method which can be overridden so that there's no need to
     * call
     * <code>super.init(config)</code>.
     * <p>
     * Instead of overriding {@link #init(ServletConfig)}, simply override this
     * method and it will be called by
     * <code>FiberGenericServlet.init(ServletConfig config)</code>. The
     * <code>ServletConfig</code> object can still be retrieved via
     * {@link #getServletConfig}.
     *
     * @exception ServletException
     * if an exception occurs that interrupts the servlet's
     * normal operation
     */
    public void init() throws ServletException {
        // NOOP by default
    }

    /**
     * Writes the specified message to a servlet log file, prepended by the
     * servlet's name. See {@link ServletContext#log(String)}.
     *
     * @param msg
     * a <code>String</code> specifying the message to be written to
     * the log file
     */
    public void log(String msg) {
        getServletContext().log(getServletName() + ": " + msg);
    }

    /**
     * Writes an explanatory message and a stack trace for a given
     * <code>Throwable</code> exception to the servlet log file, prepended by
     * the servlet's name. See {@link ServletContext#log(String, Throwable)}.
     *
     * @param message
     * a <code>String</code> that describes the error or exception
     * @param t
     * the <code>java.lang.Throwable</code> error or exception
     */
    public void log(String message, Throwable t) {
        getServletContext().log(getServletName() + ": " + message, t);
    }

    /**
     * Called by the servlet container to allow the servlet to respond to a
     * request. See {@link Servlet#service}.
     * <p>
     * This method is declared abstract so subclasses, such as
     * <code>HttpServlet</code>, must override it.
     *
     * @param req
     * the <code>ServletRequest</code> object that contains the
     * client's request
     * @param res
     * the <code>ServletResponse</code> object that will contain the
     * servlet's response
     * @exception ServletException
     * if an exception occurs that interferes with the servlet's
     * normal operation occurred
     * @exception IOException
     * if an input or output exception occurs
     */
    @Override
    public final void service(final ServletRequest req, final ServletResponse res) {
        final AsyncContext ac = req.startAsync(req, res);
        ac.start(null);
        new Fiber(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                try {
                    suspendableService(req, res);
                } catch (ServletException | IOException ex) {
                    log("error in fiberServlet " + ex);
                } finally {
                    ac.complete();
                }
            }
        }).start();
    }

    protected abstract void suspendableService(ServletRequest req, ServletResponse res)
            throws ServletException, IOException, SuspendExecution;

    /**
     * Returns the name of this servlet instance. See
     * {@link ServletConfig#getServletName}.
     *
     * @return the name of this servlet instance
     */
    @Override
    public String getServletName() {
        return config.getServletName();
    }
}