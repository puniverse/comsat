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
/*
 * Based on org.springframework.web.servlet.FrameworkServlet in Spring Framework Web MVC
 * Copyright the original authors Rod Johnson, Juergen Hoeller, Sam Brannen,
 * Chris Beams, Rossen Stoyanchev, Phillip Webb.
 * Released under the ASF 2.0 license.
 */
package co.paralleluniverse.fibers.springframework.web.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.i18n.LocaleContext;
import org.springframework.web.context.ConfigurableWebApplicationContext;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.FiberFrameworkServletProtectedWrapper;
import org.springframework.web.servlet.FrameworkServlet;

import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.Suspendable;
import javax.servlet.http.HttpServletResponseWrapper;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * Equivalent of {@link FrameworkServlet} extending the fiber-blocking {@link FiberHttpServletBean}
 * 
 * @author circlespainter
 */
public abstract class FiberFrameworkServlet extends FiberHttpServletBean implements ApplicationContextAware {
    // References to original instance and a `protected`-opening proxy of it
    protected final FrameworkServlet frameworkServlet; // Leaving it open for `FiberDispatcherServlet` to access in constructor
    private final FiberFrameworkServletProtectedWrapper fiberFrameworkServletProtectedWrapper;

    /**
     * Wrapping constructor
     * 
     * @param frameworkServlet The wrapped instance
     */
    // Constructor rule 1: wrapping constructor only as this class is abstract and not a full replica
    public FiberFrameworkServlet(FrameworkServlet frameworkServlet) {
        super(frameworkServlet);
        this.frameworkServlet = frameworkServlet;
        this.fiberFrameworkServletProtectedWrapper = new FiberFrameworkServletProtectedWrapper(frameworkServlet);
    }

    ////////////////////////////////////////////
    // Proxying public features below this point
    ////////////////////////////////////////////

    /** @see FrameworkServlet#DEFAULT_NAMESPACE_SUFFIX */
    public static final String DEFAULT_NAMESPACE_SUFFIX = FrameworkServlet.DEFAULT_NAMESPACE_SUFFIX;

    /** @see FrameworkServlet#DEFAULT_CONTEXT_CLASS */
    public static final Class<?> DEFAULT_CONTEXT_CLASS = FrameworkServlet.DEFAULT_CONTEXT_CLASS;

    /** @see FrameworkServlet#SERVLET_CONTEXT_PREFIX */
    public static final String SERVLET_CONTEXT_PREFIX = FrameworkServlet.SERVLET_CONTEXT_PREFIX;

    /** @see FiberFrameworkServlet#getServletContextAttributeName() */
    public String getServletContextAttributeName() {
        return frameworkServlet.getServletContextAttributeName();
    }

    /** @see FiberFrameworkServlet#setThreadContextInheritable(boolean) */
    public void setThreadContextInheritable(boolean threadContextInheritable) {
        frameworkServlet.setThreadContextInheritable(threadContextInheritable);
    }

    /** @see FiberFrameworkServlet#setPublishEvents(boolean) */
    public void setPublishEvents(boolean publishEvents) {
        frameworkServlet.setPublishEvents(publishEvents);
    }

    /** @see FrameworkServlet#setContextAttribute(java.lang.String) */
    public void setContextAttribute(String contextAttribute) {
        frameworkServlet.setContextAttribute(contextAttribute);
    }

    /** @see FrameworkServlet#getContextAttribute() */
    public String getContextAttribute() {
        return frameworkServlet.getContextAttribute();
    }

    /** @see FrameworkServlet#setContextClass(java.lang.Class) */
    public void setContextClass(Class<?> contextClass) {
        frameworkServlet.setContextClass(contextClass);
    }

    /** @see FrameworkServlet#getContextClass() */
    public Class<?> getContextClass() {
        return frameworkServlet.getContextClass();
    }

    /** @see FrameworkServlet#setContextId(java.lang.String) */
    public void setContextId(String contextId) {
        frameworkServlet.setContextId(contextId);
    }

    /** @see FrameworkServlet#getContextId() */
    public String getContextId() {
        return frameworkServlet.getContextId();
    }

    /** @see FrameworkServlet#setNamespace(java.lang.String) */
    public void setNamespace(String namespace) {
        frameworkServlet.setNamespace(namespace);
    }

    /** @see FrameworkServlet#getNamespace() */
    public String getNamespace() {
        return frameworkServlet.getNamespace();
    }

    /** @see FrameworkServlet#setContextConfigLocation(java.lang.String) */
    public void setContextConfigLocation(String contextConfigLocation) {
        frameworkServlet.setContextConfigLocation(contextConfigLocation);
    }

    /** @see FrameworkServlet#getContextConfigLocation() */
    public String getContextConfigLocation() {
        return frameworkServlet.getContextConfigLocation();
    }

    /** @see FiberFrameworkServlet#setPublishContext(boolean) */
    public void setPublishContext(boolean publishContext) {
        frameworkServlet.setPublishContext(publishContext);
    }

    /** @see FrameworkServlet#setContextInitializers(org.springframework.context.ApplicationContextInitializer...) */
    public void setContextInitializers(ApplicationContextInitializer<? extends ConfigurableApplicationContext>... contextInitializers) {
        frameworkServlet.setContextInitializers(contextInitializers);
    }

    /** @see FrameworkServlet#setContextInitializerClasses(java.lang.String) */
    public void setContextInitializerClasses(String contextInitializerClasses) {
        frameworkServlet.setContextInitializerClasses(contextInitializerClasses);
    }

    /** @see FrameworkServlet#setApplicationContext(org.springframework.context.ApplicationContext) */
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        frameworkServlet.setApplicationContext(applicationContext);
    }   

    /** @see FrameworkServlet#getWebApplicationContext() */
    public final WebApplicationContext getWebApplicationContext() {
        return frameworkServlet.getWebApplicationContext();
    }

    /** @see FrameworkServlet#refresh() */
    public void refresh() {
        frameworkServlet.refresh();
    }

    /** @see FrameworkServlet#onApplicationEvent(org.springframework.context.event.ContextRefreshedEvent) */
    public void onApplicationEvent(ContextRefreshedEvent event) {
        frameworkServlet.refresh();
    }

    /** @see FrameworkServlet#destroy() */
    @Override
    public void destroy() {
        frameworkServlet.destroy();
    }

    ///////////////////////////////////////////////
    // Proxying protected features below this point
    ///////////////////////////////////////////////

    /** @see FrameworkServlet#configureAndRefreshWebApplicationContext(org.springframework.web.context.ConfigurableWebApplicationContext) */
    protected void configureAndRefreshWebApplicationContext(ConfigurableWebApplicationContext wac) {
        fiberFrameworkServletProtectedWrapper.configureAndRefreshWebApplicationContext(wac);
    }

    /** @see FrameworkServlet#applyInitializers(org.springframework.context.ConfigurableApplicationContext) */
    protected void applyInitializers(ConfigurableApplicationContext wac) {
        fiberFrameworkServletProtectedWrapper.applyInitializers(wac);
    }

    /** @see FiberFrameworkServletProtectedWrapper#buildLocaleContext(javax.servlet.http.HttpServletRequest) */
    protected LocaleContext buildLocaleContext(HttpServletRequest request) {
        return fiberFrameworkServletProtectedWrapper.buildLocaleContext(request);
    }

    /** @see FiberFrameworkServletProtectedWrapper#buildRequestAttributes(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, org.springframework.web.context.request.RequestAttributes)
     */
    protected ServletRequestAttributes buildRequestAttributes (HttpServletRequest request, HttpServletResponse response, RequestAttributes previousAttributes) {
        return fiberFrameworkServletProtectedWrapper.buildRequestAttributes(request, response, previousAttributes);
    }

    /** @see FiberFrameworkServletProtectedWrapper#findWebApplicationContext() */
    protected WebApplicationContext findWebApplicationContext() {
        return fiberFrameworkServletProtectedWrapper.findWebApplicationContext();
    }

    /** @see FiberFrameworkServletProtectedWrapper#getUsernameForRequest(javax.servlet.http.HttpServletRequest) */
    protected String getUsernameForRequest(HttpServletRequest request) {
        return fiberFrameworkServletProtectedWrapper.getUsernameForRequest(request);
    }

    /** @see FiberFrameworkServletProtectedWrapper#initServletBean() */
    protected final void initServletBean() throws ServletException {
        fiberFrameworkServletProtectedWrapper.initServletBean();
    }

    /** @see FiberFrameworkServletProtectedWrapper#initWebApplicationContext() */
    protected WebApplicationContext initWebApplicationContext() {
        return fiberFrameworkServletProtectedWrapper.initWebApplicationContext();
    }

    /** @see FiberFrameworkServletProtectedWrapper#createWebApplicationContext(org.springframework.context.ApplicationContext) */
    protected WebApplicationContext createWebApplicationContext(ApplicationContext parent) {
        return fiberFrameworkServletProtectedWrapper.createWebApplicationContext(parent);
    }

    /** @see FiberFrameworkServletProtectedWrapper#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse) */
    @Override
    protected final void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException, SuspendExecution {
        fiberFrameworkServletProtectedWrapper.doGet(request, response);
    }

    /** @see FiberFrameworkServletProtectedWrapper#doPost(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse) */
    @Override
    protected final void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException, SuspendExecution {
        fiberFrameworkServletProtectedWrapper.doPost(request, response);
    }

    /** @see FiberFrameworkServletProtectedWrapper#doPut(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse) */
    @Override
    protected final void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException, SuspendExecution {
        fiberFrameworkServletProtectedWrapper.doPut(request, response);
    }

    /** @see FiberFrameworkServletProtectedWrapper#doPut(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse) */
    @Override
    protected final void doDelete(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException, SuspendExecution {
        fiberFrameworkServletProtectedWrapper.doDelete(request, response);
    }

    /** @see FiberFrameworkServletProtectedWrapper#processRequest(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse) */
    protected final void processRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException, SuspendExecution {
        fiberFrameworkServletProtectedWrapper.processRequest(request, response);
    }

    ///////////////////////////////////////////////////////////////////////
    // Re-implementing protected features below this point;
    // derived from FrameworkServlet, relevant copyright and licences apply
    ///////////////////////////////////////////////////////////////////////

    public void setDispatchOptionsRequest(boolean dispatchOptionsRequest) {
        this.dispatchOptionsRequest = dispatchOptionsRequest;
    }

    public void setDispatchTraceRequest(boolean dispatchTraceRequest) {
        this.dispatchTraceRequest = dispatchTraceRequest;
    }
    
    ///////////////////////////////////////////////////////////////////////
    // Re-implementing protected features below this point;
    // derived from FrameworkServlet, relevant copyright and licences apply
    ///////////////////////////////////////////////////////////////////////

    /** @see FiberFrameworkServlet#createWebApplicationContext(org.springframework.web.context.WebApplicationContext) */
    // Rule 4: re-implementing because and forwarding it would just be more code and more overhead
    protected WebApplicationContext createWebApplicationContext(WebApplicationContext parent) {
        return createWebApplicationContext(parent);
    }

    /** see FiberFrameworkServlet#postProcessWebApplicationContext() */
    // Rule 4: empty and meant for extension in the original, no aditional re-implementation impacts broyught by re-implementing this
    protected void postProcessWebApplicationContext(ConfigurableWebApplicationContext wac) {
    }

    /** @see FiberFrameworkServlet#initFrameworkServlet() */
    // Rule 4: empty and meant for extension in the original, no aditional re-implementation impacts broyught by re-implementing this
    protected void initFrameworkServlet() throws ServletException {
    }

    /** @see FiberFrameworkServlet#onRefresh() */
    // Rule 4: empty and meant for extension in the original, no aditional re-implementation impacts broyught by re-implementing this
    protected void onRefresh(ApplicationContext context) {
    }

    // TODO circlespainter: comment & JavaDocs
    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException, SuspendExecution {

        String method = request.getMethod();
        if (method.equalsIgnoreCase(RequestMethod.PATCH.name())) {
            processRequest(request, response);
        } else {
            super.service(request, response);
        }
    }

    // TODO circlespainter: comment & JavaDocs
    @Override
    protected void doOptions(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException, SuspendExecution {

        if (this.dispatchOptionsRequest) {
            processRequest(request, response);
            if (response.containsHeader("Allow")) {
                // Proper OPTIONS response coming from a handler - we're done.
                return;
            }
        }

		// Use response wrapper for Servlet 2.5 compatibility where
        // the getHeader() method does not exist
        super.doOptions(request, new HttpServletResponseWrapper(response) {
            @Override
            public void setHeader(String name, String value) {
                if ("Allow".equals(name)) {
                    value = (StringUtils.hasLength(value) ? value + ", " : "") + RequestMethod.PATCH.name();
                }
                super.setHeader(name, value);
            }
        });
    }

    // TODO circlespainter: comment & JavaDocs
    @Override
    @Suspendable
    protected void doTrace(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        if (this.dispatchTraceRequest) {
            try {
                processRequest(request, response);
            } catch (SuspendExecution ex) {
                throw new AssertionError(ex);
            }
            if ("message/http".equals(response.getContentType())) {
                // Proper TRACE response coming from a handler - we're done.
                return;
            }
        }
        super.doTrace(request, response);
    }
   
    /** Adds fiber-blocking support to {@link FiberFrameworkServlet#doService(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)} */
    // Rule 1: re-implementing because it needs to support suspension
    protected abstract void doService(HttpServletRequest request, HttpServletResponse response) throws Exception, SuspendExecution;

    /////////////////////////////
    // Untouched private features
    /////////////////////////////

    // private static final boolean responseGetStatusAvailable = ClassUtils.hasMethod(HttpServletResponse.class, "getStatus");
    // private boolean threadContextInheritable = false;
    // private boolean publishEvents = true;

    // TODO circlespainter: comment & JavaDocs
    private boolean dispatchOptionsRequest = false;
    
    // TODO circlespainter: comment & JavaDocs
    private boolean dispatchTraceRequest = false;

    // private static final String INIT_PARAM_DELIMITERS = ",; \t\n";
    // private String contextAttribute;
    // private Class<?> contextClass = DEFAULT_CONTEXT_CLASS;
    // private String contextId;
    // private String namespace;
    // private String contextConfigLocation;
    // private final ArrayList<ApplicationContextInitializer<ConfigurableApplicationContext>> contextInitializers = new ArrayList<ApplicationContextInitializer<ConfigurableApplicationContext>>();
    // private String contextInitializerClasses;
    // private WebApplicationContext webApplicationContext;
    // private boolean webApplicationContextInjected = false;
    // private boolean publishContext = true;
    // private boolean refreshEventReceived = false;

    // private ApplicationContextInitializer<ConfigurableApplicationContext> loadInitializer(String className, ConfigurableApplicationContext wac);
    // private void publishRequestHandledEvent(HttpServletRequest request, HttpServletResponse response, long startTime, Throwable failureCause);
    // private void resetContextHolders(HttpServletRequest request, LocaleContext prevLocaleContext, RequestAttributes previousAttributes);
    // private void initContextHolders(HttpServletRequest request, LocaleContext localeContext, RequestAttributes requestAttributes);

    // private class ContextRefreshListener implements ApplicationListener<ContextRefreshedEvent>;
    // private class RequestBindingInterceptor extends CallableProcessingInterceptorAdapter;
}