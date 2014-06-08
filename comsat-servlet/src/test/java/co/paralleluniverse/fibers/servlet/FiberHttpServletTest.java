/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.fibers.servlet;

import co.paralleluniverse.common.util.Debug;
import co.paralleluniverse.embedded.EmbeddedServer;
import co.paralleluniverse.embedded.JettyServer;
import co.paralleluniverse.embedded.TomcatServer;
import co.paralleluniverse.embedded.UndertowServer;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.Strand;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collection;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.After;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class FiberHttpServletTest {

    @Parameterized.Parameters(name="{0}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
            {JettyServer.class},
            {TomcatServer.class},
            {UndertowServer.class},});
    }
    private final Class<? extends EmbeddedServer> cls;
    private EmbeddedServer instance;
    private CloseableHttpClient client;

    public FiberHttpServletTest(Class<? extends EmbeddedServer> cls) {
        this.cls = cls;
    }

    @Before
    public void setUp() throws Exception {
        this.instance = cls.newInstance();
        instance.addServlet("test", FiberTestServlet.class, "/");
        instance.addServlet("forward", FiberForwardServlet.class, "/forward");
        instance.addServlet("inline", FiberForwardServlet.class, "/inline");
        instance.start();
        this.client = HttpClients.custom().setDefaultRequestConfig(RequestConfig.custom()
                .setSocketTimeout(5000).setConnectTimeout(5000).setConnectionRequestTimeout(5000)
                .build()).build();
    }

    @After
    public void tearDown() throws Exception {
        instance.stop();
        client.close();
    }

    @Test
    public void testGet() throws IOException, InterruptedException, Exception {
        for (int i = 0; i < 10; i++)
            assertEquals("testGet", client.execute(new HttpGet("http://localhost:8080"), BASIC_RESPONSE_HANDLER));
    }

//    @Test
    // Fails on jetty after 5-10 iterations
    // Fails on tomcat after 1000-2000 iterations
    // Passes on undertow
    public void testForward() throws IOException, InterruptedException, Exception {
        for (int i = 0; i < 5000; i++)
            assertEquals("Faild on iteration "+i,"testGet", client.execute(new HttpGet("http://localhost:8080/forward"), BASIC_RESPONSE_HANDLER));
    }

//    @Test
    // Inline is not supported yet by fiberServlet
    public void testInline() throws IOException, InterruptedException, Exception {
        for (int i = 0; i < 10; i++)
            assertEquals("testInlinetestGet", client.execute(new HttpGet("http://localhost:8080/inline"), BASIC_RESPONSE_HANDLER));
    }

    @Test
    public void testPost() throws IOException, InterruptedException, Exception {
        for (int i = 0; i < 10; i++)
            assertEquals("testPost", client.execute(new HttpPost("http://localhost:8080"), BASIC_RESPONSE_HANDLER));
    }

    public static class FiberTestServlet extends FiberHttpServlet {
        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException, SuspendExecution {
            try (PrintWriter out = resp.getWriter()) {
                Strand.sleep(1);
                out.print("testPost");
            } catch (InterruptedException ex) {
            }
        }

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException, SuspendExecution {
            try (PrintWriter out = resp.getWriter()) {
                Strand.sleep(1);
                out.print("testGet");
            } catch (InterruptedException ex) {
            }
        }
    }

    public static class FiberForwardServlet extends FiberHttpServlet {
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException, SuspendExecution {
            try {
                Strand.sleep(1);
                getServletContext().getRequestDispatcher("/").forward(req, resp);
            } catch (InterruptedException ex) {
            }
        }
    }

    public static class FiberInlineServlet extends FiberHttpServlet {
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException, SuspendExecution {
            try (PrintWriter out = resp.getWriter()) {
                Strand.sleep(1);
                out.print("testInline");
                getServletContext().getRequestDispatcher("/").include(req, resp);
            } catch (InterruptedException ex) {
            }
        }
    }

    @Rule
    public TestName name = new TestName();
    @Rule
    public TestRule watchman = new TestWatcher() {
        @Override
        protected void starting(Description desc) {
            if (Debug.isDebug()) {
                System.out.println("STARTING TEST " + desc.getMethodName());
                Debug.record(0, "STARTING TEST " + desc.getMethodName());
            }
        }

        @Override
        public void failed(Throwable e, Description desc) {
            System.out.println("FAILED TEST " + desc.getMethodName() + ": " + e.getMessage());
            e.printStackTrace(System.err);
            if (Debug.isDebug() && !(e instanceof OutOfMemoryError)) {
                Debug.record(0, "EXCEPTION IN THREAD " + Thread.currentThread().getName() + ": " + e + " - " + Arrays.toString(e.getStackTrace()));
                Debug.dumpRecorder("~/quasar.dump");
            }
        }

        @Override
        protected void succeeded(Description desc) {
            Debug.record(0, "DONE TEST " + desc.getMethodName());
        }
    };
    private static final BasicResponseHandler BASIC_RESPONSE_HANDLER = new BasicResponseHandler();

}
