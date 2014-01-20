/*
 * COMSAT
 * Copyright (c) 2013-2014, Parallel Universe Software Co. All rights reserved.
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

import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.Suspendable;
import java.util.Locale;
import javax.ws.rs.client.AsyncInvoker;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

class FiberBuilder implements Invocation.Builder {
    private static final String GET = "GET";
    private static final String PUT = "PUT";
    private static final String POST = "POST";
    private static final String DELETE = "DELETE";
    private static final String HEAD = "HEAD";
    private static final String OPTIONS = "OPTIONS";
    private static final String TRACE = "TRACE";
    final javax.ws.rs.client.Invocation.Builder builder;

    public FiberBuilder(Builder builder) {
        this.builder = builder;
    }

    // Wrap FiberInvocation
    @Override
    public Invocation build(String method) {
        return new FiberInvocation(builder.build(method));
    }

    @Override
    public Invocation build(String method, Entity<?> entity) {
        return new FiberInvocation(builder.build(method, entity));
    }

    @Override
    public Invocation buildGet() {
        return new FiberInvocation(builder.buildGet());
    }

    @Override
    public Invocation buildDelete() {
        return new FiberInvocation(builder.buildDelete());
    }

    @Override
    public Invocation buildPost(Entity<?> entity) {
        return new FiberInvocation(builder.buildPost(entity));
    }

    @Override
    public Invocation buildPut(Entity<?> entity) {
        return new FiberInvocation(builder.buildPut(entity));
    }

    // Return this instead of builder
    @Override
    public Builder accept(String... mediaTypes) {
        builder.accept(mediaTypes);
        return this;
    }

    @Override
    public Builder accept(MediaType... mediaTypes) {
        builder.accept(mediaTypes);
        return this;
    }

    @Override
    public Builder acceptLanguage(Locale... locales) {
        builder.acceptLanguage(locales);
        return this;
    }

    @Override
    public Builder acceptLanguage(String... locales) {
        builder.acceptLanguage(locales);
        return this;
    }

    @Override
    public Builder acceptEncoding(String... encodings) {
        builder.acceptEncoding(encodings);
        return this;
    }

    @Override
    public Builder cookie(Cookie cookie) {
        builder.cookie(cookie);
        return this;
    }

    @Override
    public Builder cookie(String name, String value) {
        builder.cookie(name, value);
        return this;
    }

    @Override
    public Builder cacheControl(CacheControl cacheControl) {
        builder.cacheControl(cacheControl);
        return this;
    }

    @Override
    public Builder header(String name, Object value) {
        builder.header(name, value);
        return this;
    }

    @Override
    public Builder headers(MultivaluedMap<String, Object> headers) {
        builder.headers(headers);
        return this;
    }

    @Override
    public Builder property(String name, Object value) {
        builder.property(name, value);
        return this;
    }

    // Suspendable Functions
    @Override
    @Suspendable
    public Response get() {
        return method(GET);
    }

    @Override
    @Suspendable
    public <T> T get(Class<T> responseType) {
        return method(GET,responseType);
    }

    @Override
    @Suspendable
    public <T> T get(GenericType<T> responseType) {
        return method(GET,responseType);
    }

    @Override
    @Suspendable
    public Response put(final Entity<?> entity) {
        return method(PUT,entity);
    }

    @Override
    @Suspendable
    public <T> T put(final Entity<?> entity, Class<T> responseType) {
        return method(PUT,entity,responseType);
    }

    @Override
    @Suspendable
    public <T> T put(final Entity<?> entity, GenericType<T> responseType) {
        return method(PUT,entity,responseType);
    }

    @Override
    @Suspendable
    public Response post(final Entity<?> entity) {
        return method(POST,entity);
    }

    @Override
    @Suspendable
    public <T> T post(final Entity<?> entity, Class<T> responseType) {
        return method(POST,entity,responseType);
    }

    @Override
    @Suspendable
    public <T> T post(final Entity<?> entity, GenericType<T> responseType) {
        return method(POST,entity,responseType);
    }

    @Override
    @Suspendable
    public Response delete() {
        return method(DELETE);
    }

    @Override
    @Suspendable
    public <T> T delete(Class<T> responseType) {
        return method(DELETE,responseType);
    }

    @Override
    @Suspendable
    public <T> T delete(GenericType<T> responseType) {
        return method(DELETE,responseType);
    }

    @Override
    @Suspendable
    public Response head() {
        return method(HEAD);
    }

    @Override
    @Suspendable
    public Response options() {
        return method(OPTIONS);
    }

    @Override
    @Suspendable
    public <T> T options(Class<T> responseType) {
        return method(OPTIONS,responseType);
    }

    @Override
    @Suspendable
    public <T> T options(GenericType<T> responseType) {
        return method(OPTIONS,responseType);
    }

    @Override
    @Suspendable
    public Response trace() {
        return method(TRACE);
    }

    @Override
    @Suspendable
    public <T> T trace(Class<T> responseType) {
        return method(TRACE,responseType);
    }

    @Override
    @Suspendable
    public <T> T trace(GenericType<T> responseType) {
        return method(TRACE,responseType);
    }

    @Override
    @Suspendable
    public Response method(final String name) {
        try {
            return new AsyncRs<Response>() {
                @Override
                protected Void requestAsync() {
                    builder.async().method(name, this);
                    return null;
                }
            }.run();
        } catch (SuspendExecution ex) {
            throw new AssertionError(ex);
        }
    }

    @Override
    @Suspendable
    public <T> T method(final String name, Class<T> responseType) {
        try {
            return new AsyncRs<T>() {
                @Override
                protected Void requestAsync() {
                    builder.async().method(name, this);
                    return null;
                }
            }.run();
        } catch (SuspendExecution ex) {
            throw new AssertionError(ex);
        }
    }

    @Override
    @Suspendable
    public <T> T method(String name, GenericType<T> responseType) {
        return this.method(name, (Class<T>)responseType.getRawType());
    }

    @Override
    @Suspendable
    public Response method(final String name, final Entity<?> entity) {
        try {
            return new AsyncRs<Response>() {
                @Override
                protected Void requestAsync() {
                    builder.async().method(name, entity, this);
                    return null;
                }
            }.run();
        } catch (SuspendExecution ex) {
            throw new AssertionError(ex);
        }
    }

    @Override
    @Suspendable
    public <T> T method(final String name, final Entity<?> entity, Class<T> responseType) {
        try {
            return new AsyncRs<T>() {
                @Override
                protected Void requestAsync() {
                    builder.async().method(name, entity, this);
                    return null;
                }
            }.run();
        } catch (SuspendExecution ex) {
            throw new AssertionError(ex);
        }
    }

    @Override
    @Suspendable
    public <T> T method(final String name, final Entity<?> entity, GenericType<T> responseType) {
        try {
            return new AsyncRs<T>() {
                @Override
                protected Void requestAsync() {
                    builder.async().method(name, entity, this);
                    return null;
                }
            }.run();
        } catch (SuspendExecution ex) {
            throw new AssertionError(ex);
        }
    }

    // Delegations
    @Override
    public AsyncInvoker async() {
        return builder.async();
    }

    @Override
    public int hashCode() {
        return builder.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return builder.equals(obj);
    }

    @Override
    public String toString() {
        return builder.toString();
    }
}
