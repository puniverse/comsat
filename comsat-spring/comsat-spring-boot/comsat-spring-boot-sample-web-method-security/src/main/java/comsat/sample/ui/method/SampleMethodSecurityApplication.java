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
package comsat.sample.ui.method;

import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.Suspendable;
import java.util.Date;
import java.util.Map;

import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configurers.GlobalAuthenticationConfigurerAdapter;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import co.paralleluniverse.springframework.boot.security.autoconfigure.web.FiberSecureSpringBootApplication;

@FiberSecureSpringBootApplication // This will enable fiber-blocking
@EnableGlobalMethodSecurity(securedEnabled = true)
public class SampleMethodSecurityApplication extends WebMvcConfigurerAdapter {
    @Controller
    protected static class HomeController {
        // Unfortunately Spring method security proxies classes in such a way that declared exceptions
        // get a generated, specific catch block which, for SuspendExecution prevents Quasar from instrumenting;
        // this means the exception has to be catched and the method annotated in this special case
        @RequestMapping("/")
        @Secured("ROLE_ADMIN")
        @Suspendable
        public String home(Map<String, Object> model) throws InterruptedException {
            try {
                Fiber.sleep(10);
            } catch (SuspendExecution se) {
                throw new AssertionError(se);
            }
            model.put("message", "Hello World");
            model.put("title", "Hello Home");
            model.put("date", new Date());
            return "home";
        }
    }

    public static void main(String[] args) throws Exception {
        new SpringApplicationBuilder(SampleMethodSecurityApplication.class).run(args);
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/login").setViewName("login");
        registry.addViewController("/access").setViewName("access");
    }

    @Bean
    public ApplicationSecurity applicationSecurity() {
        return new ApplicationSecurity();
    }

    @Order(Ordered.HIGHEST_PRECEDENCE)
    @Configuration
    protected static class AuthenticationSecurity extends GlobalAuthenticationConfigurerAdapter {
        @Override
        public void init(AuthenticationManagerBuilder auth) throws Exception {
            // @formatter:off
            auth.inMemoryAuthentication().withUser("admin").password("admin")
                    .roles("ADMIN", "USER").and().withUser("user").password("user")
                    .roles("USER");
            // @formatter:on
        }
    }

    @Order(SecurityProperties.ACCESS_OVERRIDE_ORDER)
    protected static class ApplicationSecurity extends WebSecurityConfigurerAdapter {
        @Override
        protected void configure(HttpSecurity http) throws Exception {
            // @formatter:off
            http.authorizeRequests().antMatchers("/login").permitAll().anyRequest()
                    .fullyAuthenticated().and().formLogin().loginPage("/login")
                    .failureUrl("/login?error").and().logout()
                    .logoutRequestMatcher(new AntPathRequestMatcher("/logout")).and()
                    .exceptionHandling().accessDeniedPage("/access?error");
            // @formatter:on
        }
    }
}
