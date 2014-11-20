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
package org.springframework.web.servlet.mvc.method.annotation;

import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.method.annotation.InitBinderDataBinderFactory;
import org.springframework.web.method.support.InvocableHandlerMethod;

// TODO circlespainter: comment & JavaDocs

/**
 * @author circlespainter
 */
public class RequestMappingHandlerAdapterProtectedProxy {
    private final RequestMappingHandlerAdapter requestMappingHandlerAdapter;

    public RequestMappingHandlerAdapterProtectedProxy(RequestMappingHandlerAdapter requestMappingHandlerAdapter) {
        this.requestMappingHandlerAdapter = requestMappingHandlerAdapter;
    }

    public ConfigurableBeanFactory getBeanFactory() {
        return requestMappingHandlerAdapter.getBeanFactory();
    }

    public boolean supportsInternal(HandlerMethod handlerMethod) {
        return requestMappingHandlerAdapter.supportsInternal(handlerMethod);
    }

    public long getLastModifiedInternal(HttpServletRequest request, HandlerMethod handlerMethod) {
        return requestMappingHandlerAdapter.getLastModifiedInternal(request, handlerMethod);
    }

    public InitBinderDataBinderFactory createDataBinderFactory(List<InvocableHandlerMethod> binderMethods) throws Exception {
        return requestMappingHandlerAdapter.createDataBinderFactory(binderMethods);
    }
}
