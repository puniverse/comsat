/*
 * COMSAT
 * Copyright (C) 2014, Parallel Universe Software Co. All rights reserved.
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
package co.paralleluniverse.fibers.jooq;

import co.paralleluniverse.fibers.instrument.LogLevel;
import co.paralleluniverse.fibers.instrument.MethodDatabase;
import co.paralleluniverse.fibers.instrument.SimpleSuspendableClassifier;
import co.paralleluniverse.fibers.instrument.SuspendableClassifier;

/**
 * Given classes and methodRegexps, Instrumenting all the extending methods in 
 * the scope of given package prefix.
 */
public class JooqClassifier implements SuspendableClassifier {
    private static final String PKG_PREFIX = "org/jooq";
    String[][] methodsArray = {
        {"java/util/Iterator", "hasNext"},
        {"java/sql/Statement", ".*"},
        {"java/sql/Connection", ".*"},
        {"org/jooq/Context", "visit", "bindValue"},
        {"org/jooq/Binding", "register", "get", "set"},
        {"org/jooq/QueryPartInternal", "accept", "bind"},
        {"org/jooq/BindContext", "bind", "bindValue", "bindValues"},
        {"org/jooq/Query", "execute"},
        {"org/jooq/ResultQuery", "getResult", "fetch.*"},
        {"org/jooq/Cursor", "fetch.*", "hasNext"},

        {"org/jooq/impl/RecordOperation", "operate"},

        {"org/jooq/impl/AbstractField", "accept"},
        {"org/jooq/impl/AbstractQuery", "prepare"},
        {"org/jooq/impl/AbstractContext", "visit0"},
        {"org/jooq/impl/AbstractBindContext", "bindValue0", "bindInternal"},
        {"org/jooq/impl/AbstractStoreQuery", "accept0"},

        {"org/jooq/impl/InsertQueryImpl", "toSQLInsert"},
        {"org/jooq/impl/SelectQueryImpl", "toSQLReference0", "toSQLReferenceLimitDefault"},
        {"org/jooq/impl/CursorImpl$CursorIterator", "fetch.*"},
        {"org/jooq/impl/CursorImpl$CursorIterator", "hasNext"},
        {"org/jooq/impl/CursorImpl$CursorIterator$CursorRecordInitialiser", "setValue"},
        {"org/jooq/impl/CursorImpl$CursorResultSet", ".*"},
        {"org/jooq/impl/RecordDelegate", "operate"},

        {"org/jooq/impl/Utils", "safeClose", "consumeWarnings", "fetch.*"},
    };

    @Override
    public MethodDatabase.SuspendableType isSuspendable (
        MethodDatabase db,
        String sourceName, String sourceDebugInfo,
        boolean isInterface, String className, String superClassName, String[] interfaces,
        String methodName, String methodDesc, String methodSignature, String[] methodExceptions
    ) {
        // declares given methods as supers
        for (String[] susExtendables : methodsArray) {
            if (className.equals(susExtendables[0]))
                for (int i = 1; i < susExtendables.length; i++) {
                    if (methodName.matches(susExtendables[i])) {
                        if (db.isVerbose())
                            db.getLog().log(LogLevel.INFO, JooqClassifier.class.getName() + ": " + className + "." + methodName + " supersOrEqual " + susExtendables[0] + "." + susExtendables[i]);
                        return MethodDatabase.SuspendableType.SUSPENDABLE;
                    }
                }
        }

        // declares extending classes in jooq packacages as suspandables
        if (!className.startsWith(PKG_PREFIX))
            return null;
        for (String[] susExtendables : methodsArray) {
            if (SimpleSuspendableClassifier.extendsOrImplements(susExtendables[0], db, className, superClassName, interfaces))
                for (int i = 1; i < susExtendables.length; i++) {
                    if (methodName.matches(susExtendables[i])) {
                        if (db.isVerbose())
                            db.getLog().log(LogLevel.INFO, JooqClassifier.class.getName() + ": " + className + "." + methodName + " extends " + susExtendables[0] + "." + susExtendables[i]);
                        return MethodDatabase.SuspendableType.SUSPENDABLE;
                    }
                }
        }
        return null;
    }
}
