package co.paralleluniverse.fibers.dropwizard;

import co.paralleluniverse.fibers.jdbc.FiberDataSource;
import io.dropwizard.db.ManagedDataSource;
import java.util.concurrent.ExecutorService;

public class FiberMangedDataSource extends FiberDataSource implements ManagedDataSource {
    private ManagedDataSource myds;

    public FiberMangedDataSource(ManagedDataSource ds, ExecutorService executor) {
        super(ds, executor);
        this.myds = ds;
    }

    public FiberMangedDataSource(ManagedDataSource ds, int numThreads) {
        super(ds, numThreads);
        this.myds = ds;
    }

    @Override
    public void start() throws Exception {
        myds.start();
    }

    @Override
    public void stop() throws Exception {
        myds.stop();
    }

}
