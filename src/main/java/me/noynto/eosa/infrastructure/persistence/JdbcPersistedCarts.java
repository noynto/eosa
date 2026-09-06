package me.noynto.eosa.infrastructure.persistence;

import me.noynto.eosa.cart.Cart;
import me.noynto.eosa.cart.CartItem;
import me.noynto.eosa.cart.CartProvider;
import me.noynto.eosa.shared.CartId;
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
import java.util.UUID;

public record JdbcPersistedCarts(
        DataSource dataSource
) implements CartProvider {

    @Override
    public Optional<Cart> read(CartId cartId) {
        UUID id;
        try {
            id = UUID.fromString(cartId.value());
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
        String sql = """
                SELECT c.id, ci.jewel_id, ci.name, ci.price, ci.image_id, ci.quantity
                FROM carts c
                LEFT JOIN cart_items ci ON ci.cart_id = c.id
                WHERE c.id = ?
                ORDER BY ci.position
                """;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                boolean found = false;
                List<CartItem> items = new ArrayList<>();
                while (resultSet.next()) {
                    found = true;
                    String jewelId = resultSet.getString("jewel_id");
                    if (jewelId == null) continue;
                    String imageId = resultSet.getString("image_id");
                    items.add(new CartItem(
                            new JewelId(jewelId),
                            resultSet.getString("name"),
                            resultSet.getBigDecimal("price"),
                            imageId != null ? new ImageId(imageId) : null,
                            resultSet.getInt("quantity")
                    ));
                }
                if (!found) {
                    return Optional.empty();
                }
                Cart cart = new Cart();
                cart.setId(new CartId(id.toString()));
                cart.setItems(items);
                return Optional.of(cart);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Impossible de lire le panier " + cartId.value() + ".", e);
        }
    }

    @Override
    public Cart write(Cart cart) {
        if (cart.getId() == null) {
            cart.setId(new CartId(UUID.randomUUID().toString()));
        }
        UUID id = UUID.fromString(cart.getId().value());
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO carts (id) VALUES (?) ON CONFLICT (id) DO NOTHING")) {
                    statement.setObject(1, id);
                    statement.executeUpdate();
                }
                try (PreparedStatement statement = connection.prepareStatement("DELETE FROM cart_items WHERE cart_id = ?")) {
                    statement.setObject(1, id);
                    statement.executeUpdate();
                }
                List<CartItem> items = cart.getItems();
                if (items != null && !items.isEmpty()) {
                    String insertItem = """
                            INSERT INTO cart_items (cart_id, position, jewel_id, name, price, image_id, quantity)
                            VALUES (?, ?, ?, ?, ?, ?, ?)
                            """;
                    try (PreparedStatement statement = connection.prepareStatement(insertItem)) {
                        for (int position = 0; position < items.size(); position++) {
                            CartItem item = items.get(position);
                            statement.setObject(1, id);
                            statement.setInt(2, position);
                            statement.setObject(3, UUID.fromString(item.jewelId().value()));
                            statement.setString(4, item.name());
                            statement.setBigDecimal(5, item.price());
                            statement.setObject(6, item.imageId() != null ? UUID.fromString(item.imageId().value()) : null);
                            statement.setInt(7, item.quantity());
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
            throw new RuntimeException("Impossible d'écrire le panier " + cart.getId().value() + ".", e);
        }
        return cart;
    }

}
