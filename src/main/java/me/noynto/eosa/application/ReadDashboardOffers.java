package me.noynto.eosa.application;

import me.noynto.eosa.giftcard.GiftCard;
import me.noynto.eosa.giftcard.GiftCardProvider;
import me.noynto.eosa.giftcard.GiftCardState;
import me.noynto.eosa.jewel.Jewel;
import me.noynto.eosa.jewel.JewelCategory;
import me.noynto.eosa.jewel.JewelProvider;
import me.noynto.eosa.jewel.JewelState;
import me.noynto.eosa.order.Order;
import me.noynto.eosa.order.OrderProvider;
import me.noynto.eosa.order.OrderState;
import me.noynto.eosa.promo.PromoCode;
import me.noynto.eosa.promo.PromoCodeProvider;
import me.noynto.eosa.service.BusinessTime;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Les offres et le catalogue d'un coup d'œil : l'état des cartes cadeau (dont ce qu'EOSA doit encore honorer),
 * les codes de promotion en cours et le nombre de bijoux par état. Les usages du mois sont ceux du mois
 * calendaire à Paris.
 */
public record ReadDashboardOffers(
        GiftCardProvider giftCardProvider,
        PromoCodeProvider promoCodeProvider,
        OrderProvider orderProvider,
        JewelProvider jewelProvider,
        Clock clock
) {

    public Result handle() {
        Instant now = clock.instant();
        Instant monthStart = now.atZone(BusinessTime.ZONE).toLocalDate().withDayOfMonth(1)
                .atStartOfDay(BusinessTime.ZONE).toInstant();
        return new Result(giftCards(now, monthStart), promoCodes(now, monthStart), catalogue());
    }

    private GiftCards giftCards(Instant now, Instant monthStart) {
        List<GiftCard> cards = giftCardProvider.readIds(null, null)
                .map(id -> giftCardProvider.read(id).orElseThrow())
                .toList();
        List<GiftCard> usable = cards.stream().filter(card -> card.isUsable(now)).toList();
        BigDecimal toHonour = usable.stream().map(GiftCard::balance).reduce(BigDecimal.ZERO, BigDecimal::add);
        int issuedThisMonth = (int) cards.stream()
                .filter(card -> card.getState() != GiftCardState.PENDING_PAYMENT)
                .filter(card -> !card.getCreatedAt().isBefore(monthStart))
                .count();
        Instant nextExpiry = usable.stream().map(GiftCard::getExpiresAt).min(Instant::compareTo).orElse(null);
        return new GiftCards(usable.size(), toHonour, issuedThisMonth, nextExpiry);
    }

    private PromoCodes promoCodes(Instant now, Instant monthStart) {
        List<PromoCode> codes = promoCodeProvider.readIds(null)
                .map(id -> promoCodeProvider.read(id).orElseThrow())
                .toList();
        int inForce = (int) codes.stream()
                .filter(PromoCode::isActive)
                .filter(code -> code.getStartsAt() == null || !now.isBefore(code.getStartsAt()))
                .filter(code -> code.getEndsAt() == null || now.isBefore(code.getEndsAt()))
                .count();

        List<Order> monthOrders = orderProvider.readIds(null, null, monthStart, null)
                .map(id -> orderProvider.read(id).orElseThrow())
                .filter(order -> order.getPromoCodeId() != null)
                .toList();
        // Un usage est compté même si la commande est annulée ; la remise accordée, non.
        int usesThisMonth = monthOrders.size();
        BigDecimal discountThisMonth = monthOrders.stream()
                .filter(order -> order.getState() != OrderState.CANCELLED)
                .map(Order::getDiscountAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Integer> usesByCode = new HashMap<>();
        monthOrders.forEach(order -> usesByCode.merge(order.getPromoCodeId().value(), 1, Integer::sum));
        TopCode top = usesByCode.entrySet().stream()
                .max(Map.Entry.<String, Integer>comparingByValue().thenComparing(Map.Entry.comparingByKey(Comparator.reverseOrder())))
                .flatMap(entry -> codes.stream().filter(code -> code.getId().value().equals(entry.getKey())).findFirst())
                .map(code -> new TopCode(code.getCode(),
                        (int) orderProvider.readIds(null, null, null, code.getId()).count(),
                        code.getMaxUses(), code.getEndsAt()))
                .orElse(null);
        return new PromoCodes(inForce, usesThisMonth, discountThisMonth, top);
    }

    private Catalogue catalogue() {
        List<Jewel> jewels = jewelProvider.readIds(null, null)
                .map(id -> jewelProvider.read(id).orElseThrow())
                .toList();
        List<Jewel> published = jewels.stream().filter(jewel -> jewel.getState() == JewelState.PUBLISHED).toList();
        return new Catalogue(
                published.size(),
                (int) published.stream().filter(jewel -> jewel.getCategory() == JewelCategory.NECKLACE).count(),
                (int) published.stream().filter(jewel -> jewel.getCategory() == JewelCategory.BRACELET).count(),
                (int) jewels.stream().filter(jewel -> jewel.getState() == JewelState.DRAFTED).count(),
                (int) jewels.stream().filter(jewel -> jewel.getState() == JewelState.ARCHIVED).count(),
                published.stream().map(Jewel::getPublishedAt).filter(Objects::nonNull)
                        .max(Instant::compareTo).orElse(null));
    }

    /**
     * @param usable        cartes utilisables : actives, non expirées, solde positif
     * @param toHonour      somme de leurs soldes : ce qu'EOSA doit encore honorer
     * @param issuedThisMonth cartes émises ce mois-ci, payées ou offertes
     * @param nextExpiry    fin de validité (exclusive) de la carte utilisable qui expire la première, null sans carte
     */
    public record GiftCards(int usable, BigDecimal toHonour, int issuedThisMonth, Instant nextExpiry) {
    }

    /**
     * @param inForce          codes actifs et dans leurs dates
     * @param usesThisMonth    commandes passées avec un code ce mois-ci
     * @param discountThisMonth remise accordée ce mois-ci, commandes annulées exclues
     * @param top              le code le plus utilisé ce mois-ci, null sans usage
     */
    public record PromoCodes(int inForce, int usesThisMonth, BigDecimal discountThisMonth, TopCode top) {
    }

    /**
     * @param uses    usages depuis la création, annulées comprises
     * @param maxUses limite d'usages, null sans limite
     * @param endsAt  fin de validité (exclusive), null sans limite
     */
    public record TopCode(String code, int uses, Integer maxUses, Instant endsAt) {
    }

    public record Catalogue(int published, int necklaces, int bracelets, int drafted, int archived, Instant lastPublishedAt) {
    }

    public record Result(GiftCards giftCards, PromoCodes promoCodes, Catalogue catalogue) {
    }

}
