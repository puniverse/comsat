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
 * Based on org.springframework.boot.autoconfigure.web.RequestMappingHandlerAdapter in Spring Boot
 * Copyright the original authors Phillip Webb and Dave Syer.
 * Released under the ASF 2.0 license.
 */
package co.paralleluniverse.fibers.springframework.boot.autoconfigure.web;

import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.bind.support.SessionAttributeStore;
import org.springframework.web.bind.support.WebBindingInitializer;

import org.springframework.web.context.request.async.CallableProcessingInterceptor;
import org.springframework.web.context.request.async.DeferredResultProcessingInterceptor;

import org.springframework.web.method.HandlerMethod;
import org.springframework.web.method.annotation.InitBinderDataBinderFactory;

import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.method.support.InvocableHandlerMethod;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.annotation.ModelAndViewResolver;
import org.springframework.web.servlet.mvc.method.AbstractHandlerMethodAdapter;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapterProtectedProxy;

// TODO circlespainter: finish, comment & JavaDocs

/**
 *
 * @author dev
 */
public class FiberRequestMappingHandlerAdapter extends AbstractHandlerMethodAdapter implements BeanFactoryAware, InitializingBean {
    private final RequestMappingHandlerAdapter requestMappingHandlerAdapter;
    private final RequestMappingHandlerAdapterProtectedProxy requestMappingHandlerAdapterProtectedProxy;

    public FiberRequestMappingHandlerAdapter() {
        this.requestMappingHandlerAdapter = new RequestMappingHandlerAdapter();
        this.requestMappingHandlerAdapterProtectedProxy = new RequestMappingHandlerAdapterProtectedProxy(requestMappingHandlerAdapter);
    }

    @Override
    protected ModelAndView handleInternal(HttpServletRequest request, HttpServletResponse response, HandlerMethod handlerMethod) throws Exception {
        // TODO circlespainter: implement
        throw new RuntimeException("Not implemented yet");
    }
    
    public ConfigurableBeanFactory getBeanFactory() {
        return requestMappingHandlerAdapterProtectedProxy.getBeanFactory();
    }

    @Override
    public boolean supportsInternal(HandlerMethod handlerMethod) {
        return requestMappingHandlerAdapterProtectedProxy.supportsInternal(handlerMethod);
    }

    @Override
    public long getLastModifiedInternal(HttpServletRequest request, HandlerMethod handlerMethod) {
        return requestMappingHandlerAdapterProtectedProxy.getLastModifiedInternal(request, handlerMethod);
    }

    public InitBinderDataBinderFactory createDataBinderFactory(List<InvocableHandlerMethod> binderMethods) throws Exception {
        return requestMappingHandlerAdapterProtectedProxy.createDataBinderFactory(binderMethods);
    }

    @Override
    public void setBeanFactory(BeanFactory bf) throws BeansException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public void setCustomArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
        requestMappingHandlerAdapter.setCustomArgumentResolvers(argumentResolvers);
    }

    public List<HandlerMethodArgumentResolver> getCustomArgumentResolvers() {
        return requestMappingHandlerAdapter.getCustomArgumentResolvers();
    }

    public void setArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
        requestMappingHandlerAdapter.setArgumentResolvers(argumentResolvers);
    }

    public List<HandlerMethodArgumentResolver> getArgumentResolvers() {
        return requestMappingHandlerAdapter.getArgumentResolvers();
    }

    public void setInitBinderArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
        requestMappingHandlerAdapter.setInitBinderArgumentResolvers(argumentResolvers);
    }

    public List<HandlerMethodArgumentResolver> getInitBinderArgumentResolvers() {
        return requestMappingHandlerAdapter.getInitBinderArgumentResolvers();
    }

    public void setCustomReturnValueHandlers(List<HandlerMethodReturnValueHandler> returnValueHandlers) {
        requestMappingHandlerAdapter.setCustomReturnValueHandlers(returnValueHandlers);
    }

    public List<HandlerMethodReturnValueHandler> getCustomReturnValueHandlers() {
        return requestMappingHandlerAdapter.getCustomReturnValueHandlers();
    }

    public void setReturnValueHandlers(List<HandlerMethodReturnValueHandler> returnValueHandlers) {
        requestMappingHandlerAdapter.setReturnValueHandlers(returnValueHandlers);
    }

    public List<HandlerMethodReturnValueHandler> getReturnValueHandlers() {
        return requestMappingHandlerAdapter.getReturnValueHandlers();
    }

    public void setModelAndViewResolvers(List<ModelAndViewResolver> modelAndViewResolvers) {
        requestMappingHandlerAdapter.setModelAndViewResolvers(modelAndViewResolvers);
    }

    public List<ModelAndViewResolver> getModelAndViewResolvers() {
        return requestMappingHandlerAdapter.getModelAndViewResolvers();
    }

    public void setMessageConverters(List<HttpMessageConverter<?>> messageConverters) {
        requestMappingHandlerAdapter.setMessageConverters(messageConverters);
    }

    public void setContentNegotiationManager(ContentNegotiationManager contentNegotiationManager) {
        requestMappingHandlerAdapter.setContentNegotiationManager(contentNegotiationManager);
    }

    public List<HttpMessageConverter<?>> getMessageConverters() {
        return requestMappingHandlerAdapter.getMessageConverters();
    }

    public void setWebBindingInitializer(WebBindingInitializer webBindingInitializer) {
        requestMappingHandlerAdapter.setWebBindingInitializer(webBindingInitializer);
    }

    public WebBindingInitializer getWebBindingInitializer() {
        return requestMappingHandlerAdapter.getWebBindingInitializer();
    }

    public void setTaskExecutor(AsyncTaskExecutor taskExecutor) {
        requestMappingHandlerAdapter.setTaskExecutor(taskExecutor);
    }

    public void setAsyncRequestTimeout(long timeout) {
        requestMappingHandlerAdapter.setAsyncRequestTimeout(timeout);
    }

    public void setCallableInterceptors(List<CallableProcessingInterceptor> interceptors) {
        requestMappingHandlerAdapter.setCallableInterceptors(interceptors);
    }

    public void setDeferredResultInterceptors(List<DeferredResultProcessingInterceptor> interceptors) {
        requestMappingHandlerAdapter.setDeferredResultInterceptors(interceptors);
    }

    public void setIgnoreDefaultModelOnRedirect(boolean ignoreDefaultModelOnRedirect) {
        requestMappingHandlerAdapter.setIgnoreDefaultModelOnRedirect(ignoreDefaultModelOnRedirect);
    }

    public void setSessionAttributeStore(SessionAttributeStore sessionAttributeStore) {
        requestMappingHandlerAdapter.setSessionAttributeStore(sessionAttributeStore);
    }

    public void setCacheSecondsForSessionAttributeHandlers(int cacheSecondsForSessionAttributeHandlers) {
        requestMappingHandlerAdapter.setCacheSecondsForSessionAttributeHandlers(cacheSecondsForSessionAttributeHandlers);
    }

    public void setSynchronizeOnSession(boolean synchronizeOnSession) {
        requestMappingHandlerAdapter.setSynchronizeOnSession(synchronizeOnSession);
    }

    public void setParameterNameDiscoverer(ParameterNameDiscoverer parameterNameDiscoverer) {
        requestMappingHandlerAdapter.setParameterNameDiscoverer(parameterNameDiscoverer);
    }
}
