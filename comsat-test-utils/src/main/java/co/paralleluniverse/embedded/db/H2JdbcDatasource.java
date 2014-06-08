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
