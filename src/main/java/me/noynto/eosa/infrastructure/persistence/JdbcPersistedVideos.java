package me.noynto.eosa.infrastructure.persistence;

import me.noynto.eosa.shared.VideoId;
import me.noynto.eosa.video.Video;
import me.noynto.eosa.video.VideoProvider;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

public record JdbcPersistedVideos(
        DataSource dataSource
) implements VideoProvider {

    @Override
    public Video write(Video video) {
        UUID id = UUID.randomUUID();
        String sql = "INSERT INTO videos (id, name, format, size, content) VALUES (?, ?, ?, ?, ?)";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, id);
            statement.setString(2, video.getName());
            statement.setString(3, video.getFormat());
            statement.setInt(4, video.getContent().length);
            statement.setBytes(5, video.getContent());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Impossible d'écrire la vidéo.", e);
        }
        video.setId(new VideoId(id.toString()));
        video.setSize(video.getContent().length);
        return video;
    }

    @Override
    public Optional<Video> read(VideoId videoId) {
        Optional<UUID> id = parse(videoId);
        if (id.isEmpty()) {
            return Optional.empty();
        }
        String sql = "SELECT name, format, size FROM videos WHERE id = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, id.get());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                Video video = new Video();
                video.setId(videoId);
                video.setName(resultSet.getString("name"));
                video.setFormat(resultSet.getString("format"));
                video.setSize(resultSet.getInt("size"));
                return Optional.of(video);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Impossible de lire la vidéo " + videoId.value() + ".", e);
        }
    }

    @Override
    public byte[] readRange(VideoId videoId, int offset, int length) {
        Optional<UUID> id = parse(videoId);
        if (id.isEmpty()) {
            return new byte[0];
        }
        // substring() compte à partir de 1 ; sur une colonne EXTERNAL, PostgreSQL ne lit que les pages de la tranche.
        String sql = "SELECT substring(content FROM ? FOR ?) FROM videos WHERE id = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, offset + 1);
            statement.setInt(2, length);
            statement.setObject(3, id.get());
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getBytes(1) : new byte[0];
            }
        } catch (SQLException e) {
            throw new RuntimeException("Impossible de lire la vidéo " + videoId.value() + ".", e);
        }
    }

    @Override
    public void delete(VideoId videoId) {
        String sql = "DELETE FROM videos WHERE id = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, UUID.fromString(videoId.value()));
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Impossible de supprimer la vidéo " + videoId.value() + ".", e);
        }
    }

    private static Optional<UUID> parse(VideoId videoId) {
        try {
            return Optional.of(UUID.fromString(videoId.value()));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

}
