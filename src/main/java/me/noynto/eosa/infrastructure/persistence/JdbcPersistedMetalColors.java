package me.noynto.eosa.infrastructure.persistence;

import me.noynto.eosa.metal.MetalColor;
import me.noynto.eosa.metal.MetalColorProvider;
import me.noynto.eosa.shared.ImageId;
import me.noynto.eosa.shared.MetalColorId;

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

public record JdbcPersistedMetalColors(
        DataSource dataSource
) implements MetalColorProvider {

    @Override
    public Stream<MetalColorId> readIds() {
        String sql = "SELECT id FROM metal_colors ORDER BY name";
        List<MetalColorId> ids = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                ids.add(new MetalColorId(resultSet.getString("id")));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Impossible de lire les couleurs de métal.", e);
        }
        return ids.stream();
    }

    @Override
    public Optional<MetalColor> read(MetalColorId metalColorId) {
        UUID id;
        try {
            id = UUID.fromString(metalColorId.value());
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
        String sql = "SELECT id, name, image_id FROM metal_colors WHERE id = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(toMetalColor(resultSet));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Impossible de lire la couleur de métal " + metalColorId.value() + ".", e);
        }
    }

    @Override
    public MetalColor write(MetalColor metalColor) {
        if (metalColor.getId() == null) {
            metalColor.setId(new MetalColorId(UUID.randomUUID().toString()));
        }
        String sql = """
                INSERT INTO metal_colors (id, name, image_id)
                VALUES (?, ?, ?)
                ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, image_id = EXCLUDED.image_id
                """;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, UUID.fromString(metalColor.getId().value()));
            statement.setString(2, metalColor.getName());
            statement.setObject(3, metalColor.getImageId() != null ? UUID.fromString(metalColor.getImageId().value()) : null);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Impossible d'écrire la couleur de métal " + metalColor.getId().value() + ".", e);
        }
        return metalColor;
    }

    private MetalColor toMetalColor(ResultSet resultSet) throws SQLException {
        MetalColor metalColor = new MetalColor();
        metalColor.setId(new MetalColorId(resultSet.getString("id")));
        metalColor.setName(resultSet.getString("name"));
        String imageId = resultSet.getString("image_id");
        if (imageId != null) metalColor.setImageId(new ImageId(imageId));
        return metalColor;
    }

}
