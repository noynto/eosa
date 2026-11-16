package me.noynto.eosa.infrastructure.persistence;

import me.noynto.eosa.promo.PromoCode;
import me.noynto.eosa.promo.PromoCodeProvider;
import me.noynto.eosa.promo.PromoCodeType;
import me.noynto.eosa.shared.PromoCodeId;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

public record JdbcPersistedPromoCodes(
        DataSource dataSource
) implements PromoCodeProvider {

    @Override
    public Stream<PromoCodeId> readIds(String code) {
        String sql = "SELECT id FROM promo_codes" + (code == null ? "" : " WHERE code = ?") + " ORDER BY created_at DESC, code";
        List<PromoCodeId> ids = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            if (code != null) {
                statement.setString(1, code);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    ids.add(new PromoCodeId(resultSet.getString("id")));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Impossible de lire les codes de promotion.", e);
        }
        return ids.stream();
    }

    @Override
    public Optional<PromoCode> read(PromoCodeId promoCodeId) {
        UUID id;
        try {
            id = UUID.fromString(promoCodeId.value());
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
        String sql = """
                SELECT id, code, type, value, starts_at, ends_at, max_uses, minimum_amount, active, created_at
                FROM promo_codes WHERE id = ?
                """;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(toPromoCode(resultSet));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Impossible de lire le code de promotion " + promoCodeId.value() + ".", e);
        }
    }

    @Override
    public PromoCode write(PromoCode promoCode) {
        if (promoCode.getId() == null) {
            promoCode.setId(new PromoCodeId(UUID.randomUUID().toString()));
        }
        String sql = """
                INSERT INTO promo_codes (id, code, type, value, starts_at, ends_at, max_uses, minimum_amount, active, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (id) DO UPDATE SET code = EXCLUDED.code, type = EXCLUDED.type, value = EXCLUDED.value,
                    starts_at = EXCLUDED.starts_at, ends_at = EXCLUDED.ends_at, max_uses = EXCLUDED.max_uses,
                    minimum_amount = EXCLUDED.minimum_amount, active = EXCLUDED.active
                """;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, UUID.fromString(promoCode.getId().value()));
            statement.setString(2, promoCode.getCode());
            statement.setString(3, promoCode.getType().name());
            statement.setBigDecimal(4, promoCode.getValue());
            statement.setObject(5, toOffsetDateTime(promoCode.getStartsAt()));
            statement.setObject(6, toOffsetDateTime(promoCode.getEndsAt()));
            if (promoCode.getMaxUses() == null) {
                statement.setNull(7, Types.INTEGER);
            } else {
                statement.setInt(7, promoCode.getMaxUses());
            }
            statement.setBigDecimal(8, promoCode.getMinimumAmount());
            statement.setBoolean(9, promoCode.isActive());
            statement.setObject(10, toOffsetDateTime(promoCode.getCreatedAt() != null ? promoCode.getCreatedAt() : Instant.now()));
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Impossible d'écrire le code de promotion " + promoCode.getId().value() + ".", e);
        }
        return promoCode;
    }

    @Override
    public void delete(PromoCodeId promoCodeId) {
        UUID id;
        try {
            id = UUID.fromString(promoCodeId.value());
        } catch (IllegalArgumentException e) {
            return;
        }
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM promo_codes WHERE id = ?")) {
            statement.setObject(1, id);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Impossible de supprimer le code de promotion " + promoCodeId.value() + ".", e);
        }
    }

    private static PromoCode toPromoCode(ResultSet resultSet) throws SQLException {
        PromoCode promoCode = new PromoCode();
        promoCode.setId(new PromoCodeId(resultSet.getString("id")));
        promoCode.setCode(resultSet.getString("code"));
        promoCode.setType(PromoCodeType.valueOf(resultSet.getString("type")));
        promoCode.setValue(resultSet.getBigDecimal("value"));
        OffsetDateTime startsAt = resultSet.getObject("starts_at", OffsetDateTime.class);
        if (startsAt != null) promoCode.setStartsAt(startsAt.toInstant());
        OffsetDateTime endsAt = resultSet.getObject("ends_at", OffsetDateTime.class);
        if (endsAt != null) promoCode.setEndsAt(endsAt.toInstant());
        int maxUses = resultSet.getInt("max_uses");
        if (!resultSet.wasNull()) promoCode.setMaxUses(maxUses);
        promoCode.setMinimumAmount(resultSet.getBigDecimal("minimum_amount"));
        promoCode.setActive(resultSet.getBoolean("active"));
        promoCode.setCreatedAt(resultSet.getObject("created_at", OffsetDateTime.class).toInstant());
        return promoCode;
    }

    private static OffsetDateTime toOffsetDateTime(Instant instant) {
        return instant == null ? null : instant.atOffset(ZoneOffset.UTC);
    }

}
