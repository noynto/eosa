package me.noynto.eosa.hash;

public interface CryptProvider {

    Hash hash(Plain plain);

    boolean check(Plain plain, Hash hash);

}
