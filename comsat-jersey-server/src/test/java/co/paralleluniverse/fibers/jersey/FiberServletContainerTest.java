/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.fibers.jersey;

import co.paralleluniverse.common.util.Debug;
import co.paralleluniverse.embedded.EmbeddedServer;
import co.paralleluniverse.embedded.JettyServer;
import co.paralleluniverse.embedded.TomcatServer;
import co.paralleluniverse.embedded.UndertowServer;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
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
public class FiberServletContainerTest {

    @Parameterized.Parameters(name="{0}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
            {JettyServer.class},
            {TomcatServer.class},
            {UndertowServer.class},
        });
    }
    private final Class<? extends EmbeddedServer> cls;
    private EmbeddedServer instance;
    private CloseableHttpClient client;
    private static final String PARAM_JERSEY_PACKAGES = "jersey.config.server.provider.packages";
    private static final Class JERSEY_FIBER_SERVLET = co.paralleluniverse.fibers.jersey.ServletContainer.class;
    private static final String PACKAGE_NAME_PREFIX = FiberServletContainerTest.class.getPackage().getName() + ".";

    public FiberServletContainerTest(Class<? extends EmbeddedServer> cls) {
        this.cls = cls;
    }

    @Before
    public void setUp() throws Exception {
        this.instance = cls.newInstance();
        instance.addServlet("api", JERSEY_FIBER_SERVLET, "/*")
                .setInitParameter(PARAM_JERSEY_PACKAGES, PACKAGE_NAME_PREFIX)
                .setLoadOnStartup(1);
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
            assertEquals("sleep was 10", client.execute(new HttpGet("http://localhost:8080/service?sleep=10"), BASIC_RESPONSE_HANDLER));
    }

    @Test
    public void testPost() throws IOException, InterruptedException, Exception {
        for (int i = 0; i < 10; i++)
            assertEquals("sleep was 10", client.execute(new HttpPost("http://localhost:8080/service?sleep=10"), BASIC_RESPONSE_HANDLER));
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
