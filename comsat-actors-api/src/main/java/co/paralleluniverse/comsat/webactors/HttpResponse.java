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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class HttpResponse implements WebResponse {
    private final String string;
    private final List<Cookie> cookies;
    private final List<Map.Entry<String, String>> headers;
    private final int status;
    private final Throwable error;
    private String redirectPath;

    public static class Builder {
        private final String string;
        private List<Cookie> cookies;
        private List<Map.Entry<String, String>> headers;
        private int status;
        private Throwable error;
        private String redirectPath;

        public Builder(String string) {
            this.string = string;
            this.status = 200;
        }

        public void addHeader(final String key, final String val) {
            if (headers == null)
                headers = new ArrayList<>();
            headers.add(new Map.Entry<String, String>() {
                @Override
                public String getKey() {
                    return key;
                }

                @Override
                public String getValue() {
                    return val;
                }

                @Override
                public String setValue(String value) {
                    throw new UnsupportedOperationException("Not supported yet.");
                }
            });
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
        this.cookies = builder.cookies;
        this.error = builder.error;
        this.headers = builder.headers;
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

    public List<Entry<String, String>> getHeaders() {
        return headers;
    }

    public int getStatus() {
        return status;
    }

    public Throwable getError() {
        return error;
    }
}
