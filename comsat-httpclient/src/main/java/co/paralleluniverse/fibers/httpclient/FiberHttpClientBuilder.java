/*
 * COMSAT
 * Copyright (C) 2014-2015, Parallel Universe Software Co. All rights reserved.
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
package co.paralleluniverse.fibers.httpclient;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.util.Collection;
import javax.net.ssl.SSLContext;
import org.apache.http.ConnectionReuseStrategy;
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.auth.AuthSchemeProvider;
import org.apache.http.client.AuthenticationStrategy;
import org.apache.http.client.CookieStore;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.RedirectStrategy;
import org.apache.http.client.UserTokenHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.ConnectionConfig;
import org.apache.http.config.Lookup;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.conn.SchemePortResolver;
import org.apache.http.conn.routing.HttpRoutePlanner;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.cookie.CookieSpecProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.apache.http.nio.conn.NHttpClientConnectionManager;
import org.apache.http.nio.conn.SchemeIOSessionStrategy;
import org.apache.http.nio.reactor.IOReactor;
import org.apache.http.protocol.HttpProcessor;

/**
 * Builder for creating fiber blocking httpClient.
 * The configuration properties are the same as those of HttpAsyncClientBuilder
 * @see HttpAsyncClientBuilder
 */
public class FiberHttpClientBuilder {
    private final HttpAsyncClientBuilder builder;
    private IOReactor ioreactor;

    protected FiberHttpClientBuilder(HttpAsyncClientBuilder builder) {
        this.builder = builder;
    }

    /**
     * Creates Builder with one io thread.
     * @return
     */
    public static FiberHttpClientBuilder create() {
        return create(1);
    }

    /**
     * @param ioThreadCount
     * @return
     */
    public static FiberHttpClientBuilder create(int ioThreadCount) {
        return new FiberHttpClientBuilder(HttpAsyncClientBuilder.create().
                setThreadFactory(new ThreadFactoryBuilder().setDaemon(true).build()).
                setDefaultIOReactorConfig(IOReactorConfig.custom().setIoThreadCount(ioThreadCount).build()));
    }

    public final FiberHttpClientBuilder setConnectionManager(NHttpClientConnectionManager connManager) {
        builder.setConnectionManager(connManager);
        return this;
    }

    public final FiberHttpClientBuilder setIOReactor(IOReactor ioreactor) {
        this.ioreactor = ioreactor;
        return this;
    }

    public final FiberHttpClientBuilder setSchemePortResolver(SchemePortResolver schemePortResolver) {
        builder.setSchemePortResolver(schemePortResolver);
        return this;
    }

    public final FiberHttpClientBuilder setMaxConnTotal(int maxConnTotal) {
        builder.setMaxConnTotal(maxConnTotal);
        return this;
    }

    public final FiberHttpClientBuilder setMaxConnPerRoute(int maxConnPerRoute) {
        builder.setMaxConnPerRoute(maxConnPerRoute);
        return this;
    }

    public final FiberHttpClientBuilder setConnectionReuseStrategy(ConnectionReuseStrategy reuseStrategy) {
        builder.setConnectionReuseStrategy(reuseStrategy);
        return this;
    }

    public final FiberHttpClientBuilder setKeepAliveStrategy(ConnectionKeepAliveStrategy keepAliveStrategy) {
        builder.setKeepAliveStrategy(keepAliveStrategy);
        return this;
    }

    public final FiberHttpClientBuilder setUserTokenHandler(UserTokenHandler userTokenHandler) {
        builder.setUserTokenHandler(userTokenHandler);
        return this;
    }

    public final FiberHttpClientBuilder setTargetAuthenticationStrategy(AuthenticationStrategy targetAuthStrategy) {
        builder.setTargetAuthenticationStrategy(targetAuthStrategy);
        return this;
    }

    public final FiberHttpClientBuilder setProxyAuthenticationStrategy(AuthenticationStrategy proxyAuthStrategy) {
        builder.setProxyAuthenticationStrategy(proxyAuthStrategy);
        return this;
    }

    public final FiberHttpClientBuilder setHttpProcessor(HttpProcessor httpprocessor) {
        builder.setHttpProcessor(httpprocessor);
        return this;
    }

    public final FiberHttpClientBuilder addInterceptorFirst(HttpResponseInterceptor itcp) {
        builder.addInterceptorFirst(itcp);
        return this;
    }

    public final FiberHttpClientBuilder addInterceptorLast(HttpResponseInterceptor itcp) {
        builder.addInterceptorLast(itcp);
        return this;
    }

    public final FiberHttpClientBuilder addInterceptorFirst(HttpRequestInterceptor itcp) {
        builder.addInterceptorFirst(itcp);
        return this;
    }

    public final FiberHttpClientBuilder addInterceptorLast(HttpRequestInterceptor itcp) {
        builder.addInterceptorLast(itcp);
        return this;
    }

    public final FiberHttpClientBuilder setRoutePlanner(HttpRoutePlanner routePlanner) {
        builder.setRoutePlanner(routePlanner);
        return this;
    }

    public final FiberHttpClientBuilder setRedirectStrategy(RedirectStrategy redirectStrategy) {
        builder.setRedirectStrategy(redirectStrategy);
        return this;
    }

    public final FiberHttpClientBuilder setDefaultCookieStore(CookieStore cookieStore) {
        builder.setDefaultCookieStore(cookieStore);
        return this;
    }

    public final FiberHttpClientBuilder setDefaultCredentialsProvider(CredentialsProvider credentialsProvider) {
        builder.setDefaultCredentialsProvider(credentialsProvider);
        return this;
    }

    public final FiberHttpClientBuilder setDefaultAuthSchemeRegistry(Lookup<AuthSchemeProvider> authSchemeRegistry) {
        builder.setDefaultAuthSchemeRegistry(authSchemeRegistry);
        return this;
    }

    public final FiberHttpClientBuilder setDefaultCookieSpecRegistry(Lookup<CookieSpecProvider> cookieSpecRegistry) {
        builder.setDefaultCookieSpecRegistry(cookieSpecRegistry);
        return this;
    }

    public final FiberHttpClientBuilder setUserAgent(String userAgent) {
        builder.setUserAgent(userAgent);
        return this;
    }

    public final FiberHttpClientBuilder setProxy(HttpHost proxy) {
        builder.setProxy(proxy);
        return this;
    }

    public final FiberHttpClientBuilder setSSLStrategy(SchemeIOSessionStrategy strategy) {
        builder.setSSLStrategy(strategy);
        return this;
    }

    public final FiberHttpClientBuilder setSSLContext(SSLContext sslcontext) {
        builder.setSSLContext(sslcontext);
        return this;
    }

    public final FiberHttpClientBuilder setHostnameVerifier(X509HostnameVerifier hostnameVerifier) {
        builder.setHostnameVerifier(hostnameVerifier);
        return this;
    }

    public final FiberHttpClientBuilder setDefaultHeaders(Collection<? extends Header> defaultHeaders) {
        builder.setDefaultHeaders(defaultHeaders);
        return this;
    }

//    public final FiberHttpClientBuilder setDefaultIOReactorConfig(IOReactorConfig config) {
//        builder.setDefaultIOReactorConfig(config);
//        return this;
//    }
    public final FiberHttpClientBuilder setDefaultConnectionConfig(ConnectionConfig config) {
        builder.setDefaultConnectionConfig(config);
        return this;
    }

    public final FiberHttpClientBuilder setDefaultRequestConfig(RequestConfig config) {
        builder.setDefaultRequestConfig(config);
        return this;
    }

//    public final FiberHttpClientBuilder setThreadFactory(ThreadFactory threadFactory) {
//        builder.setThreadFactory(threadFactory);
//        return this;
//    }
    public final FiberHttpClientBuilder disableConnectionState() {
        builder.disableConnectionState();
        return this;
    }

    public final FiberHttpClientBuilder disableCookieManagement() {
        builder.disableCookieManagement();
        return this;
    }

    public final FiberHttpClientBuilder disableAuthCaching() {
        builder.disableAuthCaching();
        return this;
    }

    public final FiberHttpClientBuilder useSystemProperties() {
        builder.useSystemProperties();
        return this;
    }

    public CloseableHttpClient build() {
        final FiberHttpClient res =
            ioreactor != null ?
                new FiberHttpClient(builder.build(), ioreactor) :
                new FiberHttpClient(builder.build());
        return res;
    }
}
