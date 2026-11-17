package me.noynto.eosa.application;

import me.noynto.eosa.order.Order;
import me.noynto.eosa.order.OrderProvider;
import me.noynto.eosa.order.OrderState;
import me.noynto.eosa.service.BusinessTime;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Function;

/**
 * Les chiffres du mois en cours (jours calendaires à Paris), d'après les commandes payées, annulées exclues.
 * Le chiffre d'affaires est la valeur des commandes, livraison comprise, y compris la part payée par une carte
 * cadeau (déjà encaissée à l'achat de la carte, elle est comptée ici quand elle sert). Il se compare au nombre de
 * commandes du mois précédent.
 */
public record ReadDashboardMonth(
        OrderProvider orderProvider,
        Clock clock
) {

    public Result handle() {
        Instant now = clock.instant();
        LocalDate firstDay = now.atZone(BusinessTime.ZONE).toLocalDate().withDayOfMonth(1);
        Instant monthStart = firstDay.atStartOfDay(BusinessTime.ZONE).toInstant();
        Instant previousMonthStart = firstDay.minusMonths(1).atStartOfDay(BusinessTime.ZONE).toInstant();

        // La commande est créée au paiement : sa date de création sert de borne basse, la date de paiement tranche.
        List<Order> orders = orderProvider.readIds(null, null, previousMonthStart, null)
                .map(id -> orderProvider.read(id).orElseThrow())
                .filter(order -> order.getState() != OrderState.CANCELLED)
                .toList();
        List<Order> thisMonth = orders.stream().filter(order -> !order.getPaidAt().isBefore(monthStart)).toList();
        int previousCount = (int) orders.stream().filter(order -> order.getPaidAt().isBefore(monthStart)).count();

        BigDecimal revenue = sum(thisMonth, order -> order.getTotalAmount().add(order.getGiftCardAmount()));
        BigDecimal shipping = sum(thisMonth, Order::getShippingAmount);
        BigDecimal items = sum(thisMonth, Order::getItemsAmount);
        BigDecimal promoDiscount = sum(thisMonth, Order::getDiscountAmount);
        BigDecimal giftCardPart = sum(thisMonth, Order::getGiftCardAmount);

        int count = thisMonth.size();
        BigDecimal averageBasket = count == 0 ? BigDecimal.ZERO
                : revenue.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);
        BigDecimal averageItems = count == 0 ? BigDecimal.ZERO
                : items.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);

        return new Result(firstDay, revenue, shipping, count, previousCount, averageBasket, averageItems,
                promoDiscount, giftCardPart);
    }

    private static BigDecimal sum(List<Order> orders, Function<Order, BigDecimal> amount) {
        return orders.stream().map(amount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * @param firstDay        premier jour du mois, pour le titre
     * @param revenue         valeur des commandes du mois, livraison et part de carte cadeau comprises
     * @param shipping        dont la livraison
     * @param orders          commandes payées du mois, annulées exclues
     * @param previousOrders  commandes du mois précédent, pour comparer
     * @param averageBasket   valeur moyenne d'une commande, livraison comprise
     * @param averageItems    valeur moyenne des bijoux d'une commande, avant remise
     * @param promoDiscount   remises accordées par des codes de promotion
     * @param giftCardPart    part des commandes payée par des cartes cadeau
     */
    public record Result(
            LocalDate firstDay,
            BigDecimal revenue,
            BigDecimal shipping,
            int orders,
            int previousOrders,
            BigDecimal averageBasket,
            BigDecimal averageItems,
            BigDecimal promoDiscount,
            BigDecimal giftCardPart
    ) {
    }

}
