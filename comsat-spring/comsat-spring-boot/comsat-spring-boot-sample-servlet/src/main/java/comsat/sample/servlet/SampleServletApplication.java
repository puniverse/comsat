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
package comsat.sample.servlet;

import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.Suspendable;
import java.io.IOException;

import co.paralleluniverse.fibers.servlet.FiberHttpServlet;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.web.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableAutoConfiguration
public class SampleServletApplication extends SpringBootServletInitializer {
    @SuppressWarnings("serial")
    @Bean
    public Servlet dispatcherServlet() {
        return new FiberHttpServlet() {
            @Override
            @Suspendable
            public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
                try {
                    Fiber.sleep(10);
                    res.setContentType("text/plain");
                    res.getWriter().append("Hello World");
                } catch (InterruptedException | SuspendExecution ex) {
                    throw new AssertionError(ex);
                }
            }
        };
    }

    public static void main(String[] args) throws Exception {
        SpringApplication.run(SampleServletApplication.class, args);
    }

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(SampleServletApplication.class);
    }
}
