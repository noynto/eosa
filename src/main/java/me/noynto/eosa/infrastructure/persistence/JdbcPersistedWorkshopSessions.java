package me.noynto.eosa.infrastructure.persistence;

import me.noynto.eosa.shared.WorkshopSessionId;
import me.noynto.eosa.shared.WorkshopVenueId;
import me.noynto.eosa.workshop.WorkshopSession;
import me.noynto.eosa.workshop.WorkshopSessionProvider;
import me.noynto.eosa.workshop.WorkshopSessionState;

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

public record JdbcPersistedWorkshopSessions(
        DataSource dataSource
) implements WorkshopSessionProvider {

    @Override
    public Stream<WorkshopSessionId> readIds() {
        String sql = "SELECT id FROM workshop_sessions ORDER BY starts_at";
        List<WorkshopSessionId> ids = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                ids.add(new WorkshopSessionId(resultSet.getString("id")));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Impossible de lire les sessions d'atelier.", e);
        }
        return ids.stream();
    }

    @Override
    public Optional<WorkshopSession> read(WorkshopSessionId workshopSessionId) {
        UUID id;
        try {
            id = UUID.fromString(workshopSessionId.value());
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
        String sql = """
                SELECT id, venue_id, starts_at, duration_minutes, capacity, state, cancelled_at
                FROM workshop_sessions WHERE id = ?
                """;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(toWorkshopSession(resultSet));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Impossible de lire la session d'atelier " + workshopSessionId.value() + ".", e);
        }
    }

    @Override
    public WorkshopSession write(WorkshopSession workshopSession) {
        if (workshopSession.getId() == null) {
            workshopSession.setId(new WorkshopSessionId(UUID.randomUUID().toString()));
        }
        String sql = """
                INSERT INTO workshop_sessions (id, venue_id, starts_at, duration_minutes, capacity, state, cancelled_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (id) DO UPDATE SET venue_id = EXCLUDED.venue_id, starts_at = EXCLUDED.starts_at,
                    duration_minutes = EXCLUDED.duration_minutes, capacity = EXCLUDED.capacity,
                    state = EXCLUDED.state, cancelled_at = EXCLUDED.cancelled_at
                """;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, UUID.fromString(workshopSession.getId().value()));
            statement.setObject(2, UUID.fromString(workshopSession.getVenueId().value()));
            statement.setObject(3, toOffsetDateTime(workshopSession.getStartsAt()));
            statement.setInt(4, workshopSession.getDurationMinutes());
            statement.setInt(5, workshopSession.getCapacity());
            statement.setString(6, workshopSession.getState().name());
            statement.setObject(7, toOffsetDateTime(workshopSession.getCancelledAt()));
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Impossible d'écrire la session d'atelier " + workshopSession.getId().value() + ".", e);
        }
        return workshopSession;
    }

    private WorkshopSession toWorkshopSession(ResultSet resultSet) throws SQLException {
        WorkshopSession workshopSession = new WorkshopSession();
        workshopSession.setId(new WorkshopSessionId(resultSet.getString("id")));
        workshopSession.setVenueId(new WorkshopVenueId(resultSet.getString("venue_id")));
        workshopSession.setStartsAt(resultSet.getObject("starts_at", OffsetDateTime.class).toInstant());
        workshopSession.setDurationMinutes(resultSet.getInt("duration_minutes"));
        workshopSession.setCapacity(resultSet.getInt("capacity"));
        workshopSession.setState(WorkshopSessionState.valueOf(resultSet.getString("state")));
        OffsetDateTime cancelledAt = resultSet.getObject("cancelled_at", OffsetDateTime.class);
        if (cancelledAt != null) workshopSession.setCancelledAt(cancelledAt.toInstant());
        return workshopSession;
    }

    private static OffsetDateTime toOffsetDateTime(Instant instant) {
        return instant != null ? instant.atOffset(ZoneOffset.UTC) : null;
    }

}
