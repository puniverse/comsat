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
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.support.ConfigurableWebBindingInitializer;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;

// TODO circlespainter: comment & JavaDocs

/**
 * @author circlespainter
 */
public class WebMvcConfigurationSupportProtectedProxy {
    private final WebMvcConfigurationSupport webMvcConfigurationSupport;

    public WebMvcConfigurationSupportProtectedProxy(WebMvcConfigurationSupport webMvcConfigurationSupport) {
        this.webMvcConfigurationSupport = webMvcConfigurationSupport;
    }

    public ConfigurableWebBindingInitializer getConfigurableWebBindingInitializer() {
        return webMvcConfigurationSupport.getConfigurableWebBindingInitializer();
    }

    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
        webMvcConfigurationSupport.addArgumentResolvers(argumentResolvers);
    }

    public void addReturnValueHandlers(List<HandlerMethodReturnValueHandler> returnValueHandlers) {
        webMvcConfigurationSupport.addReturnValueHandlers(returnValueHandlers);
    }

    public final List<HttpMessageConverter<?>> getMessageConverters() {
        return webMvcConfigurationSupport.getMessageConverters();
    }
}
