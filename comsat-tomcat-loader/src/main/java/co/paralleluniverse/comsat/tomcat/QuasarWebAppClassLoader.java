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
 * http://tomcat.apache.org/tomcat-6.0-doc/config/loader.html
 *
 * @author pron
 */
public class QuasarWebAppClassLoader extends WebappClassLoader {
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
            public void error(String msg, Exception exc) {
                System.out.println("[quasar] ERROR: " + msg);
                exc.printStackTrace(System.out);
            }
        });
//        inst.setVerbose(true);
//        inst.setDebug(true);
        return inst;
    }

    private synchronized void initInstrumentor() {
        if (instrumentor == null)
            this.instrumentor = newInstrumentor();
    }

    @Override
    protected ResourceEntry findResourceInternal(String name, String path) {
        initInstrumentor();
        ResourceEntry entry = super.findResourceInternal(name, path);
        if (entry != null && path.endsWith(".class")) {
            String className = name.substring(0, name.length() - ".class".length());
            try {
                entry.binaryContent = instrumentor.instrumentClass(className, entry.binaryContent);
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
