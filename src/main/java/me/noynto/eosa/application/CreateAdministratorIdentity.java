package me.noynto.eosa.application;

import me.noynto.eosa.hash.CryptProvider;
import me.noynto.eosa.hash.Plain;
import me.noynto.eosa.identity.Identity;
import me.noynto.eosa.identity.IdentityProvider;

public record CreateAdministratorIdentity(
        IdentityProvider identityProvider,
        CryptProvider cryptProvider
) {

    public Identity handle(Command command) {
        Identity identity = new Identity();
        identity.setName(command.name());
        identity.setSecret(cryptProvider.hash(new Plain(command.secret())).value());
        identity.setAdministrator(true);
        return this.identityProvider.write(identity);
    }

    public record Command(
            String name,
            String secret
    ) {

    }

}
