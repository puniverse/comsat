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
 * Copyright the original author(s).
 * Released under the ASF 2.0 license.
 */
package comsat.sample.data.jpa.domain;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

public class HotelSummary implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final MathContext MATH_CONTEXT = new MathContext(2,
            RoundingMode.HALF_UP);

    private final City city;

    private final String name;

    private final Double averageRating;

    private final Integer averageRatingRounded;

    public HotelSummary(City city, String name, Double averageRating) {
        this.city = city;
        this.name = name;
        this.averageRating = averageRating == null ? null : new BigDecimal(averageRating,
                MATH_CONTEXT).doubleValue();
        this.averageRatingRounded = averageRating == null ? null : (int) Math
                .round(averageRating);
    }

    public City getCity() {
        return this.city;
    }

    public String getName() {
        return this.name;
    }

    public Double getAverageRating() {
        return this.averageRating;
    }

    public Integer getAverageRatingRounded() {
        return this.averageRatingRounded;
    }
}
