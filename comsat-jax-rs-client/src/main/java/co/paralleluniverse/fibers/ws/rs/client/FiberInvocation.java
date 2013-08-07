package co.paralleluniverse.fibers.ws.rs.client;

import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.Suspendable;
import java.util.concurrent.Future;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.InvocationCallback;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;

class FiberInvocation implements Invocation {
    private final Invocation invocation;

    public FiberInvocation(Invocation invocation) {
        this.invocation = invocation;
    }

    @Override
    public FiberInvocation property(String name, Object value) {
        invocation.property(name, value);
        return this;
    }

    @Override
    @Suspendable
    public Response invoke() {
        try {
            return new AsyncRs<Response>() {
                @Override
                protected Void requestAsync(Fiber current, InvocationCallback<Response> callback) {
                    invocation.submit(callback);
                    return null;
                }
            }.run();
        } catch (SuspendExecution ex) {
            throw new AssertionError(ex);
        }
    }

    @Override
    @Suspendable
    public <T> T invoke(Class<T> responseType) {
        try {
            return new AsyncRs<T>() {
                @Override
                protected Void requestAsync(Fiber current, InvocationCallback<T> callback) {
                    invocation.submit(callback);
                    return null;
                }
            }.run();
        } catch (SuspendExecution ex) {
            throw new AssertionError(ex);
        }
    }

    @Override
    @Suspendable
    public <T> T invoke(GenericType<T> responseType) {
        try {
            return new AsyncRs<T>() {
                @Override
                protected Void requestAsync(Fiber current, InvocationCallback<T> callback) {
                    invocation.submit(callback);
                    return null;
                }
            }.run();
        } catch (SuspendExecution ex) {
            throw new AssertionError(ex);
        }
    }

    @Override
    public Future<Response> submit() {
        return invocation.submit();
    }

    @Override
    public <T> Future<T> submit(Class<T> responseType) {
        return invocation.submit(responseType);
    }

    @Override
    public <T> Future<T> submit(GenericType<T> responseType) {
        return invocation.submit(responseType);
    }

    @Override
    public <T> Future<T> submit(InvocationCallback<T> callback) {
        return invocation.submit(callback);
    }

    @Override
    public int hashCode() {
        return invocation.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return invocation.equals(obj);
    }

    @Override
    public String toString() {
        return invocation.toString();
    }
    
}
