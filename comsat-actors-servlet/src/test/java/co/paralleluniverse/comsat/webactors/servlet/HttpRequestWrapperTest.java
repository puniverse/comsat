package co.paralleluniverse.comsat.webactors.servlet;

import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.Assert.assertEquals;

/**
 * @author rodedb
 */
public class HttpRequestWrapperTest {

    @Test
    public void httpHeaderCaseInsensitivity() {
        MockHttpServletRequest mockRequest = new MockHttpServletRequest();
        String headerValue = "application/json";
        mockRequest.addHeader("Authorization", headerValue);
        HttpRequestWrapper requestWrapper = new HttpRequestWrapper(null, mockRequest, null);
        assertEquals(headerValue, requestWrapper.getHeader("authorization"));
    }
}