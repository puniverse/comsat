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

import com.google.common.collect.Multimap;
import java.nio.charset.Charset;
import java.util.Collection;

/**
 *
 * @author pron
 */
public interface HttpMessage extends WebMessage {
    Multimap<String, String> getHeaders();

    Collection<Cookie> getCookies();

    Charset getCharacterEncoding();
    
    int getContentLength();
}
