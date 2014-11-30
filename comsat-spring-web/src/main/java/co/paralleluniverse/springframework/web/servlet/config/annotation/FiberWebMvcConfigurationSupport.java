/*
 * Copyright 2002-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/*
 * Based on org.springframework.web.servlet.config.annotation.FiberWebMvcConfigurationSupport
 * in Spring Framework Web MVC.
 * Copyright the original authors Rossen Stoyanchev, Brian Clozel and Sebastien Deleuze.
 * Released under the ASF 2.0 license.
 */
package co.paralleluniverse.springframework.web.servlet.config.annotation;

import java.util.ArrayList;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.ServletConfigAnnotationProtectedProxy;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;

import co.paralleluniverses.pringframework.web.servlet.mvc.method.annotation.FiberRequestMappingHandlerAdapter;

/**
 * @author circlespainter
 * 
 * @see EnableWebMvc
 * @see WebMvcConfigurer
 * @see WebMvcConfigurerAdapter
 */
@Configuration
public class FiberWebMvcConfigurationSupport extends WebMvcConfigurationSupport {
    @Bean
    public FiberRequestMappingHandlerAdapter fiberRequestMappingHandlerAdapter() {
        List<HandlerMethodArgumentResolver> argumentResolvers = new ArrayList<>();
        addArgumentResolvers(argumentResolvers);

        List<HandlerMethodReturnValueHandler> returnValueHandlers = new ArrayList<>();
        addReturnValueHandlers(returnValueHandlers);

        FiberRequestMappingHandlerAdapter adapter = new FiberRequestMappingHandlerAdapter();
        adapter.setContentNegotiationManager(mvcContentNegotiationManager());
        adapter.setMessageConverters(getMessageConverters());
        adapter.setWebBindingInitializer(getConfigurableWebBindingInitializer());
        adapter.setCustomArgumentResolvers(argumentResolvers);
        adapter.setCustomReturnValueHandlers(returnValueHandlers);

        AsyncSupportConfigurer configurer = new AsyncSupportConfigurer();
        configureAsyncSupport(configurer);

        if (ServletConfigAnnotationProtectedProxy.getTaskExecutor(configurer) != null) {
            adapter.setTaskExecutor(ServletConfigAnnotationProtectedProxy.getTaskExecutor(configurer));
        }
        if (ServletConfigAnnotationProtectedProxy.getTimeout(configurer) != null) {
            adapter.setAsyncRequestTimeout(ServletConfigAnnotationProtectedProxy.getTimeout(configurer));
        }
        adapter.setCallableInterceptors(ServletConfigAnnotationProtectedProxy.getCallableInterceptors(configurer));
        adapter.setDeferredResultInterceptors(ServletConfigAnnotationProtectedProxy.getDeferredResultInterceptors(configurer));

        return adapter;
    }
}
