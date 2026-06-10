package me.noynto.eosa.checkout;

import me.noynto.eosa.shared.CheckoutId;
import me.noynto.eosa.shared.CheckoutSessionId;

import java.util.Optional;
import java.util.stream.Stream;

public interface CheckoutProvider {

    Stream<CheckoutId> readIds(CheckoutSessionId sessionId);

    Optional<Checkout> read(CheckoutId id);

    Checkout write(Checkout checkout);

}