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
public class QuasarWebAppClassLoader extends WebappClassLoader {

    @Override
    protected synchronized boolean filter(String name) {
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

    public QuasarWebAppClassLoader() {
    }

    public QuasarWebAppClassLoader(ClassLoader parent) {
        super(parent);
    }

    private QuasarInstrumentor newInstrumentor() {
        QuasarInstrumentor inst = new QuasarInstrumentor(this); // must be called *after* construction has completed
        inst.setLog(new Log() {
            @Override
            public void log(LogLevel level, String msg, Object... args) {
                System.out.println("[quasar] " + level + ": " + String.format(msg, args));
            }

            @Override
            public void error(String msg, Throwable exc) {
                System.out.println("[quasar] ERROR: " + msg);
                exc.printStackTrace(System.out);
            }
        });
        inst.setVerbose(false);
        inst.setDebug(false);
        return inst;
    }

    private synchronized void initInstrumentor() {
        if (instrumentor == null)
            this.instrumentor = newInstrumentor();
    }

    @Override
    protected ResourceEntry findResourceInternal(String name, String path, boolean manifestRequired) {
        initInstrumentor();
        ResourceEntry entry = super.findResourceInternal(name, path, manifestRequired);
        if (entry != null && path.endsWith(".class") && entry.binaryContent != null) {
            String className = name.substring(0, name.length() - ".class".length());
            try {
                byte[] res = instrumentor.instrumentClass(className, entry.binaryContent);
                if (res != null)
                    entry.binaryContent = res;
            } catch (Exception ex) {
                if (MethodDatabase.isProblematicClass(className))
                    instrumentor.log(LogLevel.INFO, "Skipping problematic class instrumentation %s - %s %s", className, ex, Arrays.toString(ex.getStackTrace()));
                else
                    instrumentor.error("Unable to instrument " + className, ex);
            }
        }
        return entry;
    }
}
