package io.dropwizard.server;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.common.collect.ImmutableMap;
import io.dropwizard.jetty.ConnectorFactory;
import io.dropwizard.jetty.ContextRoutingHandler;
import io.dropwizard.jetty.HttpConnectorFactory;
import io.dropwizard.setup.Environment;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.thread.ThreadPool;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

@JsonTypeName("simple")
public class SimpleServerFactory extends AbstractServerFactory {
    @Valid
    @NotNull
    private ConnectorFactory connector = HttpConnectorFactory.application();

    @NotEmpty
    private String applicationContextPath = "/application";

    @NotEmpty
    private String adminContextPath = "/admin";

    @JsonProperty
    public ConnectorFactory getConnector() {
        return connector;
    }

    @JsonProperty
    public void setConnector(ConnectorFactory factory) {
        this.connector = factory;
    }

    @JsonProperty
    public String getApplicationContextPath() {
        return applicationContextPath;
    }

    @JsonProperty
    public void setApplicationContextPath(String contextPath) {
        this.applicationContextPath = contextPath;
    }

    @JsonProperty
    public String getAdminContextPath() {
        return adminContextPath;
    }

    @JsonProperty
    public void setAdminContextPath(String contextPath) {
        this.adminContextPath = contextPath;
    }

    @Override
    public Server build(Environment environment) {
        printBanner(environment.getName());
        final ThreadPool threadPool = createThreadPool(environment.metrics());
        final Server server = buildServer(environment.lifecycle(), threadPool);

        environment.getApplicationContext().setContextPath(applicationContextPath);
        final Handler applicationHandler = createAppServlet(server,
                                                            environment.jersey(),
                                                            environment.getObjectMapper(),
                                                            environment.getValidator(),
                                                            environment.getApplicationContext(),
                                                            environment.getJerseyServlet(),
                                                            environment.metrics());

        environment.getAdminContext().setContextPath(adminContextPath);
        final Handler adminHandler = createAdminServlet(server,
                                                        environment.getAdminContext(),
                                                        environment.metrics(),
                                                        environment.healthChecks());

        final Connector conn = connector.build(server,
                                               environment.metrics(),
                                               environment.getName(),
                                               null);

        server.addConnector(conn);

        final ContextRoutingHandler routingHandler = new ContextRoutingHandler(ImmutableMap.of(
                applicationContextPath, applicationHandler,
                adminContextPath, adminHandler
        ));
        server.setHandler(addStatsHandler(addRequestLog(server, routingHandler, environment.getName())));

        return server;
    }
}
