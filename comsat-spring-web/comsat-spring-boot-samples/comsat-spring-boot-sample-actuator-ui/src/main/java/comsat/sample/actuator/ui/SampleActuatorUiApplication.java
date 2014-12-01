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
 * Based on sample.actuator.ui.SampleActuatorApplication
 * in Spring Boot Samples.
 * Copyright the original author(s).
 * Released under the ASF 2.0 license.
 */
package comsat.sample.actuator.ui;

import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.springframework.web.servlet.config.annotation.FiberWebMvcAutoConfiguration;
import java.util.Date;
import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@EnableAutoConfiguration
@ComponentScan
@Controller
@Import(FiberWebMvcAutoConfiguration.class) // This will enable fiber-blocking while still preserving web autoconf.
public class SampleActuatorUiApplication {

    @RequestMapping("/")
    public String home(Map<String, Object> model) throws InterruptedException, SuspendExecution {
        Fiber.sleep(100);
        model.put("message", "Hello World");
        model.put("title", "Hello Home");
        model.put("date", new Date());
        return "home";
    }

    @RequestMapping("/foo")
    public String foo() throws InterruptedException, SuspendExecution {
        Fiber.sleep(100);
        throw new RuntimeException("Expected exception in controller");
    }

    public static void main(String[] args) throws Exception {
        SpringApplication.run(SampleActuatorUiApplication.class, args);
    }

    @Bean
    public SecurityProperties securityProperties() {
        SecurityProperties security = new SecurityProperties();
        security.getBasic().setPath(""); // empty so home page is unsecured
        return security;
    }

}
