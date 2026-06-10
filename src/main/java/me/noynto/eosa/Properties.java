package me.noynto.eosa;

import java.net.URI;

public record Properties(
        URI baseUrl,
        String adminName,
        String adminSecret
) {
}
