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
package co.paralleluniverse.fibers.jersey;

import co.paralleluniverse.fibers.instrument.Retransform;
import co.paralleluniverse.fibers.instrument.SuspendableClassifier;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author eitan
 */
public class JerseySuspendables implements SuspendableClassifier {
    private static final String[] suspandables = {
        "org.glassfish.jersey.process.Inflector.apply",
        "org.glassfish.jersey.server.model.internal.ResourceMethodInvocationHandlerFactory$1.invoke",
        "org.glassfish.jersey.server.model.internal.AbstractJavaResourceMethodDispatcher.invoke",
        "org.glassfish.jersey.server.model.internal.AbstractJavaResourceMethodDispatcher.dispatch",
        "org.glassfish.jersey.server.model.internal.JavaResourceMethodDispatcherProvider$ResponseOutInvoker.doDispatch",
        "org.glassfish.jersey.server.model.internal.JavaResourceMethodDispatcherProvider$TypeOutInvoker.doDispatch",
        "org.glassfish.jersey.server.model.ResourceMethodInvoker.invoke",
        "org.glassfish.jersey.server.model.ResourceMethodInvoker.apply",
        "org.glassfish.jersey.server.ServerRuntime$1.run",
        "org.glassfish.jersey.internal.Errors$1.call",
        "org.glassfish.jersey.internal.Errors.process",
        "org.glassfish.jersey.process.internal.RequestScope.runInScope",
        "org.glassfish.jersey.server.ServerRuntime.process",
        "org.glassfish.jersey.server.ApplicationHandler.handle",
        "org.glassfish.jersey.servlet.WebComponent.service",
        "org.glassfish.jersey.servlet.ServletContainer.service"
    };
    private static final String[] waivers = {};
    ///////////////////////////////
    private static final Set<String> suspendableSet;

    static {
        suspendableSet = new HashSet<String>(20);
        for (String s : suspandables) {
            final int index = s.lastIndexOf('.');
            String className = s.substring(0, index).replace('.', '/');
            String methodName = s.substring(index + 1);
            suspendableSet.add(className + '.' + methodName);
        }

        for (String s : waivers) {
            final int index = s.lastIndexOf('.');
            String className = s.substring(0, index).replace('.', '/');
            String methodName = s.substring(index + 1);
            Retransform.addWaiver(className, methodName);
        }
    }

    @Override
    public boolean isSuspendable(String className, String superClassName, String[] interfaces, String methodName, String methodDesc, String methodSignature, String[] methodExceptions) {
        final boolean res = suspendableSet.contains(className + '.' + methodName);
//        if (res)
//            System.out.println("XXXXX " + className + "." + methodName);
        return res;
    }
}
