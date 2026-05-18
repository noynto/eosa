package me.noynto.eosa.identity;

import me.noynto.eosa.shared.IdentityId;

public class Identity {
    private IdentityId id;
    private String name;
    private String secret;
    private boolean administrator;

    public Identity() {
    }

    public IdentityId getId() {
        return id;
    }

    public void setId(IdentityId id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public boolean isAdministrator() {
        return administrator;
    }

    public void setAdministrator(boolean administrator) {
        this.administrator = administrator;
    }
}
