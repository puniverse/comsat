/*
 * COMSAT
 * Copyright (C) 2014, Parallel Universe Software Co. All rights reserved.
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
package co.paralleluniverse.fibers.dropwizard;

import com.google.common.base.Function;
import io.dropwizard.Application;
import io.dropwizard.Configuration;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import javax.servlet.Servlet;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;

public abstract class FiberApplication<T extends Configuration> extends Application<T> {
    @Override
    public void initialize(Bootstrap<T> bootstrap) {
    }

    @Override
    public final void run(T configuration, final Environment environment) throws Exception {
        fiberRun(configuration, environment);
        environment.jersey().replace(new Function<ResourceConfig, Servlet>() {
            @Override
            public Servlet apply(ResourceConfig f) {
                return new FiberServletContainer((ServletContainer) environment.getJerseyServletContainer());
            }
        });
    }

    public abstract void fiberRun(T configuration, Environment environment) throws Exception;
}
