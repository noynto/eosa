package me.noynto.eosa.checkout;

import me.noynto.eosa.shared.CheckoutId;
import me.noynto.eosa.shared.CheckoutSessionId;

import java.time.Instant;
import java.util.Optional;
import java.util.stream.Stream;

public interface CheckoutProvider {

    /**
     * Identifiants des sessions de paiement. Avec {@code sessionId}, la session demandée (les autres filtres sont
     * alors sans effet) ; sinon les sessions créées à partir de {@code createdSince}, de {@code status} si non null.
     */
    Stream<CheckoutId> readIds(CheckoutSessionId sessionId, CheckoutStatus status, Instant createdSince);

    Optional<Checkout> read(CheckoutId id);

    Checkout write(Checkout checkout);

}