package me.noynto.eosa.infrastructure.fetch.photon.config;

import java.net.http.HttpClient;
import java.time.Duration;
import java.time.temporal.ChronoUnit;

public class PhotonConfiguration {

    public HttpClient client() {
        return HttpClient
                .newBuilder()
                .connectTimeout(Duration.of(5, ChronoUnit.SECONDS))
                .build();
    }

    public PhotonProperties getProperties() {
        return new PhotonProperties(java.net.URI.create("https://photon.komoot.io"));
    }

}
