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
 * Based on the corresponding class in Spring Boot Samples.
 * Copyright the original author Oliver Gierke.
 * Released under the ASF 2.0 license.
 */
package comsat.sample.data.jpa.service;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import comsat.sample.data.jpa.SampleDataJpaApplication;
import comsat.sample.data.jpa.domain.City;
import comsat.sample.data.jpa.domain.Hotel;
import comsat.sample.data.jpa.domain.HotelSummary;
import comsat.sample.data.jpa.domain.Rating;
import comsat.sample.data.jpa.domain.RatingCount;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * Integration tests for {@link HotelRepository}.
 *
 * @author Oliver Gierke
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = SampleDataJpaApplication.class)
public class HotelRepositoryIntegrationTests {

    @Autowired
    CityRepository cityRepository;
    @Autowired
    HotelRepository repository;

    @Test
    public void executesQueryMethodsCorrectly() {
        City city = this.cityRepository
                .findAll(new PageRequest(0, 1, Direction.ASC, "name")).getContent()
                .get(0);
        assertThat(city.getName(), is("Atlanta"));

        Page<HotelSummary> hotels = this.repository.findByCity(city, new PageRequest(0,
                10, Direction.ASC, "name"));
        Hotel hotel = this.repository.findByCityAndName(city, hotels.getContent().get(0)
                .getName());
        assertThat(hotel.getName(), is("Doubletree"));

        List<RatingCount> counts = this.repository.findRatingCounts(hotel);
        assertThat(counts, hasSize(1));
        assertThat(counts.get(0).getRating(), is(Rating.AVERAGE));
        assertThat(counts.get(0).getCount(), is(greaterThan(1L)));
    }
}
