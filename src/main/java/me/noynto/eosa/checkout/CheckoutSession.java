package me.noynto.eosa.checkout;

import me.noynto.eosa.shared.CheckoutSessionId;

import java.net.URI;

public class CheckoutSession {

    private CheckoutSessionId id;
    private URI uri;
    private CheckoutSessionStatus status;

    public CheckoutSession() {
    }

    public CheckoutSessionId getId() {
        return id;
    }

    public void setId(CheckoutSessionId id) {
        this.id = id;
    }

    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

    public CheckoutSessionStatus getStatus() {
        return status;
    }

    public void setStatus(CheckoutSessionStatus status) {
        this.status = status;
    }
}