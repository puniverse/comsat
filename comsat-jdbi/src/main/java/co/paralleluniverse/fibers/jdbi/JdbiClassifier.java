/*
 * COMSAT
 * Copyright (C) 2015, Parallel Universe Software Co. All rights reserved.
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
package co.paralleluniverse.fibers.jdbi;

import co.paralleluniverse.fibers.instrument.LogLevel;
import co.paralleluniverse.fibers.instrument.MethodDatabase;
import co.paralleluniverse.fibers.instrument.SimpleSuspendableClassifier;
import co.paralleluniverse.fibers.instrument.SuspendableClassifier;
import co.paralleluniverse.fibers.instrument.SuspendableHelper;

/**
 * Given classes and methodRegexps, Instrumenting all the extending methods in 
 * the scope of given package prefix.
 */
public class JdbiClassifier implements SuspendableClassifier {
    private static final String PKG_PREFIX = "org/skife/jdbi/v2";
    String[][] methodsArray = {
        {"org/skife/jdbi/v2/IDBI", "open"},
        {"org/skife/jdbi/v2/Handle", "execute", "update", "close", "begin", "commit", "rollback", "execute", "checkpoint", "release"},
        {"org/skife/jdbi/v2/ResultBearing", "first", "list"},
        {"org/skife/jdbi/v2/QueryResultMunger", "munge"},
        {"org/skife/jdbi/v2/Cleanable", "cleanup"},

        {"org/skife/jdbi/v2/sqlobject/Handler", "invoke"},
        {"org/skife/jdbi/v2/sqlobject/HandleDing", "getHandle", "release", "retain"},
        {"org/skife/jdbi/v2/sqlobject/UpdateHandler$Returner", "value"},
        {"org/skife/jdbi/v2/sqlobject/ResultReturnThing", "result"},

        {"org/skife/jdbi/v2/sqlobject/mixins/Transactional", "begin", "commit", "rollback", "checkpoint", "release"},

        {"org/skife/jdbi/v2/tweak/TransactionHandler", "begin", "commit", "rollback", "checkpoint", "release"},
        {"org/skife/jdbi/v2/tweak/ConnectionFactory", "openConnection"},
        {"org/skife/jdbi/v2/tweak/StatementCustomizer", "beforeExecution", "afterExecution", "cleanup"},
        {"org/skife/jdbi/v2/tweak/RewrittenStatement", "bind"},
        {"org/skife/jdbi/v2/tweak/ResultSetMapper", "map"},
        {"org/skife/jdbi/v2/tweak/ResultColumnMapper", "mapColumn"},
        {"org/skife/jdbi/v2/tweak/Argument", "apply"},
        {"org/skife/jdbi/v2/tweak/StatementBuilder", "create", "beforeExecution", "close"},
        {"org/skife/jdbi/v2/tweak/ConnectionFactory", "openConnection"},

        {"org/skife/jdbi/v2/util/TypedMapper", "extractByName", "extractByIndex"},
    };

    @Override
    public MethodDatabase.SuspendableType isSuspendable (
        MethodDatabase db,
        String sourceName, String sourceDebugInfo,
        boolean isInterface, String className, String superClassName, String[] interfaces,
        String methodName, String methodDesc, String methodSignature, String[] methodExceptions
    ) {
        // Declares given methods as supers
        for (String[] susExtendables : methodsArray) {
            if (isJDBICGLibProxy(className, methodName)) {
                return MethodDatabase.SuspendableType.SUSPENDABLE;
            } else if (className.equals(susExtendables[0]))
                for (int i = 1; i < susExtendables.length; i++) {
                    if (methodName.matches(susExtendables[i])) {
                        if (db.isVerbose())
                            db.getLog().log(LogLevel.INFO, JdbiClassifier.class.getName() + ": " + className + "." + methodName + " supersOrEqual " + susExtendables[0] + "." + susExtendables[i]);
                        return MethodDatabase.SuspendableType.SUSPENDABLE;
                    }
                }
        }

        // Declares extending classes in jooq packages as suspendables
        if (!className.startsWith(PKG_PREFIX))
            return null;
        for (String[] susExtendables : methodsArray) {
            if (SimpleSuspendableClassifier.extendsOrImplements(susExtendables[0], db, className, superClassName, interfaces))
                for (int i = 1; i < susExtendables.length; i++) {
                    if (methodName.matches(susExtendables[i])) {
                        if (db.isVerbose())
                            db.getLog().log(LogLevel.INFO, JdbiClassifier.class.getName() + ": " + className + "." + methodName + " extends " + susExtendables[0] + "." + susExtendables[i]);
                        return MethodDatabase.SuspendableType.SUSPENDABLE;
                    }
                }
        }
        return null;
    }

    private boolean isJDBICGLibProxy(String className, String methodName) {
        boolean ret = className.startsWith("org/skife/jdbi/v2/sqlobject/CloseInternalDoNotUseThisClass$$EnhancerByCGLIB$$") && !methodName.startsWith("<");
        if (ret)
            SuspendableHelper.addWaiver(className.replace("/", "."), methodName); // @Instrumented added during instrumentation seems not to stick to CGLIB proxies, let verification know
        return ret;
    }
}
