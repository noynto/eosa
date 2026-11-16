-- Code de promotion appliqué au panier. La remise n'est jamais stockée : elle se recalcule à chaque lecture.
-- Un code supprimé se détache tout seul des paniers.
ALTER TABLE carts
    ADD COLUMN promo_code_id UUID REFERENCES promo_codes (id) ON DELETE SET NULL;
