package me.noynto.eosa.infrastructure.security;

import me.noynto.eosa.hash.CryptProvider;
import me.noynto.eosa.hash.Hash;
import me.noynto.eosa.hash.Plain;
import org.mindrot.jbcrypt.BCrypt;

import java.util.Objects;

public record SecuredCrypts(

) implements CryptProvider {

    @Override
    public Hash hash(Plain plain) {
        Objects.requireNonNull(plain);
        String hashed = BCrypt.hashpw(plain.value(), BCrypt.gensalt(12));
        return new Hash(hashed);
    }

    @Override
    public boolean check(Plain plain, Hash hash) {
        return BCrypt.checkpw(plain.value(), hash.value());
    }
}
