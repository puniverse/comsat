package co.paralleluniverse.fibers.dropwizard;

import co.paralleluniverse.fibers.jdbi.FiberDBI;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.db.ManagedDataSource;
import io.dropwizard.jdbi.DBIFactory;
import io.dropwizard.setup.Environment;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.skife.jdbi.v2.IDBI;

public class FiberDBIFactory {
    private final ExecutorService es;
    private final DBIFactory builder;

    public FiberDBIFactory(final ExecutorService es) {
        this.builder = new DBIFactory();
        this.es = es;
    }

    public FiberDBIFactory(final int threadsNum) {
        this(Executors.newFixedThreadPool(threadsNum, new ThreadFactoryBuilder().setNameFormat("jdbc-worker-%d").setDaemon(true).build()));
    }

    public FiberDBIFactory() {
        this(10);
    }

    public IDBI build(Environment environment, DataSourceFactory dsFactory, ManagedDataSource dataSource, String name) {
        IDBI build = builder.build(environment, new FiberDataSourceFactory(dsFactory),
                FiberManagedDataSource.wrap(dataSource, es), name);
        return new FiberDBI(build, es);
    }

    public IDBI build(Environment environment, DataSourceFactory dsFactory, String name) throws ClassNotFoundException {
        IDBI build = builder.build(environment, new FiberDataSourceFactory(dsFactory), name);
        return new FiberDBI(build, es);
    }

}
