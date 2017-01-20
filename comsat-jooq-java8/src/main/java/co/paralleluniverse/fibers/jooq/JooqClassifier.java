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
    private String[][] methodsArray = {

        {"java/sql/Connection", ".*"},
        {"java/sql/Statement", ".*"},

        {"java/util/Iterator", "hasNext"},
        {"java/util/concurrent/ForkJoinPool", "managedBlock"},
        {"java/util/concurrent/ForkJoinPool$ManagedBlocker", "block"},
        {"java/util/function/Supplier", "get"},
        {"java/util/function/Supplier", "get"},

        {"org/jooq/BindContext", "bind", "bindValue", "bindValues"},
        {"org/jooq/Binding", "get.*"},
        {"org/jooq/Binding", "register", "get", "set"},
        {"org/jooq/ConnectionProvider", "acquire", "release"},
        {"org/jooq/Context", "visit", "bindValue"},
        {"org/jooq/Cursor", "fetch.*", "hasNext"},
        {"org/jooq/DSLContext", "fetch.*", "execute.*", "transaction.*"},
        {"org/jooq/DeleteResultStep", "fetch.*"},
        {"org/jooq/ExecuteContext", "connection"},
        {"org/jooq/InsertResultStep", "fetch.*"},
        {"org/jooq/Query", "execute"},
        {"org/jooq/QueryPartInternal", "accept", "bind"},
        {"org/jooq/ResultQuery", "getResult", "fetch.*"},
        {"org/jooq/TransactionProvider", "begin", "rollback", "commit"},
        {"org/jooq/TransactionalCallable", "run"},
        {"org/jooq/TransactionalRunnable", "run"},
        {"org/jooq/UpdateResultStep", "fetch.*"},

        {"org/jooq/impl/AbstractBindContext", "bindValue0", "bindInternal"},
        {"org/jooq/impl/AbstractContext", "visit0"},
        {"org/jooq/impl/AbstractDMLQuery", "accept0", "selectReturning"},
        {"org/jooq/impl/AbstractField", "accept"},
        {"org/jooq/impl/AbstractQuery", "execute"},
        {"org/jooq/impl/AbstractQuery", "prepare"},
        {"org/jooq/impl/AbstractResultQuery", "getFields"},
        {"org/jooq/impl/AbstractStoreQuery", "accept0"},
        {"org/jooq/impl/CursorImpl", "close"},
        {"org/jooq/impl/CursorImpl$CursorIterator", "fetch.*"},
        {"org/jooq/impl/CursorImpl$CursorIterator", "hasNext"},
        {"org/jooq/impl/CursorImpl$CursorIterator$CursorRecordInitialiser", "setValue"},
        {"org/jooq/impl/CursorImpl$CursorResultSet", ".*"},
        {"org/jooq/impl/DSL", "using"},
        {"org/jooq/impl/DefaultConnectionProvider", "rollback", "commit", "getAutoCommit", "setAutoCommit", "setSavepoint", "releaseSavepoint"},
        {"org/jooq/impl/DefaultTransactionProvider", "connection", "brace", "autoCommit", "setSavepoint"},
        {"org/jooq/impl/InsertQueryImpl", "toSQLInsert"},
        {"org/jooq/impl/MetaDataFieldProvider", "init"},
        {"org/jooq/impl/RecordDelegate", "operate"},
        {"org/jooq/impl/RecordOperation", "operate"},
        {"org/jooq/impl/SelectQueryImpl", "toSQLReference0", "toSQLReferenceLimitDefault"},
        {"org/jooq/impl/Tools", "consumeWarnings", "safeClose", "fetch.*", },
        {"org/jooq/impl/Utils", "safeClose", "consumeWarnings", "fetch.*"},

        {"org/jooq/tools/jdbc/JDBCUtils", "dialect", "safeClose", "wasNull"}
    };

    @Override
    public MethodDatabase.SuspendableType isSuspendable (
        MethodDatabase db,
        String sourceName, String sourceDebugInfo,
        boolean isInterface, String className, String superClassName, String[] interfaces,
        String methodName, String methodDesc, String methodSignature, String[] methodExceptions
    ) {
        // skipping ctors to avoid unnecessary instrumentation errors
        if (methodName.charAt(0) == '<')
            return null;

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

        // declares extending classes in jooq packages as suspendables
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
