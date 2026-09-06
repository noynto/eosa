package me.noynto.eosa.infrastructure.persistence;

import me.noynto.eosa.identity.Identity;
import me.noynto.eosa.identity.IdentityProvider;
import me.noynto.eosa.shared.IdentityId;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

public record JdbcPersistedIdentities(
        DataSource dataSource
) implements IdentityProvider {

    @Override
    public Stream<IdentityId> readIds(Boolean isAdministrator, String name) {
        StringBuilder sql = new StringBuilder("SELECT id FROM identities WHERE 1 = 1");
        if (isAdministrator != null) sql.append(" AND administrator = ?");
        if (name != null) sql.append(" AND LOWER(name) = LOWER(?)");
        List<IdentityId> ids = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            int index = 1;
            if (isAdministrator != null) statement.setBoolean(index++, isAdministrator);
            if (name != null) statement.setString(index++, name);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    ids.add(new IdentityId(resultSet.getString("id")));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Impossible de lire les identités.", e);
        }
        return ids.stream();
    }

    @Override
    public Optional<Identity> read(IdentityId identityId) {
        UUID id;
        try {
            id = UUID.fromString(identityId.value());
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
        String sql = "SELECT id, name, secret, administrator FROM identities WHERE id = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(toIdentity(resultSet));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Impossible de lire l'identité " + identityId.value() + ".", e);
        }
    }

    @Override
    public Identity write(Identity identity) {
        if (identity.getId() == null) {
            identity.setId(new IdentityId(UUID.randomUUID().toString()));
        }
        String sql = """
                INSERT INTO identities (id, name, secret, administrator)
                VALUES (?, ?, ?, ?)
                ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, secret = EXCLUDED.secret, administrator = EXCLUDED.administrator
                """;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, UUID.fromString(identity.getId().value()));
            statement.setString(2, identity.getName());
            statement.setString(3, identity.getSecret());
            statement.setBoolean(4, identity.isAdministrator());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Impossible d'écrire l'identité " + identity.getId().value() + ".", e);
        }
        return identity;
    }

    private Identity toIdentity(ResultSet resultSet) throws SQLException {
        Identity identity = new Identity();
        identity.setId(new IdentityId(resultSet.getString("id")));
        identity.setName(resultSet.getString("name"));
        identity.setSecret(resultSet.getString("secret"));
        identity.setAdministrator(resultSet.getBoolean("administrator"));
        return identity;
    }

}
