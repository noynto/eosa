-- Traces des e-mails de commande : un e-mail parti est daté, un e-mail resté NULL est renvoyé par la tâche SettleOrders.
ALTER TABLE orders
    ADD COLUMN confirmation_sent_at TIMESTAMPTZ,
    ADD COLUMN notification_sent_at TIMESTAMPTZ;

-- Les commandes déjà enregistrées ne reçoivent pas d'e-mail rétroactif.
UPDATE orders SET confirmation_sent_at = created_at, notification_sent_at = created_at;
