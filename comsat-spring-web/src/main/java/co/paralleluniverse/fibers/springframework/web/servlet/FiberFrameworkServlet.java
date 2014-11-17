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

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
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
import org.springframework.web.servlet.FiberFrameworkServletProtectedWrapper;
import org.springframework.web.servlet.FrameworkServlet;
import org.springframework.web.util.NestedServletException;
import org.springframework.web.util.WebUtils;

import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.Suspendable;

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

    ///////////////////////////////////////////////////////////////////////
    // Re-implementing public features below this point;
    // derived from FrameworkServlet, relevant copyright and licences apply
    ///////////////////////////////////////////////////////////////////////

    /** @see FrameworkServlet#SERVLET_CONTEXT_PREFIX */
    // Rule 1: re-implementing because it needs to reference this very class, not the original one
    public static final String SERVLET_CONTEXT_PREFIX = FiberFrameworkServlet.class.getName() + ".CONTEXT.";

    /** @see FiberFrameworkServlet#createWebApplicationContext(org.springframework.web.context.WebApplicationContext) */
    // Rule 2: re-implementing because it depends on the re-implemented `SERVLET_CONTEXT_PREFIX` public constant
    public String getServletContextAttributeName() {
        return SERVLET_CONTEXT_PREFIX + getServletName();
    }

    /** @see FiberFrameworkServlet#setThreadContextInheritable(boolean) */
    // Rule 2: re-implementing because it depends on the re-implemented `threadContextInheritable` private field
    public void setThreadContextInheritable(boolean threadContextInheritable) {
        this.threadContextInheritable = threadContextInheritable;
    }

    /** @see FiberFrameworkServlet#setPublishEvents(boolean) */
    // Rule 2: re-implementing because it depends on the re-implemented `publishEvents` private field
    public void setPublishEvents(boolean publishEvents) {
        this.publishEvents = publishEvents;
    }

    /** @see FiberFrameworkServlet#dispatchOptionsRequest(boolean) */
    // Rule 2: re-implementing because it depends on the re-implemented `dispatchOptionsRequest` private field
    public void setDispatchOptionsRequest(boolean dispatchOptionsRequest) {
        this.dispatchOptionsRequest = dispatchOptionsRequest;
    }

    /** @see FiberFrameworkServlet#dispatchTraceRequest(boolean) */
    // Rule 2: re-implementing because it depends on the re-implemented `dispatchTraceRequest` private field
    public void setDispatchTraceRequest(boolean dispatchTraceRequest) {
        this.dispatchTraceRequest = dispatchTraceRequest;
    }

    ///////////////////////////////////////////////////////////////////////
    // Re-implementing protected features below this point;
    // derived from FrameworkServlet, relevant copyright and licences apply
    ///////////////////////////////////////////////////////////////////////

    /** @see FiberFrameworkServlet#createWebApplicationContext(org.springframework.web.context.WebApplicationContext) */
    // Rule 0: re-implementing because and forwarding it would just be more code and more overhead
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

    /** Adds fiber-blocking to {@link FiberFrameworkServlet#service(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)} */
    // Rule 1: re-implementing because it needs to declare throwing `SuspendExecution`
    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException, SuspendExecution {
        String method = request.getMethod();
        if (method.equalsIgnoreCase(RequestMethod.PATCH.name())) {
            processRequest(request, response);
        } else {
            super.service(request, response);
        }
    }

    /** Adds fiber-blocking to  {@link FiberFrameworkServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)} */
    // Rule 2 + 1: re-implementing because it depends on the re-implemented, non forwardable `final` method `processRequest` and it needs to declare throwing `SuspendExecution`
    @Override
    protected final void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException, SuspendExecution {
        processRequest(request, response);
    }

    /** Adds fiber-blocking support to {@link FiberFrameworkServlet#doPost(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)} */
    // Rule 2 + 1: re-implementing because it depends on the re-implemented, non forwardable `final` method `processRequest` and it needs to declare throwing `SuspendExecution`
    @Override
    protected final void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException, SuspendExecution {
        processRequest(request, response);
    }

    /** Adds fiber-blocking support to {@link FiberFrameworkServlet#doPut(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)} */
    // Rule 2 + 1: re-implementing because it depends on the re-implemented, non forwardable `final` method `processRequest` and it needs to declare throwing `SuspendExecution`
    @Override
    protected final void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException, SuspendExecution {
        processRequest(request, response);
    }

    /** Adds fiber-blocking support to {@link FiberFrameworkServlet#doPut(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)} */
    // Rule 2 + 1: re-implementing because it depends on the re-implemented, non forwardable `final` method `processRequest` and it needs to declare throwing `SuspendExecution`
    @Override
    protected final void doDelete(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException, SuspendExecution {
        processRequest(request, response);
    }

    /** Adds fiber-blocking support to {@link FiberFrameworkServlet#doOptions(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)} */
    // Rule 2 + 1: re-implementing because it depends on the re-implemented, non forwardable `final` method `processRequest` and it needs to declare throwing `SuspendExecution`
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

    /** Adds fiber-blocking support to {@link FiberFrameworkServlet#doTrace(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)} */
    // Rule 2 + 1: re-implementing because it depends on the re-implemented, non forwardable `final` method `processRequest` and it needs to be suspendable
    //             as it calls processRequest
    @Override
    @Suspendable
    protected void doTrace(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (this.dispatchTraceRequest) {
            try {
                processRequest(request, response);
            } catch (SuspendExecution ex) {
                throw new AssertionError(ex); // This should never happen
            }
            if ("message/http".equals(response.getContentType())) {
                // Proper TRACE response coming from a handler - we're done.
                return;
            }
        }
        super.doTrace(request, response);
    }

    /** Adds fiber-blocking support to {@link FiberFrameworkServlet#processRequest(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)} */
    // Rule 1: re-implementing because it needs to support suspension
    protected final void processRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException, SuspendExecution {
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

    /** Adds fiber-blocking support to {@link FiberFrameworkServlet#doService(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)} */
    // Rule 1: needs to be suspendable
    protected abstract void doService(HttpServletRequest request, HttpServletResponse response) throws Exception, SuspendExecution;

    ///////////////////////////////////////////////////////////////////////
    // Re-implementing private features below this point;
    // derived from FrameworkServlet, relevant copyright and licences apply
    ///////////////////////////////////////////////////////////////////////

    /** @see FrameworkServlet#responseGetStatusAvailable */
    // Rule 3: `publishRequestHandledEvent` depends on it
    private static final boolean responseGetStatusAvailable = ClassUtils.hasMethod(HttpServletResponse.class, "getStatus");

    /** @see FrameworkServlet#threadContextInheritable */
    // Rule 3: `initContextHolders` depends on it
    private boolean threadContextInheritable = false;

    /** @see FrameworkServlet#publishEvents */
    // Rule 3: `publishRequestHandledEvent` depends on it
    private boolean publishEvents = true;

    /** @see FrameworkServlet#dispatchOptionsRequest */
    // Rule 3: `doOptions` depends on it
    private boolean dispatchOptionsRequest = false;

    /** @see FrameworkServlet#dispatchTraceRequest */
    // Rule 3: `doTrace` depends on it
    private boolean dispatchTraceRequest = false;

    /** @see FrameworkServlet#initContextHolders */
    // Rule 3: re-implemented `processRequest` depends on it
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

    /** @see FrameworkServlet#resetContextHolders(javax.servlet.http.HttpServletRequest, org.springframework.context.i18n.LocaleContext, org.springframework.web.context.request.RequestAttributes) */
    // Rule 3: re-implemented `processRequest` depends on iy
    private void resetContextHolders(HttpServletRequest request, LocaleContext prevLocaleContext, RequestAttributes previousAttributes) {
        LocaleContextHolder.setLocaleContext(prevLocaleContext, this.threadContextInheritable);
        RequestContextHolder.setRequestAttributes(previousAttributes, this.threadContextInheritable);

        if (logger.isTraceEnabled()) {
            logger.trace("Cleared thread-bound request context: " + request);
        }
    }

    /** @see FrameworkServlet#publishRequestHandledEvent(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, long, java.lang.Throwable)} */
    // Rule 3: re-implemented `processRequest` depends on iy
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

    /** @see FrameworkServlet.RequestBindingInterceptor} */
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

    // private ApplicationContextInitializer<ConfigurableApplicationContext> loadInitializer(String className, ConfigurableApplicationContext wac)

    // private class ContextRefreshListener implements ApplicationListener<ContextRefreshedEvent>
}