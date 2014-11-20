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

package org.springframework.web.servlet.config.annotation;

import java.util.List;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.web.context.request.async.CallableProcessingInterceptor;
import org.springframework.web.context.request.async.DeferredResultProcessingInterceptor;

// TODO circlespainter: comment & JavaDocs

/**
 * @author circlespainter
 */
public class AsyncSupportConfigurerProtectedProxy {
    private final AsyncSupportConfigurer asyncSupportConfigurer;

    public AsyncSupportConfigurerProtectedProxy(AsyncSupportConfigurer asyncSupportConfigurer) {
        this.asyncSupportConfigurer = asyncSupportConfigurer;
    }

    public AsyncTaskExecutor getTaskExecutor() {
        return asyncSupportConfigurer.getTaskExecutor();
    }

    public Long getTimeout() {
        return asyncSupportConfigurer.getTimeout();
    }

    public List<CallableProcessingInterceptor> getCallableInterceptors() {
        return asyncSupportConfigurer.getCallableInterceptors();
    }

    public List<DeferredResultProcessingInterceptor> getDeferredResultInterceptors() {
        return asyncSupportConfigurer.getDeferredResultInterceptors();
    }
}
