/*
 * COMSAT
 * Copyright (c) 2013-2014, Parallel Universe Software Co. All rights reserved.
 *
 * This program and the accompanying materials are dual-licensed under
 * either the terms of the Eclipse Public License v1.0 as published by
 * the Eclipse Foundation
 *
 *   or (per the licensee's choosing)
 *
 * under the terms of the GNU Lesser General Public License version 3.0
 * as published by the Free Software Foundation.
 */
/*
 * Based on sample.jetty.SampleJettyApplication
 * in Spring Boot Samples.
 * Copyright the original author Brock Mills.
 * Released under the ASF 2.0 license.
 */
package comsat.sample.tomcat;

import comsat.sample.tomcat.SampleTomcatTwoConnectorsApplication;
import java.io.IOException;
import java.net.HttpURLConnection;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.client.RestTemplate;

import static org.junit.Assert.assertEquals;

/**
 * Basic integration tests for 2 connector demo application.
 *
 * @author Brock Mills
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = SampleTomcatTwoConnectorsApplication.class)
@WebAppConfiguration
@IntegrationTest("server.port=0")
@DirtiesContext
public class SampleTomcatTwoConnectorsApplicationTests {

    @Value("${local.server.port}")
    private String port;

    @Autowired
    private ApplicationContext context;

    @BeforeClass
    public static void setUp() {
        try {
            // setup ssl context to ignore certificate errors
            SSLContext ctx = SSLContext.getInstance("TLS");
            X509TrustManager tm = new X509TrustManager() {

                @Override
                public void checkClientTrusted(
                        java.security.cert.X509Certificate[] chain, String authType)
                        throws java.security.cert.CertificateException {
                }

                @Override
                public void checkServerTrusted(
                        java.security.cert.X509Certificate[] chain, String authType)
                        throws java.security.cert.CertificateException {
                }

                @Override
                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                    return null;
                }
            };
            ctx.init(null, new TrustManager[]{tm}, null);
            SSLContext.setDefault(ctx);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Test
    public void testHello() throws Exception {
        RestTemplate template = new RestTemplate();
        final MySimpleClientHttpRequestFactory factory = new MySimpleClientHttpRequestFactory(
                new HostnameVerifier() {

                    @Override
                    public boolean verify(final String hostname, final SSLSession session) {
                        return true; // these guys are alright by me...
                    }
                });
        template.setRequestFactory(factory);

        ResponseEntity<String> entity = template.getForEntity("http://localhost:"
                + this.port + "/hello", String.class);
        assertEquals(HttpStatus.OK, entity.getStatusCode());
        assertEquals("hello", entity.getBody());

        ResponseEntity<String> httpsEntity = template.getForEntity("https://localhost:"
                + this.context.getBean("port") + "/hello", String.class);
        assertEquals(HttpStatus.OK, httpsEntity.getStatusCode());
        assertEquals("hello", httpsEntity.getBody());
    }

    /**
     * Http Request Factory for ignoring SSL hostname errors. Not for production
     * use!
     */
    class MySimpleClientHttpRequestFactory extends SimpleClientHttpRequestFactory {

        private final HostnameVerifier verifier;

        public MySimpleClientHttpRequestFactory(final HostnameVerifier verifier) {
            this.verifier = verifier;
        }

        @Override
        protected void prepareConnection(final HttpURLConnection connection,
                final String httpMethod) throws IOException {
            if (connection instanceof HttpsURLConnection) {
                ((HttpsURLConnection) connection).setHostnameVerifier(this.verifier);
            }
            super.prepareConnection(connection, httpMethod);
        }
    }
}
