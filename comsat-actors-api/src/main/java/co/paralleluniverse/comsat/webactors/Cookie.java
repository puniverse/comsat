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

/**
 * A HTTP cookie
 */
public class Cookie {
    /**
     * Constructs a cookie with the specified name and value.
     *
     * <p>The name must conform to RFC 2109. However, vendors may
     * provide a configuration option that allows cookie names conforming
     * to the original Netscape Cookie Specification to be accepted.
     *
     * <p>The name of a cookie cannot be changed once the cookie has been created.
     *
     * <p>The value can be anything the server chooses to send.
     * Its value is probably of interest only to the server.
     *
     * <p>By default, cookies are created according to the Netscape cookie specification.
     * The version can be changed with the {@link Builder#setVersion(int) setVersion} method.
     *
     * @param name  the name of the cookie
     * @param value the value of the cookie
     * @return a new {@link Cookie.Builder}
     */
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
        private boolean secure;

        private Builder(String name, String value) {
            this.name = name;
            this.value = value;
        }

        /**
         * Specifies a path for the cookie to which the client should return the cookie.
         *
         * <p>The cookie is visible to all the pages in the directory
         * you specify, and all the pages in that directory's subdirectories.
         * A cookie's path must include the servlet that set the cookie,
         * for example, <i>/catalog</i>, which makes the cookie
         * visible to all directories on the server under <i>/catalog</i>.
         *
         * <p>Consult RFC 2109 (available on the Internet) for more
         * information on setting path names for cookies.
         *
         * @param uri a {@code String} specifying a path
         *
         * @see Cookie#getPath
         */
        public Builder setPath(String uri) {
            this.path = uri;
            return this;
        }

        /**
         *
         * Specifies the domain within which this cookie should be presented.
         *
         * <p>The form of the domain name is specified by RFC 2109.
         * A domain name begins with a dot ({@code .foo.com}) and means that
         * the cookie is visible to servers in a specified Domain Name System
         * (DNS) zone (for example,
         * {@code www.foo.com}, but not {@code a.b.foo.com}).
         * By default, cookies are only returned to the server that sent them.
         *
         * @param domain the domain name within which this cookie is visible;
         *               form is according to RFC 2109
         *
         * @see Cookie#getDomain
         */
        public Builder setDomain(String domain) {
            this.domain = domain;
            return this;
        }

        /**
         * Sets the maximum age in seconds for this Cookie.
         *
         * <p>A positive value indicates that the cookie will expire
         * after that many seconds have passed. Note that the value is
         * the <i>maximum</i> age when the cookie will expire, not the cookie's
         * current age.
         *
         * <p>A negative value means that the cookie is not stored persistently and will
         * be deleted when the Web browser exits.
         * A zero value causes the cookie to be deleted.
         *
         * @param maxAge an integer specifying the maximum age of the
         *               cookie in seconds; if negative, means
         *               the cookie is not stored; if zero, deletes the cookie
         *
         * @see Cookie#getMaxAge
         */
        public Builder setMaxAge(int maxAge) {
            this.maxAge = maxAge;
            return this;
        }

        /**
         * Specifies a comment that describes a cookie's purpose.
         * The comment is useful if the browser presents the cookie
         * to the user. Comments
         * are not supported by Netscape Version 0 cookies.
         *
         * @param comment a {@code String} specifying the comment to display to the user
         *
         * @see Cookie#getComment
         */
        public Builder setComment(String comment) {
            this.comment = comment;
            return this;
        }

        /**
         * Sets the version of the cookie protocol that this Cookie complies with.
         *
         * <p>Version 0 complies with the original Netscape cookie specification.
         * Version 1 complies with RFC 2109.
         *
         * <p>Since RFC 2109 is still somewhat new, consider
         * version 1 as experimental; do not use it yet on production sites.
         *
         * @param version 0 if the cookie should comply with the original Netscape
         *                specification; 1 if the cookie should comply with RFC 2109
         *
         * @see Cookie#getVersion
         */
        public Builder setVersion(int version) {
            this.version = version;
            return this;
        }

        /**
         * Marks or unmarks this Cookie as <i>HttpOnly</i>.
         *
         * <p>If {@code httpOnly} is set to {@code true}, this cookie is
         * marked as <i>HttpOnly</i>, by adding the {@code HttpOnly} attribute to it.
         *
         * <p><i>HttpOnly</i> cookies are not supposed to be exposed to
         * client-side scripting code, and may therefore help mitigate certain
         * kinds of cross-site scripting attacks.
         *
         * @param httpOnly {@code true} if this cookie is to be marked as <i>HttpOnly</i>, {@code false} otherwise
         *
         * @see Cookie#isHttpOnly()
         */
        public Builder setHttpOnly(boolean httpOnly) {
            this.httpOnly = httpOnly;
            return this;
        }

        /**
         * Indicates to the browser whether the cookie should only be sent
         * using a secure protocol, such as HTTPS or SSL.
         *
         * <p>The default value is {@code false}.
         *
         * @param flag if {@code true}, sends the cookie from the browser
         *             to the server only when using a secure protocol;
         *             if {@code false}, sent on any protocol
         *
         * @see Cookie#isSecure
         */
        public Builder setSecure(boolean flag) {
            this.secure = flag;
            return this;
        }

        /**
         * Instantiates a new immutable {@link Cookie} based on the values set in this builder.
         *
         * @return a new {@link Cookie}
         */
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
        this.secure = builder.secure;
    }

    /**
     * The name of the cookie.
     *
     * @see #cookie(String, String)
     */
    public String getName() {
        return name;
    }

    /**
     * The value of this Cookie.
     *
     * @see #cookie(String, String)
     */
    public String getValue() {
        return value;
    }

    /**
     * Returns the path on the server to which the browser returns this cookie.
     * The cookie is visible to all subpaths on the server.
     *
     * @return	a {@code String} specifying a path that contains a servlet name, for example, <i>/catalog</i>
     *
     * @see Builder#setPath
     */
    public String getPath() {
        return path;
    }

    /**
     * The domain name of this Cookie.
     *
     * <p>Domain names are formatted according to RFC 2109.
     *
     * @see Builder#setDomain
     */
    public String getDomain() {
        return domain;
    }

    /**
     * The maximum age in seconds of this Cookie.
     *
     * <p>By default, {@code -1} is returned, which indicates that the cookie will persist until browser shutdown.
     *
     * @return an integer specifying the maximum age of the cookie in seconds;
     *         if negative, means the cookie persists until browser shutdown
     *
     * @see Builder#setMaxAge
     */
    public int getMaxAge() {
        return maxAge;
    }

    /**
     * The comment describing the purpose of this cookie, or {@code null} if the cookie has no comment.
     *
     * @see Builder#setComment
     */
    public String getComment() {
        return comment;
    }

    /**
     * The version of the protocol this cookie complies with.
     * Version 1 complies with RFC 2109,
     * and version 0 complies with the original cookie specification drafted by Netscape.
     * Cookies provided by a browser use and identify the browser's cookie version.
     *
     * @return {@code 0} if the cookie complies with the original Netscape specification;
     *         {@code 1} if the cookie complies with RFC 2109
     *
     * @see Builder#setVersion
     */
    public int getVersion() {
        return version;
    }

    /**
     * Whether this Cookie has been marked as <i>HttpOnly</i>.
     *
     * @return {@code true} if this Cookie has been marked as <i>HttpOnly</i>, {@code false} otherwise
     */
    public boolean isHttpOnly() {
        return httpOnly;
    }

    /**
     * Returns {@code true} if the browser is sending cookies only over a secure protocol,
     * or {@code false} if the browser can send cookies using any protocol.
     *
     * @return {@code true} if the browser uses a secure protocol, {@code false} otherwise
     *
     * @see Builder#setSecure
     */
    public boolean isSecure() {
        return secure;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Cookie[");
        sb.append("name: ").append(name);
        sb.append(" value: ").append(value);
        if (path != null)
            sb.append(" path: ").append(path);
        if (domain != null)
            sb.append(" domain: ").append(domain);
        sb.append(" maxAge: ").append(maxAge);
        if (comment != null)
            sb.append(" comment: ").append(comment);
        sb.append(" version: ").append(version);
        if (httpOnly)
            sb.append(" httpOnly: ").append(httpOnly);
        sb.append(" secure: ").append(secure);
        sb.append(']');
        return sb.toString();
    }
}
