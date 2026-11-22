package me.noynto.eosa.application;

import me.noynto.eosa.charm.CharmProvider;
import me.noynto.eosa.jewel.JewelProvider;
import me.noynto.eosa.metal.MetalColorProvider;
import me.noynto.eosa.shared.CharmId;
import me.noynto.eosa.shared.JewelId;
import me.noynto.eosa.shared.MetalColorId;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * La sélection en cours sur la fiche d'un bijou : la couleur du métal (la première si le client n'en a pas
 * encore choisi, comme dans le formulaire), les breloques cochées et le prix total, calculé comme au panier.
 */
public record ReadJewelSelection(
        JewelProvider jewelProvider,
        MetalColorProvider metalColorProvider,
        CharmProvider charmProvider
) {

    public Result handle(Command command) {
        if (command.jewelId == null || command.jewelId.value() == null) {
            throw new RuntimeException("L'identifiant du bijou est nécessaire.");
        }
        var jewel = jewelProvider.read(command.jewelId)
                .orElseThrow(() -> new RuntimeException("Le bijou " + command.jewelId.value() + " n'existe pas."));

        Optional<String> metalColorName = (command.metalColorId != null && command.metalColorId.value() != null
                ? metalColorProvider.read(command.metalColorId)
                : metalColorProvider.readIds().findFirst().flatMap(metalColorProvider::read))
                .map(metalColor -> metalColor.getName());

        List<CharmId> requestedCharmIds = command.charmIds != null ? command.charmIds : List.of();
        var charms = requestedCharmIds.stream()
                .map(charmProvider::read)
                .flatMap(Optional::stream)
                .toList();

        BigDecimal total = charms.stream()
                .map(charm -> charm.getPrice())
                .reduce(jewel.getPrice(), BigDecimal::add);

        return new Result(
                metalColorName.orElse(null),
                charms.stream().map(charm -> charm.getName()).toList(),
                total
        );
    }

    public record Command(
            JewelId jewelId,
            MetalColorId metalColorId,
            List<CharmId> charmIds
    ) {
    }

    public record Result(
            String metalColorName,
            List<String> charmNames,
            BigDecimal total
    ) {
    }

}
