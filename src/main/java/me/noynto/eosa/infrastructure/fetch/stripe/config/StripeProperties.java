package me.noynto.eosa.infrastructure.fetch.stripe.config;

public record StripeProperties(
        String secretKey,
        String successPath,
        String cancelPath
) {
}