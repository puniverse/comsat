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

import co.paralleluniverse.jersey.connector.JettyConnector;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.security.KeyStore;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Configuration;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.spi.RequestExecutorsProvider;

/**
 * Main entry point to the client API used to bootstrap Client instances that integrate with Quasar fibers.
 */
public class AsyncClientBuilder extends ClientBuilder {
    private final ClientBuilder clientBuilder;

    protected AsyncClientBuilder(ClientBuilder clientBuilder) {
        this.clientBuilder = clientBuilder;
    }

    /**
     * Create a new {@code ClientBuilder} instance using the default client builder
     * implementation class provided by the JAX-RS implementation provider.
     *
     * @return new client builder instance.
     */
    public static ClientBuilder newBuilder() {
        return new AsyncClientBuilder(ClientBuilder.newBuilder());
    }

    /**
     * Create a new {@link Client} instance using the default client builder implementation
     * class provided by the JAX-RS implementation provider.
     *
     * @return new client instance.
     */
    public static Client newClient() {
        return newClient(null);
    }

    /**
     * Create a new custom-configured {@link Client} instance using the default client builder
     * implementation class provided by the JAX-RS implementation provider.
     *
     * @param configuration data used to provide initial configuration for the new client instance.
     * @return a new, configured, client instance.
     */
    public static Client newClient(Configuration configuration) {
        final RequestExecutorsProvider singleThreadPool = new RequestExecutorsProvider() {
            private ExecutorService tp = Executors.newSingleThreadExecutor(new ThreadFactoryBuilder().setDaemon(true).setNameFormat("jersey-puniverse-single-worker-%d").build());

            @Override
            public ExecutorService getRequestingExecutor() {
                return tp;
            }
        };
        final ClientConfig config = new ClientConfig().
                register(singleThreadPool, RequestExecutorsProvider.class);
        if (configuration != null)
            config.loadFrom(configuration);
        if (config.getConnector() == null)
            config.connector(new JettyConnector(new ClientConfig().
                    property(ClientProperties.ASYNC_THREADPOOL_SIZE, 20)));

        return new FiberClient(ClientBuilder.newClient(config));
    }

    @Override
    public Client build() {
        return new FiberClient(clientBuilder.build());
    }

    @Override
    public ClientBuilder withConfig(Configuration config) {
        clientBuilder.withConfig(config);
        return this;
    }

    @Override
    public ClientBuilder sslContext(SSLContext sslContext) {
        clientBuilder.sslContext(sslContext);
        return this;
    }

    @Override
    public ClientBuilder keyStore(KeyStore keyStore, char[] password) {
        clientBuilder.keyStore(keyStore, password);
        return this;
    }

    @Override
    public ClientBuilder keyStore(KeyStore keyStore, String password) {
        clientBuilder.keyStore(keyStore, password);
        return this;
    }

    @Override
    public ClientBuilder trustStore(KeyStore trustStore) {
        clientBuilder.trustStore(trustStore);
        return this;
    }

    @Override
    public ClientBuilder hostnameVerifier(HostnameVerifier verifier) {
        clientBuilder.hostnameVerifier(verifier);
        return this;
    }

    @Override
    public ClientBuilder property(String name, Object value) {
        clientBuilder.property(name, value);
        return this;
    }

    @Override
    public ClientBuilder register(Class<?> componentClass) {
        clientBuilder.register(componentClass);
        return this;
    }

    @Override
    public ClientBuilder register(Class<?> componentClass, int priority) {
        clientBuilder.register(componentClass, priority);
        return this;
    }

    @Override
    public ClientBuilder register(Class<?> componentClass, Class<?>... contracts) {
        clientBuilder.register(componentClass, contracts);
        return this;
    }

    @Override
    public ClientBuilder register(Class<?> componentClass, Map<Class<?>, Integer> contracts) {
        clientBuilder.register(componentClass, contracts);
        return this;
    }

    @Override
    public ClientBuilder register(Object component) {
        clientBuilder.register(component);
        return this;
    }

    @Override
    public ClientBuilder register(Object component, int priority) {
        clientBuilder.register(component, priority);
        return this;
    }

    @Override
    public ClientBuilder register(Object component, Class<?>... contracts) {
        clientBuilder.register(component, contracts);
        return this;
    }

    @Override
    public ClientBuilder register(Object component, Map<Class<?>, Integer> contracts) {
        clientBuilder.register(component, contracts);
        return this;
    }

    @Override
    public Configuration getConfiguration() {
        return clientBuilder.getConfiguration();
    }

    @Override
    public int hashCode() {
        return clientBuilder.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return clientBuilder.equals(obj);
    }

    @Override
    public String toString() {
        return clientBuilder.toString();
    }
}
