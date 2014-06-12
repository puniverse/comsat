package co.paralleluniverse.fibers.retrofit;

import co.paralleluniverse.fibers.httpclient.FiberHttpClientBuilder;
import java.util.concurrent.Executor;
import org.apache.http.client.HttpClient;
import retrofit.Endpoint;
import retrofit.ErrorHandler;
import retrofit.Profiler;
import retrofit.RequestInterceptor;
import retrofit.RestAdapter;
import retrofit.client.ApacheClient;
import retrofit.client.Client;
import retrofit.converter.Converter;

public class FiberRestAdaptherBuilder {
    private final RestAdapter.Builder builder;

    public FiberRestAdaptherBuilder() {
        this(FiberHttpClientBuilder.create().setUserAgent("").build());
    }
    
    public FiberRestAdaptherBuilder(HttpClient client) {
        this.builder = new RestAdapter.Builder().setClient(new ApacheClient(client));
    }

    public RestAdapter.Builder setEndpoint(String endpoint) {
        return builder.setEndpoint(endpoint);
    }

    public RestAdapter.Builder setEndpoint(Endpoint endpoint) {
        return builder.setEndpoint(endpoint);
    }

    public RestAdapter.Builder setClient(Client client) {
        return builder.setClient(client);
    }

    public RestAdapter.Builder setClient(Client.Provider clientProvider) {
        return builder.setClient(clientProvider);
    }

    public RestAdapter.Builder setExecutors(Executor httpExecutor, Executor callbackExecutor) {
        return builder.setExecutors(httpExecutor, callbackExecutor);
    }

    public RestAdapter.Builder setRequestInterceptor(RequestInterceptor requestInterceptor) {
        return builder.setRequestInterceptor(requestInterceptor);
    }

    public RestAdapter.Builder setConverter(Converter converter) {
        return builder.setConverter(converter);
    }

    public RestAdapter.Builder setProfiler(Profiler profiler) {
        return builder.setProfiler(profiler);
    }

    public RestAdapter.Builder setErrorHandler(ErrorHandler errorHandler) {
        return builder.setErrorHandler(errorHandler);
    }

    public RestAdapter.Builder setLog(RestAdapter.Log log) {
        return builder.setLog(log);
    }

    public RestAdapter.Builder setLogLevel(RestAdapter.LogLevel logLevel) {
        return builder.setLogLevel(logLevel);
    }

    public RestAdapter build() {
        return builder.build();
    }
}
