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
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

/**
 * An HTTP request message.
 */
public abstract class HttpRequest extends HttpMessage {
    /**
     * A multimap of the parameters contained in this message and (all) their values.
     * If the request has no parameters, returns an empty multimap.
     */
    public abstract Multimap<String, String> getParameters();

    /**
     * Returns the names values of all attributed associated with this request.
     *
     * <p>
     * The container may set
     * attributes to make available custom information about a request.
     * For example, for requests made using HTTPS, the attribute
     * <code>javax.servlet.request.X509Certificate</code> can be used to
     * retrieve information on the certificate of the client.
     *
     * @return an {@code Object} containing the value of the attribute,
     *         or {@code null} if the attribute does not exist
     */
    public abstract Map<String, Object> getAttributes();

    /**
     * Returns all values associated with the given parameter
     *
     * @param name the parameter name
     * @return all values associated with the given parameter
     */
    public Collection<String> getParametersValues(String name) {
        return getParameters().get(name);
    }

    /**
     * Returns the value of the given parameter.
     * If the parameter is not found in the message, this method returns {@code null}.
     * If the parameter has more than one value, this method returns the first value.
     *
     * @param name the parameter name
     * @return the (first) value of the given parameter name; {@code null} if the parameter is not found
     */
    public String getParameter(String name) {
        return first(getParameters().get(name));
    }

    /**
     * Returns the value of the given attribute.
     * If the attribute is not found in the message, this method returns {@code null}.
     *
     * @param name the attribute name
     * @return the value of the given attribute; {@code null} if the attribute is not found
     */
    public Object getAttribute(String name) {
        return getAttributes().get(name);
    }

    /**
     * Returns the name of the scheme used to make this request, for example, {@code http}, {@code https}, or {@code ftp}.
     * Different schemes have different rules for constructing URLs, as noted in RFC 1738.
     */
    public abstract String getScheme();

    /**
     * The name of the HTTP method with which this request was made;
     * for example, GET, POST, or PUT.
     *
     * @return the name of the method with which this request was made
     */
    public abstract String getMethod();

    /**
     * Returns the value of the specified request header as a {@code long} value that represents a {@code Date} object.
     * Use this method with headers that contain dates, such as {@code If-Modified-Since}.
     *
     * <p>
     * The date is returned as the number of milliseconds since January 1, 1970 GMT.
     *
     * <p>
     * If the request does not have a header of the specified name, this method returns -1.
     * If the header can't be converted to a date, the method throws an {@code IllegalArgumentException}.
     *
     * @param name the name of the header
     *
     * @return	a {@code long} value representing the date specified in the header expressed as
     *         the number of milliseconds since January 1, 1970 GMT,
     *         or {@code -1} if the named header was not included with the request
     *
     * @exception IllegalArgumentException If the header value can't be converted to a date
     */
    public long getDateHeader(String name) {
        String value = getHeader(name);
        if (value == null)
            return (-1L);

        long result = parseDate(value);
        if (result != (-1L))
            return result;

        throw new IllegalArgumentException(value);
    }

    /**
     * Returns any extra path information associated with the URL the client sent when it made this request.
     * The extra path information follows the servlet path but precedes the query string and will start with
     * a "/" character.
     *
     * <p>
     * This method returns {@code null} if there was no extra path information.
     *
     * @return a {@code String} decoded by the web container, specifying
     *         extra path information that comes after the servlet path but before the query string in the request URL;
     *         or {@code null} if the URL does not have any extra path information
     */
    public abstract String getPathInfo();

    /**
     * The portion of the request URI that indicates the context of the request.
     * The context path always comes first in a request URI.
     * The path starts with a "/" character but does not end with a "/" character.
     * For servlets in the default (root) context, this method returns "".
     * The container does not decode this string.
     *
     * <p>
     * It is possible that a container may match a context by
     * more than one context path. In such cases this method will return the
     * actual context path used by the request.
     *
     * @return the portion of the request URI that indicates the context of the request
     */
    public abstract String getContextPath();

    /**
     * The query string that is contained in the request URL after the path,
     * or {@code null} if the URL does not have a query string.
     */
    public abstract String getQueryString();

    /**
     * Returns the part of this request's URL from the protocol name up to the query string in the first line of the HTTP request.
     * The web container does not decode this string.
     * For example:
     *
     * <table summary="Examples of Returned Values">
     * <tr align=left>
     * <th>First line of HTTP request </th><th> Returned Value</th>
     * <tr><td>POST /some/path.html HTTP/1.1<td><td>/some/path.html
     * <tr><td>GET http://foo.bar/a.html HTTP/1.0<td><td>/a.html
     * <tr><td>HEAD /xyz?a=b HTTP/1.1<td><td>/xyz
     * </table>
     *
     * @return the part of the URL from the protocol name up to the query string
     */
    public abstract String getRequestURI();

    /**
     * Reconstructs the URL the client used to make the request.
     * The returned URL contains a protocol, server name, port
     * number, and server path, but it does not include query
     * string parameters.
     *
     * <p>
     * This method is useful for creating redirect messages
     * and for reporting errors.
     *
     * @return the reconstructed URL
     */
    public String getRequestURL() {
        StringBuilder url = new StringBuilder();
        String scheme = getScheme();
        int port = getServerPort();
        if (port < 0)
            port = 80; // Work around java.net.URL bug

        url.append(scheme);
        url.append("://");
        url.append(getServerName());
        if ((scheme.equals("http") && (port != 80))
                || (scheme.equals("https") && (port != 443))) {
            url.append(':');
            url.append(port);
        }
        url.append(getRequestURI());

        return url.toString();
    }

    /**
     * The host name of the server to which the request was sent.
     * It is the value of the part before ":" in the {@code Host} header value, if any,
     * or the resolved server name, or the server IP address.
     */
    public abstract String getServerName();

    /**
     * The port number to which the request was sent.
     * It is the value of the part after ":" in the {@code Host} header value, if any,
     * or the server port where the client connection was accepted on.
     *
     * @return the port number
     */
    public abstract int getServerPort();

    /**
     * Returns an actor representing the client to which an {@link HttpResponse} should be sent as a response to this request.
     * All {@code HttpRequest}s from the same session will have the same sender. It will appear to have died (i.e. send an
     * {@link co.paralleluniverse.actors.ExitMessage ExitMessage} if {@link co.paralleluniverse.actors.Actor#watch(co.paralleluniverse.actors.ActorRef) watched})
     * when the session is terminated.
     *
     * @return an actor representing the client
     */
    @Override
    public abstract ActorRef<WebMessage> getFrom();

    @Override
    protected String contentString() {
        StringBuilder sb = new StringBuilder();
        sb.append(" ").append(getMethod());
        sb.append(" uri: ").append(getRequestURI());
        sb.append(" query: ").append(getQueryString());
        sb.append(" params: ").append(getParameters());
        sb.append(" headers: ").append(getHeaders());
        sb.append(" cookies: ").append(getCookies());
        sb.append(" contentLength: ").append(getContentLength());
        sb.append(" charEncoding: ").append(getCharacterEncoding());
        sb.append(" body: ").append(getStringBody());
        return super.contentString() + sb;
    }

    public static class Builder {
        private final ActorRef<WebMessage> sender;
        private final String strBody;
        private final ByteBuffer binBody;
        private String contentType;
        private Charset charset;
        private List<Cookie> cookies;
        private ListMultimap<String, String> headers;
        private String method;
        private String scheme;
        private String server;
        private int port;
        private String path;
        private Multimap<String, String> params;

        public Builder(ActorRef<? super WebMessage> from, String body) {
            this.sender = (ActorRef<WebMessage>) from;
            this.strBody = body;
            this.binBody = null;
        }

        public Builder(ActorRef<? super WebMessage> from, ByteBuffer body) {
            this.sender = (ActorRef<WebMessage>) from;
            this.binBody = body;
            this.strBody = null;
        }

        public Builder(ActorRef<? super WebMessage> from) {
            this(from, (String) null);
        }

        /**
         * Sets the content type of the request being sent to the client.
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
         * Adds a request header with the given name and value.
         * This method allows response headers to have multiple values.
         *
         * @param name  the name of the header
         * @param value the additional header value.
         *              If it contains octet string, it should be encoded according to RFC 2047 (http://www.ietf.org/rfc/rfc2047.txt)
         */
        public Builder addHeader(final String name, final String value) {
            if (headers == null)
                headers = LinkedListMultimap.create();
            headers.put(name, value);
            return this;
        }

        /**
         * Adds the specified cookie to the request.
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

        public Builder setMethod(String method) {
            this.method = method;
            return this;
        }

        public Builder setScheme(String scheme) {
            this.scheme = scheme;
            return this;
        }

        public Builder setServer(String server) {
            this.server = server;
            return this;
        }

        public Builder setPort(int port) {
            this.port = port;
            return this;
        }

        public Builder setPath(String path) {
            this.path = path;
            return this;
        }

        /**
         * Adds a request parameter with the given name and value.
         *
         * @param name  the name of the parameter
         * @param value the additional header value.
         *              If it contains octet string, it should be encoded according to RFC 2047 (http://www.ietf.org/rfc/rfc2047.txt)
         */
        public Builder addParam(final String name, final String value) {
            if (params == null)
                params = LinkedListMultimap.create();
            params.put(name, value);
            return this;
        }
    }

    private static class SimpleHttpRequest extends HttpRequest {
        private final ActorRef<WebMessage> sender;
        private final String contentType;
        private final Charset charset;
        private final String strBody;
        private final ByteBuffer binBody;
        private final Collection<Cookie> cookies;
        private final ListMultimap<String, String> headers;
        private final Multimap<String, String> params;
        private final String method;
        private final String scheme;
        private final String server;
        private final int port;
        private final String uri;

        /**
         * Use when forwarding
         *
         * @param from
         * @param httpRequest
         */
        public SimpleHttpRequest(ActorRef<? super WebMessage> from, HttpRequest httpRequest) {
            this.sender = (ActorRef<WebMessage>) from;
            this.contentType = httpRequest.getContentType();
            this.charset = httpRequest.getCharacterEncoding();
            this.strBody = httpRequest.getStringBody();
            this.binBody = httpRequest.getByteBufferBody() != null ? httpRequest.getByteBufferBody().asReadOnlyBuffer() : null;
            this.cookies = httpRequest.getCookies();
            this.headers = httpRequest.getHeaders();
            this.method = httpRequest.getMethod();
            this.scheme = httpRequest.getScheme();
            this.server = httpRequest.getServerName();
            this.port = httpRequest.getServerPort();
            this.params = httpRequest.getParameters();
            this.uri = httpRequest.getRequestURI();
        }

        public SimpleHttpRequest(ActorRef<? super WebMessage> from, HttpRequest.Builder builder) {
            this.sender = (ActorRef<WebMessage>) from;
            this.contentType = builder.contentType;
            this.charset = builder.charset;
            this.strBody = builder.strBody;
            this.binBody = builder.binBody != null ? builder.binBody.asReadOnlyBuffer() : null;
            this.cookies = builder.cookies != null ? ImmutableList.copyOf(builder.cookies) : null;
            this.headers = builder.headers != null ? ImmutableListMultimap.copyOf(builder.headers) : null;
            this.params = builder.params != null ? ImmutableListMultimap.copyOf(builder.params) : null;
            this.method = builder.method;
            this.scheme = builder.scheme;
            this.server = builder.server;
            this.port = builder.port;
            this.uri = builder.path;
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
        public ListMultimap<String, String> getHeaders() {
            return headers;
        }

        @Override
        public Map<String, Object> getAttributes() {
            return null;
        }

        @Override
        public String getScheme() {
            return scheme;
        }

        @Override
        public String getMethod() {
            return method;
        }

        @Override
        public String getServerName() {
            return server;
        }

        @Override
        public int getServerPort() {
            return port;
        }

        @Override
        public Multimap<String, String> getParameters() {
            return params;
        }

        @Override
        public String getRequestURI() {
            return uri;
        }

        @Override
        public String getQueryString() {
            if(params == null)
                return null;
            StringBuilder sb = new StringBuilder();
            for(Map.Entry<String, String> entry : params.entries())
                sb.append(entry.getKey()).append('=').append(entry.getValue()).append('&');
            sb.delete(sb.length()-1, sb.length());
            return sb.toString();
        }

        @Override
        public String getPathInfo() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getContextPath() {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * The only date format permitted when generating HTTP headers.
     */
    public static final String RFC1123_DATE = "EEE, dd MMM yyyy HH:mm:ss zzz";
    private static final SimpleDateFormat format = new SimpleDateFormat(RFC1123_DATE, Locale.US);

    /**
     * The set of SimpleDateFormat formats to use in getDateHeader().
     */
    private static final SimpleDateFormat formats[] = {
        new SimpleDateFormat(RFC1123_DATE, Locale.US),
        new SimpleDateFormat("EEEEEE, dd-MMM-yy HH:mm:ss zzz", Locale.US),
        new SimpleDateFormat("EEE MMMM d HH:mm:ss yyyy", Locale.US)
    };

    private static final TimeZone gmtZone = TimeZone.getTimeZone("GMT");

    /**
     * All HTTP dates are on GMT
     */
    static {
        format.setTimeZone(gmtZone);
        formats[0].setTimeZone(gmtZone);
        formats[1].setTimeZone(gmtZone);
        formats[2].setTimeZone(gmtZone);
    }

    private static long parseDate(String value) {
        Date date = null;
        for (int i = 0; (date == null) && (i < formats.length); i++) {
            try {
                date = formats[i].parse(value);
            } catch (ParseException e) {
                // Ignore
            }
        }
        if (date == null)
            return -1L;

        return date.getTime();
    }
}
