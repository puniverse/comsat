package co.paralleluniverse.fibers.ws.rs.client;

import java.net.URI;
import java.util.Map;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.UriBuilder;

public class Client implements javax.ws.rs.client.Client {
    private final javax.ws.rs.client.Client client;

    public Client(javax.ws.rs.client.Client client) {
        this.client = client;
    }

    // Wrap webTarget
    @Override
    public WebTarget target(String uri) {
        return new WebTarget(client.target(uri));
    }

    @Override
    public WebTarget target(URI uri) {
        return new WebTarget(client.target(uri));
    }

    @Override
    public WebTarget target(UriBuilder uriBuilder) {
        return new WebTarget(client.target(uriBuilder));
    }

    @Override
    public WebTarget target(Link link) {
        return new WebTarget(client.target(link));
    }

    // Wrap builder
    @Override
    public Builder invocation(Link link) {
        return new Builder(client.invocation(link));
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
