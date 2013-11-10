package co.paralleluniverse.fibers.jdbc;

import java.util.Hashtable;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.Name;
import javax.naming.RefAddr;
import javax.naming.Reference;
import javax.naming.spi.ObjectFactory;
import javax.sql.DataSource;

public class FiberDataSourceFactory implements ObjectFactory {
    @Override
    public Object getObjectInstance(Object obj, Name name, Context nameCtx, Hashtable<?, ?> environment) throws Exception {
        if ((obj == null) || !(obj instanceof Reference))
            return null;
        Reference ref = (Reference) obj;
        if (!"javax.sql.DataSource".equals(ref.getClassName()))
            return null;
        RefAddr ra;
        ra = ref.get("rawDataSource");
        if (ra == null)
            throw new RuntimeException("mising rawDataSource name");
        String rawDS = ra.getContent().toString();
        ra = ref.get("threadsCount");
        if (ra == null)
            throw new RuntimeException("mising rawDataSource name");
        int tc = Integer.parseInt(ra.getContent().toString());
        Context envCtx = (Context) new InitialContext().lookup("java:comp/env");
        DataSource ds = (DataSource) envCtx.lookup(rawDS);
        return new FiberDataSource(ds, tc);
    }
}
