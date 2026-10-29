package me.noynto.eosa.mail;

public interface MailProvider {

    /**
     * Envoie l'e-mail ; lève une exception si le serveur d'envoi le refuse ou est injoignable.
     */
    void send(Mail mail);

}
