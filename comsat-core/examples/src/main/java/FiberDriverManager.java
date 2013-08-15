
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.futures.AsyncListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author eitan
 */
public class FiberDriverManager {
    private final static ListeningExecutorService exec = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(10));

    public static Connection getConnection(final String url) throws SQLException, SuspendExecution {
        try {
            return AsyncListenableFuture.get(exec.submit(new Callable<Connection>() {
                @Override
                public Connection call() throws Exception {
                    return DriverManager.getConnection(url);
                }
            }));
        } catch (ExecutionException | InterruptedException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static Connection getConnection(final String url, final java.util.Properties info) throws SQLException, SuspendExecution {
        try {
            return AsyncListenableFuture.get(exec.submit(new Callable<Connection>() {
                @Override
                public Connection call() throws Exception {
                    return DriverManager.getConnection(url, info);
                }
            }));
        } catch (ExecutionException | InterruptedException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static Connection getConnection(final String url, final String user, final String password) throws SQLException, SuspendExecution {
        try {
            return AsyncListenableFuture.get(exec.submit(new Callable<Connection>() {
                @Override
                public Connection call() throws Exception {
                    return DriverManager.getConnection(url, user, password);
                }
            }));
        } catch (ExecutionException | InterruptedException ex) {
            throw new RuntimeException(ex);
        }
    }
}
