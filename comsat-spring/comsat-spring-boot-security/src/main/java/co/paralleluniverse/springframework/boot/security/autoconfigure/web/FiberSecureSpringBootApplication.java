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
package co.paralleluniverse.springframework.boot.security.autoconfigure.web;

import co.paralleluniverse.springframework.boot.autoconfigure.web.FiberSpringBootApplication;
import co.paralleluniverse.springframework.security.config.FiberSecurityContextHolderConfig;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Convenience annotation for fiber-enabled Spring Boot applications
 * 
 * @author circlespainter
 */
@Target(value = {ElementType.TYPE})
@Retention(value = RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Configuration
@FiberSpringBootApplication
// The following will enable security context inheritance for fibers
@Import(FiberSecurityContextHolderConfig.class)
public @interface FiberSecureSpringBootApplication {
    public Class<?>[] exclude() default {};
}
