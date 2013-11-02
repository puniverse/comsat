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

public class Cookie {
    public static Builder cookie(String name, String value) {
        return new Builder(name, value);
    }
    
    public static class Builder {
        private final String name;
        private final String value;
        private String path;
        private String domain;
        private int maxAge = -1;
        private String comment;
        private int version;
        private boolean httpOnly;
        private boolean discard;
        private boolean secure;

        private Builder(String name, String value) {
            this.name = name;
            this.value = value;
        }

        public Builder setPath(String path) {
            this.path = path;
            return this;
        }

        public Builder setDomain(String domain) {
            this.domain = domain;
            return this;
        }

        public Builder setMaxAge(int maxAge) {
            this.maxAge = maxAge;
            return this;
        }

        public Builder setComment(String comment) {
            this.comment = comment;
            return this;
        }

        public Builder setVersion(int version) {
            this.version = version;
            return this;
        }

        public Builder setHttpOnly(boolean httpOnly) {
            this.httpOnly = httpOnly;
            return this;
        }

        public Builder setDiscard(boolean discard) {
            this.discard = discard;
            return this;
        }

        public Builder setSecure(boolean secure) {
            this.secure = secure;
            return this;
        }

        public Cookie build() {
            return new Cookie(this);
        }
    }
    private final String name;
    private final String value;
    private final String path;
    private final String domain;
    private final int maxAge;
    private final String comment;
    private final int version;
    private final boolean httpOnly;
    private final boolean discard;
    private final boolean secure;

    private Cookie(Builder builder) {
        this.name = builder.name;
        this.value = builder.value;
        this.path = builder.path;
        this.domain = builder.domain;
        this.maxAge = builder.maxAge;
        this.comment = builder.comment;
        this.version = builder.version;
        this.httpOnly = builder.httpOnly;
        this.discard = builder.discard;
        this.secure = builder.secure;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    public String getPath() {
        return path;
    }

    public String getDomain() {
        return domain;
    }

    public int getMaxAge() {
        return maxAge;
    }

    public String getComment() {
        return comment;
    }

    public int getVersion() {
        return version;
    }

    public boolean isHttpOnly() {
        return httpOnly;
    }

    public boolean isDiscard() {
        return discard;
    }

    public boolean isSecure() {
        return secure;
    }

    @Override
    public String toString() {
        return "Cookie{" + "name=" + name + ", value=" + value + ", path=" + path + ", domain=" + domain + ", maxAge=" + maxAge + ", comment=" + comment + ", version=" + version + ", httpOnly=" + httpOnly + ", discard=" + discard + ", secure=" + secure + '}';
    }
}
