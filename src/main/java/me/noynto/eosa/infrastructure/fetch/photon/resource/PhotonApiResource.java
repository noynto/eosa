package me.noynto.eosa.infrastructure.fetch.photon.resource;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import me.noynto.eosa.infrastructure.fetch.photon.config.PhotonProperties;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Stream;

public record PhotonApiResource(
        HttpClient client,
        PhotonProperties properties
) {

    public Stream<Response.Feature> get(String query, Long limit, String lang) throws IOException, InterruptedException {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("La recherche est obligatoire");
        }
        HttpRequest request = HttpRequest.newBuilder()
                .uri(properties.baseUrl().resolve(getUri(query, limit, lang)))
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return new Gson().fromJson(response.body(), Response.class).features.stream();
    }

    private static URI getUri(String query, Long limit, String lang) {
        StringBuilder builder = new StringBuilder();
        builder.append("/");
        builder.append("api");
        builder.append("/");
        builder.append("?q=");
        builder.append(URLEncoder.encode(query, StandardCharsets.UTF_8));
        builder.append("&limit=");
        if (limit == null) {
            builder.append("5");
        } else {
            builder.append(URLEncoder.encode(String.valueOf(limit), StandardCharsets.UTF_8));
        }
        builder.append("&lang=");
        if (lang == null || lang.isBlank()) {
            builder.append("en");
        } else {
            builder.append(URLEncoder.encode(lang, StandardCharsets.UTF_8));
        }
        return URI.create(builder.toString());
    }

    public record Response(
            String type,
            List<Feature> features
    ) {

        public record Feature(
                String type,
                Properties properties,
                Geometry geometry
        ) {
        }

        public record Properties(
                @SerializedName("osm_type") String osmType,
                @SerializedName("osm_id") long osmId,
                @SerializedName("osm_key") String osmKey,
                @SerializedName("osm_value") String osmValue,
                String type,
                String name,
                String district,
                String city,
                String county,
                String state,
                String country,
                String postcode,
                String countrycode,
                List<Double> extent
        ) {
        }

        public record Geometry(
                String type,
                List<Double> coordinates
        ) {
        }

    }

}
