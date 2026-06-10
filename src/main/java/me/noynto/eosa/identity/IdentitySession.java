package me.noynto.eosa.identity;

import me.noynto.eosa.shared.IdentityId;
import me.noynto.eosa.shared.IdentitySessionId;

import java.time.LocalDateTime;

public class IdentitySession {
    private IdentitySessionId id;
    private IdentityId identityId;
    private LocalDateTime begin;

    public IdentitySessionId getId() { return id; }
    public void setId(IdentitySessionId id) { this.id = id; }

    public IdentityId getIdentityId() { return identityId; }
    public void setIdentityId(IdentityId identityId) { this.identityId = identityId; }

    public LocalDateTime getBegin() {
        return begin;
    }

    public void setBegin(LocalDateTime begin) {
        this.begin = begin;
    }
}
