package me.noynto.eosa.infrastructure.web;

import com.google.gson.Gson;

import java.util.List;
import java.util.Map;

/**
 * Structured data (schema.org) for search engines, written in the {@code <head>} by {@code header-main} from the
 * {@code jsonLd} entry of the model.
 * <p>
 * Gson escapes {@code <}, {@code >}, {@code &}, {@code =} and {@code '} as {@code <}…: the JSON can be written
 * without HTML escaping ({@code {{{jsonLd}}}}) and still cannot close the {@code <script>} tag. Mustache's HTML
 * escaping would instead leave {@code &#39;} in the names read by Google.
 */
final class JsonLd {

    private static final Gson GSON = new Gson();

    static final String INSTAGRAM_URL = "https://www.instagram.com/eosa.bijoux/";

    private JsonLd() {
    }

    static String of(Map<String, Object> data) {
        return GSON.toJson(data);
    }

    /**
     * La marque, reprise comme vendeur d'un bijou et organisatrice d'un atelier.
     */
    static Map<String, Object> organization(String baseUrl) {
        return Map.of(
                "@type", "Organization",
                "name", "EOSA",
                "url", baseUrl + "/",
                "logo", baseUrl + "/icon-512.png",
                "email", "contact@eosa.me",
                "sameAs", List.of(INSTAGRAM_URL),
                "address", Map.of(
                        "@type", "PostalAddress",
                        "addressLocality", "Nancy",
                        "addressCountry", "FR"
                )
        );
    }

}
