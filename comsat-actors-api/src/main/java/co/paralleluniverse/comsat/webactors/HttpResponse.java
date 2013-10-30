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
import java.util.ArrayList;
import java.util.List;

public class HttpResponse implements WebResponse {
    private final String string;
    private final List<Cookie> cookies;
    private final Multimap<String, String> headers;
    private final int status;
    private final Throwable error;
    private final String redirectPath;

    public static class Builder {
        private final String string;
        private List<Cookie> cookies;
        private Multimap<String, String> headers;
        private int status;
        private Throwable error;
        private String redirectPath;

        public Builder(String string) {
            this.string = string;
            this.status = 200;
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
        this.string = builder.string;
        this.cookies = ImmutableList.copyOf(builder.cookies);
        this.error = builder.error;
        this.headers = ImmutableMultimap.copyOf(builder.headers);
        this.status = builder.status;
        this.redirectPath = builder.redirectPath;
    }

    public String getRedirectPath() {
        return redirectPath;
    }

    public String getString() {
        return string;
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
