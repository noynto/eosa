package me.noynto.eosa.infrastructure.persistence.jdbc;

import me.noynto.eosa.infrastructure.persistence.JdbcPersistedCarts;
import me.noynto.eosa.infrastructure.persistence.JdbcPersistedIdentities;
import me.noynto.eosa.infrastructure.persistence.JdbcPersistedIdentitySessions;
import me.noynto.eosa.infrastructure.persistence.JdbcPersistedImages;
import me.noynto.eosa.infrastructure.persistence.JdbcPersistedJewels;
import org.flywaydb.core.Flyway;
import org.postgresql.ds.PGSimpleDataSource;

import javax.sql.DataSource;
import java.util.Objects;

public class JdbcConfiguration {

    private static final String JDBC_URL = "EOSA_JDBC_URL";
    private static final String JDBC_USERNAME = "EOSA_JDBC_USERNAME";
    private static final String JDBC_PASSWORD = "EOSA_JDBC_PASSWORD";

    private final DataSource dataSource;

    public JdbcConfiguration(String url, String username, String password) {
        PGSimpleDataSource pgDataSource = new PGSimpleDataSource();
        pgDataSource.setUrl(url);
        pgDataSource.setUser(username);
        pgDataSource.setPassword(password);
        this.dataSource = pgDataSource;
        migrate();
    }

    public static JdbcConfiguration fromEnvironment() {
        return new JdbcConfiguration(
                Objects.requireNonNull(System.getenv(JDBC_URL), JDBC_URL + " est obligatoire."),
                Objects.requireNonNull(System.getenv(JDBC_USERNAME), JDBC_USERNAME + " est obligatoire."),
                Objects.requireNonNull(System.getenv(JDBC_PASSWORD), JDBC_PASSWORD + " est obligatoire.")
        );
    }

    private void migrate() {
        Flyway.configure()
                .dataSource(dataSource)
                .load()
                .migrate();
    }

    public DataSource dataSource() {
        return dataSource;
    }

    public JdbcPersistedIdentities identities() {
        return new JdbcPersistedIdentities(dataSource);
    }

    public JdbcPersistedIdentitySessions identitySessions() {
        return new JdbcPersistedIdentitySessions(dataSource);
    }

    public JdbcPersistedJewels jewels() {
        return new JdbcPersistedJewels(dataSource);
    }

    public JdbcPersistedImages images() {
        return new JdbcPersistedImages(dataSource);
    }

    public JdbcPersistedCarts carts() {
        return new JdbcPersistedCarts(dataSource);
    }

}
