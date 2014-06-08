package co.paralleluniverse.fibers.dropwizard;

import com.google.common.base.Function;
import com.sun.jersey.api.core.ResourceConfig;
import io.dropwizard.Application;
import io.dropwizard.Configuration;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import javax.servlet.Servlet;

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
                return new FiberServletContainer(environment.getJerseyServletContainer());
            }
        });
    }

    public abstract void fiberRun(T configuration, Environment environment) throws Exception;
}
