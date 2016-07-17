package co.paralleluniverse.comsat.webactors.undertow;

import io.undertow.server.HttpServerExchange;
import io.undertow.util.HttpString;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author rodedb
 */
public class HttpRequestWrapperTest {

    @Test
    public void httpHeaderCaseInsensitivity() {
        String headerValue = "application/json";
        HttpServerExchange httpServerExchange = new HttpServerExchange(null);
        httpServerExchange.getRequestHeaders().put(new HttpString("Content-Type"), headerValue);
        HttpRequestWrapper requestWrapper = new HttpRequestWrapper(null, httpServerExchange, null);
        assertEquals(headerValue, requestWrapper.getHeader("content-type"));
    }
}
