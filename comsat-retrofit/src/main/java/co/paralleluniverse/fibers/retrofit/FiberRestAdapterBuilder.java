package co.paralleluniverse.fibers.retrofit;

import co.paralleluniverse.fibers.httpclient.FiberHttpClient;
import co.paralleluniverse.fibers.httpclient.FiberHttpClientBuilder;
import org.apache.http.client.HttpClient;
import retrofit.RestAdapter;
import retrofit.client.ApacheClient;
import retrofit.client.Client;

public class FiberRestAdapterBuilder extends RestAdapter.Builder {
    private HttpClient httpClient;

    @Override
    public RestAdapter.Builder setClient(Client.Provider clientProvider) {
        throw new UnsupportedOperationException("Only Fiber Apache Http clients are allowed here. Use setClient(FiberHttpClient) instead.");
    }

    @Override
    public RestAdapter.Builder setClient(Client client) {
        throw new UnsupportedOperationException("Only Fiber Apache Http clients are allowed here. Use setClient(FiberHttpClient) instead.");

    }

    public RestAdapter.Builder setClient(FiberHttpClient client) {
        this.httpClient = client;
        return this;
    }

    @Override
    public RestAdapter build() {
        if (httpClient == null)
            // setUserAgent("") is not default it ApacheHttpClient, and is needed for the github API  
            this.httpClient = FiberHttpClientBuilder.create().setUserAgent("").build();
        super.setClient(providerFor(new ApacheClient(httpClient)));
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
