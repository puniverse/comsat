/*
 * COMSAT
 * Copyright (c) 2013-2014, Parallel Universe Software Co. All rights reserved.
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
package co.paralleluniverse.comsat.jetty;

import co.paralleluniverse.fibers.instrument.QuasarURLClassLoaderHelper;
import java.io.IOException;
import java.io.InputStream;
import org.eclipse.jetty.webapp.WebAppClassLoader;



/**
 * See:
 * http://wiki.eclipse.org/Jetty/Reference/Jetty_Classloading
 *
 * @author pron
 */
public class QuasarWebAppClassLoader extends WebAppClassLoader {
    private final QuasarURLClassLoaderHelper helper;

    public QuasarWebAppClassLoader(Context context) throws IOException {
        super(context);
        this.helper = new QuasarURLClassLoaderHelper(this);
    }

    public QuasarWebAppClassLoader(ClassLoader parent, Context context) throws IOException {
        super(parent, context);
        this.helper = new QuasarURLClassLoaderHelper(this);
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        return helper.findClass(name);
    }

    @Override
    public InputStream getResourceAsStream(String name) {
        return helper.getResourceAsStream(name);
    }
}
