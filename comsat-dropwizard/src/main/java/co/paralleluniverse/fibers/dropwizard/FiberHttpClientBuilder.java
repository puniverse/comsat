/*
 * COMSAT
 * Copyright (C) 2014, Parallel Universe Software Co. All rights reserved.
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
package co.paralleluniverse.fibers.dropwizard;

import co.paralleluniverse.fibers.httpclient.FiberHttpClient;
import com.codahale.metrics.MetricRegistry;
import io.dropwizard.client.HttpClientConfiguration;
import io.dropwizard.setup.Environment;
import io.dropwizard.util.Duration;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.DnsResolver;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContexts;
import org.apache.http.conn.ssl.SSLInitializationException;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.impl.NoConnectionReuseStrategy;
import org.apache.http.impl.client.DefaultConnectionKeepAliveStrategy;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.conn.SchemeRegistryFactory;
import org.apache.http.impl.conn.SystemDefaultDnsResolver;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.http.impl.nio.conn.PoolingNHttpClientConnectionManager;
import org.apache.http.impl.nio.reactor.DefaultConnectingIOReactor;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.apache.http.nio.conn.NHttpClientConnectionManager;
import org.apache.http.nio.conn.NoopIOSessionStrategy;
import org.apache.http.nio.conn.SchemeIOSessionStrategy;
import org.apache.http.nio.conn.ssl.SSLIOSessionStrategy;
import org.apache.http.nio.reactor.ConnectingIOReactor;
import org.apache.http.nio.reactor.IOReactorException;
import org.apache.http.protocol.HttpContext;

/**
 * A convenience class for building {@link HttpClient} instances.
 * <p>
 * Among other things,
 * <ul>
 * <li>Disables stale connection checks</li>
 * <li>Disables Nagle's algorithm</li>
 * <li>Disables cookie management by default</li>
 * </ul>
 */
public class FiberHttpClientBuilder {

    private final MetricRegistry metricRegistry;
    private HttpClientConfiguration configuration = new HttpClientConfiguration();
    private DnsResolver resolver = new SystemDefaultDnsResolver();
    private HttpRequestRetryHandler httpRequestRetryHandler;
    private SchemeRegistry registry = SchemeRegistryFactory.createSystemDefault();

    public FiberHttpClientBuilder(MetricRegistry metricRegistry) {
        this.metricRegistry = metricRegistry;
    }

    public FiberHttpClientBuilder(Environment environment) {
        this.metricRegistry = environment.metrics();
    }

    /**
     * Use the given {@link HttpClientConfiguration} instance.
     *
     * @param configuration a {@link HttpClientConfiguration} instance
     * @return {@code this}
     */
    public FiberHttpClientBuilder using(HttpClientConfiguration configuration) {
        this.configuration = configuration;
        return this;
    }

    /**
     * Use the given {@link DnsResolver} instance.
     *
     * @param resolver a {@link DnsResolver} instance
     * @return {@code this}
     */
    public FiberHttpClientBuilder using(DnsResolver resolver) {
        this.resolver = resolver;
        return this;
    }

    /**
     * Uses the {@link httpRequestRetryHandler} for handling request retries.
     *
     * @param httpRequestRetryHandler an httpRequestRetryHandler
     * @return {@code this}
     */
    public FiberHttpClientBuilder using(HttpRequestRetryHandler httpRequestRetryHandler) {
        this.httpRequestRetryHandler = httpRequestRetryHandler;
        return this;
    }

    /**
     * Use the given {@link SchemeRegistry} instance.
     *
     * @param registry a {@link SchemeRegistry} instance
     * @return {@code this}
     */
    public FiberHttpClientBuilder using(SchemeRegistry registry) {
        this.registry = registry;
        return this;
    }

    /**
     * Builds the {@link HttpClient}.
     *
     * @return an {@link HttpClient}
     */
    public HttpClient build(String name) {
        RequestConfig createHttpParams = createHttpParams();
        final NHttpClientConnectionManager manager = createConnectionManager(registry, name);
        HttpAsyncClientBuilder clientBuilder = new InstrumentedNHttpClientBuilder(metricRegistry, name);
        clientBuilder.setConnectionManager(manager);
        clientBuilder.setDefaultRequestConfig(createHttpParams);
        setStrategiesForClient(clientBuilder);
        CloseableHttpAsyncClient client = clientBuilder.build();
        client.start();
        return new FiberHttpClient(client, getRetryHandler());
    }

    /**
     * Add strategies to client such as ConnectionReuseStrategy and KeepAliveStrategy Note that this
     * method mutates the client object by setting the strategies
     *
     * @param client The InstrumentedHttpClient that should be configured with strategies
     */
    protected void setStrategiesForClient(HttpAsyncClientBuilder client) {
        final long keepAlive = configuration.getKeepAlive().toMilliseconds();

        // don't keep alive the HTTP connection and thus don't reuse the TCP socket
        if (keepAlive == 0) {
            client.setConnectionReuseStrategy(new NoConnectionReuseStrategy());
        } else {
            client.setConnectionReuseStrategy(new DefaultConnectionReuseStrategy());
            // either keep alive based on response header Keep-Alive,
            // or if the server can keep a persistent connection (-1), then override based on client's configuration
            client.setKeepAliveStrategy(new DefaultConnectionKeepAliveStrategy() {
                @Override
                public long getKeepAliveDuration(HttpResponse response, HttpContext context) {
                    final long duration = super.getKeepAliveDuration(response, context);
                    return (duration == -1) ? keepAlive : duration;
                }
            });
        }
    }

    private HttpRequestRetryHandler getRetryHandler() {
        return configuration.getRetries() == 0 ? null
                : httpRequestRetryHandler != null ? httpRequestRetryHandler
                : new DefaultHttpRequestRetryHandler(configuration.getRetries(), false);
    }

    /**
     * Map the parameters in HttpClientConfiguration to a BasicHttpParams object
     *
     * @return a BasicHttpParams object from the HttpClientConfiguration
     */
    protected RequestConfig createHttpParams() {
        RequestConfig.Builder rcb = RequestConfig.custom();
        rcb.setCookieSpec(CookieSpecs.BEST_MATCH);
        if (configuration.isCookiesEnabled())
            rcb.setCookieSpec(CookieSpecs.BEST_MATCH);
        else
            rcb.setCookieSpec(CookieSpecs.IGNORE_COOKIES);
        rcb.setStaleConnectionCheckEnabled(false);
        return rcb.build();
    }

    /**
     * Create a InstrumentedClientConnManager based on the HttpClientConfiguration. It sets the
     * maximum connections per route and the maximum total connections that the connection manager
     * can create
     *
     * @param registry the SchemeRegistry
     * @return a InstrumentedClientConnManger instance
     */
    protected NHttpClientConnectionManager createConnectionManager(SchemeRegistry registry, String name) {
        final Duration ttl = configuration.getTimeToLive();
        ConnectingIOReactor ioReactor = createDefaultIOReactor(IOReactorConfig.custom()
                .setSoTimeout((int) configuration.getTimeout().toMilliseconds())
                .setConnectTimeout((int) configuration.getConnectionTimeout().toMilliseconds())
                .setTcpNoDelay(true).build());

        PoolingNHttpClientConnectionManager manager
                = new InstrumentedNClientConnManager(
                        ioReactor, null, null, //TODO: add this parameters values
                        metricRegistry,
                        convertRegistry(this.registry),
                        ttl.getQuantity(),
                        ttl.getUnit(),
                        resolver,
                        name);
        manager.setDefaultMaxPerRoute(configuration.getMaxConnectionsPerRoute());
        manager.setMaxTotal(configuration.getMaxConnections());
        return manager;
    }

    private static ConnectingIOReactor createDefaultIOReactor(final IOReactorConfig spec) {
        try {
            return new DefaultConnectingIOReactor(spec);
        } catch (IOReactorException ex) {
            throw new RuntimeException(ex);
        }
    }

    private static Registry<SchemeIOSessionStrategy> convertRegistry(final SchemeRegistry oldRegistry) throws SSLInitializationException {
        SchemeRegistry baseRegistry = oldRegistry;
        //TODO: use values from old registry;
        Registry<SchemeIOSessionStrategy> defaultRegistry = RegistryBuilder.<SchemeIOSessionStrategy>create()
                .register("http", NoopIOSessionStrategy.INSTANCE)
                .register("https", new SSLIOSessionStrategy(
                                SSLContexts.createDefault(), null, null, SSLConnectionSocketFactory.BROWSER_COMPATIBLE_HOSTNAME_VERIFIER))
                .build();
        return defaultRegistry;
    }

}
