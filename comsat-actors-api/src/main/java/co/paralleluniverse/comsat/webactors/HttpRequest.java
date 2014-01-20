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
import com.google.common.collect.Multimap;
import java.util.Collection;
import java.util.Map;

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
     * <p>The container may set
     * attributes to make available custom information about a request.
     * For example, for requests made using HTTPS, the attribute
     * <code>javax.servlet.request.X509Certificate</code> can be used to
     * retrieve information on the certificate of the client.
     *
     * @return an {@code Object} containing the value of the attribute,
     *         or {@code null} if the attribute does not exist
     */
    public abstract Map<String, Object> getAtrributes();

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
        return getAtrributes().get(name);
    }

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
     * <p>The date is returned as the number of milliseconds since January 1, 1970 GMT.
     *
     * <p>If the request does not have a header of the specified name, this method returns -1.
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
    public abstract long getDateHeader(String name);

    /**
     * Returns any extra path information associated with the URL the client sent when it made this request.
     * The extra path information follows the servlet path but precedes the query string and will start with
     * a "/" character.
     *
     * <p>This method returns {@code null} if there was no extra path information.
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
     * <p>It is possible that a container may match a context by
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

//    /**
//     * Reconstructs the URL the client used to make the request.
//     * The returned URL contains a protocol, server name, port
//     * number, and server path, but it does not include query
//     * string parameters.
//     *
//     * <p>If this request has been forwarded using
//     * {@link javax.servlet.RequestDispatcher#forward}, the server path in the
//     * reconstructed URL must reflect the path used to obtain the
//     * RequestDispatcher, and not the server path specified by the client.
//     *
//     * <p>This method is useful for creating redirect messages
//     * and for reporting errors.
//     *
//     * @return the reconstructed URL
//     */
//    public abstract String getRequestURL();
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
    public abstract ActorRef<HttpResponse> getFrom();

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
}
