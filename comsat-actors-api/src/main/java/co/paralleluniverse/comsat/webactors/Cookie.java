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

import java.util.Date;


public class Cookie {
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

    public Cookie(String name, String value) {
        this.name = name;
        this.value = value;
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

    public Cookie setPath(String path) {
        this.path = path;
        return this;
    }

    public Cookie setDomain(String domain) {
        this.domain = domain;
        return this;
    }

    public Cookie setMaxAge(int maxAge) {
        this.maxAge = maxAge;
        return this;
    }

    public Cookie setComment(String comment) {
        this.comment = comment;
        return this;
    }

    public Cookie setVersion(int version) {
        this.version = version;
        return this;
    }

    public Cookie setHttpOnly(boolean httpOnly) {
        this.httpOnly = httpOnly;
        return this;
    }

    public Cookie setDiscard(boolean discard) {
        this.discard = discard;
        return this;
    }

    public Cookie setSecure(boolean secure) {
        this.secure = secure;
        return this;
    }

    @Override
    public String toString() {
        return "WebCookie{" + "name=" + name + ", value=" + value + ", path=" + path + ", domain=" + domain + ", maxAge=" + maxAge + ", comment=" + comment + ", version=" + version + ", httpOnly=" + httpOnly + ", discard=" + discard + ", secure=" + secure + '}';
    }
}
