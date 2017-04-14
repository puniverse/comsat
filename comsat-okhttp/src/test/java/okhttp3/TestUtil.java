package okhttp3;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import co.paralleluniverse.fibers.okhttp.FiberOkHttpClient;
import okhttp3.internal.SingleInetAddressDns;
import okhttp3.internal.http2.Header;

public final class TestUtil {
    private TestUtil() {
    }

    private static final ConnectionPool connectionPool = new ConnectionPool();
    private static final Dispatcher dispatcher = new Dispatcher();

    /**
     * Returns an OkHttpClient for all tests to use as a starting point.
     *
     * <p>The shared instance allows all tests to share a single connection pool, which prevents idle
     * connections from consuming unnecessary resources while connections wait to be evicted.
     *
     * <p>This client is also configured to be slightly more deterministic, returning a single IP
     * address for all hosts, regardless of the actual number of IP addresses reported by DNS.
     */
    public static OkHttpClient defaultClient() {
        //COMSAT Added FiberOkHttpClient to the builder
        return new OkHttpClient.Builder(new FiberOkHttpClient())
                .connectionPool(connectionPool)
                .dispatcher(dispatcher)
                .dns(new SingleInetAddressDns()) // Prevent unexpected fallback addresses.
                .build();
    }

    public static List<Header> headerEntries(String... elements) {
        List<Header> result = new ArrayList<>(elements.length / 2);
        for (int i = 0; i < elements.length; i += 2) {
            result.add(new Header(elements[i], elements[i + 1]));
        }
        return result;
    }

    public static String repeat(char c, int count) {
        char[] array = new char[count];
        Arrays.fill(array, c);
        return new String(array);
    }

    /**
     * See FinalizationTester for discussion on how to best trigger GC in tests.
     * https://android.googlesource.com/platform/libcore/+/master/support/src/test/java/libcore/
     * java/lang/ref/FinalizationTester.java
     */
    public static void awaitGarbageCollection() throws InterruptedException {
        Runtime.getRuntime().gc();
        Thread.sleep(100);
        System.runFinalization();
    }
}