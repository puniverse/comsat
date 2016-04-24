/*
 * COMSAT
 * Copyright (c) 2015-2016, Parallel Universe Software Co. All rights reserved.
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
package co.paralleluniverse.comsat.webactors.undertow;

import co.paralleluniverse.actors.ActorRef;
import co.paralleluniverse.comsat.webactors.Cookie;
import co.paralleluniverse.comsat.webactors.HttpRequest;
import co.paralleluniverse.comsat.webactors.HttpResponse;
import co.paralleluniverse.comsat.webactors.WebMessage;
import com.google.common.collect.*;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HeaderMap;
import io.undertow.util.Headers;
import io.undertow.util.HttpString;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Collection;
import java.util.Deque;
import java.util.List;
import java.util.Map;

/**
 * @author circlespainter
 */
final class HttpRequestWrapper extends HttpRequest {
    final ActorRef<? super HttpResponse> actorRef;
    final HttpServerExchange xch;
    private final ByteBuffer reqContent;
    private InetSocketAddress sourceAddress;
    private ImmutableMultimap<String, String> params;
    private URI uri;
    private Collection<Cookie> cookies;
    private ListMultimap<String, String> heads;
    private ByteBuffer byteBufferBody;
    private String stringBody;
    private String contentType;
    private Charset encoding;

    public HttpRequestWrapper(ActorRef<? super HttpResponse> actorRef, HttpServerExchange xch, ByteBuffer reqContent) {
        this.actorRef = actorRef;
        this.xch = xch;
        this.reqContent = reqContent;
    }

    static ImmutableListMultimap<String, String> extractHeaders(HeaderMap headers) {
        if (headers != null) {
            final ImmutableListMultimap.Builder<String, String> builder = ImmutableListMultimap.builder();
            for (final HttpString n : headers.getHeaderNames())
                builder.putAll(n.toString(), headers.get(n));
            return builder.build();
        }
        return null;
    }

    @Override
    public final String getSourceHost() {
        fillSourceAddress();
        return sourceAddress != null ? sourceAddress.getHostString() : null;
    }

    @Override
    public final int getSourcePort() {
        fillSourceAddress();
        return sourceAddress != null ? sourceAddress.getPort() : -1;
    }

    private void fillSourceAddress() {
        if (sourceAddress == null)
            sourceAddress = xch.getSourceAddress();
    }

    @Override
    public Multimap<String, String> getParameters() {
        if (params == null) {
            final ImmutableMultimap.Builder<String, String> builder = ImmutableMultimap.builder();
            final Map<String, Deque<String>> parameters = xch.getQueryParameters();
            for (final String k : parameters.keySet())
                builder.putAll(k, parameters.get(k));
            params = builder.build();
        }
        return params;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return null;
    }

    @Override
    public String getScheme() {
        initUri();
        return uri.getScheme();
    }

    private void initUri() {
        if (uri == null) {
            try {
                uri = new URI(xch.getRequestURI());
            } catch (final URISyntaxException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public String getMethod() {
        return xch.getRequestMethod().toString();
    }

    @Override
    public String getPathInfo() {
        initUri();
        return uri.getPath();
    }

    @Override
    public String getContextPath() {
        return "/"; // Context path makes sense only for servlets
    }

    @Override
    public String getQueryString() {
        initUri();
        return uri.getQuery();
    }

    @Override
    public String getRequestURI() {
        return xch.getRequestURI();
    }

    @Override
    public String getServerName() {
        return xch.getHostName();
    }

    @Override
    public int getServerPort() {
        return xch.getHostPort();
    }

    @SuppressWarnings("unchecked")
    @Override
    public ActorRef<WebMessage> getFrom() {
        return (ActorRef<WebMessage>) actorRef;
    }

    @Override
    public ListMultimap<String, String> getHeaders() {
        if (heads == null) {
            heads = extractHeaders(xch.getRequestHeaders());
        }
        return heads;
    }

    @Override
    public Collection<Cookie> getCookies() {
        if (cookies == null) {
            final ImmutableList.Builder<Cookie> builder = ImmutableList.builder();
            for (io.undertow.server.handlers.Cookie c : xch.getRequestCookies().values()) {
                builder.add(
                    Cookie.cookie(c.getName(), c.getValue())
                        .setComment(c.getComment())
                        .setDomain(c.getDomain())
                        .setPath(c.getPath())
                        .setHttpOnly(c.isHttpOnly())
                        .setMaxAge(c.getMaxAge())
                        .setSecure(c.isSecure())
                        .setVersion(c.getVersion())
                        .build()
                );
            }
            cookies = builder.build();
        }
        return cookies;
    }

    @Override
    public int getContentLength() {
        return (int) xch.getRequestContentLength();
    }

    @Override
    public Charset getCharacterEncoding() {
        if (encoding == null) {
            final String charsetName = xch.getRequestCharset();
            if (charsetName != null) {
                try {
                    encoding = Charset.forName(charsetName);
                } catch (final UnsupportedCharsetException ignored) {}
            }

            if (encoding == null)
                encoding = Charset.defaultCharset();
        }
        return encoding;
    }

    @Override
    public String getContentType() {
        getHeaders();
        if (heads != null) {
            if (contentType == null) {
                final List<String> cts = heads.get(Headers.CONTENT_TYPE_STRING);
                if (cts != null && cts.size() > 0)
                    contentType = cts.get(0);
            }
        }
        return null;
    }

    @Override
    public String getStringBody() {
        if (stringBody == null) {
            if (byteBufferBody != null)
                return null;
            decodeStringBody();
        }
        return stringBody;
    }

    @Override
    public ByteBuffer getByteBufferBody() {
        if (byteBufferBody == null) {
            if (stringBody != null)
                return null;
            if (reqContent != null)
                byteBufferBody = reqContent;
        }
        return byteBufferBody;
    }

    private String decodeStringBody() {
        if (reqContent != null) {
            try {
                //noinspection ConstantConditions
                stringBody =
                    getCharacterEncoding()
                        .newDecoder()
                        .onMalformedInput(CodingErrorAction.REPORT)
                        .onUnmappableCharacter(CodingErrorAction.REPORT)
                        .decode(reqContent)
                        .toString();
            } catch (final CharacterCodingException ignored) {
            }
        }
        return stringBody;
    }
}
