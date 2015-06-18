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
package co.paralleluniverse.fibers.jdbc;

import java.util.Hashtable;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.RefAddr;
import javax.naming.Reference;
import javax.naming.spi.ObjectFactory;
import javax.sql.DataSource;

/**
 * @author eitan
 */
public class FiberDataSourceFactory implements ObjectFactory {
    @Override
    public Object getObjectInstance(final Object obj, final Name name, final Context nameCtx, final Hashtable<?, ?> environment) throws Exception {
        if ((obj == null) || !(obj instanceof Reference))
            return null;
        final Reference ref = (Reference) obj;
        if (!"javax.sql.DataSource".equals(ref.getClassName()))
            return null;
        RefAddr ra;
        ra = ref.get("rawDataSource");
        if (ra == null)
            throw new RuntimeException("mising rawDataSource name");
        final String rawDS = ra.getContent().toString();
        ra = ref.get("threadsCount");
        if (ra == null)
            throw new RuntimeException("mising rawDataSource name");
        final int tc = Integer.parseInt(ra.getContent().toString());
        return create(rawDS, tc);
    }

    public static DataSource create(final String rawDS, final int tc) throws NamingException {
        final Context envCtx = (Context) new InitialContext().lookup("java:comp/env");
        final DataSource ds = (DataSource) envCtx.lookup(rawDS);
        return FiberDataSource.wrap(ds, tc);
    }
}
