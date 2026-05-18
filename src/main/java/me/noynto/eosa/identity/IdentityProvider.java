package me.noynto.eosa.identity;

import me.noynto.eosa.shared.IdentityId;

import java.util.stream.Stream;

public interface IdentityProvider {

    Stream<IdentityId> readIds();

    Identity read(IdentityId identityId) throws UnknownIdentity;

    Identity write(Identity identity);

    class UnknownIdentity extends Exception {
        private final IdentityId identityId;

        public UnknownIdentity(IdentityId identityId) {
            this.identityId = identityId;
        }

        public IdentityId getIdentityId() {
            return identityId;
        }
    }
}
