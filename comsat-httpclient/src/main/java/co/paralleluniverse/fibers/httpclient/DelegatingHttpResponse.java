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
package co.paralleluniverse.fibers.httpclient;

import java.util.Locale;
import org.apache.http.Header;
import org.apache.http.HeaderIterator;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.StatusLine;
import org.apache.http.params.HttpParams;

/**
 *
 * @author pron
 */
class DelegatingHttpResponse implements HttpResponse {
    protected final HttpResponse response;

    public DelegatingHttpResponse(HttpResponse response) {
        this.response = response;
    }
    
    @Override
    public StatusLine getStatusLine() {
        return response.getStatusLine();
    }

    @Override
    public void setStatusLine(StatusLine statusline) {
        response.setStatusLine(statusline);
    }

    @Override
    public void setStatusLine(ProtocolVersion ver, int code) {
        response.setStatusLine(ver, code);
    }

    @Override
    public void setStatusLine(ProtocolVersion ver, int code, String reason) {
        response.setStatusLine(ver, code, reason);
    }

    @Override
    public void setStatusCode(int code) throws IllegalStateException {
        response.setStatusCode(code);
    }

    @Override
    public void setReasonPhrase(String reason) throws IllegalStateException {
        response.setReasonPhrase(reason);
    }

    @Override
    public HttpEntity getEntity() {
        return response.getEntity();
    }

    @Override
    public void setEntity(HttpEntity entity) {
        response.setEntity(entity);
    }

    @Override
    public Locale getLocale() {
        return response.getLocale();
    }

    @Override
    public void setLocale(Locale loc) {
        response.setLocale(loc);
    }

    @Override
    public ProtocolVersion getProtocolVersion() {
        return response.getProtocolVersion();
    }

    @Override
    public boolean containsHeader(String name) {
        return response.containsHeader(name);
    }

    @Override
    public Header[] getHeaders(String name) {
        return response.getHeaders(name);
    }

    @Override
    public Header getFirstHeader(String name) {
        return response.getFirstHeader(name);
    }

    @Override
    public Header getLastHeader(String name) {
        return response.getLastHeader(name);
    }

    @Override
    public Header[] getAllHeaders() {
        return response.getAllHeaders();
    }

    @Override
    public void addHeader(Header header) {
        response.addHeader(header);
    }

    @Override
    public void addHeader(String name, String value) {
        response.addHeader(name, value);
    }

    @Override
    public void setHeader(Header header) {
        response.setHeader(header);
    }

    @Override
    public void setHeader(String name, String value) {
        response.setHeader(name, value);
    }

    @Override
    public void setHeaders(Header[] headers) {
        response.setHeaders(headers);
    }

    @Override
    public void removeHeader(Header header) {
        response.removeHeader(header);
    }

    @Override
    public void removeHeaders(String name) {
        response.removeHeaders(name);
    }

    @Override
    public HeaderIterator headerIterator() {
        return response.headerIterator();
    }

    @Override
    public HeaderIterator headerIterator(String name) {
        return response.headerIterator(name);
    }

    @Override
    public HttpParams getParams() {
        return response.getParams();
    }

    @Override
    public void setParams(HttpParams params) {
        response.setParams(params);
    }

    @Override
    public int hashCode() {
        return response.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return response.equals(obj);
    }

    @Override
    public String toString() {
        return response.toString();
    }
}
