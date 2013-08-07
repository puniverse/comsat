package co.paralleluniverse.fibers.ws.rs.client;

import java.net.URI;
import java.util.Map;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;

class FiberWebTarget implements WebTarget {
    private final WebTarget webTarget;

    public FiberWebTarget(WebTarget webTarget) {
        this.webTarget = webTarget;
    }

    // Return wrapped builder
    @Override
    public Builder request() {
        return new FiberBuilder(webTarget.request());
    }

    @Override
    public Builder request(String... acceptedResponseTypes) {
        return new FiberBuilder(webTarget.request(acceptedResponseTypes));
    }

    @Override
    public Builder request(MediaType... acceptedResponseTypes) {
        return new FiberBuilder(webTarget.request(acceptedResponseTypes));
    }

    // Return this instead of webTarget
    @Override
    public WebTarget path(String path) {
        webTarget.path(path);
        return this;
    }

    @Override
    public WebTarget resolveTemplate(String name, Object value) {
        webTarget.resolveTemplate(name, value);
        return this;
    }

    @Override
    public WebTarget resolveTemplate(String name, Object value, boolean encodeSlashInPath) {
        webTarget.resolveTemplate(name, value, encodeSlashInPath);
        return this;
    }

    @Override
    public WebTarget resolveTemplateFromEncoded(String name, Object value) {
        webTarget.resolveTemplateFromEncoded(name, value);
        return this;
    }

    @Override
    public WebTarget resolveTemplates(Map<String, Object> templateValues) {
        webTarget.resolveTemplates(templateValues);
        return this;
    }

    @Override
    public WebTarget resolveTemplates(Map<String, Object> templateValues, boolean encodeSlashInPath) {
        webTarget.resolveTemplates(templateValues, encodeSlashInPath);
        return this;
    }

    @Override
    public WebTarget resolveTemplatesFromEncoded(Map<String, Object> templateValues) {
        webTarget.resolveTemplatesFromEncoded(templateValues);
        return this;
    }

    @Override
    public WebTarget matrixParam(String name, Object... values) {
        webTarget.matrixParam(name, values);
        return this;
    }

    @Override
    public WebTarget queryParam(String name, Object... values) {
        webTarget.queryParam(name, values);
        return this;
    }

    @Override
    public WebTarget property(String name, Object value) {
        webTarget.property(name, value);
        return this;
    }

    @Override
    public WebTarget register(Class<?> componentClass) {
        webTarget.register(componentClass);
        return this;
    }

    @Override
    public WebTarget register(Class<?> componentClass, int priority) {
        webTarget.register(componentClass, priority);
        return this;
    }

    @Override
    public WebTarget register(Class<?> componentClass, Class<?>... contracts) {
        webTarget.register(componentClass, contracts);
        return this;
    }

    @Override
    public WebTarget register(Class<?> componentClass, Map<Class<?>, Integer> contracts) {
        webTarget.register(componentClass, contracts);
        return this;
    }

    @Override
    public WebTarget register(Object component) {
        webTarget.register(component);
        return this;
    }

    @Override
    public WebTarget register(Object component, int priority) {
        webTarget.register(component, priority);
        return this;
    }

    @Override
    public WebTarget register(Object component, Class<?>... contracts) {
        webTarget.register(component, contracts);
        return this;
    }

    @Override
    public WebTarget register(Object component, Map<Class<?>, Integer> contracts) {
        webTarget.register(component, contracts);
        return this;
    }

    // Delegations
    @Override
    public URI getUri() {
        return webTarget.getUri();
    }

    @Override
    public UriBuilder getUriBuilder() {
        return webTarget.getUriBuilder();
    }

    @Override
    public Configuration getConfiguration() {
        return webTarget.getConfiguration();
    }

    @Override
    public int hashCode() {
        return webTarget.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return webTarget.equals(obj);
    }

    @Override
    public String toString() {
        return webTarget.toString();
    }
}
