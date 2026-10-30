package me.noynto.eosa.infrastructure.persistence;

import me.noynto.eosa.shared.WorkshopBookingId;
import me.noynto.eosa.shared.WorkshopSessionId;
import me.noynto.eosa.workshop.WorkshopBooking;
import me.noynto.eosa.workshop.WorkshopBookingProvider;
import me.noynto.eosa.workshop.WorkshopBookingState;

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
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

public record JdbcPersistedWorkshopBookings(
        DataSource dataSource
) implements WorkshopBookingProvider {

    @Override
    public Stream<WorkshopBookingId> readIds(WorkshopSessionId sessionId, Set<WorkshopBookingState> states,
                                             String email, String token) {
        UUID sessionUuid = null;
        if (sessionId != null) {
            try {
                sessionUuid = UUID.fromString(sessionId.value());
            } catch (IllegalArgumentException e) {
                return Stream.empty();
            }
        }
        StringBuilder sql = new StringBuilder("SELECT id FROM workshop_bookings WHERE TRUE");
        List<Object> parameters = new ArrayList<>();
        if (sessionUuid != null) {
            sql.append(" AND session_id = ?");
            parameters.add(sessionUuid);
        }
        if (states != null && !states.isEmpty()) {
            sql.append(" AND state = ANY (?)");
            parameters.add(states.stream().map(Enum::name).toArray(String[]::new));
        }
        if (email != null) {
            sql.append(" AND LOWER(email) = LOWER(?)");
            parameters.add(email);
        }
        if (token != null) {
            sql.append(" AND cancel_token = ?");
            parameters.add(token);
        }
        sql.append(" ORDER BY created_at, id");

        List<WorkshopBookingId> ids = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            for (int i = 0; i < parameters.size(); i++) {
                Object parameter = parameters.get(i);
                if (parameter instanceof String[] values) {
                    statement.setArray(i + 1, connection.createArrayOf("text", values));
                } else {
                    statement.setObject(i + 1, parameter);
                }
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    ids.add(new WorkshopBookingId(resultSet.getString("id")));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Impossible de lire les réservations d'atelier.", e);
        }
        return ids.stream();
    }

    @Override
    public Optional<WorkshopBooking> read(WorkshopBookingId workshopBookingId) {
        UUID id;
        try {
            id = UUID.fromString(workshopBookingId.value());
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
        String sql = """
                SELECT id, session_id, first_name, last_name, email, phone, state, cancel_token,
                       created_at, confirmed_at, cancelled_at, confirmation_email_sent_at, cancellation_email_sent_at,
                       reminder_email_sent_at
                FROM workshop_bookings WHERE id = ?
                """;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(toWorkshopBooking(resultSet));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Impossible de lire la réservation d'atelier " + workshopBookingId.value() + ".", e);
        }
    }

    @Override
    public WorkshopBooking write(WorkshopBooking workshopBooking) {
        if (workshopBooking.getId() == null) {
            workshopBooking.setId(new WorkshopBookingId(UUID.randomUUID().toString()));
        }
        String sql = """
                INSERT INTO workshop_bookings (id, session_id, first_name, last_name, email, phone, state, cancel_token,
                                               created_at, confirmed_at, cancelled_at, confirmation_email_sent_at,
                                               cancellation_email_sent_at, reminder_email_sent_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (id) DO UPDATE SET first_name = EXCLUDED.first_name, last_name = EXCLUDED.last_name,
                    email = EXCLUDED.email, phone = EXCLUDED.phone, state = EXCLUDED.state,
                    confirmed_at = EXCLUDED.confirmed_at, cancelled_at = EXCLUDED.cancelled_at,
                    confirmation_email_sent_at = EXCLUDED.confirmation_email_sent_at,
                    cancellation_email_sent_at = EXCLUDED.cancellation_email_sent_at,
                    reminder_email_sent_at = EXCLUDED.reminder_email_sent_at
                """;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, UUID.fromString(workshopBooking.getId().value()));
            statement.setObject(2, UUID.fromString(workshopBooking.getSessionId().value()));
            statement.setString(3, workshopBooking.getFirstName());
            statement.setString(4, workshopBooking.getLastName());
            statement.setString(5, workshopBooking.getEmail());
            statement.setString(6, workshopBooking.getPhone());
            statement.setString(7, workshopBooking.getState().name());
            statement.setString(8, workshopBooking.getToken());
            statement.setObject(9, toOffsetDateTime(workshopBooking.getCreatedAt()));
            statement.setObject(10, toOffsetDateTime(workshopBooking.getConfirmedAt()));
            statement.setObject(11, toOffsetDateTime(workshopBooking.getCancelledAt()));
            statement.setObject(12, toOffsetDateTime(workshopBooking.getConfirmationEmailSentAt()));
            statement.setObject(13, toOffsetDateTime(workshopBooking.getCancellationEmailSentAt()));
            statement.setObject(14, toOffsetDateTime(workshopBooking.getReminderEmailSentAt()));
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Impossible d'écrire la réservation d'atelier " + workshopBooking.getId().value() + ".", e);
        }
        return workshopBooking;
    }

    private WorkshopBooking toWorkshopBooking(ResultSet resultSet) throws SQLException {
        WorkshopBooking workshopBooking = new WorkshopBooking();
        workshopBooking.setId(new WorkshopBookingId(resultSet.getString("id")));
        workshopBooking.setSessionId(new WorkshopSessionId(resultSet.getString("session_id")));
        workshopBooking.setFirstName(resultSet.getString("first_name"));
        workshopBooking.setLastName(resultSet.getString("last_name"));
        workshopBooking.setEmail(resultSet.getString("email"));
        workshopBooking.setPhone(resultSet.getString("phone"));
        workshopBooking.setState(WorkshopBookingState.valueOf(resultSet.getString("state")));
        workshopBooking.setToken(resultSet.getString("cancel_token"));
        workshopBooking.setCreatedAt(toInstant(resultSet, "created_at"));
        workshopBooking.setConfirmedAt(toInstant(resultSet, "confirmed_at"));
        workshopBooking.setCancelledAt(toInstant(resultSet, "cancelled_at"));
        workshopBooking.setConfirmationEmailSentAt(toInstant(resultSet, "confirmation_email_sent_at"));
        workshopBooking.setCancellationEmailSentAt(toInstant(resultSet, "cancellation_email_sent_at"));
        workshopBooking.setReminderEmailSentAt(toInstant(resultSet, "reminder_email_sent_at"));
        return workshopBooking;
    }

    private static Instant toInstant(ResultSet resultSet, String column) throws SQLException {
        OffsetDateTime value = resultSet.getObject(column, OffsetDateTime.class);
        return value != null ? value.toInstant() : null;
    }

    private static OffsetDateTime toOffsetDateTime(Instant instant) {
        return instant != null ? instant.atOffset(ZoneOffset.UTC) : null;
    }

}
