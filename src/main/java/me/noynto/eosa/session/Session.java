package me.noynto.eosa.session;

import me.noynto.eosa.shared.IdentityId;
import me.noynto.eosa.shared.SessionId;

public class Session {
    private SessionId id;
    private IdentityId identityId;

    public Session() {
    }

    public SessionId getId() {
        return id;
    }

    public void setId(SessionId id) {
        this.id = id;
    }

    public IdentityId getIdentityId() {
        return identityId;
    }

    public void setIdentityId(IdentityId identityId) {
        this.identityId = identityId;
    }
}
