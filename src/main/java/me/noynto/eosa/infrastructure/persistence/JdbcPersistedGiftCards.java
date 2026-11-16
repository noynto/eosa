package me.noynto.eosa.infrastructure.persistence;

import me.noynto.eosa.giftcard.GiftCard;
import me.noynto.eosa.giftcard.GiftCardMovement;
import me.noynto.eosa.giftcard.GiftCardMovementKind;
import me.noynto.eosa.giftcard.GiftCardProvider;
import me.noynto.eosa.giftcard.GiftCardState;
import me.noynto.eosa.shared.GiftCardId;
import me.noynto.eosa.shared.GiftCardMovementId;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

public record JdbcPersistedGiftCards(
        DataSource dataSource
) implements GiftCardProvider {

    @Override
    public Stream<GiftCardId> readIds(String code, String token) {
        StringBuilder sql = new StringBuilder("SELECT id FROM gift_cards WHERE TRUE");
        List<String> parameters = new ArrayList<>();
        if (code != null) {
            sql.append(" AND code = ?");
            parameters.add(code);
        }
        if (token != null) {
            sql.append(" AND token = ?");
            parameters.add(token);
        }
        sql.append(" ORDER BY created_at DESC, code");
        List<GiftCardId> ids = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            for (int i = 0; i < parameters.size(); i++) {
                statement.setString(i + 1, parameters.get(i));
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    ids.add(new GiftCardId(resultSet.getString("id")));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Impossible de lire les cartes cadeau.", e);
        }
        return ids.stream();
    }

    @Override
    public Optional<GiftCard> read(GiftCardId giftCardId) {
        UUID id;
        try {
            id = UUID.fromString(giftCardId.value());
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
        String sql = """
                SELECT id, code, token, state, initial_amount, buyer_first_name, buyer_email, email_sent_at,
                       expires_at, stripe_session_id, created_at
                FROM gift_cards WHERE id = ?
                """;
        String movementsSql = """
                SELECT id, kind, amount, checkout_session_id, reason, created_at
                FROM gift_card_movements WHERE gift_card_id = ? ORDER BY created_at, id
                """;
        try (Connection connection = dataSource.getConnection()) {
            GiftCard giftCard;
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setObject(1, id);
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (!resultSet.next()) {
                        return Optional.empty();
                    }
                    giftCard = toGiftCard(resultSet);
                }
            }
            try (PreparedStatement statement = connection.prepareStatement(movementsSql)) {
                statement.setObject(1, id);
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        giftCard.getMovements().add(toMovement(resultSet));
                    }
                }
            }
            return Optional.of(giftCard);
        } catch (SQLException e) {
            throw new RuntimeException("Impossible de lire la carte cadeau " + giftCardId.value() + ".", e);
        }
    }

    @Override
    public GiftCard write(GiftCard giftCard) {
        if (giftCard.getId() == null) {
            giftCard.setId(new GiftCardId(UUID.randomUUID().toString()));
        }
        String sql = """
                INSERT INTO gift_cards (id, code, token, state, initial_amount, buyer_first_name, buyer_email,
                                        email_sent_at, expires_at, stripe_session_id, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (id) DO UPDATE SET state = EXCLUDED.state, buyer_first_name = EXCLUDED.buyer_first_name,
                    buyer_email = EXCLUDED.buyer_email, email_sent_at = EXCLUDED.email_sent_at,
                    expires_at = EXCLUDED.expires_at, stripe_session_id = EXCLUDED.stripe_session_id
                """;
        String movementSql = """
                INSERT INTO gift_card_movements (id, gift_card_id, kind, amount, checkout_session_id, reason, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        UUID cardId = UUID.fromString(giftCard.getId().value());
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    statement.setObject(1, cardId);
                    statement.setString(2, giftCard.getCode());
                    statement.setString(3, giftCard.getToken());
                    statement.setString(4, giftCard.getState().name());
                    statement.setBigDecimal(5, giftCard.getInitialAmount());
                    statement.setString(6, giftCard.getBuyerFirstName());
                    statement.setString(7, giftCard.getBuyerEmail());
                    statement.setObject(8, toOffsetDateTime(giftCard.getEmailSentAt()));
                    statement.setObject(9, toOffsetDateTime(giftCard.getExpiresAt()));
                    statement.setString(10, giftCard.getStripeSessionId());
                    statement.setObject(11, toOffsetDateTime(giftCard.getCreatedAt() != null ? giftCard.getCreatedAt() : Instant.now()));
                    statement.executeUpdate();
                }
                for (GiftCardMovement movement : giftCard.getMovements()) {
                    if (movement.getId() != null) {
                        // Le journal ne se réécrit pas, sauf pour une réservation : elle est écrite avant la session de
                        // paiement, et la session lui est rattachée une fois créée (une seule fois).
                        if (movement.getCheckoutSessionId() != null) {
                            try (PreparedStatement statement = connection.prepareStatement(
                                    "UPDATE gift_card_movements SET checkout_session_id = ? "
                                            + "WHERE id = ? AND checkout_session_id IS NULL")) {
                                statement.setString(1, movement.getCheckoutSessionId());
                                statement.setObject(2, UUID.fromString(movement.getId().value()));
                                statement.executeUpdate();
                            }
                        }
                        continue;
                    }
                    movement.setId(new GiftCardMovementId(UUID.randomUUID().toString()));
                    try (PreparedStatement statement = connection.prepareStatement(movementSql)) {
                        statement.setObject(1, UUID.fromString(movement.getId().value()));
                        statement.setObject(2, cardId);
                        statement.setString(3, movement.getKind().name());
                        statement.setBigDecimal(4, movement.getAmount());
                        statement.setString(5, movement.getCheckoutSessionId());
                        statement.setString(6, movement.getReason());
                        statement.setObject(7, toOffsetDateTime(movement.getCreatedAt() != null ? movement.getCreatedAt() : Instant.now()));
                        statement.executeUpdate();
                    }
                }
                connection.commit();
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Impossible d'écrire la carte cadeau " + giftCard.getId().value() + ".", e);
        }
        return giftCard;
    }

    @Override
    public void delete(GiftCardId giftCardId) {
        UUID id;
        try {
            id = UUID.fromString(giftCardId.value());
        } catch (IllegalArgumentException e) {
            return;
        }
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM gift_cards WHERE id = ?")) {
            statement.setObject(1, id);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Impossible de supprimer la carte cadeau " + giftCardId.value() + ".", e);
        }
    }

    private static GiftCard toGiftCard(ResultSet resultSet) throws SQLException {
        GiftCard giftCard = new GiftCard();
        giftCard.setId(new GiftCardId(resultSet.getString("id")));
        giftCard.setCode(resultSet.getString("code"));
        giftCard.setToken(resultSet.getString("token"));
        giftCard.setState(GiftCardState.valueOf(resultSet.getString("state")));
        giftCard.setInitialAmount(resultSet.getBigDecimal("initial_amount"));
        giftCard.setBuyerFirstName(resultSet.getString("buyer_first_name"));
        giftCard.setBuyerEmail(resultSet.getString("buyer_email"));
        OffsetDateTime emailSentAt = resultSet.getObject("email_sent_at", OffsetDateTime.class);
        if (emailSentAt != null) giftCard.setEmailSentAt(emailSentAt.toInstant());
        giftCard.setExpiresAt(resultSet.getObject("expires_at", OffsetDateTime.class).toInstant());
        giftCard.setStripeSessionId(resultSet.getString("stripe_session_id"));
        giftCard.setCreatedAt(resultSet.getObject("created_at", OffsetDateTime.class).toInstant());
        return giftCard;
    }

    private static GiftCardMovement toMovement(ResultSet resultSet) throws SQLException {
        GiftCardMovement movement = new GiftCardMovement();
        movement.setId(new GiftCardMovementId(resultSet.getString("id")));
        movement.setKind(GiftCardMovementKind.valueOf(resultSet.getString("kind")));
        movement.setAmount(resultSet.getBigDecimal("amount"));
        movement.setCheckoutSessionId(resultSet.getString("checkout_session_id"));
        movement.setReason(resultSet.getString("reason"));
        movement.setCreatedAt(resultSet.getObject("created_at", OffsetDateTime.class).toInstant());
        return movement;
    }

    private static OffsetDateTime toOffsetDateTime(Instant instant) {
        return instant == null ? null : instant.atOffset(ZoneOffset.UTC);
    }

}
