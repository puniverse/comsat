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
package co.paralleluniverse.comsat.webactors.netty;

import co.paralleluniverse.actors.ActorRef;
import co.paralleluniverse.comsat.webactors.Cookie;
import co.paralleluniverse.comsat.webactors.HttpRequest;
import co.paralleluniverse.comsat.webactors.HttpResponse;
import co.paralleluniverse.comsat.webactors.WebMessage;
import com.google.common.collect.*;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.UnsupportedCharsetException;
import java.util.*;

import static io.netty.handler.codec.http.HttpHeaders.Names.*;

/**
 * @author circlespainter
 */
final class HttpRequestWrapper extends HttpRequest {
    public static final String CHARSET_MARKER_STRING = "charset=";

    final ActorRef<? super HttpResponse> actorRef;
    final FullHttpRequest req;
    final ChannelHandlerContext ctx;
    final String sessionId;

    private static final Set<io.netty.handler.codec.http.cookie.Cookie> EMPTY_SET = new HashSet<>();

    private final ByteBuf reqContent;

    private InetSocketAddress sourceAddress;
    private ImmutableMultimap<String, String> params;
    private URI uri;
    private Collection<Cookie> cookies;
    private ListMultimap<String, String> heads;
    private ByteBuffer byteBufferBody;
    private String stringBody;
    private Charset encoding;
    private String contentType;

    public HttpRequestWrapper(ActorRef<? super HttpResponse> actorRef, ChannelHandlerContext ctx, FullHttpRequest req, String sessionId) {
        this.actorRef = actorRef;
        this.ctx = ctx;
        this.req = req;
        this.sessionId = sessionId;

        reqContent = Unpooled.copiedBuffer(req.content());
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
        final SocketAddress remoteAddress = ctx.channel().remoteAddress();
        if (sourceAddress == null && remoteAddress instanceof InetSocketAddress) {
            sourceAddress = (InetSocketAddress) remoteAddress;
        }
    }

    @Override
    public final Multimap<String, String> getParameters() {
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
    public final Map<String, Object> getAttributes() {
        return ImmutableMap.of(); // No attributes in Netty; Guava's impl. will return a pre-built instance
    }

    @Override
    public final String getScheme() {
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
    public final String getMethod() {
        return req.getMethod().name();
    }

    @Override
    public final String getPathInfo() {
        initUri();
        return uri.getPath();
    }

    @Override
    public final String getContextPath() {
        return "/"; // Context path makes sense only for servlets
    }

    @Override
    public final String getQueryString() {
        initUri();
        return uri.getQuery();
    }

    @Override
    public final String getRequestURI() {
        return req.getUri();
    }

    @Override
    public final String getServerName() {
        initUri();
        return uri.getHost();
    }

    @Override
    public final int getServerPort() {
        initUri();
        return uri.getPort();
    }

    @SuppressWarnings("unchecked")
    @Override
    public final ActorRef<WebMessage> getFrom() {
        return (ActorRef<WebMessage>) actorRef;
    }

    @Override
    public final ListMultimap<String, String> getHeaders() {
        if (heads == null) {
            heads = extractHeaders(req.headers());
        }
        return heads;
    }

    @Override
    public final Collection<Cookie> getCookies() {
        if (cookies == null) {
            final ImmutableList.Builder<Cookie> builder = ImmutableList.builder();
            for (io.netty.handler.codec.http.cookie.Cookie c : getNettyCookies(req)) {
                builder.add(
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

    static Set<io.netty.handler.codec.http.cookie.Cookie> getNettyCookies(FullHttpRequest req) {
        final HttpHeaders heads = req.headers();
        final String head = heads != null ? heads.get(HttpHeaders.Names.COOKIE) : null;
        if (head != null)
            return ServerCookieDecoder.LAX.decode(head);
        else
            return EMPTY_SET;
    }

    @Override
    public final int getContentLength() {
        final String stringBody = getStringBody();
        if (stringBody != null)
            return stringBody.length();
        final ByteBuffer bufferBody = getByteBufferBody();
        if (bufferBody != null)
            return bufferBody.remaining();
        return 0;
    }

    @Override
    public final Charset getCharacterEncoding() {
        if (encoding == null)
            encoding = extractCharacterEncoding(getHeaders());
        return encoding;
    }

    @Override
    public final String getContentType() {
        if (contentType == null) {
            getHeaders();
            if (heads != null) {
                final List<String> cts = heads.get(CONTENT_TYPE);
                if (cts != null && cts.size() > 0)
                    contentType = cts.get(0);
            }
        }
        return null;
    }

    @Override
    public final String getStringBody() {
        if (stringBody == null) {
            if (byteBufferBody != null)
                return null;
            decodeStringBody();
        }
        return stringBody;
    }

    @Override
    public final ByteBuffer getByteBufferBody() {
        if (byteBufferBody == null) {
            if (stringBody != null)
                return null;
            if (reqContent != null)
                byteBufferBody = reqContent.nioBuffer();
        }
        return byteBufferBody;
    }

    public final String getSessionId() {
        return sessionId;
    }

    private String decodeStringBody() {
        if (reqContent != null) {
            try {
                stringBody =
                    getCharacterEncodingOrDefault()
                        .newDecoder()
                        .onMalformedInput(CodingErrorAction.REPORT)
                        .onUnmappableCharacter(CodingErrorAction.REPORT)
                        .decode(reqContent.nioBuffer())
                        .toString();
            } catch (CharacterCodingException ignored) {
            }
        }
        return stringBody;
    }

    Charset getCharacterEncodingOrDefault() {
        return getCharacterEncodingOrDefault(getCharacterEncoding());
    }

    static Charset extractCharacterEncodingOrDefault(HttpHeaders headers) {
        return getCharacterEncodingOrDefault(extractCharacterEncoding(extractHeaders(headers)));
    }

    static Charset extractCharacterEncoding(ListMultimap<String, String> heads) {
        if (heads != null) {
            final List<String> cts = heads.get(CONTENT_TYPE);
            if (cts != null && cts.size() > 0) {
                final String ct = cts.get(0).trim().toLowerCase();
                if (ct.contains(CHARSET_MARKER_STRING)) {
                    try {
                        return Charset.forName(ct.substring(ct.indexOf(CHARSET_MARKER_STRING) + CHARSET_MARKER_STRING.length()).trim());
                    } catch (UnsupportedCharsetException ignored) {
                    }
                }
            }
        }
        return null;
    }

    static ImmutableListMultimap<String, String> extractHeaders(HttpHeaders headers) {
        if (headers != null) {
            final ImmutableListMultimap.Builder<String, String> builder = ImmutableListMultimap.builder();
            for (final String n : headers.names())
                // Normalize header names by their conversion to lower case
                builder.putAll(n.toLowerCase(Locale.ENGLISH), headers.getAll(n));
            return builder.build();
        }
        return null;
    }

    private static Charset getCharacterEncodingOrDefault(Charset characterEncoding) {
        if (characterEncoding == null)
            return Charset.defaultCharset();
        return characterEncoding;
    }
}
