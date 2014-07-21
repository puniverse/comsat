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

import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.Suspendable;
import co.paralleluniverse.fibers.dropwizard.MyDropwizardApp.MyConfig;
import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.sun.jersey.spi.resource.Singleton;
import io.dropwizard.Configuration;
import io.dropwizard.client.HttpClientConfiguration;
import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.setup.Environment;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.hibernate.validator.constraints.Length;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.IDBI;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.util.StringMapper;

@Singleton
@Path("/")
@Produces(MediaType.APPLICATION_JSON)
//snippet app
public class MyDropwizardApp extends FiberApplication<MyConfig> {
    private IDBI jdbi;
    private MyDAO dao;
    private HttpClient httpClient;

    @Override
    public void fiberRun(MyConfig config, Environment env) throws Exception {
        this.httpClient = new FiberHttpClientBuilder(env).
                using(config.getHttpClientConfiguration()).build("MyClient");
        this.jdbi = new FiberDBIFactory().build(env, config.getDB(), "MyDB");
        this.dao = jdbi.onDemand(MyDAO.class);
        env.jersey().register(MY_RESOURCE_OBJ);
    }
    // snippet_exclude_begin

    public static class MyConfig extends Configuration {
        @Valid
        @NotNull
        @JsonProperty
        private final HttpClientConfiguration httpClient = new HttpClientConfiguration();

        @JsonProperty
        public HttpClientConfiguration getHttpClientConfiguration() {
            return httpClient;
        }

        @Valid
        @NotNull
        @JsonProperty
        private final DataSourceFactory database = new DataSourceFactory();

        public DataSourceFactory getDB() {
            return database;
        }
    }

    public static class Saying {
        private long id;

        @Length(max = 3)
        private String content;

        public Saying() {
        }

        public Saying(long id, String content) {
            this.id = id;
            this.content = content;
        }

        @JsonProperty
        public long getId() {
            return id;
        }

        @JsonProperty
        public String getContent() {
            return content;
        }
    }

    @Suspendable
    public interface MyDAO {
        @SqlUpdate("create table if not exists something (id int primary key, name varchar(100))")
        void createSomethingTable();

        @SqlUpdate("drop table something")
        void dropSomethingTable();

        @SqlUpdate("insert into something (id, name) values (:id, :name)")
        void insert(@Bind("id") int id, @Bind("name") String name);

        @SqlQuery("select name from something where id = :id")
        String findNameById(@Bind("id") int id);
    }

    private final AtomicInteger ai = new AtomicInteger();

    @GET
    @Timed
    public Saying get(@QueryParam("name") Optional<String> name,
            @QueryParam("sleep") Optional<Integer> sleepParameter) throws InterruptedException, SuspendExecution {
        Fiber.sleep(sleepParameter.or(10));
        return new Saying(ai.incrementAndGet(), name.or("name"));
    }

    @GET
    @Path("/http")
    @Timed
    public String http(@QueryParam("name") Optional<String> name) throws InterruptedException, SuspendExecution, IOException {
        return httpClient.execute(new HttpGet("http://localhost:8080/?sleep=10&name=" + name.or("name")), new BasicResponseHandler());
    }

    @GET
    @Path("/fluent")
    @Timed
    public Integer fluentAPI(@QueryParam("id") Optional<Integer> id) throws InterruptedException, SuspendExecution, IOException {
        try (Handle h = jdbi.open()) {
            h.execute("create table if not exists fluentAPI (id int primary key, name varchar(100))");
            for (int i = 0; i < 100; i++)
                h.execute("insert into fluentAPI (id, name) values (?, ?)", i, "stranger " + i);
            int size = h.createQuery("select name from fluentAPI where id < :id order by id")
                    .bind("id", id.or(50)).map(StringMapper.FIRST).list().size();
            h.execute("drop table fluentAPI");
            return size;
        }
    }

    @GET
    @Path("/dao")
    @Timed
    public String dao(@QueryParam("id") Optional<Integer> id) throws InterruptedException, SuspendExecution, IOException {
        dao.createSomethingTable();
        for (int i = 0; i < 100; i++)
            dao.insert(i, "name" + i);
        String res = dao.findNameById(37);
        dao.dropSomethingTable();
        return res;
    }
    private Object MY_RESOURCE_OBJ = this;
    // snippet_exclude_end
}
// end of snippet
