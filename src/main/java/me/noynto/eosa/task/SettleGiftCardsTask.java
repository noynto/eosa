package me.noynto.eosa.task;

import me.noynto.eosa.application.SettleGiftCards;

import java.util.logging.Level;
import java.util.logging.Logger;

public record SettleGiftCardsTask(
        SettleGiftCards settleGiftCards
) {
    private static final String ENABLE = "EOSA_SETTLE_GIFT_CARDS_TASK";
    private static final Logger LOGGER = Logger.getLogger(SettleGiftCardsTask.class.getName());

    public boolean task() {
        LOGGER.log(Level.INFO, "Démarrage de la tâche de rattrapage des cartes cadeau.");
        try {
            var result = settleGiftCards.handle();
            LOGGER.log(Level.INFO, result.activated() + " carte(s) activée(s), " + result.emailsSent() + " e-mail(s) envoyé(s), "
                    + result.deleted() + " carte(s) jamais payée(s) supprimée(s), " + result.failed() + " échec(s).");
            return result.failed() == 0;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Le rattrapage des cartes cadeau a échoué.", e);
            return false;
        }
    }

    public static boolean activate() {
        return Boolean.parseBoolean(System.getenv().getOrDefault(ENABLE, "false"));
    }
}
