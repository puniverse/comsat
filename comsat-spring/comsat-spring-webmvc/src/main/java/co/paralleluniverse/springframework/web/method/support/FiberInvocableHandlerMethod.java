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
/*
 * Based on org.springframework.web.method.support.InvocableHandlerMethod in
 * Spring Framework Web MVC.
 * Copyright the original author Rossen Stoyanchev.
 * Released under the ASF 2.0 license.
 */
package co.paralleluniverse.springframework.web.method.support;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.GenericTypeResolver;
import org.springframework.core.MethodParameter;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.method.support.HandlerMethodArgumentResolverComposite;
import org.springframework.web.method.support.ModelAndViewContainer;

import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.SuspendableRunnable;
import java.util.concurrent.Callable;

// TODO subclass instead of this copy&paste horror when https://jira.spring.io/browse/SPR-12484 is released

/**
 * Provides a method for invoking the handler method for a given request after resolving its method argument
 * values through registered {@link HandlerMethodArgumentResolver}s.
 *
 * <p>
 * Argument resolution often requires a {@link WebDataBinder} for data binding or for type conversion.
 * Use the {@link #setDataBinderFactory(WebDataBinderFactory)} property to supply a binder factory to pass to
 * argument resolvers.
 *
 * <p>
 * Use {@link #setHandlerMethodArgumentResolvers(HandlerMethodArgumentResolverComposite)} to customize
 * the list of argument resolvers.
 *
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public class FiberInvocableHandlerMethod extends HandlerMethod {

    // TODO need avoiding async error handling due to https://bugs.eclipse.org/bugs/show_bug.cgi?id=454022, remove as sson as it's fixed
    private static final String SPRING_BOOT_ERROR_CONTROLLER_CLASS_NAME = "org.springframework.boot.autoconfigure.web.ErrorController";
    private static Class springBootErrorControllerClass;
    static {
        try {
            springBootErrorControllerClass = Class.forName(SPRING_BOOT_ERROR_CONTROLLER_CLASS_NAME);
        } catch( ClassNotFoundException e ) {}
    }    

    private WebDataBinderFactory dataBinderFactory;

    private HandlerMethodArgumentResolverComposite argumentResolvers = new HandlerMethodArgumentResolverComposite();

    private ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    /**
     * Create an instance from the given handler and method.
     */
    public FiberInvocableHandlerMethod(Object bean, Method method) {
        super(bean, method);
    }

    /**
     * Create an instance from a {@code HandlerMethod}.
     */
    public FiberInvocableHandlerMethod(HandlerMethod handlerMethod) {
        super(handlerMethod);
    }

    /**
     * Construct a new handler method with the given bean instance, method name and parameters.
     *
     * @param bean           the object bean
     * @param methodName     the method name
     * @param parameterTypes the method parameter types
     * @throws NoSuchMethodException when the method cannot be found
     */
    public FiberInvocableHandlerMethod(Object bean, String methodName, Class<?>... parameterTypes) throws NoSuchMethodException {
        super(bean, methodName, parameterTypes);
    }

    /**
     * Set the {@link WebDataBinderFactory} to be passed to argument resolvers allowing them to create
     * a {@link WebDataBinder} for data binding and type conversion purposes.
     *
     * @param dataBinderFactory the data binder factory.
     */
    public void setDataBinderFactory(WebDataBinderFactory dataBinderFactory) {
        this.dataBinderFactory = dataBinderFactory;
    }

    /**
     * Set {@link HandlerMethodArgumentResolver}s to use to use for resolving method argument values.
     */
    public void setHandlerMethodArgumentResolvers(HandlerMethodArgumentResolverComposite argumentResolvers) {
        this.argumentResolvers = argumentResolvers;
    }

    /**
     * Set the ParameterNameDiscoverer for resolving parameter names when needed
     * (e.g. default request attribute name).
     * <p>
     * Default is a {@link org.springframework.core.DefaultParameterNameDiscoverer}.
     */
    public void setParameterNameDiscoverer(ParameterNameDiscoverer parameterNameDiscoverer) {
        this.parameterNameDiscoverer = parameterNameDiscoverer;
    }

    /**
     * Invoke the method after resolving its argument values in the context of the given request.
     * <p>
     * Argument
     * values are commonly resolved through {@link HandlerMethodArgumentResolver}s. The {@code provideArgs}
     * parameter however may supply argument values to be used directly, i.e. without argument resolution.
     * Examples of provided argument values include a {@link WebDataBinder}, a {@link SessionStatus}, or
     * a thrown exception instance. Provided argument values are checked before argument resolvers.
     *
     * @param request      the current request
     * @param mavContainer the ModelAndViewContainer for this request
     * @param providedArgs "given" arguments matched by type, not resolved
     * @return the raw value returned by the invoked method
     * @exception Exception raised if no suitable argument resolver can be found, or the method raised an exception
     */
    protected final Object invokeForRequest(NativeWebRequest request, ModelAndViewContainer mavContainer,
            Object... providedArgs) throws Exception {

        Object[] args = getMethodArgumentValues(request, mavContainer, providedArgs);
        if (logger.isTraceEnabled()) {
            StringBuilder sb = new StringBuilder("Invoking [");
            sb.append(getBeanType().getSimpleName()).append(".");
            sb.append(getMethod().getName()).append("] method with arguments ");
            sb.append(Arrays.asList(args));
            logger.trace(sb.toString());
        }
        Object returnValue = invoke(args);
        if (logger.isTraceEnabled()) {
            logger.trace("Method [" + getMethod().getName() + "] returned [" + returnValue + "]");
        }
        return returnValue;
    }

    /**
     * Get the method argument values for the current request.
     */
    private Object[] getMethodArgumentValues(NativeWebRequest request, ModelAndViewContainer mavContainer,
            Object... providedArgs) throws Exception {

        MethodParameter[] parameters = getMethodParameters();
        Object[] args = new Object[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            MethodParameter parameter = parameters[i];
            parameter.initParameterNameDiscovery(this.parameterNameDiscoverer);
            GenericTypeResolver.resolveParameterType(parameter, getBean().getClass());
            args[i] = resolveProvidedArgument(parameter, providedArgs);
            if (args[i] != null) {
                continue;
            }
            if (this.argumentResolvers.supportsParameter(parameter)) {
                try {
                    args[i] = this.argumentResolvers.resolveArgument(
                            parameter, mavContainer, request, this.dataBinderFactory);
                    continue;
                } catch (Exception ex) {
                    if (logger.isTraceEnabled()) {
                        logger.trace(getArgumentResolutionErrorMessage("Error resolving argument", i), ex);
                    }
                    throw ex;
                }
            }
            if (args[i] == null) {
                String msg = getArgumentResolutionErrorMessage("No suitable resolver for argument", i);
                throw new IllegalStateException(msg);
            }
        }
        return args;
    }

    private String getArgumentResolutionErrorMessage(String message, int index) {
        MethodParameter param = getMethodParameters()[index];
        message += " [" + index + "] [type=" + param.getParameterType().getName() + "]";
        return getDetailedErrorMessage(message);
    }

    /**
     * Adds HandlerMethod details such as the controller type and method signature to the given error message.
     *
     * @param message error message to append the HandlerMethod details to
     */
    protected String getDetailedErrorMessage(String message) {
        StringBuilder sb = new StringBuilder(message).append("\n");
        sb.append("HandlerMethod details: \n");
        sb.append("Controller [").append(getBeanType().getName()).append("]\n");
        sb.append("Method [").append(getBridgedMethod().toGenericString()).append("]\n");
        return sb.toString();
    }

    /**
     * Attempt to resolve a method parameter from the list of provided argument values.
     */
    private Object resolveProvidedArgument(MethodParameter parameter, Object... providedArgs) {
        if (providedArgs == null) {
            return null;
        }
        for (Object providedArg : providedArgs) {
            if (parameter.getParameterType().isInstance(providedArg)) {
                return providedArg;
            }
        }
        return null;
    }

    /**
     * Invoke the handler method with the given argument values.
     */
    protected Object invoke(final Object... args) throws Exception {
        // TODO need avoiding async error handling due to https://bugs.eclipse.org/bugs/show_bug.cgi?id=454022, remove as sson as it's fixed
        if (springBootErrorControllerClass != null && springBootErrorControllerClass.isAssignableFrom(getBean().getClass())) {
            return blockingInvoke(args);
        } else {
            return fiberDispatchInvoke(args);
        }
    }

    /**
     * Assert that the target bean class is an instance of the class where the given
     * method is declared. In some cases the actual controller instance at request-
     * processing time may be a JDK dynamic proxy (lazy initialization, prototype
     * beans, and others). {@code @Controller}'s that require proxying should prefer
     * class-based proxy mechanisms.
     */
    protected final void assertTargetBean(Method method, Object targetBean, Object[] args) {
        Class<?> methodDeclaringClass = method.getDeclaringClass();
        Class<?> targetBeanClass = targetBean.getClass();
        if (!methodDeclaringClass.isAssignableFrom(targetBeanClass)) {
            String msg = "The mapped controller method class '" + methodDeclaringClass.getName()
                    + "' is not an instance of the actual controller bean instance '"
                    + targetBeanClass.getName() + "'. If the controller requires proxying "
                    + "(e.g. due to @Transactional), please use class-based proxying.";
            throw new IllegalStateException(getInvocationErrorMessage(msg, args));
        }
    }

    protected final String getInvocationErrorMessage(String message, Object[] resolvedArgs) {
        StringBuilder sb = new StringBuilder(getDetailedErrorMessage(message));
        sb.append("Resolved arguments: \n");
        for (int i = 0; i < resolvedArgs.length; i++) {
            sb.append("[").append(i).append("] ");
            if (resolvedArgs[i] == null) {
                sb.append("[null] \n");
            } else {
                sb.append("[type=").append(resolvedArgs[i].getClass().getName()).append("] ");
                sb.append("[value=").append(resolvedArgs[i]).append("]\n");
            }
        }
        return sb.toString();
    }

    protected Object blockingInvoke(Object... args) throws IllegalAccessException, Exception {
        ReflectionUtils.makeAccessible(getBridgedMethod());
        try {
            return getBridgedMethod().invoke(getBean(), args);
        } catch (IllegalArgumentException ex) {
            assertTargetBean(getBridgedMethod(), getBean(), args);
            throw new IllegalStateException(getInvocationErrorMessage(ex.getMessage(), args), ex);
        } catch (InvocationTargetException ex) {
            // Unwrap for HandlerExceptionResolvers ...
            Throwable targetException = ex.getTargetException();
            if (targetException instanceof RuntimeException) {
                throw (RuntimeException) targetException;
            } else if (targetException instanceof Error) {
                throw (Error) targetException;
            } else if (targetException instanceof Exception) {
                throw (Exception) targetException;
            } else {
                String msg = getInvocationErrorMessage("Failed to invoke controller method", args);
                throw new IllegalStateException(msg, targetException);
            }
        }
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
}
