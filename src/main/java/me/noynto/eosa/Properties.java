package me.noynto.eosa;

import java.net.URI;

public record Properties(
    URI publicBaseUrl,
    int publicPort,
    URI adminBaseUrl,
    int adminPort,
    String adminName,
    String adminSecret
) {}
