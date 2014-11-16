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
import java.util.concurrent.Callable;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import org.springframework.beans.BeanUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationContextException;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.i18n.LocaleContext;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.context.ConfigurableWebApplicationContext;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.context.request.async.CallableProcessingInterceptorAdapter;
import org.springframework.web.context.request.async.WebAsyncManager;
import org.springframework.web.context.request.async.WebAsyncUtils;
import org.springframework.web.context.support.ServletRequestHandledEvent;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.servlet.FiberFrameworkServletProtectedWrapper;
import org.springframework.web.servlet.FrameworkServlet;
import org.springframework.web.util.NestedServletException;
import org.springframework.web.util.WebUtils;

import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.Suspendable;

/**
 * Mirror of {@link FrameworkServlet} implementing the extended {@link FiberHttpServletBean} fiber-blocking servlet interface
 * 
 * @author circlespainter
 */
public abstract class FiberFrameworkServlet extends FiberHttpServletBean implements ApplicationContextAware {
    // References to original instance and a `protected`-opening proxy of it
    private final FrameworkServlet frameworkServlet;
    private final FiberFrameworkServletProtectedWrapper fiberFrameworkServletProtectedWrapper;

    /**
     * Constructor rule 1: wrapping constructor only as this class is abstract and not a full replica
     * 
     * @param frameworkServlet The wrapped instance
     */
    public FiberFrameworkServlet(FrameworkServlet frameworkServlet) {
        super(frameworkServlet);
        this.frameworkServlet = frameworkServlet;
        this.fiberFrameworkServletProtectedWrapper = new FiberFrameworkServletProtectedWrapper(frameworkServlet);
    }
    
    ////////////////////////////////////////////
    // Proxying public features below this point
    ////////////////////////////////////////////

    /**
     * Proxy for {@link FrameworkServlet#DEFAULT_NAMESPACE_SUFFIX} 
     */
    public static final String DEFAULT_NAMESPACE_SUFFIX = FrameworkServlet.DEFAULT_NAMESPACE_SUFFIX;
    /**
     * Proxy for {@link FrameworkServlet#DEFAULT_CONTEXT_CLASS} 
     */
    public static final Class<?> DEFAULT_CONTEXT_CLASS = FrameworkServlet.DEFAULT_CONTEXT_CLASS;

    /**
     * Proxy for {@link FrameworkServlet#setContextAttribute(java.lang.String)}
     */
    public void setContextAttribute(String contextAttribute) {
        frameworkServlet.setContextAttribute(contextAttribute);
    }

    /**
     * Proxy for {@link FrameworkServlet#getContextAttribute()}
     */
    public String getContextAttribute() {
        return frameworkServlet.getContextAttribute();
    }

    /**
     * Proxy for {@link FrameworkServlet#setContextClass(java.lang.Class)}
     */
    public void setContextClass(Class<?> contextClass) {
        frameworkServlet.setContextClass(contextClass);
    }

    /**
     * Proxy for {@link FrameworkServlet#getCContextClass()}
     */
    public Class<?> getContextClass() {
        return frameworkServlet.getContextClass();
    }

    /**
     * Proxy for {@link FrameworkServlet#setContextId(java.lang.String)}
     */
    public void setContextId(String contextId) {
        frameworkServlet.setContextId(contextId);
    }

    /**
     * Proxy for {@link FrameworkServlet#getContextId()}
     */
    public String getContextId() {
        return frameworkServlet.getContextId();
    }

    /**
     * Proxy for {@link FrameworkServlet#setNamespace(java.lang.String)}
     */
    public void setNamespace(String namespace) {
        frameworkServlet.setNamespace(namespace);
    }

    /**
     * Proxy for {@link FrameworkServlet#getNamespace()}
     */
    public String getNamespace() {
        return frameworkServlet.getNamespace();
    }

    /**
     * Proxy for {@link FrameworkServlet#setContextConfigLocation(java.lang.String)}
     */
    public void setContextConfigLocation(String contextConfigLocation) {
        frameworkServlet.setContextConfigLocation(contextConfigLocation);
    }

    /**
     * Proxy for {@link FrameworkServlet#getContextConfigLocation()}
     */
    public String getContextConfigLocation() {
        return frameworkServlet.getContextConfigLocation();
    }

    /**
     * Proxy for {@link FrameworkServlet#setContextInitializers(org.springframework.context.ApplicationContextInitializer...)}
     */
    public void setContextInitializers(ApplicationContextInitializer<? extends ConfigurableApplicationContext>... contextInitializers) {
        frameworkServlet.setContextInitializers(contextInitializers);
    }

    /**
     * Proxy for {@link FrameworkServlet#setContextInitializerClasses(java.lang.String)}
     */
    public void setContextInitializerClasses(String contextInitializerClasses) {
        frameworkServlet.setContextInitializerClasses(contextInitializerClasses);
    }

    /**
     * Proxy for {@link FrameworkServlet#setApplicationContext(org.springframework.context.ApplicationContext)}
     */
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        frameworkServlet.setApplicationContext(applicationContext);
    }   

    /**
     * Proxy for {@link FrameworkServlet#getWebApplicationContext()}
     */
    public final WebApplicationContext getWebApplicationContext() {
        return frameworkServlet.getWebApplicationContext();
    }

    /**
     * Proxy for {@link FrameworkServlet#refresh()}
     */
    public void refresh() {
        frameworkServlet.refresh();
    }
    
    /**
     * Proxy for {@link FrameworkServlet#onApplicationEvent(org.springframework.context.event.ContextRefreshedEvent)}
     */
    public void onApplicationEvent(ContextRefreshedEvent event) {
        frameworkServlet.refresh();
    }

    /**
     * Proxy for {@link FrameworkServlet#destroy()}
     */
    @Override
    public void destroy() {
        frameworkServlet.destroy();
    }

    ///////////////////////////////////////////////
    // Proxying protected features below this point
    ///////////////////////////////////////////////
    
    /**
     * Proxy for {@link FrameworkServlet#configureAndRefreshWebApplicationContext(org.springframework.web.context.ConfigurableWebApplicationContext)}
     */
    protected void configureAndRefreshWebApplicationContext(ConfigurableWebApplicationContext wac) {
        fiberFrameworkServletProtectedWrapper.configureAndRefreshWebApplicationContext(wac);
    }
    
    /**
     * Proxy for {@link FrameworkServlet#applyInitializers(org.springframework.context.ConfigurableApplicationContext)}
     */
    protected void applyInitializers(ConfigurableApplicationContext wac) {
        fiberFrameworkServletProtectedWrapper.applyInitializers(wac);
    }
    
    /**
     * Proxy for {@link FiberFrameworkServletProtectedWrapper#buildLocaleContext(javax.servlet.http.HttpServletRequest)}
     */
    protected LocaleContext buildLocaleContext(HttpServletRequest request) {
        return fiberFrameworkServletProtectedWrapper.buildLocaleContext(request);
    }
    
    /**
     * Proxy for
     * {@link FiberFrameworkServletProtectedWrapper#buildRequestAttributes(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, org.springframework.web.context.request.RequestAttributes)}
     */
    protected ServletRequestAttributes buildRequestAttributes (HttpServletRequest request, HttpServletResponse response, RequestAttributes previousAttributes) {
        return fiberFrameworkServletProtectedWrapper.buildRequestAttributes(request, response, previousAttributes);
    }
    
    /**
     * Proxy for {@link FiberFrameworkServletProtectedWrapper#findWebApplicationContext()}
     */
    protected WebApplicationContext findWebApplicationContext() {
        return fiberFrameworkServletProtectedWrapper.findWebApplicationContext();
    }
    
    /**
     * Proxy for {@link FiberFrameworkServletProtectedWrapper#getUsernameForRequest(javax.servlet.http.HttpServletRequest)}
     */
    protected String getUsernameForRequest(HttpServletRequest request) {
        return fiberFrameworkServletProtectedWrapper.getUsernameForRequest(request);
    }
    
    ///////////////////////////////////////////////////////////
    // Rewriting public features below this point;
    // adapted from HttpServletBean, © and Apache License apply
    ///////////////////////////////////////////////////////////

    /**
     * Exact replica of {@link FrameworkServlet#SERVLET_CONTEXT_PREFIX}
     */
    // Rule 1: rewriting because it needs to reference this very class, not the original one
    public static final String SERVLET_CONTEXT_PREFIX = FiberFrameworkServlet.class.getName() + ".CONTEXT.";

    /**
     * Exact replica of {@link FiberFrameworkServlet#createWebApplicationContext(org.springframework.web.context.WebApplicationContext)}
     */
    // Rule 2: reimplementing because it depends on the reimplemented `SERVLET_CONTEXT_PREFIX` public constant
    public String getServletContextAttributeName() {
        return SERVLET_CONTEXT_PREFIX + getServletName();
    }

    /**
     * Exact replica of {@link FiberFrameworkServlet#setPublishContext(boolean)}
     */
    // Rule 2: reimplementing because it depends on the reimplemented `publicContext` private field
    public void setPublishContext(boolean publishContext) {
        this.publishContext = publishContext;
    }

    /**
     * Exact replica of {@link FiberFrameworkServlet#setThreadContextInheritable(boolean)}
     */
    // Rule 2: reimplementing because it depends on the reimplemented `threadContextInheritable` private field
    public void setThreadContextInheritable(boolean threadContextInheritable) {
        this.threadContextInheritable = threadContextInheritable;
    }

    /**
     * Exact replica of {@link FiberFrameworkServlet#setPublishEvents(boolean)}
     */
    // Rule 2: reimplementing because it depends on the reimplemented `publishEvents` private field
    public void setPublishEvents(boolean publishEvents) {
        this.publishEvents = publishEvents;
    }

    /**
     * Exact replica of {@link FiberFrameworkServlet#dispatchOptionsRequest(boolean)}
     */
    // Rule 2: reimplementing because it depends on the reimplemented `publishEvents` private field
    public void setDispatchOptionsRequest(boolean dispatchOptionsRequest) {
        this.dispatchOptionsRequest = dispatchOptionsRequest;
    }

    /**
     * Exact replica of {@link FiberFrameworkServlet#dispatchTraceRequest(boolean)}
     */
    // Rule 2: reimplementing because it depends on the reimplemented `publishEvents` private field
    public void setDispatchTraceRequest(boolean dispatchTraceRequest) {
        this.dispatchTraceRequest = dispatchTraceRequest;
    }

    ///////////////////////////////////////////////////////////
    // Rewriting protected features below this point;
    // adapted from HttpServletBean, © and Apache License apply
    ///////////////////////////////////////////////////////////

    /**
     * Exact replica of {@link FiberFrameworkServlet#initServletBean()}
     */
    // Rule 2: reimplementing because it depends on the reimplemented (protected) logger
    @Override
    protected final void initServletBean() throws ServletException {
        getServletContext().log("Initializing Spring FrameworkServlet '" + getServletName() + "'");
        if (this.logger.isInfoEnabled()) {
            this.logger.info("FrameworkServlet '" + getServletName() + "': initialization started");
        }
        long startTime = System.currentTimeMillis();

        try {
            setApplicationContext(initWebApplicationContext());
            initFrameworkServlet();
        } catch (ServletException ex) {
            this.logger.error("Context initialization failed", ex);
            throw ex;
        } catch (RuntimeException ex) {
            this.logger.error("Context initialization failed", ex);
            throw ex;
        }

        if (this.logger.isInfoEnabled()) {
            long elapsedTime = System.currentTimeMillis() - startTime;
            this.logger.info("FrameworkServlet '" + getServletName() + "': initialization completed in "
                    + elapsedTime + " ms");
        }
    }

    /**
     * Exact replica of {@link FiberFrameworkServlet#initWebApplicationContext()}
     */
    // Rule 2: reimplementing because it depends on the reimplemented (protected) logger
    protected WebApplicationContext initWebApplicationContext() {
        WebApplicationContext rootContext
                = WebApplicationContextUtils.getWebApplicationContext(getServletContext());
        WebApplicationContext wac = null;

        if (getWebApplicationContext() != null) {
            // A context instance was injected at construction time -> use it
            wac = getWebApplicationContext();
            if (wac instanceof ConfigurableWebApplicationContext) {
                ConfigurableWebApplicationContext cwac = (ConfigurableWebApplicationContext) wac;
                if (!cwac.isActive()) {
					// The context has not yet been refreshed -> provide services such as
                    // setting the parent context, setting the application context id, etc
                    if (cwac.getParent() == null) {
						// The context instance was injected without an explicit parent -> set
                        // the root application context (if any; may be null) as the parent
                        cwac.setParent(rootContext);
                    }
                    configureAndRefreshWebApplicationContext(cwac);
                }
            }
        }
        if (wac == null) {
			// No context instance was injected at construction time -> see if one
            // has been registered in the servlet context. If one exists, it is assumed
            // that the parent context (if any) has already been set and that the
            // user has performed any initialization such as setting the context id
            wac = findWebApplicationContext();
        }
        if (wac == null) {
            // No context instance is defined for this servlet -> create a local one
            wac = createWebApplicationContext(rootContext);
        }

        if (!this.refreshEventReceived) {
			// Either the context is not a ConfigurableApplicationContext with refresh
            // support or the context injected at construction time had already been
            // refreshed -> trigger initial onRefresh manually here.
            onRefresh(wac);
        }

        if (this.publishContext) {
            // Publish the context as a servlet context attribute.
            String attrName = getServletContextAttributeName();
            getServletContext().setAttribute(attrName, wac);
            if (this.logger.isDebugEnabled()) {
                this.logger.debug("Published WebApplicationContext of servlet '" + getServletName()
                        + "' as ServletContext attribute with name [" + attrName + "]");
            }
        }

        return wac;
    }

    /**
     * Exact replica of {@link FiberFrameworkServlet#createWebApplicationContext(org.springframework.context.ApplicationContext)}
     */
    // Rule 2: reimplementing because it depends on the reimplemented protected inherited logger
    protected WebApplicationContext createWebApplicationContext(ApplicationContext parent) {
        Class<?> contextClass = getContextClass();
        if (this.logger.isDebugEnabled()) {
            this.logger.debug("Servlet with name '" + getServletName()
                    + "' will try to create custom WebApplicationContext context of class '"
                    + contextClass.getName() + "'" + ", using parent context [" + parent + "]");
        }
        if (!ConfigurableWebApplicationContext.class.isAssignableFrom(contextClass)) {
            throw new ApplicationContextException(
                    "Fatal initialization error in servlet with name '" + getServletName()
                    + "': custom WebApplicationContext class [" + contextClass.getName()
                    + "] is not of type ConfigurableWebApplicationContext");
        }
        ConfigurableWebApplicationContext wac
                = (ConfigurableWebApplicationContext) BeanUtils.instantiateClass(contextClass);

        wac.setEnvironment(getEnvironment());
        wac.setParent(parent);
        wac.setConfigLocation(getContextConfigLocation());

        configureAndRefreshWebApplicationContext(wac);

        return wac;
    }

    /**
     * Exact replica of {@link FiberFrameworkServlet#createWebApplicationContext(org.springframework.web.context.WebApplicationContext)}
     */
    // Rule 2 + 0: reimplementing because it depends on the reimplemented method by the same name (and forwarding it would just be more code and more overhead)
    protected WebApplicationContext createWebApplicationContext(WebApplicationContext parent) {
        return createWebApplicationContext(parent);
    }

    /**
     * Exact replica of {@link FiberFrameworkServlet#postProcessWebApplicationContext()}
     */
    // Rule 4: empty and meant for extension in the original, no aditional reimpl. impacts broyught by reimplementing this
    protected void postProcessWebApplicationContext(ConfigurableWebApplicationContext wac) {
    }
    
    /**
     * Exact replica of {@link FiberFrameworkServlet#initFrameworkServlet()}
     */
    // Rule 4: empty and meant for extension in the original, no aditional reimpl. impacts broyught by reimplementing this
    protected void initFrameworkServlet() throws ServletException {
    }

    /**
     * Exact replica of {@link FiberFrameworkServlet#onRefresh()}
     */
    // Rule 4: empty and meant for extension in the original, no aditional reimpl. impacts broyught by reimplementing this
    protected void onRefresh(ApplicationContext context) {
    }

    /**
     * Exact replica of {@link FiberFrameworkServlet#service(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)}
     */
    // Rule 1: reimplementing because it needs to declare throwing `SuspendExecution`
    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException, SuspendExecution {

        String method = request.getMethod();
        if (method.equalsIgnoreCase(RequestMethod.PATCH.name())) {
            processRequest(request, response);
        } else {
            super.service(request, response);
        }
    }

    /**
     * Exact replica of {@link FiberFrameworkServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)}
     */
    // Rule 2: reimplementing because it depends on the reimplemented, non forwardable `final` method `processRequest`
    @Override
    protected final void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        processRequest(request, response);
    }

    /**
     * Exact replica of {@link FiberFrameworkServlet#doPost(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)}
     */
    // Rule 2: reimplementing because it depends on the reimplemented, non forwardable `final` method `processRequest`
    @Override
    protected final void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException, SuspendExecution {
        processRequest(request, response);
    }

    /**
     * Exact replica of {@link FiberFrameworkServlet#doPut(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)}
     */
    // Rule 2: reimplementing because it depends on the reimplemented, non forwardable `final` method `processRequest`
    @Override
    protected final void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException, SuspendExecution {
        processRequest(request, response);
    }

    /**
     * Exact replica of {@link FiberFrameworkServlet#doPut(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)}
     */
    // Rule 2: reimplementing because it depends on the reimplemented, non forwardable `final` method `processRequest`
    @Override
    protected final void doDelete(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException, SuspendExecution {
        processRequest(request, response);
    }

    /**
     * Exact replica of {@link FiberFrameworkServlet#doOptions(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)}
     */
    // Rule 2: reimplementing because it depends on the reimplemented, non forwardable `final` method `processRequest`
    @Override
    protected void doOptions(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException, SuspendExecution {
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

    /**
     * Exact replica of {@link FiberFrameworkServlet#doTrace(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)}
     */
    // Rule 2 + 1: reimplementing because it depends on the reimplemented, non forwardable `final` method `processRequest` and it needs to be suspendable
    //             as it calls processRequest
    @Override
    @Suspendable
    protected void doTrace(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (this.dispatchTraceRequest) {
            processRequest(request, response);
            if ("message/http".equals(response.getContentType())) {
                // Proper TRACE response coming from a handler - we're done.
                return;
            }
        }
        super.doTrace(request, response);
    }
    
    /**
     * Exact replica of {@link FiberFrameworkServlet#processRequest(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)}
     */
    // Rule 2 + 1: reimplementing because it depends on the reimplemented protected inherited logger and needs to support suspension
    @Suspendable
    protected final void processRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        long startTime = System.currentTimeMillis();
        Throwable failureCause = null;

        LocaleContext previousLocaleContext = LocaleContextHolder.getLocaleContext();
        LocaleContext localeContext = buildLocaleContext(request);

        RequestAttributes previousAttributes = RequestContextHolder.getRequestAttributes();
        ServletRequestAttributes requestAttributes = buildRequestAttributes(request, response, previousAttributes);

        WebAsyncManager asyncManager = WebAsyncUtils.getAsyncManager(request);
        asyncManager.registerCallableInterceptor(FrameworkServlet.class.getName(), new RequestBindingInterceptor());

        initContextHolders(request, localeContext, requestAttributes);

        try {
            doService(request, response);
        } catch (ServletException ex) {
            failureCause = ex;
            throw ex;
        } catch (IOException ex) {
            failureCause = ex;
            throw ex;
        } catch (Throwable ex) {
            failureCause = ex;
            throw new NestedServletException("Request processing failed", ex);
        } finally {
            resetContextHolders(request, previousLocaleContext, previousAttributes);
            if (requestAttributes != null) {
                requestAttributes.requestCompleted();
            }

            if (logger.isDebugEnabled()) {
                if (failureCause != null) {
                    this.logger.debug("Could not complete request", failureCause);
                } else {
                    if (asyncManager.isConcurrentHandlingStarted()) {
                        logger.debug("Leaving response open for concurrent processing");
                    } else {
                        this.logger.debug("Successfully completed request");
                    }
                }
            }

            publishRequestHandledEvent(request, response, startTime, failureCause);
        }
    }

    /**
    * Exact replica of {@link FiberFrameworkServlet#doService(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)}
    */
    // Rule 1: needs to be suspendable
    protected abstract void doService(HttpServletRequest request, HttpServletResponse response) throws Exception, SuspendExecution;
    
    ///////////////////////////////////////////////////////////
    // Rewriting private features below this point;
    // adapted from HttpServletBean, © and Apache License apply
    ///////////////////////////////////////////////////////////

    /** Reimplementation of {@link FrameworkServlet#responseGetStatusAvailable} */
    // Rule 3: `publishRequestHandledEvent` depends on it
    private static final boolean responseGetStatusAvailable = ClassUtils.hasMethod(HttpServletResponse.class, "getStatus");
    
    /** Reimplementation of {@link FrameworkServlet#publishContext} */
    // Rule 3: `initWebApplicationContext` depends on it
    private boolean publishContext = true;

    /** Reimplementation of {@link FrameworkServlet#refreshEventReceived} */
    // Rule 3: `initWebApplicationContext`depends on it
    private boolean refreshEventReceived = false;

    /** Reimplementation of {@link FrameworkServlet#threadContextInheritable} */
    // Rule 3: `initContextHolders` depends on it
    private boolean threadContextInheritable = false;

    /** Reimplementation of {@link FrameworkServlet#publishEvents} */
    // Rule 3: `publishRequestHandledEvent` depends on it
    private boolean publishEvents = true;
    
    /** Reimplementation of {@link FrameworkServlet#dispatchOptionsRequest} */
    // Rule 3: `doOptions` depends on it
    private boolean dispatchOptionsRequest = false;
    
    /** Reimplementation of {@link FrameworkServlet#dispatchTraceRequest} */
    // Rule 3: `doTrace` depends on it
    private boolean dispatchTraceRequest = false;

    /** Exact replica of {@link FrameworkServlet#initContextHolders} */
    // Rule 2 + 3: it depends on inherited protected logger and `processRequest` depends on iy
    private void initContextHolders(
            HttpServletRequest request, LocaleContext localeContext, RequestAttributes requestAttributes) {

        if (localeContext != null) {
            LocaleContextHolder.setLocaleContext(localeContext, threadContextInheritable);
        }
        if (requestAttributes != null) {
            RequestContextHolder.setRequestAttributes(requestAttributes, this.threadContextInheritable);
        }
        if (logger.isTraceEnabled()) {
            logger.trace("Bound request context to thread: " + request);
        }
    }

    /** Exact replica of
     * {@link FrameworkServlet#resetContextHolders(javax.servlet.http.HttpServletRequest, org.springframework.context.i18n.LocaleContext, org.springframework.web.context.request.RequestAttributes)}
     */
    // Rule 2 + 3: it depends on inherited protected logger and `processRequest` depends on iy
    private void resetContextHolders(HttpServletRequest request, LocaleContext prevLocaleContext, RequestAttributes previousAttributes) {
        LocaleContextHolder.setLocaleContext(prevLocaleContext, this.threadContextInheritable);
        RequestContextHolder.setRequestAttributes(previousAttributes, this.threadContextInheritable);

        if (logger.isTraceEnabled()) {
            logger.trace("Cleared thread-bound request context: " + request);
        }
    }

    /** Exact replica of {@link FrameworkServlet#publishRequestHandledEvent(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, long, java.lang.Throwable)} */
    // Rule 2 + 3: it depends on inherited protected logger and `processRequest` depends on iy
    private void publishRequestHandledEvent(HttpServletRequest request, HttpServletResponse response, long startTime, Throwable failureCause) {
        if (this.publishEvents) {
            // Whether or not we succeeded, publish an event.
            long processingTime = System.currentTimeMillis() - startTime;
            int statusCode = (responseGetStatusAvailable ? response.getStatus() : -1);
            getWebApplicationContext().publishEvent(
                new ServletRequestHandledEvent(this,
                    request.getRequestURI(), request.getRemoteAddr(),
                    request.getMethod(), getServletConfig().getServletName(),
                    WebUtils.getSessionId(request), getUsernameForRequest(request),
                    processingTime, failureCause, statusCode));
        }
    }

    /**
     * CallableProcessingInterceptor implementation that initializes and resets
     * FrameworkServlet's context holders, i.e. LocaleContextHolder and RequestContextHolder.
     */
    private class RequestBindingInterceptor extends CallableProcessingInterceptorAdapter {
        @Override
        public <T> void preProcess(NativeWebRequest webRequest, Callable<T> task) {
            HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
            if (request != null) {
                HttpServletResponse response = webRequest.getNativeRequest(HttpServletResponse.class);
                initContextHolders(request, buildLocaleContext(request), buildRequestAttributes(request, response, null));
            }
        }

        @Override
        public <T> void postProcess(NativeWebRequest webRequest, Callable<T> task, Object concurrentResult) {
            HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
            if (request != null) {
                resetContextHolders(request, null, null);
            }
        }
    }
    
    /////////////////////////////
    // Untouched private features
    /////////////////////////////

    // private ApplicationContextInitializer<ConfigurableApplicationContext> loadInitializer(String className, ConfigurableApplicationContext wac)

    // private class ContextRefreshListener implements ApplicationListener<ContextRefreshedEvent>
    
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
}