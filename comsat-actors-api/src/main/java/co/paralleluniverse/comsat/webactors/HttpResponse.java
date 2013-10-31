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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class HttpResponse implements WebResponse {
    private final String contentType;
    private final String charset;
    private final String strBody;
    private final ByteBuffer binBody;
    private final List<Cookie> cookies;
    private final Multimap<String, String> headers;
    private final int status;
    private final Throwable error;
    private final String redirectPath;

    public static class Builder {
        private final String strBody;
        private final ByteBuffer binBody;
        private String contentType;
        private String charset;
        private List<Cookie> cookies;
        private Multimap<String, String> headers;
        private int status;
        private Throwable error;
        private String redirectPath;

        public Builder(String body) {
            this.strBody = body;
            this.binBody = null;
            this.status = 200;
        }

        public Builder(ByteBuffer body) {
            this.binBody = body;
            this.strBody = null;
            this.status = 200;
        }

        public void setContentType(String contentType) {
            this.contentType = contentType;
        }

        public void setCharacterEncoding(String charset) {
            this.charset = charset;
        }

        public void addHeader(final String key, final String val) {
            if (headers == null)
                headers = LinkedHashMultimap.create();
            headers.put(key, val);
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
            return new HttpResponse(this);
        }
    }

    private HttpResponse(Builder builder) {
        this.contentType = builder.contentType;
        this.charset = builder.charset;
        this.strBody = builder.strBody;
        this.binBody = builder.binBody;
        this.cookies = ImmutableList.copyOf(builder.cookies);
        this.error = builder.error;
        this.headers = ImmutableMultimap.copyOf(builder.headers);
        this.status = builder.status;
        this.redirectPath = builder.redirectPath;
    }

    public boolean isBinary() {
        return binBody != null;
    }

    public String getContentType() {
        return contentType;
    }

    public String getCharacterEncoding() {
        return charset;
    }
    
    public String getRedirectPath() {
        return redirectPath;
    }

    public String getStringBody() {
        return strBody;
    }

    public ByteBuffer getBinBody() {
        return binBody;
    }

    public List<Cookie> getCookies() {
        return cookies;
    }

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
