-- Remise en main propre : la commande n'est pas expédiée, elle est remise à la cliente sans transporteur ni étiquette.
ALTER TABLE orders
    ADD COLUMN hand_delivery BOOLEAN NOT NULL DEFAULT FALSE;
