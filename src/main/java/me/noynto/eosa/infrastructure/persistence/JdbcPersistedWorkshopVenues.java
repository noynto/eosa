package me.noynto.eosa.infrastructure.persistence;

import me.noynto.eosa.shared.ImageId;
import me.noynto.eosa.shared.WorkshopVenueId;
import me.noynto.eosa.workshop.WorkshopVenue;
import me.noynto.eosa.workshop.WorkshopVenueProvider;

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

public record JdbcPersistedWorkshopVenues(
        DataSource dataSource
) implements WorkshopVenueProvider {

    @Override
    public Stream<WorkshopVenueId> readIds() {
        String sql = "SELECT id FROM workshop_venues ORDER BY archived, LOWER(name)";
        List<WorkshopVenueId> ids = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                ids.add(new WorkshopVenueId(resultSet.getString("id")));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Impossible de lire les lieux d'atelier.", e);
        }
        return ids.stream();
    }

    @Override
    public Optional<WorkshopVenue> read(WorkshopVenueId workshopVenueId) {
        UUID id;
        try {
            id = UUID.fromString(workshopVenueId.value());
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
        String sql = """
                SELECT id, name, description, address, postal_code, city, link_url, image_id,
                       default_capacity, default_duration_minutes, archived
                FROM workshop_venues WHERE id = ?
                """;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(toWorkshopVenue(resultSet));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Impossible de lire le lieu d'atelier " + workshopVenueId.value() + ".", e);
        }
    }

    @Override
    public WorkshopVenue write(WorkshopVenue workshopVenue) {
        if (workshopVenue.getId() == null) {
            workshopVenue.setId(new WorkshopVenueId(UUID.randomUUID().toString()));
        }
        String sql = """
                INSERT INTO workshop_venues (id, name, description, address, postal_code, city, link_url, image_id,
                                             default_capacity, default_duration_minutes, archived)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, description = EXCLUDED.description,
                    address = EXCLUDED.address, postal_code = EXCLUDED.postal_code, city = EXCLUDED.city,
                    link_url = EXCLUDED.link_url, image_id = EXCLUDED.image_id,
                    default_capacity = EXCLUDED.default_capacity,
                    default_duration_minutes = EXCLUDED.default_duration_minutes, archived = EXCLUDED.archived
                """;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, UUID.fromString(workshopVenue.getId().value()));
            statement.setString(2, workshopVenue.getName());
            statement.setString(3, workshopVenue.getDescription());
            statement.setString(4, workshopVenue.getAddress());
            statement.setString(5, workshopVenue.getPostalCode());
            statement.setString(6, workshopVenue.getCity());
            statement.setString(7, workshopVenue.getLinkUrl());
            statement.setObject(8, workshopVenue.getImageId() != null ? UUID.fromString(workshopVenue.getImageId().value()) : null);
            statement.setInt(9, workshopVenue.getDefaultCapacity());
            statement.setInt(10, workshopVenue.getDefaultDurationMinutes());
            statement.setBoolean(11, workshopVenue.isArchived());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Impossible d'écrire le lieu d'atelier " + workshopVenue.getId().value() + ".", e);
        }
        return workshopVenue;
    }

    private WorkshopVenue toWorkshopVenue(ResultSet resultSet) throws SQLException {
        WorkshopVenue workshopVenue = new WorkshopVenue();
        workshopVenue.setId(new WorkshopVenueId(resultSet.getString("id")));
        workshopVenue.setName(resultSet.getString("name"));
        workshopVenue.setDescription(resultSet.getString("description"));
        workshopVenue.setAddress(resultSet.getString("address"));
        workshopVenue.setPostalCode(resultSet.getString("postal_code"));
        workshopVenue.setCity(resultSet.getString("city"));
        workshopVenue.setLinkUrl(resultSet.getString("link_url"));
        String imageId = resultSet.getString("image_id");
        if (imageId != null) workshopVenue.setImageId(new ImageId(imageId));
        workshopVenue.setDefaultCapacity(resultSet.getInt("default_capacity"));
        workshopVenue.setDefaultDurationMinutes(resultSet.getInt("default_duration_minutes"));
        workshopVenue.setArchived(resultSet.getBoolean("archived"));
        return workshopVenue;
    }

}
