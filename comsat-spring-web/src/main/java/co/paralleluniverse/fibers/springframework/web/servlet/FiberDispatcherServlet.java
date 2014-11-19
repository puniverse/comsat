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

import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;

import org.springframework.context.ApplicationContext;
import org.springframework.context.i18n.LocaleContext;
import org.springframework.ui.context.ThemeSource;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.FiberDispatcherServletProtectedWrapper;
import org.springframework.web.servlet.HandlerAdapter;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.View;

import co.paralleluniverse.fibers.SuspendExecution;


/**
 * Equivalent of {@link DispatcherServlet} extending the fiber-blocking {@link FiberFrameworkServlet}
 * 
 * @author circlespainter
 */
public class FiberDispatcherServlet extends FiberFrameworkServlet {
    // References to original instance and a `protected`-opening proxy of it
    private final DispatcherServlet dispatcherServlet;
    private final FiberDispatcherServletProtectedWrapper fiberDispatcherServletProtectedWrapper;

    /** Default constructor, see {@link DispatcherServlet#DispatcherServlet()} */
    // Constructor rule number 2, mirroring original constructor API
    public FiberDispatcherServlet() {
        super(new DispatcherServlet());
        dispatcherServlet = (DispatcherServlet) super.frameworkServlet;
        // Being a final field, it's not possible to factor out this logic into a separate method ;-(
        fiberDispatcherServletProtectedWrapper = new FiberDispatcherServletProtectedWrapper(dispatcherServlet);
    }

    /** See {@link DispatcherServlet#DispatcherServlet(org.springframework.web.context.WebApplicationContext)} */
    // Constructor rule number 2, mirroring original constructor API
    public FiberDispatcherServlet(WebApplicationContext webApplicationContext) {
        this();
        setApplicationContext(webApplicationContext);
    }

    /**
     * Wrapping constructor
     * 
     * @param dispatcherServlet The wrapped instance
     */
    // Constructor rule number 2, providing wrapping constructor too
    public FiberDispatcherServlet(DispatcherServlet dispatcherServlet) {
        super(dispatcherServlet);
        this.dispatcherServlet = dispatcherServlet;
        // Being a final field, it's not possible to factor out this logic into a separate method ;-(
        fiberDispatcherServletProtectedWrapper = new FiberDispatcherServletProtectedWrapper(dispatcherServlet);
    }

    ////////////////////////////////////////////
    // Proxying public features below this point
    ////////////////////////////////////////////

    /** @see DispatcherServlet#MULTIPART_RESOLVER_BEAN_NAME */
    public static final String MULTIPART_RESOLVER_BEAN_NAME = DispatcherServlet.MULTIPART_RESOLVER_BEAN_NAME;

    /** @see DispatcherServlet#LOCALE_RESOLVER_BEAN_NAME */
    public static final String LOCALE_RESOLVER_BEAN_NAME = DispatcherServlet.LOCALE_RESOLVER_BEAN_NAME;

    /** @see DispatcherServlet#THEME_RESOLVER_BEAN_NAME */
    public static final String THEME_RESOLVER_BEAN_NAME = DispatcherServlet.THEME_RESOLVER_BEAN_NAME;

    /** @see DispatcherServlet#HANDLER_MAPPING_BEAN_NAME */
    public static final String HANDLER_MAPPING_BEAN_NAME = DispatcherServlet.HANDLER_MAPPING_BEAN_NAME;

    /** @see DispatcherServlet#HANDLER_ADAPTER_BEAN_NAME */
    public static final String HANDLER_ADAPTER_BEAN_NAME = DispatcherServlet.HANDLER_ADAPTER_BEAN_NAME;

    /** @see DispatcherServlet#HANDLER_EXCEPTION_RESOLVER_BEAN_NAME */
    public static final String HANDLER_EXCEPTION_RESOLVER_BEAN_NAME = DispatcherServlet.HANDLER_EXCEPTION_RESOLVER_BEAN_NAME;

    /** @see DispatcherServlet#REQUEST_TO_VIEW_NAME_TRANSLATOR_BEAN_NAME */
    public static final String REQUEST_TO_VIEW_NAME_TRANSLATOR_BEAN_NAME = DispatcherServlet.REQUEST_TO_VIEW_NAME_TRANSLATOR_BEAN_NAME;

    /** @see DispatcherServlet#VIEW_RESOLVER_BEAN_NAME */
    public static final String VIEW_RESOLVER_BEAN_NAME = DispatcherServlet.VIEW_RESOLVER_BEAN_NAME;

    /** @see DispatcherServlet#FLASH_MAP_MANAGER_BEAN_NAME */
    public static final String FLASH_MAP_MANAGER_BEAN_NAME = DispatcherServlet.FLASH_MAP_MANAGER_BEAN_NAME;

    // TODO circlespainter: double-check but not going to reimplement the following *_ATTRIBUTE constants even though they reference the original class name.
    //                      I believe they are part of the public interface (e.g. referenced or meant to be in conf files) and/or implementation wiring, so
    //                      changing them could make porting much more difficult

    /** @see DispatcherServlet#WEB_APPLICATION_CONTEXT_ATTRIBUTE */
    public static final String WEB_APPLICATION_CONTEXT_ATTRIBUTE = DispatcherServlet.WEB_APPLICATION_CONTEXT_ATTRIBUTE;

    /** @see DispatcherServlet#LOCALE_RESOLVER_ATTRIBUTE */
    public static final String LOCALE_RESOLVER_ATTRIBUTE = DispatcherServlet.LOCALE_RESOLVER_ATTRIBUTE;

    /** @see DispatcherServlet#THEME_RESOLVER_ATTRIBUTE */
    public static final String THEME_RESOLVER_ATTRIBUTE = DispatcherServlet.THEME_RESOLVER_ATTRIBUTE;

    /** @see DispatcherServlet#THEME_RESOLVER_ATTRIBUTE */
    public static final String THEME_SOURCE_ATTRIBUTE = DispatcherServlet.THEME_SOURCE_ATTRIBUTE;

    /** @see DispatcherServlet#INPUT_FLASH_MAP_ATTRIBUTE */
    public static final String INPUT_FLASH_MAP_ATTRIBUTE = DispatcherServlet.INPUT_FLASH_MAP_ATTRIBUTE;

    /** @see DispatcherServlet#OUTPUT_FLASH_MAP_ATTRIBUTE */
    public static final String OUTPUT_FLASH_MAP_ATTRIBUTE = DispatcherServlet.OUTPUT_FLASH_MAP_ATTRIBUTE;

    /** @see DispatcherServlet#FLASH_MAP_MANAGER_ATTRIBUTE */
    public static final String FLASH_MAP_MANAGER_ATTRIBUTE = DispatcherServlet.FLASH_MAP_MANAGER_ATTRIBUTE;

    /** @see DispatcherServlet#PAGE_NOT_FOUND_LOG_CATEGORY */
    public static final String PAGE_NOT_FOUND_LOG_CATEGORY = DispatcherServlet.PAGE_NOT_FOUND_LOG_CATEGORY;

    /** @see DispatcherServlet#setDetectAllHandlerMappings(boolean) */
    public void setDetectAllHandlerMappings(boolean detectAllHandlerMappings) {
        dispatcherServlet.setDetectAllHandlerMappings(detectAllHandlerMappings);
    }

    /** @see DispatcherServlet#setDetectAllHandlerAdapters(boolean) */
    public void setDetectAllHandlerAdapters(boolean detectAllHandlerAdapters) {
        dispatcherServlet.setDetectAllHandlerAdapters(detectAllHandlerAdapters);
    }

    /** @see DispatcherServlet#setDetectAllHandlerExceptionResolvers(boolean) */
    public void setDetectAllHandlerExceptionResolvers(boolean detectAllHandlerExceptionResolvers) {
        dispatcherServlet.setDetectAllHandlerExceptionResolvers(detectAllHandlerExceptionResolvers);
    }

    /** @see DispatcherServlet#setDetectAllViewResolvers(boolean) */
    public void setDetectAllViewResolvers(boolean detectAllViewResolvers) {
        dispatcherServlet.setDetectAllViewResolvers(detectAllViewResolvers);
    }

    /** @see DispatcherServlet#setThrowExceptionIfNoHandlerFound(boolean) */
    public void setThrowExceptionIfNoHandlerFound(boolean throwExceptionIfNoHandlerFound) {
        dispatcherServlet.setThrowExceptionIfNoHandlerFound(throwExceptionIfNoHandlerFound);
    }

    /** @see DispatcherServlet#getThemeSource() */
    public final ThemeSource getThemeSource() {
        return dispatcherServlet.getThemeSource();
    }
    
    /** @see DispatcherServlet#getMultipartResolver() */
    public final MultipartResolver getMultipartResolver() {
        return dispatcherServlet.getMultipartResolver();
    }

    /** @see DispatcherServlet#setCleanupAfterInclude(boolean) */
    public void setCleanupAfterInclude(boolean cleanupAfterInclude) {
        dispatcherServlet.setCleanupAfterInclude(cleanupAfterInclude);
    }

    ///////////////////////////////////////////////
    // Proxying protected features below this point
    ///////////////////////////////////////////////

    /** @see FiberDispatcherServletProtectedWrapper#pageNotFoundLogger */
    protected static final Log pageNotFoundLogger = FiberDispatcherServletProtectedWrapper.pageNotFoundLogger;

    /** @see FiberDispatcherServletProtectedWrapper#onRefresh(org.springframework.context.ApplicationContext) */
    protected void onRefresh(ApplicationContext context) {
        fiberDispatcherServletProtectedWrapper.onRefresh(context);
    }

    /** @see FiberDispatcherServletProtectedWrapper#getDefaultStrategy(org.springframework.context.ApplicationContext, java.lang.Class) */
    protected <T> T getDefaultStrategy(ApplicationContext context, Class<T> strategyInterface) {
        return fiberDispatcherServletProtectedWrapper.getDefaultStrategy(context, strategyInterface);
    }

    /** @see FiberDispatcherServletProtectedWrapper#getDefaultStrategies(org.springframework.context.ApplicationContext, java.lang.Class) */
    protected <T> List<T> getDefaultStrategies(ApplicationContext context, Class<T> strategyInterface) {
        return fiberDispatcherServletProtectedWrapper.getDefaultStrategies(context, strategyInterface);
    }

    /** @see FiberDispatcherServletProtectedWrapper#createDefaultStrategy(org.springframework.context.ApplicationContext, java.lang.Class) */
    protected Object createDefaultStrategy(ApplicationContext context, Class<?> clazz) {
        return fiberDispatcherServletProtectedWrapper.createDefaultStrategy(context, clazz);
    }

    /** @see FiberDispatcherServletProtectedWrapper#buildLocaleContext(javax.servlet.http.HttpServletRequest) */
    protected LocaleContext buildLocaleContext(final HttpServletRequest request) {
        return fiberDispatcherServletProtectedWrapper.buildLocaleContext(request);
    }
    
    /** @see FiberDispatcherServletProtectedWrapper#getHandler(javax.servlet.http.HttpServletRequest) */
    protected HandlerExecutionChain getHandler(HttpServletRequest request) throws Exception {
        return fiberDispatcherServletProtectedWrapper.getHandler(request);
    }

    /** @see FiberDispatcherServletProtectedWrapper#noHandlerFound(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)  */
    protected void noHandlerFound(HttpServletRequest request, HttpServletResponse response) throws Exception {
        fiberDispatcherServletProtectedWrapper.noHandlerFound(request, response);
    }

    /** @see FiberDispatcherServletProtectedWrapper#getHandlerAdapter(java.lang.Object) */
    protected HandlerAdapter getHandlerAdapter(Object handler) throws ServletException {
        return fiberDispatcherServletProtectedWrapper.getHandlerAdapter(handler);
    }

    /** @see FiberDispatcherServletProtectedWrapper#processHandlerException(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, java.lang.Object, java.lang.Exception) */
    protected ModelAndView processHandlerException(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        return fiberDispatcherServletProtectedWrapper.processHandlerException(request, response, handler, ex);
    }

    /** @see FiberDispatcherServletProtectedWrapper#render(org.springframework.web.servlet.ModelAndView, javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse) */
    protected void render(ModelAndView mv, HttpServletRequest request, HttpServletResponse response) throws Exception {
        fiberDispatcherServletProtectedWrapper.render(mv, request, response);
    }

    /** @see FiberDispatcherServletProtectedWrapper#getDefaultViewName(javax.servlet.http.HttpServletRequest) */
    protected String getDefaultViewName(HttpServletRequest request) throws Exception {
        return fiberDispatcherServletProtectedWrapper.getDefaultViewName(request);
    }

    /** @see FiberDispatcherServletProtectedWrapper#resolveViewName(java.lang.String, java.util.Map, java.util.Locale, javax.servlet.http.HttpServletRequest) */
    protected View resolveViewName(String viewName, Map<String, Object> model, Locale locale, HttpServletRequest request) throws Exception {
        return fiberDispatcherServletProtectedWrapper.resolveViewName(viewName, model, locale, request);
    }

    /** @see FiberDispatcherServletProtectedWrapper#initStrategies(org.springframework.context.ApplicationContext) */
    protected void initStrategies(ApplicationContext context) {
        fiberDispatcherServletProtectedWrapper.initStrategies(context);
    }

    /** @see FiberDispatcherServletProtectedWrapper#doService(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse) */
    @Override
    protected void doService(HttpServletRequest request, HttpServletResponse response) throws Exception, SuspendExecution {
        fiberDispatcherServletProtectedWrapper.doService(request, response);
    }

    /** @see DispatcherServlet#doDispatch(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse) */
    protected void doDispatch(HttpServletRequest request, HttpServletResponse response) throws Exception, SuspendExecution {
        fiberDispatcherServletProtectedWrapper.doDispatch(request, response);
    }

    /** @see FiberDispatcherServletProtectedWrapper#checkMultipart(javax.servlet.http.HttpServletRequest) */
    protected HttpServletRequest checkMultipart(HttpServletRequest request) throws MultipartException {
        return fiberDispatcherServletProtectedWrapper.checkMultipart(request);
    }

    /** @see FiberDispatcherServletProtectedWrapper#cleanupMultipart(javax.servlet.http.HttpServletRequest) */
    protected void cleanupMultipart(HttpServletRequest request) {
        fiberDispatcherServletProtectedWrapper.cleanupMultipart(request);
    }

    ////////////////////////////////////////////////////////////////////////
    // Re-implementing public features below this point;
    // derived from DospatcherServlet, relevant copyright and licences apply
    ////////////////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////////////////
    // Re-implementing protected features below this point;
    // derived from DospatcherServlet, relevant copyright and licences apply
    ////////////////////////////////////////////////////////////////////////
    
    ///////////////////////////////////////////////////////////////////////
    // Re-implementing private features below this point;
    // derived from FrameworkServlet, relevant copyright and licences apply
    ///////////////////////////////////////////////////////////////////////

    /////////////////////////////
    // Untouched private features
    /////////////////////////////
    
    // private static final String DEFAULT_STRATEGIES_PATH = "DispatcherServlet.properties";
    // private static final Properties defaultStrategies;
    // private boolean detectAllHandlerMappings = true;
    // private boolean detectAllHandlerAdapters = true;
    // private boolean detectAllHandlerExceptionResolvers = true;
    // private boolean detectAllViewResolvers = true;
    // private boolean cleanupAfterInclude = true;
    // private boolean throwExceptionIfNoHandlerFound = false;
    // private LocaleResolver localeResolver;
    // private MultipartResolver multipartResolver;
    // private ThemeResolver themeResolver;
    // private List<HandlerMapping> handlerMappings;
    // private List<HandlerAdapter> handlerAdapters;
    // private List<HandlerExceptionResolver> handlerExceptionResolvers;
    // private RequestToViewNameTranslator viewNameTranslator;
    // private FlashMapManager flashMapManager;
    // private List<ViewResolver> viewResolvers;

    // private void initMultipartResolver(ApplicationContext context);
    // private void initLocaleResolver(ApplicationContext context);
    // private void initThemeResolver(ApplicationContext context);
    // private void initHandlerMappings(ApplicationContext context);
    // private void initHandlerAdapters(ApplicationContext context);
    // private void initHandlerExceptionResolvers(ApplicationContext context);
    // private void initRequestToViewNameTranslator(ApplicationContext context);
    // private void initViewResolvers(ApplicationContext context);
    // private void initFlashMapManager(ApplicationContext context);
    // private void triggerAfterCompletion(HttpServletRequest request, HttpServletResponse response, HandlerExecutionChain mappedHandler, Exception ex) throws Exception;
    // private void triggerAfterCompletionWithError(HttpServletRequest request, HttpServletResponse response, HandlerExecutionChain mappedHandler, Error error) throws Exception;
    // private void restoreAttributesAfterInclude(HttpServletRequest request, Map<?,?> attributesSnapshot);
}