package me.noynto.eosa.application;

import me.noynto.eosa.order.Order;
import me.noynto.eosa.order.OrderProvider;
import me.noynto.eosa.order.OrderState;
import me.noynto.eosa.service.BusinessTime;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Commandes pour l'administration, les plus récentes d'abord, filtrées par état et par une recherche sur le numéro,
 * le nom ou l'e-mail, chacune avec son éventuel retard.
 */
public record ReadOrders(
        OrderProvider orderProvider,
        Clock clock
) {

    /**
     * Délai de fabrication annoncé, en heures ouvrées, à compter du paiement.
     */
    public static final long CRAFTING_HOURS = 48;

    public Result handle(Command command) {
        Instant now = clock.instant();
        String search = command.search == null ? "" : command.search.strip().toLowerCase(Locale.ROOT);
        List<Entry> entries = orderProvider.readIds(null, command.states)
                .map(id -> orderProvider.read(id).orElseThrow())
                .filter(order -> matches(order, search))
                .map(order -> new Entry(order, isOverdue(order, now)))
                .toList();
        return new Result(entries);
    }

    private static boolean matches(Order order, String search) {
        if (search.isEmpty()) {
            return true;
        }
        return contains(order.getNumber(), search)
                || contains(order.getCustomerName(), search)
                || contains(order.getCustomerEmail(), search);
    }

    private static boolean contains(String value, String search) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(search);
    }

    private static boolean isOverdue(Order order, Instant now) {
        return order.getState() == OrderState.PAID
                && BusinessTime.isOverdue(order.getPaidAt(), CRAFTING_HOURS, now);
    }

    /**
     * @param states états recherchés, tous si null
     * @param search morceau du numéro, du nom ou de l'e-mail, ignoré s'il est vide ou null
     */
    public record Command(
            Set<OrderState> states,
            String search
    ) {
    }

    public record Result(
            List<Entry> orders
    ) {
    }

    public record Entry(
            Order order,
            boolean overdue
    ) {
    }

}
