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
 * Copyright the original author Dave Syer.
 * Released under the ASF 2.0 license.
 */
package comsat.sample.jpa.repository;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import comsat.sample.jpa.SampleJpaApplication;
import comsat.sample.jpa.domain.Tag;

import static org.junit.Assert.assertEquals;

/**
 * Integration tests for {@link JpaTagRepository}.
 *
 * @author Andy Wilkinson
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = SampleJpaApplication.class)
public class JpaTagRepositoryIntegrationTests {
    @Autowired
    JpaTagRepository repository;

    @Test
    public void findsAllTags() {
        List<Tag> tags = this.repository.findAll();
        assertEquals(3, tags.size());
    }
}
