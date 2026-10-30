package me.noynto.eosa.infrastructure.persistence;

import me.noynto.eosa.shared.WorkshopSessionId;
import me.noynto.eosa.shared.WorkshopWaitlistEntryId;
import me.noynto.eosa.workshop.WorkshopWaitlistEntry;
import me.noynto.eosa.workshop.WorkshopWaitlistEntryProvider;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

public record JdbcPersistedWorkshopWaitlistEntries(
        DataSource dataSource
) implements WorkshopWaitlistEntryProvider {

    @Override
    public Stream<WorkshopWaitlistEntryId> readIds(WorkshopSessionId sessionId, String email, String token, Boolean waiting) {
        UUID sessionUuid = null;
        if (sessionId != null) {
            try {
                sessionUuid = UUID.fromString(sessionId.value());
            } catch (IllegalArgumentException e) {
                return Stream.empty();
            }
        }
        StringBuilder sql = new StringBuilder("SELECT id FROM workshop_waitlist_entries WHERE TRUE");
        List<Object> parameters = new ArrayList<>();
        if (sessionUuid != null) {
            sql.append(" AND session_id = ?");
            parameters.add(sessionUuid);
        }
        if (email != null) {
            sql.append(" AND LOWER(email) = LOWER(?)");
            parameters.add(email);
        }
        if (token != null) {
            sql.append(" AND unsubscribe_token = ?");
            parameters.add(token);
        }
        if (waiting != null) {
            sql.append(waiting ? " AND removed_at IS NULL" : " AND removed_at IS NOT NULL");
        }
        sql.append(" ORDER BY created_at, id");

        List<WorkshopWaitlistEntryId> ids = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            for (int i = 0; i < parameters.size(); i++) {
                statement.setObject(i + 1, parameters.get(i));
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    ids.add(new WorkshopWaitlistEntryId(resultSet.getString("id")));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Impossible de lire la liste d'attente des ateliers.", e);
        }
        return ids.stream();
    }

    @Override
    public Optional<WorkshopWaitlistEntry> read(WorkshopWaitlistEntryId workshopWaitlistEntryId) {
        UUID id;
        try {
            id = UUID.fromString(workshopWaitlistEntryId.value());
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
        String sql = """
                SELECT id, session_id, first_name, email, unsubscribe_token, created_at, last_notified_at, removed_at
                FROM workshop_waitlist_entries WHERE id = ?
                """;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(toWorkshopWaitlistEntry(resultSet));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Impossible de lire l'inscription en liste d'attente " + workshopWaitlistEntryId.value() + ".", e);
        }
    }

    @Override
    public WorkshopWaitlistEntry write(WorkshopWaitlistEntry workshopWaitlistEntry) {
        if (workshopWaitlistEntry.getId() == null) {
            workshopWaitlistEntry.setId(new WorkshopWaitlistEntryId(UUID.randomUUID().toString()));
        }
        String sql = """
                INSERT INTO workshop_waitlist_entries (id, session_id, first_name, email, unsubscribe_token, created_at,
                                                       last_notified_at, removed_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (id) DO UPDATE SET first_name = EXCLUDED.first_name, email = EXCLUDED.email,
                    last_notified_at = EXCLUDED.last_notified_at, removed_at = EXCLUDED.removed_at
                """;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, UUID.fromString(workshopWaitlistEntry.getId().value()));
            statement.setObject(2, UUID.fromString(workshopWaitlistEntry.getSessionId().value()));
            statement.setString(3, workshopWaitlistEntry.getFirstName());
            statement.setString(4, workshopWaitlistEntry.getEmail());
            statement.setString(5, workshopWaitlistEntry.getToken());
            statement.setObject(6, toOffsetDateTime(workshopWaitlistEntry.getCreatedAt()));
            statement.setObject(7, toOffsetDateTime(workshopWaitlistEntry.getLastNotifiedAt()));
            statement.setObject(8, toOffsetDateTime(workshopWaitlistEntry.getRemovedAt()));
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Impossible d'écrire l'inscription en liste d'attente " + workshopWaitlistEntry.getId().value() + ".", e);
        }
        return workshopWaitlistEntry;
    }

    private WorkshopWaitlistEntry toWorkshopWaitlistEntry(ResultSet resultSet) throws SQLException {
        WorkshopWaitlistEntry entry = new WorkshopWaitlistEntry();
        entry.setId(new WorkshopWaitlistEntryId(resultSet.getString("id")));
        entry.setSessionId(new WorkshopSessionId(resultSet.getString("session_id")));
        entry.setFirstName(resultSet.getString("first_name"));
        entry.setEmail(resultSet.getString("email"));
        entry.setToken(resultSet.getString("unsubscribe_token"));
        entry.setCreatedAt(toInstant(resultSet, "created_at"));
        entry.setLastNotifiedAt(toInstant(resultSet, "last_notified_at"));
        entry.setRemovedAt(toInstant(resultSet, "removed_at"));
        return entry;
    }

    private static Instant toInstant(ResultSet resultSet, String column) throws SQLException {
        OffsetDateTime value = resultSet.getObject(column, OffsetDateTime.class);
        return value != null ? value.toInstant() : null;
    }

    private static OffsetDateTime toOffsetDateTime(Instant instant) {
        return instant != null ? instant.atOffset(ZoneOffset.UTC) : null;
    }

}
