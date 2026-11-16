-- Utilisation d'une carte cadeau au panier : la carte attachée au panier (détachée si elle est supprimée) et la part
-- qu'elle a payée sur la commande, photographiée comme les prix. Une commande ne porte jamais à la fois une remise
-- de code de promotion et une part de carte (decision 10 de la spécification).
ALTER TABLE carts ADD COLUMN gift_card_id UUID REFERENCES gift_cards (id) ON DELETE SET NULL;

ALTER TABLE orders ADD COLUMN gift_card_id UUID REFERENCES gift_cards (id);
ALTER TABLE orders ADD COLUMN gift_card_amount NUMERIC(10, 2) NOT NULL DEFAULT 0;
