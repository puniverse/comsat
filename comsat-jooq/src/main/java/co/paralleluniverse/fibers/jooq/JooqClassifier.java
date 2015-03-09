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
        {"java/sql/Statement", "execute.*", "getResultSet"},
        {"java/sql/Connection", "prepareStatement"},
        {"org/jooq/Query", "execute"},
        {"org/jooq/ResultQuery", "getResult", "fetch.*"},
        {"org/jooq/impl/AbstractQuery", "prepare"}
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
