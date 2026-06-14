package me.noynto.eosa.identity;

import me.noynto.eosa.shared.IdentityId;

import java.util.Optional;
import java.util.stream.Stream;

public interface IdentityProvider {

    Stream<IdentityId> readIds(Boolean isAdministrator, String name);

    Optional<Identity> read(IdentityId identityId);

    Identity write(Identity identity);

}
