/*
 * COMSAT
 * Copyright (C) 2013, Parallel Universe Software Co. All rights reserved.
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
package co.paralleluniverse.fibers.ws.rs.client;

import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.Suspendable;
import co.paralleluniverse.strands.SettableFuture;
import java.util.concurrent.Future;
import javax.security.auth.callback.Callback;
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
                protected Void requestAsync() {
                    invocation.submit(this);
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
                protected Void requestAsync() {
                    invocation.submit(this);
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
                protected Void requestAsync() {
                    invocation.submit(this);
                    return null;
                }
            }.run();
        } catch (SuspendExecution ex) {
            throw new AssertionError(ex);
        }
    }

    @Override
    public Future<Response> submit() {
        final SettableFuture<Response> responseFuture = new SettableFuture<>();
        invocation.submit(new InvocationCallback<Response>() {

            @Override
            public void completed(Response response) {
                responseFuture.set(response);
            }

            @Override
            public void failed(Throwable throwable) {
                responseFuture.setException(throwable);
            }
        });                
        return responseFuture;
    }

    @Override
    public <T> Future<T> submit(Class<T> responseType) {
        final SettableFuture<T> responseFuture = new SettableFuture<>();
        invocation.submit(new InvocationCallback<T>() {

            @Override
            public void completed(T response) {
                responseFuture.set(response);
            }

            @Override
            public void failed(Throwable throwable) {
                responseFuture.setException(throwable);
            }
        });                
        return responseFuture;
    }

    @Override
    public <T> Future<T> submit(GenericType<T> responseType) {
        final SettableFuture<T> responseFuture = new SettableFuture<>();
        invocation.submit(new InvocationCallback<T>() {

            @Override
            public void completed(T response) {
                responseFuture.set(response);
            }

            @Override
            public void failed(Throwable throwable) {
                responseFuture.setException(throwable);
            }
        });                
        return responseFuture;
    }

    @Override
    public <T> Future<T> submit(final InvocationCallback<T> callback) {
        final SettableFuture<T> responseFuture = new SettableFuture<>();
        invocation.submit(new InvocationCallback<T>() {

            @Override
            public void completed(T response) {
                responseFuture.set(response);
                callback.completed(response);
            }

            @Override
            public void failed(Throwable throwable) {
                responseFuture.setException(throwable);
                callback.failed(throwable);
            }
        });                
        return responseFuture;
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
