-- Une carte n'est émise qu'une fois : l'activation peut être lancée en même temps par le retour de la personne
-- et par la tâche de rattrapage, le second « ISSUED » est refusé au lieu de doubler le solde.
CREATE UNIQUE INDEX gift_card_movements_single_issue_idx ON gift_card_movements (gift_card_id) WHERE kind = 'ISSUED';
