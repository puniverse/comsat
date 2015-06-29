/*
 * COMSAT
 * Copyright (c) 2013-2015, Parallel Universe Software Co. All rights reserved.
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
/*
 * Based on org.springframework.web.method.support.InvocableHandlerMethod in
 * Spring Framework Web MVC.
 * Copyright the original author Rossen Stoyanchev.
 * Released under the ASF 2.0 license.
 */
package co.paralleluniverse.springframework.web.method.support;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.springframework.util.ReflectionUtils;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.method.HandlerMethod;

import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.Suspendable;
import co.paralleluniverse.fibers.instrument.SuspendableHelper;
import co.paralleluniverse.strands.SuspendableRunnable;
import java.util.Arrays;
import java.util.concurrent.Callable;
import org.springframework.web.method.support.InvocableHandlerMethod;

/**
 * A fiber-blocking {@link InvocableHandlerMethod}
 *
 * @author circlespainter
 */
public class FiberInvocableHandlerMethod extends InvocableHandlerMethod {
    // TODO need avoiding async error handling due to https://bugs.eclipse.org/bugs/show_bug.cgi?id=454022, remove as sson as it's fixed
    private static final String SPRING_BOOT_ERROR_CONTROLLER_CLASS_NAME = "org.springframework.boot.autoconfigure.web.ErrorController";
    private static Class springBootErrorControllerClass;
    static {
        try {
            springBootErrorControllerClass = Class.forName(SPRING_BOOT_ERROR_CONTROLLER_CLASS_NAME);
        } catch( ClassNotFoundException e ) {}
    }    

    public FiberInvocableHandlerMethod(Object bean, Method method) {
        super(bean, method);
    }

    public FiberInvocableHandlerMethod(HandlerMethod handlerMethod) {
        super(handlerMethod);
    }

    public FiberInvocableHandlerMethod(Object bean, String methodName, Class<?>... parameterTypes) throws NoSuchMethodException {
        super(bean, methodName, parameterTypes);
    }

    /**
     * @return `true` if the controller is a Spring Boot error controller
     */
    private boolean isBootErrorController() {
        return springBootErrorControllerClass != null && springBootErrorControllerClass.isAssignableFrom(getBean().getClass());
    }

    /**
     * @return `true` if the method is not annotated with `@Suspendable` and it doesn't throw `SuspendExecution` nor it is instrumented in any other way.
     */
    private boolean isSpringTraditionalThreadBlockingControllerMethod() {
        final Method m = getMethod();
        return
            m.getAnnotation(Suspendable.class) == null &&
            !Arrays.asList(m.getExceptionTypes()).contains(SuspendExecution.class) &&
            !SuspendableHelper.isInstrumented(m);
    }
    
    /**
     * Invoke the handler method with the given argument values, either traditional thread-blocking or in a new fiber.
     */
    @Override
    protected Object doInvoke(final Object... args) throws Exception {
        if (isBootErrorController() // TODO need avoiding async error handling due to https://bugs.eclipse.org/bugs/show_bug.cgi?id=454022, remove as sson as it's fixed
            || isSpringTraditionalThreadBlockingControllerMethod())
            return threadBlockingInvoke(args);
        else
            return fiberDispatchInvoke(args);
    }

    protected Object threadBlockingInvoke(Object... args) throws IllegalAccessException, Exception {
        return super.doInvoke(args);
    }

    protected Object fiberDispatchInvoke(final Object... args) {
        final Object b = getBean();
        final Method m = getBridgedMethod();
        ReflectionUtils.makeAccessible(m);
        
        // Returning deferred even for normal return values, Spring return handlers will take care dynamically based on the actual returned value
        final DeferredResult ret = new DeferredResult();
        
        // The actual method execution and deferred completion is dispatched to a new fiber
        new Fiber(new SuspendableRunnable() {
            private Object deAsync(final Object o) throws SuspendExecution, Exception {
                if (o instanceof Callable)
                    return deAsync(((Callable) o).call());
                else
                    return o;
            }
    
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                try {
                    Object originalRet = m.invoke(b, args);
                    ret.setResult(deAsync(originalRet));
                } catch (IllegalArgumentException ex) {
                    assertTargetBean(m, b, args);
                    ret.setErrorResult(new IllegalStateException(getInvocationErrorMessage(ex.getMessage(), args), ex));
                } catch (InvocationTargetException ex) {
                    // Unwrap for HandlerExceptionResolvers ...
                    Throwable targetException = ex.getTargetException();
                    if (targetException instanceof RuntimeException || targetException instanceof Error || targetException instanceof Exception) {
                        ret.setErrorResult(targetException);
                    } else {
                        String msg = getInvocationErrorMessage("Failed to invoke controller method", args);
                        ret.setErrorResult(new IllegalStateException(msg, targetException));
                    }
                } catch (Exception ex) {
                    ret.setErrorResult(ex);
                }
            }
        }).start();

        return ret;
    }

    /**
     * Assert that the target bean class is an instance of the class where the given
     * method is declared. In some cases the actual controller instance at request-
     * processing time may be a JDK dynamic proxy (lazy initialization, prototype
     * beans, and others). {@code @Controller}'s that require proxying should prefer
     * class-based proxy mechanisms.
     */
    private void assertTargetBean(Method method, Object targetBean, Object[] args) {
        Class<?> methodDeclaringClass = method.getDeclaringClass();
        Class<?> targetBeanClass = targetBean.getClass();
        if (!methodDeclaringClass.isAssignableFrom(targetBeanClass)) {
            String msg = "The mapped controller method class '" + methodDeclaringClass.getName() +
                         "' is not an instance of the actual controller bean instance '" +
                         targetBeanClass.getName() + "'. If the controller requires proxying " +
                         "(e.g. due to @Transactional), please use class-based proxying.";
            throw new IllegalStateException(getInvocationErrorMessage(msg, args));
        }
    }

    private String getInvocationErrorMessage(String message, Object[] resolvedArgs) {
        StringBuilder sb = new StringBuilder(getDetailedErrorMessage(message));
        sb.append("Resolved arguments: \n");
        for (int i=0; i < resolvedArgs.length; i++) {
            sb.append("[").append(i).append("] ");
            if (resolvedArgs[i] == null) {
                sb.append("[null] \n");
            }
            else {
                sb.append("[type=").append(resolvedArgs[i].getClass().getName()).append("] ");
                sb.append("[value=").append(resolvedArgs[i]).append("]\n");
            }
        }
        return sb.toString();
    }
}
