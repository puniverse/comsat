/*
 * COMSAT
 * Copyright (C) 2014, Parallel Universe Software Co. All rights reserved.
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
package co.paralleluniverse.embedded.db;

import org.h2.jdbcx.JdbcDataSource;

public class H2JdbcDatasource extends JdbcDataSource {
    public H2JdbcDatasource() {
        this("jdbc:h2:./build/h2default");
    }

    public H2JdbcDatasource(String url) {
        setURL(url);
    }
}
