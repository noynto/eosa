package me.noynto.eosa.infrastructure.persistence;

import me.noynto.eosa.order.Order;
import me.noynto.eosa.order.OrderItem;
import me.noynto.eosa.order.OrderProvider;
import me.noynto.eosa.order.OrderShippingAddress;
import me.noynto.eosa.order.OrderState;
import me.noynto.eosa.shared.CheckoutSessionId;
import me.noynto.eosa.shared.ImageId;
import me.noynto.eosa.shared.JewelId;
import me.noynto.eosa.shared.OrderId;
import me.noynto.eosa.shipping.ShippingCarrier;
import me.noynto.eosa.shipping.ShippingLabel;
import me.noynto.eosa.shipping.ShippingLabelStatus;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

public record JdbcPersistedOrders(
        DataSource dataSource
) implements OrderProvider {

    private static final ZoneId ZONE = ZoneId.of("Europe/Paris");

    @Override
    public Stream<OrderId> readIds(CheckoutSessionId stripeSessionId, Set<OrderState> states, Instant createdSince) {
        StringBuilder sql = new StringBuilder("SELECT id FROM orders WHERE TRUE");
        if (stripeSessionId != null) {
            sql.append(" AND stripe_session_id = ?");
        }
        if (states != null) {
            sql.append(" AND state IN (").append(String.join(", ", Collections.nCopies(Math.max(states.size(), 1), "?"))).append(")");
        }
        if (createdSince != null) {
            sql.append(" AND created_at >= ?");
        }
        sql.append(" ORDER BY created_at DESC, id");

        List<OrderId> ids = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            int index = 1;
            if (stripeSessionId != null) {
                statement.setString(index++, stripeSessionId.getStripe());
            }
            if (states != null) {
                if (states.isEmpty()) {
                    statement.setString(index++, "");
                }
                for (OrderState state : states) {
                    statement.setString(index++, state.name());
                }
            }
            if (createdSince != null) {
                statement.setObject(index, toOffsetDateTime(createdSince));
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    ids.add(new OrderId(resultSet.getString("id")));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Impossible de lire les commandes.", e);
        }
        return ids.stream();
    }

    @Override
    public Optional<Order> read(OrderId orderId) {
        UUID id;
        try {
            id = UUID.fromString(orderId.value());
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
        String sql = """
                SELECT id, number, state, stripe_session_id, customer_name, customer_email, customer_phone,
                       shipping_name, shipping_line1, shipping_line2, shipping_postal_code, shipping_city,
                       shipping_country, items_amount, shipping_amount, total_amount, paid_at, created_at,
                       confirmation_sent_at, notification_sent_at, crafted_at, carrier, tracking_number,
                       parcel_weight_grams, label_reference, label_status, shipped_at, delivered_at, cancelled_at,
                       cancel_reason, shipping_sent_at, personal_data_purged_at, hand_delivery,
                       parcel_length_cm, parcel_width_cm, parcel_depth_cm
                FROM orders WHERE id = ?
                """;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, id);
            Order order;
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                order = toOrder(resultSet);
            }
            order.setItems(readItems(connection, id));
            return Optional.of(order);
        } catch (SQLException e) {
            throw new RuntimeException("Impossible de lire la commande " + orderId.value() + ".", e);
        }
    }

    @Override
    public Order write(Order order) {
        return order.getId() == null ? insert(order) : update(order);
    }

    private Order insert(Order order) {
        UUID id = UUID.randomUUID();
        String numberPrefix = "EOSA-" + order.getPaidAt().atZone(ZONE).getYear() + "-";
        // Le numéro vient d'une séquence : le préfixe porte l'année, le reste est complété à 4 chiffres au moins.
        // Si une commande existe déjà pour la session Stripe, rien n'est inséré (ON CONFLICT DO NOTHING).
        String sql = """
                INSERT INTO orders (id, number, state, stripe_session_id, customer_name, customer_email, customer_phone,
                                    shipping_name, shipping_line1, shipping_line2, shipping_postal_code, shipping_city,
                                    shipping_country, items_amount, shipping_amount, total_amount, paid_at)
                VALUES (?, ? || (SELECT CASE WHEN n < 10000 THEN lpad(n::text, 4, '0') ELSE n::text END
                                 FROM (SELECT nextval('order_number_seq') AS n) sequence),
                        ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (stripe_session_id) DO NOTHING
                RETURNING number, created_at
                """;
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                String number;
                Instant createdAt;
                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    statement.setObject(1, id);
                    statement.setString(2, numberPrefix);
                    statement.setString(3, order.getState().name());
                    statement.setString(4, order.getStripeSessionId().getStripe());
                    statement.setString(5, order.getCustomerName());
                    statement.setString(6, order.getCustomerEmail());
                    statement.setString(7, order.getCustomerPhone());
                    setShippingAddress(statement, 8, order.getShippingAddress());
                    statement.setBigDecimal(14, order.getItemsAmount());
                    statement.setBigDecimal(15, order.getShippingAmount());
                    statement.setBigDecimal(16, order.getTotalAmount());
                    statement.setObject(17, toOffsetDateTime(order.getPaidAt()));
                    try (ResultSet resultSet = statement.executeQuery()) {
                        if (!resultSet.next()) {
                            connection.rollback();
                            return readIds(order.getStripeSessionId(), null, null)
                                    .findFirst()
                                    .flatMap(this::read)
                                    .orElseThrow(() -> new RuntimeException("Commande introuvable pour la session de paiement."));
                        }
                        number = resultSet.getString("number");
                        createdAt = toInstant(resultSet, "created_at");
                    }
                }
                insertItems(connection, id, order.getItems());
                connection.commit();
                order.setId(new OrderId(id.toString()));
                order.setNumber(number);
                order.setCreatedAt(createdAt);
                return order;
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Impossible d'écrire la commande de la session " + order.getStripeSessionId().getStripe() + ".", e);
        }
    }

    private Order update(Order order) {
        // Les lignes, le numéro, la session Stripe et les montants ne changent jamais après la création.
        String sql = """
                UPDATE orders SET state = ?, customer_name = ?, customer_email = ?, customer_phone = ?,
                    shipping_name = ?, shipping_line1 = ?, shipping_line2 = ?, shipping_postal_code = ?,
                    shipping_city = ?, shipping_country = ?, confirmation_sent_at = ?, notification_sent_at = ?,
                    crafted_at = ?, carrier = ?, tracking_number = ?, parcel_weight_grams = ?, label_reference = ?,
                    label_status = ?, shipped_at = ?, delivered_at = ?, cancelled_at = ?, cancel_reason = ?,
                    shipping_sent_at = ?, personal_data_purged_at = ?, hand_delivery = ?,
                    parcel_length_cm = ?, parcel_width_cm = ?, parcel_depth_cm = ?
                WHERE id = ?
                """;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, order.getState().name());
            statement.setString(2, order.getCustomerName());
            statement.setString(3, order.getCustomerEmail());
            statement.setString(4, order.getCustomerPhone());
            setShippingAddress(statement, 5, order.getShippingAddress());
            statement.setObject(11, toOffsetDateTime(order.getConfirmationSentAt()));
            statement.setObject(12, toOffsetDateTime(order.getNotificationSentAt()));
            ShippingLabel label = order.getLabel();
            statement.setObject(13, toOffsetDateTime(order.getCraftedAt()));
            statement.setString(14, label != null ? label.getCarrier().name() : null);
            statement.setString(15, label != null ? label.getTrackingNumber() : null);
            statement.setObject(16, label != null ? label.getWeightGrams() : null);
            statement.setString(17, label != null ? label.getReference() : null);
            statement.setString(18, label != null ? label.getStatus().name() : null);
            statement.setObject(19, toOffsetDateTime(order.getShippedAt()));
            statement.setObject(20, toOffsetDateTime(order.getDeliveredAt()));
            statement.setObject(21, toOffsetDateTime(order.getCancelledAt()));
            statement.setString(22, order.getCancelReason());
            statement.setObject(23, toOffsetDateTime(order.getShippingSentAt()));
            statement.setObject(24, toOffsetDateTime(order.getPersonalDataPurgedAt()));
            statement.setBoolean(25, order.isHandDelivery());
            statement.setObject(26, label != null ? label.getLengthCm() : null);
            statement.setObject(27, label != null ? label.getWidthCm() : null);
            statement.setObject(28, label != null ? label.getDepthCm() : null);
            statement.setObject(29, UUID.fromString(order.getId().value()));
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Impossible d'écrire la commande " + order.getId().value() + ".", e);
        }
        return order;
    }

    @Override
    public void delete(OrderId orderId) {
        UUID id;
        try {
            id = UUID.fromString(orderId.value());
        } catch (IllegalArgumentException e) {
            return;
        }
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM orders WHERE id = ?")) {
            statement.setObject(1, id);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Impossible de supprimer la commande " + orderId.value() + ".", e);
        }
    }

    private static void setShippingAddress(PreparedStatement statement, int firstIndex, OrderShippingAddress address)
            throws SQLException {
        statement.setString(firstIndex, address != null ? address.name() : null);
        statement.setString(firstIndex + 1, address != null ? address.line1() : null);
        statement.setString(firstIndex + 2, address != null ? address.line2() : null);
        statement.setString(firstIndex + 3, address != null ? address.postalCode() : null);
        statement.setString(firstIndex + 4, address != null ? address.city() : null);
        statement.setString(firstIndex + 5, address != null ? address.country() : null);
    }

    private static void insertItems(Connection connection, UUID orderId, List<OrderItem> items) throws SQLException {
        String sql = """
                INSERT INTO order_items (id, order_id, position, name, unit_price, quantity, jewel_id, image_id)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            int position = 0;
            for (OrderItem item : items) {
                statement.setObject(1, UUID.randomUUID());
                statement.setObject(2, orderId);
                statement.setInt(3, position++);
                statement.setString(4, item.getName());
                statement.setBigDecimal(5, item.getUnitPrice());
                statement.setInt(6, item.getQuantity());
                statement.setObject(7, item.getJewelId() != null ? UUID.fromString(item.getJewelId().value()) : null);
                statement.setObject(8, item.getImageId() != null ? UUID.fromString(item.getImageId().value()) : null);
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private static List<OrderItem> readItems(Connection connection, UUID orderId) throws SQLException {
        String sql = """
                SELECT name, unit_price, quantity, jewel_id, image_id
                FROM order_items WHERE order_id = ? ORDER BY position
                """;
        List<OrderItem> items = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, orderId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    OrderItem item = new OrderItem();
                    item.setName(resultSet.getString("name"));
                    item.setUnitPrice(resultSet.getBigDecimal("unit_price"));
                    item.setQuantity(resultSet.getInt("quantity"));
                    String jewelId = resultSet.getString("jewel_id");
                    item.setJewelId(jewelId != null ? new JewelId(jewelId) : null);
                    String imageId = resultSet.getString("image_id");
                    item.setImageId(imageId != null ? new ImageId(imageId) : null);
                    items.add(item);
                }
            }
        }
        return items;
    }

    private static Order toOrder(ResultSet resultSet) throws SQLException {
        Order order = new Order();
        order.setId(new OrderId(resultSet.getString("id")));
        order.setNumber(resultSet.getString("number"));
        order.setState(OrderState.valueOf(resultSet.getString("state")));
        CheckoutSessionId sessionId = new CheckoutSessionId();
        sessionId.setStripe(resultSet.getString("stripe_session_id"));
        order.setStripeSessionId(sessionId);
        order.setCustomerName(resultSet.getString("customer_name"));
        order.setCustomerEmail(resultSet.getString("customer_email"));
        order.setCustomerPhone(resultSet.getString("customer_phone"));
        String line1 = resultSet.getString("shipping_line1");
        if (line1 != null) {
            order.setShippingAddress(new OrderShippingAddress(
                    resultSet.getString("shipping_name"), line1, resultSet.getString("shipping_line2"),
                    resultSet.getString("shipping_postal_code"), resultSet.getString("shipping_city"),
                    resultSet.getString("shipping_country")));
        }
        order.setItemsAmount(resultSet.getBigDecimal("items_amount"));
        order.setShippingAmount(resultSet.getBigDecimal("shipping_amount"));
        order.setTotalAmount(resultSet.getBigDecimal("total_amount"));
        order.setPaidAt(toInstant(resultSet, "paid_at"));
        order.setCreatedAt(toInstant(resultSet, "created_at"));
        order.setConfirmationSentAt(toInstant(resultSet, "confirmation_sent_at"));
        order.setNotificationSentAt(toInstant(resultSet, "notification_sent_at"));
        order.setCraftedAt(toInstant(resultSet, "crafted_at"));
        order.setShippedAt(toInstant(resultSet, "shipped_at"));
        order.setDeliveredAt(toInstant(resultSet, "delivered_at"));
        order.setCancelledAt(toInstant(resultSet, "cancelled_at"));
        order.setCancelReason(resultSet.getString("cancel_reason"));
        order.setShippingSentAt(toInstant(resultSet, "shipping_sent_at"));
        order.setPersonalDataPurgedAt(toInstant(resultSet, "personal_data_purged_at"));
        order.setHandDelivery(resultSet.getBoolean("hand_delivery"));
        String trackingNumber = resultSet.getString("tracking_number");
        if (trackingNumber != null) {
            ShippingLabel label = new ShippingLabel();
            label.setCarrier(ShippingCarrier.valueOf(resultSet.getString("carrier")));
            label.setTrackingNumber(trackingNumber);
            label.setWeightGrams(resultSet.getInt("parcel_weight_grams"));
            // Nul pour une étiquette créée avant la saisie des dimensions : getInt rend alors 0.
            label.setLengthCm(resultSet.getInt("parcel_length_cm"));
            label.setWidthCm(resultSet.getInt("parcel_width_cm"));
            label.setDepthCm(resultSet.getInt("parcel_depth_cm"));
            label.setReference(resultSet.getString("label_reference"));
            label.setStatus(ShippingLabelStatus.valueOf(resultSet.getString("label_status")));
            label.setCreatedAt(order.getCraftedAt());
            order.setLabel(label);
        }
        return order;
    }

    private static Instant toInstant(ResultSet resultSet, String column) throws SQLException {
        OffsetDateTime value = resultSet.getObject(column, OffsetDateTime.class);
        return value != null ? value.toInstant() : null;
    }

    private static OffsetDateTime toOffsetDateTime(Instant instant) {
        return instant != null ? instant.atOffset(ZoneOffset.UTC) : null;
    }

}
