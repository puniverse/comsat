/*
 * COMSAT
 * Copyright (c) 2013-2016, Parallel Universe Software Co. All rights reserved.
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
package co.paralleluniverse.fibers.retrofit;

import co.paralleluniverse.fibers.httpclient.FiberHttpClient;
import co.paralleluniverse.fibers.httpclient.FiberHttpClientBuilder;
import co.paralleluniverse.fibers.okhttp.FiberOkHttpClient;
import com.squareup.okhttp.OkHttpClient;
import org.apache.http.client.HttpClient;
import retrofit.RestAdapter;
import retrofit.client.ApacheClient;
import retrofit.client.Client;
import retrofit.client.OkClient;

public final class FiberRestAdapterBuilder extends RestAdapter.Builder {
    private HttpClient httpClient;
    private OkHttpClient okHttpClient;

    @Override
    public final RestAdapter.Builder setClient(Client.Provider clientProvider) {
        throw new UnsupportedOperationException("Only Fiber Apache Http or OkHttp clients are allowed here. Use setClient(FiberHttpClient) or setClient(FiberOkHttpClient) instead.");
    }

    @Override
    public final RestAdapter.Builder setClient(Client client) {
        throw new UnsupportedOperationException("Only Fiber Apache Http or OkHttp clients are allowed here. Use setClient(FiberHttpClient) or setClient(FiberOkHttpClient) instead.");

    }

    public final RestAdapter.Builder setClient(FiberHttpClient client) {
        this.httpClient = client;
        return this;
    }

    public final RestAdapter.Builder setClient(FiberOkHttpClient client) {
        this.okHttpClient = client;
        return this;
    }

    @Override
    public RestAdapter build() {
        if (okHttpClient != null) {
            super.setClient(providerFor(new OkClient(okHttpClient)));
        } else {
            if (httpClient == null)
                // setUserAgent("") is not default it ApacheHttpClient, and is needed for the github API  
                this.httpClient = FiberHttpClientBuilder.create().setUserAgent("").build();
            super.setClient(providerFor(new ApacheClient(httpClient)));
        }
        return super.build();
    }

    private static Client.Provider providerFor(final Client client) {
        return new Client.Provider() {
            @Override
            public Client get() {
                return client;
            }
        };
    }
}
