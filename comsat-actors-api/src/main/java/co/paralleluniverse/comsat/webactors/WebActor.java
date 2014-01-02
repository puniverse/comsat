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
package co.paralleluniverse.comsat.webactors;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A class annotated with this annotation will be automatically loaded by COMSAT
 * and spawned as a web actor.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface WebActor {
    /**
     * The name of the WebActor.
     */
    String name() default "";

    /**
     * Display name of this WebActor, if present.
     */
    String displayName() default "";

    /**
     * Description of this WebActor, if present.
     */
    String description() default "";

    /**
     * Array of HTTP URL patterns to which this WebActor applies.
     * For example {@code /myservice}, or {@code /myservice/*}.
     */
    String[] httpUrlPatterns() default {};

    /**
     * Array of WebSocket URI patterns to which this WebActor applies.
     * For example {@code /myservice/ws}, or {@code /myservice/ws/*}.
     */
    String[] webSocketUrlPatterns() default {};

    /**
     * A convenience property, to allow extremely simple annotation of a class -
     * Array of HTTP URL patterns.
     *
     * @see #httpUrlPatterns()
     */
    String[] value() default {};
}
