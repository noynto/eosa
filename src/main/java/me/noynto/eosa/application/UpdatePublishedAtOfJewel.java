package me.noynto.eosa.application;

import me.noynto.eosa.jewel.Jewel;
import me.noynto.eosa.jewel.JewelProvider;
import me.noynto.eosa.service.BusinessTime;
import me.noynto.eosa.shared.JewelId;

import java.time.Clock;
import java.time.LocalDate;

/**
 * Change la date de publication d'un bijou, qui ordonne les nouveautés. Seul un bijou déjà publié a une date : elle
 * se pose toute seule à la première publication. La date est un jour, à minuit heure de Paris, jamais dans le futur.
 */
public record UpdatePublishedAtOfJewel(
        JewelProvider jewelProvider,
        Clock clock
) {

    public Result handle(Command command) {
        if (command.jewelId == null || command.jewelId.value() == null) {
            throw new RuntimeException("L'identifiant du produit est nécessaire.");
        }
        if (command.publishedOn == null) {
            throw new RuntimeException("La date de publication est nécessaire.");
        }
        if (command.publishedOn.isAfter(clock.instant().atZone(BusinessTime.ZONE).toLocalDate())) {
            throw new RuntimeException("La date de publication ne peut pas être dans le futur.");
        }

        Jewel jewel = jewelProvider.read(command.jewelId)
                .orElseThrow(() -> new RuntimeException("Le produit " + command.jewelId.value() + " n'existe pas."));
        if (jewel.getPublishedAt() == null) {
            throw new RuntimeException("Ce produit n'a jamais été publié : sa date se pose à la première publication.");
        }

        jewel.setPublishedAt(command.publishedOn.atStartOfDay(BusinessTime.ZONE).toInstant());
        return new Result(jewelProvider.write(jewel));
    }

    public record Result(
            Jewel jewel
    ) {
    }

    public record Command(
            JewelId jewelId,
            LocalDate publishedOn
    ) {
    }

}
