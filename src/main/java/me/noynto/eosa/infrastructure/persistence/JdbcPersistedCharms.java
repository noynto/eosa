package me.noynto.eosa.infrastructure.persistence;

import me.noynto.eosa.charm.Charm;
import me.noynto.eosa.charm.CharmProvider;
import me.noynto.eosa.shared.CharmId;
import me.noynto.eosa.shared.ImageId;

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

public record JdbcPersistedCharms(
        DataSource dataSource
) implements CharmProvider {

    @Override
    public Stream<CharmId> readIds() {
        String sql = "SELECT id FROM charms ORDER BY name";
        List<CharmId> ids = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                ids.add(new CharmId(resultSet.getString("id")));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Impossible de lire les breloques.", e);
        }
        return ids.stream();
    }

    @Override
    public Optional<Charm> read(CharmId charmId) {
        UUID id;
        try {
            id = UUID.fromString(charmId.value());
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
        String sql = "SELECT id, name, price, image_id FROM charms WHERE id = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(toCharm(resultSet));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Impossible de lire la breloque " + charmId.value() + ".", e);
        }
    }

    @Override
    public Charm write(Charm charm) {
        if (charm.getId() == null) {
            charm.setId(new CharmId(UUID.randomUUID().toString()));
        }
        String sql = """
                INSERT INTO charms (id, name, price, image_id)
                VALUES (?, ?, ?, ?)
                ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, price = EXCLUDED.price, image_id = EXCLUDED.image_id
                """;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, UUID.fromString(charm.getId().value()));
            statement.setString(2, charm.getName());
            statement.setBigDecimal(3, charm.getPrice());
            statement.setObject(4, charm.getImageId() != null ? UUID.fromString(charm.getImageId().value()) : null);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Impossible d'écrire la breloque " + charm.getId().value() + ".", e);
        }
        return charm;
    }

    @Override
    public void delete(CharmId charmId) {
        UUID id;
        try {
            id = UUID.fromString(charmId.value());
        } catch (IllegalArgumentException e) {
            return;
        }
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM charms WHERE id = ?")) {
            statement.setObject(1, id);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Impossible de supprimer la breloque " + charmId.value() + ".", e);
        }
    }

    private Charm toCharm(ResultSet resultSet) throws SQLException {
        Charm charm = new Charm();
        charm.setId(new CharmId(resultSet.getString("id")));
        charm.setName(resultSet.getString("name"));
        charm.setPrice(resultSet.getBigDecimal("price"));
        String imageId = resultSet.getString("image_id");
        if (imageId != null) charm.setImageId(new ImageId(imageId));
        return charm;
    }

}
