package co.paralleluniverse.fibers.jersey;

import java.io.IOException;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.ext.Provider;

import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.Suspendable;

@Provider
public class AddTestFiltersFeature implements Feature {

    @Override
    public boolean configure(FeatureContext context) {
        context.register(TestRequestFilter.class);
        context.register(TestResponseFilter.class);
        return true;
    }

}

class TestRequestFilter implements ContainerRequestFilter {

    @Override
    @Suspendable
    public void filter(ContainerRequestContext requestContext) throws IOException {
        try {
            Fiber.sleep(5);
            requestContext.getHeaders().add(FiberServletContainerTest.REQUEST_FILTER_HEADER,
                    FiberServletContainerTest.REQUEST_FILTER_HEADER_VALUE);
        } catch (InterruptedException | SuspendExecution e) {
            throw new Error(e);
        }
    }
}

class TestResponseFilter implements ContainerResponseFilter {

    @Override
    @Suspendable
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext)
            throws IOException {
        try {
            Fiber.sleep(5);
            responseContext.getHeaders().add(FiberServletContainerTest.RESPONSE_FILTER_HEADER,
                    FiberServletContainerTest.RESPONSE_FILTER_HEADER_VALUE);
        } catch (InterruptedException | SuspendExecution e) {
            throw new Error(e);
        }
    }
}
