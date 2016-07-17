package co.paralleluniverse.comsat.webactors.netty;

import io.netty.buffer.EmptyByteBuf;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author rodedb
 */
public class HttpRequestWrapperTest {

    @Test
    public void httpHeaderCaseInsensitivity() {
        DefaultFullHttpRequest httpRequest =
                new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "uri", new EmptyByteBuf(UnpooledByteBufAllocator.DEFAULT));
        String headerValue = "application/json";
        httpRequest.headers().add("Content-Type", headerValue);
        HttpRequestWrapper requestWrapper = new HttpRequestWrapper(null, null, httpRequest, "sessionId");
        assertEquals(headerValue, requestWrapper.getHeader("content-type"));
    }
}
