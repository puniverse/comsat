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
 * Based on the corresponding class in Spring Boot Samples.
 * Copyright the original author(s).
 * Released under the ASF 2.0 license.
 */
package comsat.sample.undertow.web;

import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.Suspendable;
import java.util.concurrent.Callable;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import comsat.sample.undertow.service.HelloWorldService;

@RestController
public class SampleController {
    @Autowired
    private HelloWorldService helloWorldService;

    @RequestMapping("/")
    public String helloWorld() throws InterruptedException, SuspendExecution {
        return this.helloWorldService.getHelloMessage();
    }

    @RequestMapping("/async")
    public Callable<String> helloWorldAsync() throws SuspendExecution {
        return new Callable<String>() {
            @Override
            @Suspendable
            public String call() throws Exception {
                return "async: " + SampleController.this.helloWorldService.getHelloMessage();
            }
        };
    }
}
