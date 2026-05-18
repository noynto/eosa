package me.noynto.eosa.infrastructure.web;

import java.util.Base64;

public class BasicAuth {

    public static boolean isAuthorized(String header, String expectedId, String expectedSecret) {
        if (header == null || !header.startsWith("Basic ")) return false;
        String decoded = new String(Base64.getDecoder().decode(header.substring(6)));
        String[] parts = decoded.split(":", 2);
        return parts.length == 2 && parts[0].equals(expectedId) && parts[1].equals(expectedSecret);
    }

}