package co.paralleluniverse.fibers.retrofit;

import co.paralleluniverse.fibers.httpclient.FiberHttpClientBuilder;
import org.apache.http.client.HttpClient;
import retrofit.Endpoint;
import retrofit.ErrorHandler;
import retrofit.Profiler;
import retrofit.RequestInterceptor;
import retrofit.RestAdapter;
import retrofit.Server;
import retrofit.client.ApacheClient;
import retrofit.converter.Converter;

public class FiberRestAdaptherBuilder {
    private final RestAdapter.Builder builder;

    public FiberRestAdaptherBuilder() {
        this(FiberHttpClientBuilder.create().setUserAgent("").build());
    }
    
    public FiberRestAdaptherBuilder(HttpClient client) {
        this.builder = new RestAdapter.Builder().setClient(new ApacheClient(client));
    }

    public RestAdapter.Builder setServer(String server) {
        return builder.setServer(server);
    }

    public RestAdapter.Builder setServer(Server server) {
        return builder.setServer(server);
    }

    public RestAdapter.Builder setEndpoint(String endpoint) {
        return builder.setEndpoint(endpoint);
    }

    public RestAdapter.Builder setEndpoint(Endpoint endpoint) {
        return builder.setEndpoint(endpoint);
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
