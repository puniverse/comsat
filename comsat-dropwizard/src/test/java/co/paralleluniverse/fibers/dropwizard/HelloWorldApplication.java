package co.paralleluniverse.fibers.dropwizard;

import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.Suspendable;
import co.paralleluniverse.fibers.dropwizard.HelloWorldApplication.HelloWorldConfiguration;
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
public class HelloWorldApplication extends FiberApplication<HelloWorldConfiguration> {
    private IDBI jdbi;
    private MyDAO dao;
    private HttpClient httpClient;

    @Override
    public void fiberRun(HelloWorldConfiguration configuration,
            final Environment environment) throws ClassNotFoundException {

        this.httpClient = new FiberHttpClientBuilder(environment).
                using(configuration.getHttpClientConfiguration()).
                build("FiberHttpClient");

        this.jdbi = new FiberDBIFactory().build(environment, configuration.getDatabase(), "postgresql");
        this.dao = jdbi.onDemand(MyDAO.class);
        environment.jersey().register(this);
    }

    public static class HelloWorldConfiguration extends Configuration {
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

        public DataSourceFactory getDatabase() {
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
}
