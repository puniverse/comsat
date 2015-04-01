/*
 * COMSAT
 * Copyright (c) 2013-2015, Parallel Universe Software Co. All rights reserved.
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
 * Based on the corresponding class in Spring Boot Samples.
 * Copyright the original author(s).
 * Released under the ASF 2.0 license.
 */
package comsat.sample.undertow;

import co.paralleluniverse.test.categories.CI;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import static org.junit.Assert.assertEquals;
import org.junit.experimental.categories.Category;

/**
 * Basic integration tests for demo application.
 *
 * @author Ivan Sopov
 * @author Andy Wilkinson
 */
@Category(CI.class)
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = SampleUndertowApplication.class)
@WebAppConfiguration
@IntegrationTest("server.port:0")
@DirtiesContext
public class SampleUndertowApplicationTests {
    @Value("${local.server.port}")
    private int port;

    @Test
    public void testHome() throws Exception {
        assertOkResponse("/", "Hello World");
    }

    @Test
    public void testAsync() throws Exception {
        assertOkResponse("/async", "async: Hello World");
    }

    private void assertOkResponse(String path, String body) {
        ResponseEntity<String> entity = new TestRestTemplate().getForEntity("http://localhost:" + this.port + path, String.class);
        assertEquals(HttpStatus.OK, entity.getStatusCode());
        assertEquals(body, entity.getBody());
    }
}
