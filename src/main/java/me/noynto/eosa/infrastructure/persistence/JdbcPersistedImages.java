package me.noynto.eosa.infrastructure.persistence;

import me.noynto.eosa.image.Image;
import me.noynto.eosa.image.ImageProvider;
import me.noynto.eosa.shared.ImageId;

import javax.sql.DataSource;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

public record JdbcPersistedImages(
        DataSource dataSource
) implements ImageProvider {

    @Override
    public Image upload(Image image) {
        UUID id = UUID.randomUUID();
        byte[] content;
        try {
            content = image.getContent().readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("Impossible de lire le contenu de l'image.", e);
        }
        String sql = "INSERT INTO images (id, name, format, content) VALUES (?, ?, ?, ?)";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, id);
            statement.setString(2, image.getName());
            statement.setString(3, image.getFormat());
            statement.setBytes(4, content);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Impossible d'écrire l'image.", e);
        }
        image.setId(new ImageId(id.toString()));
        return image;
    }

    @Override
    public Optional<Image> download(ImageId imageId) {
        UUID id;
        try {
            id = UUID.fromString(imageId.value());
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
        String sql = "SELECT name, format, content FROM images WHERE id = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                Image image = new Image();
                image.setId(imageId);
                image.setName(resultSet.getString("name"));
                image.setFormat(resultSet.getString("format"));
                image.setContent(new ByteArrayInputStream(resultSet.getBytes("content")));
                return Optional.of(image);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Impossible de lire l'image " + imageId.value() + ".", e);
        }
    }

}
