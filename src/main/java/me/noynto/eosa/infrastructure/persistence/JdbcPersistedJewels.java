package me.noynto.eosa.infrastructure.persistence;

import me.noynto.eosa.jewel.Jewel;
import me.noynto.eosa.jewel.JewelCategory;
import me.noynto.eosa.jewel.JewelProvider;
import me.noynto.eosa.jewel.JewelState;
import me.noynto.eosa.shared.ImageId;
import me.noynto.eosa.shared.JewelId;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public record JdbcPersistedJewels(
        DataSource dataSource
) implements JewelProvider {

    @Override
    public Stream<JewelId> readIds(Set<JewelState> states, Set<JewelCategory> categories) {
        StringBuilder sql = new StringBuilder("SELECT id FROM jewels WHERE 1 = 1");
        if (states != null && !states.isEmpty()) {
            sql.append(" AND state IN (").append(placeholders(states.size())).append(")");
        }
        if (categories != null && !categories.isEmpty()) {
            sql.append(" AND category IN (").append(placeholders(categories.size())).append(")");
        }
        sql.append(" ORDER BY id DESC");
        List<JewelId> ids = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            int index = 1;
            if (states != null && !states.isEmpty()) {
                for (JewelState state : states) statement.setString(index++, state.name());
            }
            if (categories != null && !categories.isEmpty()) {
                for (JewelCategory category : categories) statement.setString(index++, category.name());
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    ids.add(new JewelId(resultSet.getString("id")));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Impossible de lire les identifiants des bijoux.", e);
        }
        return ids.stream();
    }

    @Override
    public Optional<Jewel> read(JewelId jewelId) {
        UUID id;
        try {
            id = UUID.fromString(jewelId.value());
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
        String sql = """
                SELECT j.id, j.name, j.tagline, j.price, j.state, j.category, ji.image_id
                FROM jewels j
                LEFT JOIN jewel_images ji ON ji.jewel_id = j.id
                WHERE j.id = ?
                ORDER BY ji.position
                """;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                Jewel jewel = null;
                List<ImageId> imageIds = new ArrayList<>();
                while (resultSet.next()) {
                    if (jewel == null) {
                        jewel = toJewel(resultSet);
                    }
                    String imageId = resultSet.getString("image_id");
                    if (imageId != null) imageIds.add(new ImageId(imageId));
                }
                if (jewel == null) {
                    return Optional.empty();
                }
                jewel.setImageIds(imageIds);
                return Optional.of(jewel);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Impossible de lire le bijou " + jewelId.value() + ".", e);
        }
    }

    @Override
    public Jewel write(Jewel jewel) {
        if (jewel.getId() == null) {
            jewel.setId(new JewelId(UUID.randomUUID().toString()));
        }
        UUID id = UUID.fromString(jewel.getId().value());
        String upsertJewel = """
                INSERT INTO jewels (id, name, tagline, price, state, category)
                VALUES (?, ?, ?, ?, ?, ?)
                ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, tagline = EXCLUDED.tagline, price = EXCLUDED.price, state = EXCLUDED.state, category = EXCLUDED.category
                """;
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement statement = connection.prepareStatement(upsertJewel)) {
                    statement.setObject(1, id);
                    statement.setString(2, jewel.getName());
                    statement.setString(3, jewel.getTagline());
                    statement.setBigDecimal(4, jewel.getPrice());
                    statement.setString(5, jewel.getState() != null ? jewel.getState().name() : null);
                    statement.setString(6, jewel.getCategory() != null ? jewel.getCategory().name() : null);
                    statement.executeUpdate();
                }
                try (PreparedStatement statement = connection.prepareStatement("DELETE FROM jewel_images WHERE jewel_id = ?")) {
                    statement.setObject(1, id);
                    statement.executeUpdate();
                }
                List<ImageId> imageIds = jewel.getImageIds();
                if (imageIds != null && !imageIds.isEmpty()) {
                    String insertImage = "INSERT INTO jewel_images (jewel_id, image_id, position) VALUES (?, ?, ?)";
                    try (PreparedStatement statement = connection.prepareStatement(insertImage)) {
                        for (int position = 0; position < imageIds.size(); position++) {
                            statement.setObject(1, id);
                            statement.setObject(2, UUID.fromString(imageIds.get(position).value()));
                            statement.setInt(3, position);
                            statement.addBatch();
                        }
                        statement.executeBatch();
                    }
                }
                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Impossible d'écrire le bijou " + jewel.getId().value() + ".", e);
        }
        return jewel;
    }

    private Jewel toJewel(ResultSet resultSet) throws SQLException {
        Jewel jewel = new Jewel();
        jewel.setId(new JewelId(resultSet.getString("id")));
        jewel.setName(resultSet.getString("name"));
        jewel.setTagline(resultSet.getString("tagline"));
        jewel.setPrice(resultSet.getBigDecimal("price"));
        String state = resultSet.getString("state");
        if (state != null) jewel.setState(JewelState.valueOf(state));
        String category = resultSet.getString("category");
        if (category != null) jewel.setCategory(JewelCategory.valueOf(category));
        return jewel;
    }

    private static String placeholders(int count) {
        return Stream.generate(() -> "?").limit(count).collect(Collectors.joining(", "));
    }

}
