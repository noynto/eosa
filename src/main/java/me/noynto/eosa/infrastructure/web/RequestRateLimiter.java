package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

/**
 * Limite le nombre de demandes par adresse IP sur une fenêtre glissante, en mémoire (par instance).
 */
public class RequestRateLimiter {

    private final int maxRequests;
    private final Duration window;
    private final Clock clock;
    private final Map<String, Deque<Instant>> requestsByClient = new HashMap<>();

    public RequestRateLimiter(int maxRequests, Duration window, Clock clock) {
        this.maxRequests = maxRequests;
        this.window = window;
        this.clock = clock;
    }

    /**
     * Compte une demande du client ; renvoie false s'il a déjà atteint la limite sur la fenêtre.
     */
    public synchronized boolean tryAcquire(String client) {
        Instant now = clock.instant();
        Instant windowStart = now.minus(window);
        // Oublie les clients sans demande récente, pour que la mémoire ne grossisse pas indéfiniment.
        requestsByClient.values().forEach(requests -> {
            while (!requests.isEmpty() && !requests.peekFirst().isAfter(windowStart)) {
                requests.pollFirst();
            }
        });
        requestsByClient.values().removeIf(Deque::isEmpty);

        Deque<Instant> requests = requestsByClient.computeIfAbsent(client, key -> new ArrayDeque<>());
        if (requests.size() >= maxRequests) {
            return false;
        }
        requests.addLast(now);
        return true;
    }

    /**
     * Adresse du client : la première de l'en-tête X-Forwarded-For posé par l'ingress, sinon celle de la connexion.
     */
    public static String clientOf(Context ctx) {
        String forwardedFor = ctx.header("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].strip();
        }
        return ctx.ip();
    }

}
