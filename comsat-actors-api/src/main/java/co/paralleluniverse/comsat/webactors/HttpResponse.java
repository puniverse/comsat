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
package co.paralleluniverse.comsat.webactors;

import co.paralleluniverse.actors.ActorRef;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class HttpResponse implements HttpMessage {
    public static Builder ok(HttpRequest request, String body) {
        return new Builder(request, body);
    }

    public static Builder ok(HttpRequest request, ByteBuffer body) {
        return new Builder(request, body);
    }

    public static Builder error(HttpRequest request, int status, Throwable cause) {
        return new Builder(request).status(status).error(cause);
    }

    public static Builder error(HttpRequest request, int status, String body) {
        return new Builder(request, body).status(status);
    }

    public static Builder redirect(HttpRequest request, String redirectPath) {
        return new Builder(request).redirect(redirectPath);
    }

    public static class Builder {
        private final ActorRef<WebMessage> sender;
        private final HttpRequest request;
        private final String strBody;
        private final ByteBuffer binBody;
        private String contentType;
        private Charset charset;
        private List<Cookie> cookies;
        private Multimap<String, String> headers;
        private int status;
        private Throwable error;
        private String redirectPath;

        public Builder(ActorRef<? super WebMessage> from, HttpRequest request, String body) {
            this.sender = (ActorRef<WebMessage>) from;
            this.request = request;
            this.strBody = body;
            this.binBody = null;
            this.status = 200;
        }

        public Builder(ActorRef<? super WebMessage> from, HttpRequest request, ByteBuffer body) {
            this.sender = (ActorRef<WebMessage>) from;
            this.request = request;
            this.binBody = body;
            this.strBody = null;
            this.status = 200;
        }

        public Builder(ActorRef<? super WebMessage> from, HttpRequest request) {
            this(from, request, (String) null);
        }

        Builder(HttpRequest request, String body) {
            this(null, request, body);
        }

        Builder(HttpRequest request, ByteBuffer body) {
            this(null, request, body);
        }

        Builder(HttpRequest request) {
            this(request, (String) null);
        }

        public Builder setContentType(String contentType) {
            this.contentType = contentType;
            return this;
        }

        public Builder setCharacterEncoding(Charset charset) {
            this.charset = charset;
            return this;
        }

        public Builder addHeader(final String key, final String val) {
            if (headers == null)
                headers = LinkedHashMultimap.create();
            headers.put(key, val);
            return this;
        }

        public Builder redirect(String redirectPath) {
            this.redirectPath = redirectPath;
            return this;
        }

        public Builder addCookie(Cookie wc) {
            if (cookies == null)
                cookies = new ArrayList<>();
            cookies.add(wc);
            return this;
        }

        public Builder status(int status) {
            this.status = status;
            return this;
        }

        public Builder error(Throwable error) {
            this.error = error;
            return this;
        }

        public HttpResponse build() {
            return new HttpResponse(sender, this);
        }
    }
    //
    private final ActorRef<WebMessage> sender;
    private final HttpRequest request;
    private final String contentType;
    private final Charset charset;
    private final String strBody;
    private final ByteBuffer binBody;
    private final List<Cookie> cookies;
    private final Multimap<String, String> headers;
    private final int status;
    private final Throwable error;
    private final String redirectPath;

    /**
     * Use when forwarding
     *
     * @param from
     * @param httpResponse
     */
    public HttpResponse(ActorRef<? super WebMessage> from, HttpResponse httpResponse) {
        this.sender = (ActorRef<WebMessage>) from;
        this.request = httpResponse.request;
        this.contentType = httpResponse.contentType;
        this.charset = httpResponse.charset;
        this.strBody = httpResponse.strBody;
        this.binBody = httpResponse.binBody != null ? httpResponse.binBody.asReadOnlyBuffer() : null;
        this.cookies = httpResponse.cookies;
        this.error = httpResponse.error;
        this.headers = httpResponse.headers;
        this.status = httpResponse.status;
        this.redirectPath = httpResponse.redirectPath;
    }

    public HttpResponse(ActorRef<? super WebMessage> from, Builder builder) {
        this.sender = (ActorRef<WebMessage>) from;
        this.request = builder.request;
        this.contentType = builder.contentType;
        this.charset = builder.charset;
        this.strBody = builder.strBody;
        this.binBody = builder.binBody != null ? builder.binBody.asReadOnlyBuffer() : null;
        this.cookies = builder.cookies != null ? ImmutableList.copyOf(builder.cookies) : null;
        this.error = builder.error;
        this.headers = builder.headers != null ? ImmutableMultimap.copyOf(builder.headers) : null;
        this.status = builder.status;
        this.redirectPath = builder.redirectPath;
    }

    @Override
    public ActorRef<WebMessage> sender() {
        return sender;
    }

    public HttpRequest getRequest() {
        return request;
    }

    public String getContentType() {
        return contentType;
    }

    @Override
    public Charset getCharacterEncoding() {
        return charset;
    }

    @Override
    public int getContentLength() {
        if (binBody != null)
            return binBody.remaining();
        else
            return -1;
    }

    public String getRedirectPath() {
        return redirectPath;
    }

    @Override
    public String getStringBody() {
        return strBody;
    }

    @Override
    public ByteBuffer getByteBufferBody() {
        return binBody != null ? binBody.duplicate() : null;
    }

    @Override
    public Collection<Cookie> getCookies() {
        return cookies;
    }

    @Override
    public Multimap<String, String> getHeaders() {
        return headers;
    }

    public int getStatus() {
        return status;
    }

    public Throwable getError() {
        return error;
    }
}
