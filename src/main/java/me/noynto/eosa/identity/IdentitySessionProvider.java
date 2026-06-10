package me.noynto.eosa.identity;

import me.noynto.eosa.shared.IdentitySessionId;

import java.util.Optional;
import java.util.stream.Stream;

public interface IdentitySessionProvider {

    Stream<IdentitySessionId> readIds();

    Optional<IdentitySession> read(IdentitySessionId identitySessionId);

    IdentitySession write(IdentitySession identitySession);

}
