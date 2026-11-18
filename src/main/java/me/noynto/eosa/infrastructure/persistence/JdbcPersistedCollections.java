package me.noynto.eosa.infrastructure.persistence;

import me.noynto.eosa.collection.Collection;
import me.noynto.eosa.collection.CollectionProvider;
import me.noynto.eosa.collection.CollectionSeason;
import me.noynto.eosa.collection.CollectionState;
import me.noynto.eosa.shared.CollectionId;
import me.noynto.eosa.shared.ImageId;
import me.noynto.eosa.shared.JewelId;
import me.noynto.eosa.shared.VideoId;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

public record JdbcPersistedCollections(
        DataSource dataSource
) implements CollectionProvider {

    @Override
    public Stream<CollectionId> readIds(Set<CollectionState> states) {
        StringBuilder sql = new StringBuilder("SELECT id FROM collections");
        boolean filtered = states != null && !states.isEmpty();
        if (filtered) {
            sql.append(" WHERE state IN (").append(String.join(", ", Collections.nCopies(states.size(), "?"))).append(")");
        }
        // Les plus récemment publiées d'abord ; les brouillons (jamais publiés) en tête ; l'identifiant départage deux dates égales.
        sql.append(" ORDER BY published_at DESC NULLS FIRST, year DESC, id");
        List<CollectionId> ids = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            if (filtered) {
                int index = 1;
                for (CollectionState state : states) statement.setString(index++, state.name());
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    ids.add(new CollectionId(resultSet.getString("id")));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Impossible de lire les identifiants des collections.", e);
        }
        return ids.stream();
    }

    @Override
    public Optional<Collection> read(CollectionId collectionId) {
        UUID id;
        try {
            id = UUID.fromString(collectionId.value());
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
        String sql = "SELECT id, name, season, year, state, published_at, video_id, poster_image_id FROM collections WHERE id = ?";
        String jewelsSql = "SELECT jewel_id FROM collection_jewels WHERE collection_id = ? ORDER BY jewel_id";
        try (Connection connection = dataSource.getConnection()) {
            Collection collection;
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setObject(1, id);
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (!resultSet.next()) {
                        return Optional.empty();
                    }
                    collection = toCollection(resultSet);
                }
            }
            List<JewelId> jewelIds = new ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement(jewelsSql)) {
                statement.setObject(1, id);
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        jewelIds.add(new JewelId(resultSet.getString("jewel_id")));
                    }
                }
            }
            collection.setJewelIds(jewelIds);
            return Optional.of(collection);
        } catch (SQLException e) {
            throw new RuntimeException("Impossible de lire la collection " + collectionId.value() + ".", e);
        }
    }

    @Override
    public Collection write(Collection collection) {
        if (collection.getId() == null) {
            collection.setId(new CollectionId(UUID.randomUUID().toString()));
        }
        UUID id = UUID.fromString(collection.getId().value());
        String upsert = """
                INSERT INTO collections (id, name, season, year, state, published_at, video_id, poster_image_id)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, season = EXCLUDED.season, year = EXCLUDED.year,
                    state = EXCLUDED.state, published_at = EXCLUDED.published_at, video_id = EXCLUDED.video_id,
                    poster_image_id = EXCLUDED.poster_image_id
                """;
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement statement = connection.prepareStatement(upsert)) {
                    statement.setObject(1, id);
                    statement.setString(2, collection.getName());
                    statement.setString(3, collection.getSeason().name());
                    statement.setInt(4, collection.getYear());
                    statement.setString(5, collection.getState().name());
                    statement.setObject(6, collection.getPublishedAt() != null ? collection.getPublishedAt().atOffset(ZoneOffset.UTC) : null);
                    statement.setObject(7, collection.getVideoId() != null ? UUID.fromString(collection.getVideoId().value()) : null);
                    statement.setObject(8, collection.getPosterImageId() != null ? UUID.fromString(collection.getPosterImageId().value()) : null);
                    statement.executeUpdate();
                }
                try (PreparedStatement statement = connection.prepareStatement("DELETE FROM collection_jewels WHERE collection_id = ?")) {
                    statement.setObject(1, id);
                    statement.executeUpdate();
                }
                List<JewelId> jewelIds = collection.getJewelIds();
                if (jewelIds != null && !jewelIds.isEmpty()) {
                    try (PreparedStatement statement = connection.prepareStatement("INSERT INTO collection_jewels (collection_id, jewel_id) VALUES (?, ?)")) {
                        for (JewelId jewelId : jewelIds) {
                            statement.setObject(1, id);
                            statement.setObject(2, UUID.fromString(jewelId.value()));
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
            throw new RuntimeException("Impossible d'écrire la collection " + collection.getId().value() + ".", e);
        }
        return collection;
    }

    @Override
    public void delete(CollectionId collectionId) {
        String sql = "DELETE FROM collections WHERE id = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, UUID.fromString(collectionId.value()));
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Impossible de supprimer la collection " + collectionId.value() + ".", e);
        }
    }

    private static Collection toCollection(ResultSet resultSet) throws SQLException {
        Collection collection = new Collection();
        collection.setId(new CollectionId(resultSet.getString("id")));
        collection.setName(resultSet.getString("name"));
        collection.setSeason(CollectionSeason.valueOf(resultSet.getString("season")));
        collection.setYear(resultSet.getInt("year"));
        collection.setState(CollectionState.valueOf(resultSet.getString("state")));
        var publishedAt = resultSet.getObject("published_at", OffsetDateTime.class);
        collection.setPublishedAt(publishedAt != null ? publishedAt.toInstant() : null);
        String videoId = resultSet.getString("video_id");
        collection.setVideoId(videoId != null ? new VideoId(videoId) : null);
        String posterImageId = resultSet.getString("poster_image_id");
        collection.setPosterImageId(posterImageId != null ? new ImageId(posterImageId) : null);
        return collection;
    }

}
