package me.noynto.eosa.application;

import me.noynto.eosa.mail.MailProvider;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class SendTestEmailTest {

    @Mock MailProvider mailProvider;

    @Test
    void handle_sendsTheTestTemplateToTheGivenAddress() {
        new SendTestEmail(mailProvider).handle(new SendTestEmail.Command(" contact@eosa.me "));

        verify(mailProvider).send(argThat(mail ->
                "contact@eosa.me".equals(mail.to()) && "test".equals(mail.template())
        ));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "contact", "contact@eosa", "con tact@eosa.me"})
    void handle_throwsWhenAddressIsInvalid(String to) {
        assertThrows(RuntimeException.class, () ->
                new SendTestEmail(mailProvider).handle(new SendTestEmail.Command(to))
        );
        verifyNoInteractions(mailProvider);
    }

}
