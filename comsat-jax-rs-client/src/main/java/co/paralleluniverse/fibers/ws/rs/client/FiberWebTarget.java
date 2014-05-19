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
package co.paralleluniverse.fibers.ws.rs.client;

import java.net.URI;
import java.util.Map;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;

class FiberWebTarget implements WebTarget {
    private final WebTarget webTarget;

    public FiberWebTarget(WebTarget webTarget) {
        this.webTarget = webTarget;
    }

    private static Builder wrap(Builder builder) {
        return new FiberBuilder(builder);
    }
    
    private WebTarget wrap(WebTarget webTarget) {
        if (webTarget instanceof FiberWebTarget && ((FiberWebTarget) webTarget).webTarget == this.webTarget)
            return this;
        return new FiberWebTarget(webTarget);
    }
    
    @Override
    public Builder request() {
        return wrap(webTarget.request());
    }

    @Override
    public Builder request(String... acceptedResponseTypes) {
        return wrap(webTarget.request(acceptedResponseTypes));
    }

    @Override
    public Builder request(MediaType... acceptedResponseTypes) {
        return wrap(webTarget.request(acceptedResponseTypes));
    }

    @Override
    public WebTarget path(String path) {
        return wrap(webTarget.path(path));
    }

    @Override
    public WebTarget resolveTemplate(String name, Object value) {
        return wrap(webTarget.resolveTemplate(name, value));
    }

    @Override
    public WebTarget resolveTemplate(String name, Object value, boolean encodeSlashInPath) {
        return wrap(webTarget.resolveTemplate(name, value, encodeSlashInPath));
    }

    @Override
    public WebTarget resolveTemplateFromEncoded(String name, Object value) {
        return wrap(webTarget.resolveTemplateFromEncoded(name, value));
    }

    @Override
    public WebTarget resolveTemplates(Map<String, Object> templateValues) {
        return wrap(webTarget.resolveTemplates(templateValues));
    }

    @Override
    public WebTarget resolveTemplates(Map<String, Object> templateValues, boolean encodeSlashInPath) {
        return wrap(webTarget.resolveTemplates(templateValues, encodeSlashInPath));
    }

    @Override
    public WebTarget resolveTemplatesFromEncoded(Map<String, Object> templateValues) {
        return wrap(webTarget.resolveTemplatesFromEncoded(templateValues));
    }

    @Override
    public WebTarget matrixParam(String name, Object... values) {
        return wrap(webTarget.matrixParam(name, values));
    }

    @Override
    public WebTarget queryParam(String name, Object... values) {
        return wrap(webTarget.queryParam(name, values));
    }

    @Override
    public WebTarget property(String name, Object value) {
        webTarget.property(name, value);
        return this;
    }

    @Override
    public WebTarget register(Class<?> componentClass) {
        webTarget.register(componentClass);
        return this;
    }

    @Override
    public WebTarget register(Class<?> componentClass, int priority) {
        webTarget.register(componentClass, priority);
        return this;
    }

    @Override
    public WebTarget register(Class<?> componentClass, Class<?>... contracts) {
        webTarget.register(componentClass, contracts);
        return this;
    }

    @Override
    public WebTarget register(Class<?> componentClass, Map<Class<?>, Integer> contracts) {
        webTarget.register(componentClass, contracts);
        return this;
    }

    @Override
    public WebTarget register(Object component) {
        webTarget.register(component);
        return this;
    }

    @Override
    public WebTarget register(Object component, int priority) {
        webTarget.register(component, priority);
        return this;
    }

    @Override
    public WebTarget register(Object component, Class<?>... contracts) {
        webTarget.register(component, contracts);
        return this;
    }

    @Override
    public WebTarget register(Object component, Map<Class<?>, Integer> contracts) {
        webTarget.register(component, contracts);
        return this;
    }

    // Delegations
    @Override
    public URI getUri() {
        return webTarget.getUri();
    }

    @Override
    public UriBuilder getUriBuilder() {
        return webTarget.getUriBuilder();
    }

    @Override
    public Configuration getConfiguration() {
        return webTarget.getConfiguration();
    }

    @Override
    public int hashCode() {
        return webTarget.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return webTarget.equals(obj);
    }

    @Override
    public String toString() {
        return webTarget.toString();
    }
}
