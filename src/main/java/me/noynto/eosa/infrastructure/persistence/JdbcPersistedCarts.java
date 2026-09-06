package me.noynto.eosa.infrastructure.persistence;

import me.noynto.eosa.cart.Cart;
import me.noynto.eosa.cart.CartItem;
import me.noynto.eosa.cart.CartProvider;
import me.noynto.eosa.cart.SelectedCharm;
import me.noynto.eosa.shared.CartId;
import me.noynto.eosa.shared.CartItemId;
import me.noynto.eosa.shared.CharmId;
import me.noynto.eosa.shared.ImageId;
import me.noynto.eosa.shared.JewelId;
import me.noynto.eosa.shared.MetalColorId;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
                SELECT c.id, ci.id AS item_id, ci.jewel_id, ci.name, ci.price, ci.image_id, ci.quantity,
                       ci.metal_color_id, ci.metal_color_name, ci.metal_color_image_id,
                       cic.charm_id, cic.charm_name, cic.charm_price, cic.charm_image_id
                FROM carts c
                LEFT JOIN cart_items ci ON ci.cart_id = c.id
                LEFT JOIN cart_item_charms cic ON cic.cart_item_id = ci.id
                WHERE c.id = ?
                ORDER BY ci.position, cic.position
                """;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                boolean found = false;
                Map<String, CartItem> itemsById = new LinkedHashMap<>();
                while (resultSet.next()) {
                    found = true;
                    String itemId = resultSet.getString("item_id");
                    if (itemId == null) continue;

                    if (!itemsById.containsKey(itemId)) {
                        String imageId = resultSet.getString("image_id");
                        String metalColorId = resultSet.getString("metal_color_id");
                        String metalColorImageId = resultSet.getString("metal_color_image_id");
                        itemsById.put(itemId, new CartItem(
                                new CartItemId(itemId),
                                new JewelId(resultSet.getString("jewel_id")),
                                resultSet.getString("name"),
                                resultSet.getBigDecimal("price"),
                                imageId != null ? new ImageId(imageId) : null,
                                resultSet.getInt("quantity"),
                                metalColorId != null ? new MetalColorId(metalColorId) : null,
                                resultSet.getString("metal_color_name"),
                                metalColorImageId != null ? new ImageId(metalColorImageId) : null,
                                new ArrayList<>()
                        ));
                    }
                    CartItem item = itemsById.get(itemId);

                    String charmId = resultSet.getString("charm_id");
                    if (charmId != null) {
                        String charmImageId = resultSet.getString("charm_image_id");
                        item.getCharms().add(new SelectedCharm(
                                new CharmId(charmId),
                                resultSet.getString("charm_name"),
                                resultSet.getBigDecimal("charm_price"),
                                charmImageId != null ? new ImageId(charmImageId) : null
                        ));
                    }
                }
                if (!found) {
                    return Optional.empty();
                }
                Cart cart = new Cart();
                cart.setId(new CartId(id.toString()));
                cart.setItems(new ArrayList<>(itemsById.values()));
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
                            INSERT INTO cart_items (id, cart_id, position, jewel_id, name, price, image_id, quantity,
                                                     metal_color_id, metal_color_name, metal_color_image_id)
                            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                            """;
                    try (PreparedStatement statement = connection.prepareStatement(insertItem)) {
                        for (int position = 0; position < items.size(); position++) {
                            CartItem item = items.get(position);
                            statement.setObject(1, UUID.fromString(item.id().value()));
                            statement.setObject(2, id);
                            statement.setInt(3, position);
                            statement.setObject(4, UUID.fromString(item.jewelId().value()));
                            statement.setString(5, item.name());
                            statement.setBigDecimal(6, item.price());
                            statement.setObject(7, item.imageId() != null ? UUID.fromString(item.imageId().value()) : null);
                            statement.setInt(8, item.quantity());
                            statement.setObject(9, item.metalColorId() != null ? UUID.fromString(item.metalColorId().value()) : null);
                            statement.setString(10, item.metalColorName());
                            statement.setObject(11, item.metalColorImageId() != null ? UUID.fromString(item.metalColorImageId().value()) : null);
                            statement.addBatch();
                        }
                        statement.executeBatch();
                    }

                    String insertCharm = """
                            INSERT INTO cart_item_charms (cart_item_id, charm_id, charm_name, charm_price, charm_image_id, position)
                            VALUES (?, ?, ?, ?, ?, ?)
                            """;
                    try (PreparedStatement statement = connection.prepareStatement(insertCharm)) {
                        boolean hasAnyCharm = false;
                        for (CartItem item : items) {
                            List<SelectedCharm> charms = item.charms();
                            for (int position = 0; position < charms.size(); position++) {
                                SelectedCharm charm = charms.get(position);
                                statement.setObject(1, UUID.fromString(item.id().value()));
                                statement.setObject(2, UUID.fromString(charm.charmId().value()));
                                statement.setString(3, charm.name());
                                statement.setBigDecimal(4, charm.price());
                                statement.setObject(5, charm.imageId() != null ? UUID.fromString(charm.imageId().value()) : null);
                                statement.setInt(6, position);
                                statement.addBatch();
                                hasAnyCharm = true;
                            }
                        }
                        if (hasAnyCharm) {
                            statement.executeBatch();
                        }
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
