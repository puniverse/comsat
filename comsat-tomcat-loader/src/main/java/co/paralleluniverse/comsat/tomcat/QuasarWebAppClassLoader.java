/*
 * COMSAT
 * Copyright (c) 2013-2016, Parallel Universe Software Co. All rights reserved.
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
package co.paralleluniverse.comsat.tomcat;

import co.paralleluniverse.fibers.instrument.Log;
import co.paralleluniverse.fibers.instrument.LogLevel;
import co.paralleluniverse.fibers.instrument.MethodDatabase;
import co.paralleluniverse.fibers.instrument.QuasarInstrumentor;
import java.util.Arrays;
import org.apache.catalina.loader.ResourceEntry;
import org.apache.catalina.loader.WebappClassLoader;

/**
 * See:
 * http://tomcat.apache.org/tomcat-7.0-doc/config/loader.html
 * http://tomcat.apache.org/tomcat-8.0-doc/config/loader.html
 *
 * @author pron
 */
public final class QuasarWebAppClassLoader extends WebappClassLoader {
    @Override
    protected final synchronized boolean filter(String name) {
        // Don't re-load the instrumentation logic, including the `SuspendableClassifier` interface,
        // else implementations in the webapp classloader will have trouble loading when running in
        // a standalone servlet container
        return
            name.startsWith("co.paralleluniverse.common.") ||
            name.startsWith("co.paralleluniverse.fibers.instrument.") ||
            name.startsWith("jsr166e") ||
            name.startsWith("co.paralleluniverse.asm.");
    }

    private QuasarInstrumentor instrumentor;

    public QuasarWebAppClassLoader() {}

    public QuasarWebAppClassLoader(ClassLoader parent) {
        super(parent);
    }

    private QuasarInstrumentor newInstrumentor() {
        final QuasarInstrumentor inst = new QuasarInstrumentor(this); // must be called *after* construction has completed
        inst.setLog(new Log() {
            @Override
            public final void log(LogLevel level, String msg, Object... args) {
                System.err.println("[quasar] " + level + ": " + String.format(msg, args));
            }

            @Override
            public final void error(String msg, Throwable exc) {
                System.err.println("[quasar] ERROR: " + msg);
                exc.printStackTrace(System.err);
            }
        });
        inst.setVerbose(false);
        inst.setDebug(false);
        return inst;
    }

    private synchronized void initInstrumentor() {
        if (instrumentor == null)
            instrumentor = newInstrumentor();
    }

    @Override
    protected final ResourceEntry findResourceInternal(String name, String path, boolean manifestRequired) {
        initInstrumentor();
        final ResourceEntry entry = super.findResourceInternal(name, path, manifestRequired);
        if (name != null && path != null && path.endsWith(CLASS_SUFFIX) && entry != null && entry.binaryContent != null) {
            final int nameLen = name.length();
            final String className = name.substring(0, name.endsWith(CLASS_SUFFIX) ? nameLen - CLASS_SUFFIX_LENGTH : nameLen);
            try {
                final byte[] res = instrumentor.instrumentClass(className, entry.binaryContent);
                if (res != null)
                    entry.binaryContent = res;
            } catch (final Exception ex) {
                if (MethodDatabase.isProblematicClass(className))
                    instrumentor.log(LogLevel.INFO, "Skipping problematic class instrumentation %s - %s %s", className, ex, Arrays.toString(ex.getStackTrace()));
                else
                    instrumentor.error("Unable to instrument " + className, ex);
            }
        }
        return entry;
    }

    private static final String CLASS_SUFFIX = ".class";
    private static final int CLASS_SUFFIX_LENGTH = CLASS_SUFFIX.length();
}
