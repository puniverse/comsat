package co.paralleluniverse.fibers.ws.rs.client;

import java.util.concurrent.Future;
import javax.ws.rs.client.InvocationCallback;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;

public class Invocation implements javax.ws.rs.client.Invocation {
    private final javax.ws.rs.client.Invocation invocation;

    public Invocation(javax.ws.rs.client.Invocation invocation) {
        this.invocation = invocation;
    }

    public javax.ws.rs.client.Invocation property(String name, Object value) {
        return invocation.property(name, value);
    }

    public Response invoke() {
        return invocation.invoke();
    }

    public <T> T invoke(Class<T> responseType) {
        return invocation.invoke(responseType);
    }

    public <T> T invoke(GenericType<T> responseType) {
        return invocation.invoke(responseType);
    }

    public Future<Response> submit() {
        return invocation.submit();
    }

    public <T> Future<T> submit(Class<T> responseType) {
        return invocation.submit(responseType);
    }

    public <T> Future<T> submit(GenericType<T> responseType) {
        return invocation.submit(responseType);
    }

    public <T> Future<T> submit(InvocationCallback<T> callback) {
        return invocation.submit(callback);
    }

    public int hashCode() {
        return invocation.hashCode();
    }

    public boolean equals(Object obj) {
        return invocation.equals(obj);
    }

    public String toString() {
        return invocation.toString();
    }
    
}
