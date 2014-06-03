package co.paralleluniverse.fibers.jooq;

import co.paralleluniverse.fibers.instrument.LogLevel;
import co.paralleluniverse.fibers.instrument.MethodDatabase;
import co.paralleluniverse.fibers.instrument.SimpleSuspendableClassifier;
import co.paralleluniverse.fibers.instrument.SuspendableClassifier;

public class JooqClassifier implements SuspendableClassifier {
    String[][] methodsArray = {
        {"java/sql/Statement", "execute.*"},
        {"java/sql/Connection", "prepareStatement", "execute.*", "getResultSet"},
        {"org/jooq/Query", "execute"},
        {"org/jooq/ResultQuery", "getResult", "fetch.*"},
        {"org/jooq/impl/AbstractQuery", "prepare"}
    };

    public JooqClassifier() {
    }

    @Override
    public MethodDatabase.SuspendableType isSuspendable(MethodDatabase db, String className, String superClassName, String[] interfaces, String methodName, String methodDesc, String methodSignature, String[] methodExceptions) {
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
        if (!className.startsWith("org/jooq"))
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
