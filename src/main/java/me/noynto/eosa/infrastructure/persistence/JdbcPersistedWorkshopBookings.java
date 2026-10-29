package me.noynto.eosa.infrastructure.persistence;

import me.noynto.eosa.shared.WorkshopSessionId;
import me.noynto.eosa.workshop.WorkshopBookingProvider;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public record JdbcPersistedWorkshopBookings(
        DataSource dataSource
) implements WorkshopBookingProvider {

    @Override
    public int countOccupiedPlaces(WorkshopSessionId workshopSessionId) {
        // Une réservation PENDING n'occupe sa place que pendant la validité de sa session Stripe (30 min),
        // pour qu'une place abandonnée se libère même si la tâche d'expiration n'est pas encore passée.
        String sql = """
                SELECT COUNT(*) FROM workshop_bookings
                WHERE session_id = ?
                  AND (state IN ('CONFIRMED', 'ATTENDED', 'NO_SHOW')
                       OR (state = 'PENDING' AND created_at > NOW() - INTERVAL '30 minutes'))
                """;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, UUID.fromString(workshopSessionId.value()));
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Impossible de compter les places réservées de la session " + workshopSessionId.value() + ".", e);
        }
    }

}
