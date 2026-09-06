package me.noynto.eosa.infrastructure.persistence;

import me.noynto.eosa.identity.IdentitySession;
import me.noynto.eosa.identity.IdentitySessionProvider;
import me.noynto.eosa.shared.IdentityId;
import me.noynto.eosa.shared.IdentitySessionId;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

public record JdbcPersistedIdentitySessions(
        DataSource dataSource
) implements IdentitySessionProvider {

    @Override
    public Stream<IdentitySessionId> readIds() {
        String sql = "SELECT id FROM identity_sessions";
        List<IdentitySessionId> ids = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                ids.add(new IdentitySessionId(resultSet.getString("id")));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Impossible de lire les sessions d'identité.", e);
        }
        return ids.stream();
    }

    @Override
    public Optional<IdentitySession> read(IdentitySessionId id) {
        UUID sessionId;
        try {
            sessionId = UUID.fromString(id.value());
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
        String sql = "SELECT id, identity_id, begin_at FROM identity_sessions WHERE id = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, sessionId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(toIdentitySession(resultSet));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Impossible de lire la session d'identité " + id.value() + ".", e);
        }
    }

    @Override
    public IdentitySession write(IdentitySession session) {
        if (session.getId() == null) {
            session.setId(new IdentitySessionId(UUID.randomUUID().toString()));
        }
        String sql = """
                INSERT INTO identity_sessions (id, identity_id, begin_at)
                VALUES (?, ?, ?)
                ON CONFLICT (id) DO UPDATE SET identity_id = EXCLUDED.identity_id, begin_at = EXCLUDED.begin_at
                """;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, UUID.fromString(session.getId().value()));
            statement.setObject(2, session.getIdentityId() != null ? UUID.fromString(session.getIdentityId().value()) : null);
            statement.setObject(3, session.getBegin() != null ? OffsetDateTime.of(session.getBegin(), ZoneOffset.UTC) : null);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Impossible d'écrire la session d'identité " + session.getId().value() + ".", e);
        }
        return session;
    }

    private IdentitySession toIdentitySession(ResultSet resultSet) throws SQLException {
        IdentitySession session = new IdentitySession();
        session.setId(new IdentitySessionId(resultSet.getString("id")));
        String identityId = resultSet.getString("identity_id");
        if (identityId != null) session.setIdentityId(new IdentityId(identityId));
        OffsetDateTime beginAt = resultSet.getObject("begin_at", OffsetDateTime.class);
        if (beginAt != null) session.setBegin(beginAt.withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime());
        return session;
    }

}
