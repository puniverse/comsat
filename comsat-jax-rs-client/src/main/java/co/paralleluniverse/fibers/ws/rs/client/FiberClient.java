/*
 * COMSAT
 * Copyright (C) 2013, Parallel Universe Software Co. All rights reserved.
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
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.UriBuilder;

class FiberClient implements Client {
    private final Client client;

    public FiberClient(Client client) {
        client.target("http://www.dummyCallLoadLazyObjects.com").request().buildGet();
        this.client = client;
    }

    // Wrap webTarget
    @Override
    public WebTarget target(String uri) {
        return new FiberWebTarget(client.target(uri));
    }

    @Override
    public WebTarget target(URI uri) {
        return new FiberWebTarget(client.target(uri));
    }

    @Override
    public WebTarget target(UriBuilder uriBuilder) {
        return new FiberWebTarget(client.target(uriBuilder));
    }

    @Override
    public WebTarget target(Link link) {
        return new FiberWebTarget(client.target(link));
    }

    // Wrap builder
    @Override
    public Builder invocation(Link link) {
        return new FiberBuilder(client.invocation(link));
    }

    // return this
    @Override
    public Client property(String name, Object value) {
        client.property(name, value);
        return this;
    }

    @Override
    public Client register(Class<?> componentClass) {
        client.register(componentClass);
        return this;
    }

    @Override
    public Client register(Class<?> componentClass, int priority) {
        client.register(componentClass, priority);
        return this;
    }

    @Override
    public Client register(Class<?> componentClass, Class<?>... contracts) {
        client.register(componentClass, contracts);
        return this;
    }

    @Override
    public Client register(Class<?> componentClass, Map<Class<?>, Integer> contracts) {
        client.register(componentClass, contracts);
        return this;
    }

    @Override
    public Client register(Object component) {
        client.register(component);
        return this;
    }

    @Override
    public Client register(Object component, int priority) {
        client.register(component, priority);
        return this;
    }

    @Override
    public Client register(Object component, Class<?>... contracts) {
        client.register(component, contracts);
        return this;
    }

    @Override
    public Client register(Object component, Map<Class<?>, Integer> contracts) {
        client.register(component, contracts);
        return this;
    }

    // Delegations
    @Override
    public SSLContext getSslContext() {
        return client.getSslContext();
    }

    @Override
    public HostnameVerifier getHostnameVerifier() {
        return client.getHostnameVerifier();
    }

    @Override
    public Configuration getConfiguration() {
        return client.getConfiguration();
    }

    @Override
    public void close() {
        client.close();
    }

    @Override
    public int hashCode() {
        return client.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return client.equals(obj);
    }

    @Override
    public String toString() {
        return client.toString();
    }
}
