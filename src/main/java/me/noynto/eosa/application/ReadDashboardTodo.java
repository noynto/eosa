package me.noynto.eosa.application;

import me.noynto.eosa.order.Order;
import me.noynto.eosa.order.OrderProvider;
import me.noynto.eosa.order.OrderState;
import me.noynto.eosa.service.BusinessTime;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Set;

/**
 * Ce qui attend l'administratrice : les pièces à fabriquer (avec l'échéance de la plus pressée), les colis à
 * expédier ou à remettre en main propre, et les colis en route. Les retards suivent les mêmes délais que la liste
 * des commandes ({@link Order#CRAFTING_HOURS}, {@link Order#SHIPPING_HOURS}, en heures ouvrées).
 */
public record ReadDashboardTodo(
        OrderProvider orderProvider,
        Clock clock
) {

    public Result handle() {
        Instant now = clock.instant();

        List<Order> toCraft = orders(OrderState.PAID);
        long craftOverdue = toCraft.stream()
                .filter(order -> BusinessTime.isOverdue(order.getPaidAt(), Order.CRAFTING_HOURS, now))
                .count();
        Instant nextCraftDeadline = toCraft.stream()
                .map(order -> BusinessTime.deadline(order.getPaidAt(), Order.CRAFTING_HOURS))
                .min(Instant::compareTo)
                .orElse(null);

        List<Order> toShip = orders(OrderState.CRAFTED);
        // Une remise en main propre se convient avec la cliente : pas d'échéance d'expédition.
        long shipOverdue = toShip.stream()
                .filter(order -> !order.isHandDelivery() && order.getCraftedAt() != null
                        && BusinessTime.isOverdue(order.getCraftedAt(), Order.SHIPPING_HOURS, now))
                .count();

        int inTransit = orders(OrderState.SHIPPED).size();

        return new Result(toCraft.size(), (int) craftOverdue, nextCraftDeadline,
                toShip.size(), (int) shipOverdue, inTransit);
    }

    private List<Order> orders(OrderState state) {
        return orderProvider.readIds(null, Set.of(state), null, null)
                .map(id -> orderProvider.read(id).orElseThrow())
                .toList();
    }

    /**
     * @param toCraft          commandes payées à fabriquer
     * @param craftOverdue     parmi elles, celles dont l'échéance de fabrication est passée
     * @param nextCraftDeadline échéance la plus proche (ou la plus dépassée), null sans commande à fabriquer
     * @param toShip           commandes fabriquées, à expédier ou à remettre en main propre
     * @param shipOverdue      parmi elles, celles à expédier dont l'échéance est passée
     * @param inTransit        commandes expédiées, pas encore livrées
     */
    public record Result(
            int toCraft,
            int craftOverdue,
            Instant nextCraftDeadline,
            int toShip,
            int shipOverdue,
            int inTransit
    ) {
    }

}
