package me.noynto.eosa.infrastructure.persistence.jdbc;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import me.noynto.eosa.infrastructure.persistence.JdbcPersistedCarts;
import me.noynto.eosa.infrastructure.persistence.JdbcPersistedCharms;
import me.noynto.eosa.infrastructure.persistence.JdbcPersistedIdentities;
import me.noynto.eosa.infrastructure.persistence.JdbcPersistedIdentitySessions;
import me.noynto.eosa.infrastructure.persistence.JdbcPersistedImages;
import me.noynto.eosa.infrastructure.persistence.JdbcPersistedJewels;
import me.noynto.eosa.infrastructure.persistence.JdbcPersistedMetalColors;
import org.flywaydb.core.Flyway;

import javax.sql.DataSource;
import java.util.Objects;

public class JdbcConfiguration {

    private static final String JDBC_URL = "EOSA_JDBC_URL";
    private static final String JDBC_USERNAME = "EOSA_JDBC_USERNAME";
    private static final String JDBC_PASSWORD = "EOSA_JDBC_PASSWORD";

    private final DataSource dataSource;

    public JdbcConfiguration(String url, String username, String password) {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setPoolName("eosa");
        hikariConfig.setJdbcUrl(url);
        hikariConfig.setUsername(username);
        hikariConfig.setPassword(password);
        hikariConfig.setMinimumIdle(2);
        hikariConfig.setMaximumPoolSize(10);
        // Fail a borrow fast rather than queue the HTTP request thread for the 30s default.
        hikariConfig.setConnectionTimeout(10_000);
        // Shrink back down to minimumIdle a couple of minutes after a traffic burst.
        hikariConfig.setIdleTimeout(120_000);
        hikariConfig.setMaxLifetime(1_800_000);
        // Keep idle connections alive through any network path that silently drops idle TCP.
        hikariConfig.setKeepaliveTime(300_000);
        // Tolerate Postgres not being ready yet at pod startup instead of failing on the first try.
        hikariConfig.setInitializationFailTimeout(30_000);
        // Safety net: every JdbcPersistedX call already uses try-with-resources, so this should
        // never fire — it's here to catch a future regression rather than silently starve the pool.
        hikariConfig.setLeakDetectionThreshold(30_000);
        this.dataSource = new HikariDataSource(hikariConfig);
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

    public JdbcPersistedMetalColors metalColors() {
        return new JdbcPersistedMetalColors(dataSource);
    }

    public JdbcPersistedCharms charms() {
        return new JdbcPersistedCharms(dataSource);
    }

}
