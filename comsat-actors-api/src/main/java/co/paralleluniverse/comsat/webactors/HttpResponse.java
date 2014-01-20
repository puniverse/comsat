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

/**
 * An HTTP response message sent as a {@link HttpRequest#sender() response} to an {@link HttpRequest}.
 * When this response is sent to an {@link HttpRequest}'s {@link HttpRequest#sender() sender}, the connection stream will be closed
 * if {@link HttpRequest#openChannel() openChannel} has not been called on the request,
 * and will be flushed but not closed if {@link HttpRequest#openChannel() openChannel} <i>has</i> been called on the request.
 */
public class HttpResponse extends HttpMessage {
    /**
     * Creates an {@link HttpResponse} with a text body and response code {@code 200}.
     *
     * @param request the {@link HttpRequest} this is a response to.
     * @param body    the response body
     * @return A response {@link Builder} that can be used to add headers and other metadata to the response.
     */
    public static Builder ok(HttpRequest request, String body) {
        return new Builder(request, body);
    }

    /**
     * Creates an {@link HttpResponse} with a binary body and response code {@code 200}.
     *
     * @param request the {@link HttpRequest} this is a response to.
     * @param body    the response body
     * @return A response {@link Builder} that can be used to add headers and other metadata to the response.
     */
    public static Builder ok(HttpRequest request, ByteBuffer body) {
        return new Builder(request, body);
    }

    /**
     * Creates an {@link HttpResponse} indicating an error, with a given status code and an attached exception that may be reported
     * back to the client.
     *
     * @param request the {@link HttpRequest} this is a response to.
     * @param status  the response status code
     * @param cause   the exception that caused the error
     * @return A response {@link Builder} that can be used to add headers and other metadata to the response.
     */
    public static Builder error(HttpRequest request, int status, Throwable cause) {
        return new Builder(request).status(status).error(cause);
    }

    /**
     * Creates an {@link HttpResponse} indicating an error, with a given status code and a text body.
     *
     * @param request the {@link HttpRequest} this is a response to.
     * @param status  the response status code
     * @param body    the response body
     * @return A response {@link Builder} that can be used to add headers and other metadata to the response.
     */
    public static Builder error(HttpRequest request, int status, String body) {
        return new Builder(request, body).status(status);
    }

    /**
     * Sends a temporary redirect response to the client using the
     * specified redirect location URL and clears the buffer.
     * The status code is set to {@code SC_FOUND} 302 (Found).
     * This method can accept relative URLs;
     * the container must convert the relative URL to an absolute URL before sending the response to the client.
     * If the location is relative without a leading '/' the container interprets it as relative to
     * the current request URI.
     * If the location is relative with a leading '/' the container interprets it as relative to the container root.
     * If the location is relative with two leading '/' the container interprets
     * it as a network-path reference
     * (see <a href="http://www.ietf.org/rfc/rfc3986.txt"> RFC 3986: Uniform Resource Identifier (URI): Generic Syntax</a>, section 4.2 &quot;Relative Reference&quot;).
     *
     * @param redirectPath the redirect location URL
     */
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
        private boolean startActor;

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

        /**
         * Sets the content type of the response being sent to the client.
         * <p/>
         * The given content type may include a character encoding
         * specification, for example, {@code text/html;charset=UTF-8}.
         * <p/>
         * The {@code Content-Type} header is used to communicate the content type and the character
         * encoding used in the response writer to the client
         *
         * @param contentType the MIME type of the content
         *
         */
        public Builder setContentType(String contentType) {
            this.contentType = contentType;
            return this;
        }

        /**
         * Sets the character encoding (MIME charset) of the response being sent to the client,
         * for example, {@code UTF-8}.
         * If the character encoding has already been set by {@link #setContentType}, this method overrides it.
         * Calling {@link #setContentType} with {@code "text/html"} and calling this method with {@code Charset.forName("UTF-8")}
         * is equivalent with calling {@code setContentType} with {@code "text/html; charset=UTF-8"}.
         * <p/>
         * Note that the character encoding cannot be communicated via HTTP headers if
         * content type is not specified; however, it is still used to encode text
         * written in this response's body.
         *
         * @param charset only the character sets defined by IANA Character Sets
         *                (http://www.iana.org/assignments/character-sets)
         *
         * @see #setContentType
         */
        public Builder setCharacterEncoding(Charset charset) {
            this.charset = charset;
            return this;
        }

        /**
         * Adds a response header with the given name and value.
         * This method allows response headers to have multiple values.
         *
         * @param name  the name of the header
         * @param value the additional header value.
         *              If it contains octet string, it should be encoded according to RFC 2047 (http://www.ietf.org/rfc/rfc2047.txt)
         */
        public Builder addHeader(final String name, final String value) {
            if (headers == null)
                headers = LinkedHashMultimap.create();
            headers.put(name, value);
            return this;
        }

        /**
         * Adds the specified cookie to the response.
         * This method can be called multiple times to set multiple cookies.
         *
         * @param cookie the {@link Cookie} to return to the client
         * @return {@code this}
         */
        public Builder addCookie(Cookie cookie) {
            if (cookies == null)
                cookies = new ArrayList<>();
            cookies.add(cookie);
            return this;
        }

        /**
         * Sets the status code for this response.
         *
         * <p>This method is used to set the return status code
         * Valid status codes are those in the 2XX, 3XX, 4XX, and 5XX ranges.
         * Other status codes are treated as container specific.
         * <p>codes in the 4XX and 5XX range will be treated as error codes, and
         * will trigger the container's error reporting.
         *
         * @param sc the status code
         * @return {@code this}
         * @see #error
         */
        public Builder status(int sc) {
            this.status = sc;
            return this;
        }

        /**
         * Associates an exception with an error status. The exception may be used in the error report
         * which might be sent to the client.
         *
         * @param error the exception responsible for the error
         * @return {@code this}
         */
        public Builder error(Throwable error) {
            this.error = error;
            return this;
        }

        /**
         * Indicates that the connection to the client must not be closed after sending this response;
         * rather an {@link HttpStreamOpened} message will be sent to the actor sending this response.
         *
         * @return {@code this}
         */
        public Builder startActor() {
            this.startActor = true;
            return this;
        }

        Builder redirect(String redirectPath) {
            this.redirectPath = redirectPath;
            this.status = 302;
            return this;
        }

        /**
         * Instantiates a new immutable {@link HttpResponse} based on the values set in this builder.
         *
         * @return a new {@link HttpResponse}
         */
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
    private final boolean startActor;

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
        this.startActor = httpResponse.startActor;
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
        this.startActor = builder.startActor;
    }

    @Override
    public ActorRef<WebMessage> getFrom() {
        return sender;
    }

    @Override
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

    /**
     * The {@link HttpRequest} this is a response to.
     */
    public HttpRequest getRequest() {
        return request;
    }

    /**
     * The response's HTTP status code.
     */
    public int getStatus() {
        return status;
    }

    /**
     * An exception optionally associated with an error status code.
     */
    public Throwable getError() {
        return error;
    }

    /**
     * The redirect URL target if this is a {@link #redirect(HttpRequest, String) redirect} response.
     */
    public String getRedirectPath() {
        return redirectPath;
    }

    public boolean shouldStartActor() {
        return startActor;
    }

    @Override
    protected String contentString() {
        StringBuilder sb = new StringBuilder();
        sb.append(" ").append(getStatus());
        sb.append(" headers: ").append(getHeaders());
        sb.append(" cookies: ").append(getCookies());
        sb.append(" contentLength: ").append(getContentLength());
        sb.append(" charEncoding: ").append(getCharacterEncoding());
        if (strBody != null)
            sb.append(" body: ").append(strBody);
        if(redirectPath != null)
            sb.append(" redirectPath: ").append(getRedirectPath());
        if(error != null)
            sb.append(" error: ").append(getError());
        sb.append(" shouldStartActor: ").append(shouldStartActor());
        return super.contentString() + sb;
    }
}
