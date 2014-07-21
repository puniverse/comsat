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

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.codahale.metrics.httpclient.HttpClientMetricNameStrategies;
import com.codahale.metrics.httpclient.HttpClientMetricNameStrategy;
import java.io.IOException;
import java.util.concurrent.Future;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.http.nio.protocol.HttpAsyncRequestProducer;
import org.apache.http.nio.protocol.HttpAsyncResponseConsumer;
import org.apache.http.protocol.HttpContext;

public class InstrumentedNHttpClientBuilder extends HttpAsyncClientBuilder {
    private final MetricRegistry metricRegistry;
    private final String name;
    private final HttpClientMetricNameStrategy metricNameStrategy;

    public InstrumentedNHttpClientBuilder(MetricRegistry metricRegistry, HttpClientMetricNameStrategy metricNameStrategy, String name) {
        super();
        this.metricRegistry = metricRegistry;
        this.metricNameStrategy = metricNameStrategy;
        this.name = name;
    }

    public InstrumentedNHttpClientBuilder(MetricRegistry metricRegistry, String name) {
        this(metricRegistry, HttpClientMetricNameStrategies.METHOD_ONLY, name);
    }

    private Timer timer(HttpRequest request) {
        return metricRegistry.timer(metricNameStrategy.getNameFor(name, request));
    }

    @Override
    public CloseableHttpAsyncClient build() {

        final CloseableHttpAsyncClient ac = super.build();
        return new CloseableHttpAsyncClient() {

            @Override
            public boolean isRunning() {
                return ac.isRunning();
            }

            @Override
            public void start() {
                ac.start();
            }

            @Override
            public <T> Future<T> execute(HttpAsyncRequestProducer requestProducer, HttpAsyncResponseConsumer<T> responseConsumer, HttpContext context, FutureCallback<T> callback) {
                final Timer.Context timerContext;
                try {
                    timerContext = timer(requestProducer.generateRequest()).time();
                } catch (IOException | HttpException ex) {
                    throw new AssertionError();
                }
                try {
                    return ac.execute(requestProducer, responseConsumer, context, callback);
                } finally {
                    timerContext.stop();
                }
            }

            @Override
            public void close() throws IOException {
                ac.close();
            }
        };
    }

}
