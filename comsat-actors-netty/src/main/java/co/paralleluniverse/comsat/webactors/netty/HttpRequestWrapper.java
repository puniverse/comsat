/*
 * COMSAT
 * Copyright (c) 2015, Parallel Universe Software Co. All rights reserved.
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
package co.paralleluniverse.comsat.webactors.netty;

import co.paralleluniverse.actors.ActorRef;
import co.paralleluniverse.comsat.webactors.Cookie;
import co.paralleluniverse.comsat.webactors.HttpRequest;
import co.paralleluniverse.comsat.webactors.HttpResponse;
import co.paralleluniverse.comsat.webactors.WebMessage;
import com.google.common.collect.*;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.*;

import static io.netty.handler.codec.http.HttpHeaders.Names.*;

/**
 *
 * @author circlespainter
 */
final class HttpRequestWrapper extends HttpRequest {
    final ActorRef<? super HttpResponse> actorRef;
    final FullHttpRequest req;
    final ChannelHandlerContext ctx;

    private ImmutableMultimap<String, String> params;
    private URI uri;
    private Collection<Cookie> cookies;
    private ListMultimap<String, String> heads;

    public HttpRequestWrapper(ActorRef<? super HttpResponse> actorRef, ChannelHandlerContext ctx, FullHttpRequest req) {
        this.actorRef = actorRef;
        this.ctx = ctx;
        this.req = req;
    }

    @Override
    public Multimap<String, String> getParameters() {
        QueryStringDecoder queryStringDecoder;
        if (params == null) {
            queryStringDecoder = new QueryStringDecoder(req.getUri());
            final ImmutableMultimap.Builder<String, String> builder = ImmutableMultimap.builder();
            final Map<String, List<String>> parameters = queryStringDecoder.parameters();
            for (final String k : parameters.keySet())
                builder.putAll(k, parameters.get(k));
            params = builder.build();
        }
        return params;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return ImmutableMap.of(); // No attributes in Netty; Guava's impl. will return a pre-built instance
    }

    @Override
    public String getScheme() {
        initUri();
        return uri.getScheme();
    }

    private void initUri() {
        if (uri == null) {
            try {
                uri = new URI(req.getUri());
            } catch (final URISyntaxException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public String getMethod() {
        return req.getMethod().name();
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
        return req.getUri();
    }

    @Override
    public String getServerName() {
        initUri();
        return uri.getHost();
    }

    @Override
    public int getServerPort() {
        initUri();
        return uri.getPort();
    }

    @SuppressWarnings("unchecked")
    @Override
    public ActorRef<WebMessage> getFrom() {
        return (ActorRef<WebMessage>) actorRef;
    }

    @Override
    public ListMultimap<String, String> getHeaders() {
        if (heads == null) {
            final ImmutableListMultimap.Builder<String, String> builder = ImmutableListMultimap.builder();
            final HttpHeaders headers = req.headers();
            for (final String n : headers.names())
                builder.putAll(n, headers.getAll(n));
            heads = builder.build();
        }
        return heads;
    }

    @Override
    public Collection<Cookie> getCookies() {
        if (cookies == null) {
            final ImmutableList.Builder<Cookie> builder = ImmutableList.<Cookie>builder();
            for (io.netty.handler.codec.http.cookie.Cookie c : ServerCookieDecoder.LAX.decode(req.headers().get("Cookies"))) {
                builder.add (
                    Cookie.cookie(c.name(), c.value())
                        .setDomain(c.domain())
                        .setPath(c.path())
                        .setHttpOnly(c.isHttpOnly())
                        .setMaxAge((int) c.maxAge())
                        .setSecure(c.isSecure())
                        .build()
                );
            }
            cookies = builder.build();
        }
        return cookies;
    }

    @Override
    public int getContentLength() {
        final List<String> contentLength = heads.get(CONTENT_LENGTH);
        if (contentLength != null && contentLength.size() > 0)
            return Integer.parseInt(contentLength.get(0));
        return -1;
    }

    @Override
    public Charset getCharacterEncoding() {
        final List<String> charEnc = heads.get(CONTENT_ENCODING);
        if (charEnc != null && charEnc.size() > 0)
            return Charset.forName(charEnc.get(0));
        return null;
    }

    @Override
    public String getContentType() {
        final List<String> contentType = heads.get(CONTENT_TYPE);
        if (contentType != null && contentType.size() > 0)
            return contentType.get(0);
        return null;
    }

    @Override
    public String getStringBody() {
        return req.content().toString();
    }

    @Override
    public ByteBuffer getByteBufferBody() {
        return req.content().nioBuffer();
    }
}
