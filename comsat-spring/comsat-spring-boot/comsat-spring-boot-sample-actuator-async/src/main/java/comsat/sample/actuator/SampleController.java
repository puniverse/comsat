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
 * Based on the corresponding class in Spring Boot Samples.
 * Copyright the original author(s).
 * Released under the ASF 2.0 license.
 */
package comsat.sample.actuator;

import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.Suspendable;
import co.paralleluniverse.strands.SuspendableCallable;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import org.hibernate.validator.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Description;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.async.DeferredResult;

@Controller
@Description("A controller for handling requests for hello messages")
public class SampleController {

    @Autowired
    private HelloWorldService helloWorldService;

    private Callable<Map<String, String>> helloCallable(final DeferredResult<Map<String, String>> optDeferred) throws SuspendExecution {
        return new Callable<Map<String, String>>() {
            @Override
            @Suspendable
            public Map<String, String> call() throws Exception {
                try {
                    Fiber.sleep(100);
                    Map<String, String> ret = Collections.singletonMap("message", helloWorldService.getHelloMessage());
                    if (optDeferred != null) optDeferred.setResult(ret);
                    return ret;
                } catch (Throwable t) {
                    if (optDeferred != null) optDeferred.setErrorResult(t);
                    throw t;
                }
            }
        };
    }
    
    @RequestMapping(value = "/callable", method = RequestMethod.GET)
    @ResponseBody
    public Callable<Map<String, String>> helloCallable() throws SuspendExecution {
        return helloCallable(null);
    }
    
    @RequestMapping(value = "/deferred", method = RequestMethod.GET)
    @ResponseBody
    public DeferredResult<Map<String, String>> helloDeferred() {
        final DeferredResult<Map<String, String>> ret = new DeferredResult<>();
        new Fiber(new SuspendableCallable<Map<String, String>>() {
            @Override
            public Map<String, String> run() throws SuspendExecution, InterruptedException {
                try {
                    return helloCallable(ret).call();
                } catch (Exception ex) {
                    ret.setErrorResult(ret);
                    throw new RuntimeException(ex);
                }
            }
        }).start();
        return ret;
    }
    
    private Callable<Map<String, Object>> ollehCallable(final Message message, final DeferredResult<Map<String, Object>> optDeferred) throws SuspendExecution {
        return new Callable<Map<String, Object>>() {
            @Override
            @Suspendable
            public Map<String, Object> call() throws Exception {
                try {
                    Fiber.sleep(100);
                    Map<String, Object> model = new LinkedHashMap<>();
                    model.put("message", message.getValue());
                    model.put("title", "Hello Home");
                    model.put("date", new Date());
                    if (optDeferred != null) optDeferred.setResult(model);
                    return model;
                } catch (Throwable t) {
                    if (optDeferred != null) optDeferred.setErrorResult(t);
                    throw t;
                }
            }
        };
    }
    
    @RequestMapping(value = "/callable", method = RequestMethod.POST)
    @ResponseBody
    public Callable<Map<String, Object>> ollehCallable(final Message message) throws SuspendExecution {
        return ollehCallable(message, null);
    }
    
    @RequestMapping(value = "/deferred", method = RequestMethod.POST)
    @ResponseBody
    public DeferredResult<Map<String, Object>> ollehDeferred(@Validated final Message message) {
        final DeferredResult<Map<String, Object>> ret = new DeferredResult<>();
        new Fiber(new SuspendableCallable<Map<String, Object>>() {
            @Override
            public Map<String, Object> run() throws SuspendExecution, InterruptedException {
                try {
                    return ollehCallable(message, ret).call();
                } catch (Exception ex) {
                    ret.setErrorResult(ret);
                    throw new RuntimeException(ex);
                }
            }
        }).start();
        return ret;
    }

    private Callable<String> fooCallable(final DeferredResult<String> optDeferred) throws SuspendExecution {
        return new Callable<String>() {
            @Override
            @Suspendable
            public String call() throws Exception {
                try {
                    Fiber.sleep(100);
                    throw new IllegalArgumentException("Server error");
                } catch (Throwable t) {
                    if (optDeferred != null) optDeferred.setErrorResult(t);
                    throw t;
                }
            }
        };
    }
    
    @RequestMapping("/callable/foo")
    @ResponseBody
    public Callable<String> fooCallable() throws SuspendExecution {
        return fooCallable(null);
    }
    
    @RequestMapping("/deferred/foo")
    @ResponseBody
    public DeferredResult<String> fooDeferred() {
        final DeferredResult<String> ret = new DeferredResult<>();
        new Fiber(new SuspendableCallable<String>() {
            @Override
            public String run() throws SuspendExecution, InterruptedException {
                try {
                    return fooCallable(ret).call();
                } catch (Exception ex) {
                    ret.setErrorResult(ret);
                    throw new RuntimeException(ex);
                }
            }
        }).start();
        return ret;
    }

    protected static class Message {

        @NotBlank(message = "Message value cannot be empty")
        private String value;

        public String getValue() {
            return this.value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }
}
