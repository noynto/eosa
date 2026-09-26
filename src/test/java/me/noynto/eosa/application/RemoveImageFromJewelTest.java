package me.noynto.eosa.application;

import me.noynto.eosa.identity.Identity;
import me.noynto.eosa.identity.IdentityProvider;
import me.noynto.eosa.image.ImageProvider;
import me.noynto.eosa.jewel.Jewel;
import me.noynto.eosa.jewel.JewelProvider;
import me.noynto.eosa.shared.IdentityId;
import me.noynto.eosa.shared.ImageId;
import me.noynto.eosa.shared.JewelId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RemoveImageFromJewelTest {

    @Mock IdentityProvider identityProvider;
    @Mock JewelProvider jewelProvider;
    @Mock ImageProvider imageProvider;

    @Test
    void handle_detachesImageThenDeletesIt() {
        var identityId = new IdentityId("admin1");
        when(identityProvider.read(identityId)).thenReturn(Optional.of(identityWith(identityId, true)));
        var jewelId = new JewelId("prod1");
        when(jewelProvider.read(jewelId)).thenReturn(Optional.of(jewelWith(jewelId, "img1", "img2", "img3")));
        when(jewelProvider.write(any())).thenAnswer(inv -> inv.getArgument(0));

        removeImageFromJewel().handle(
                new RemoveImageFromJewel.Command(identityId, jewelId, new ImageId("img2"))
        );

        InOrder order = inOrder(jewelProvider, imageProvider);
        order.verify(jewelProvider).write(argThat(p ->
                p.getImageIds().equals(List.of(new ImageId("img1"), new ImageId("img3")))
        ));
        order.verify(imageProvider).delete(new ImageId("img2"));
    }

    @Test
    void handle_throwsWhenImageDoesNotBelongToJewel() {
        var identityId = new IdentityId("admin1");
        when(identityProvider.read(identityId)).thenReturn(Optional.of(identityWith(identityId, true)));
        var jewelId = new JewelId("prod1");
        when(jewelProvider.read(jewelId)).thenReturn(Optional.of(jewelWith(jewelId, "img1")));

        assertThrows(RuntimeException.class, () ->
                removeImageFromJewel().handle(
                        new RemoveImageFromJewel.Command(identityId, jewelId, new ImageId("other"))
                )
        );
        verify(jewelProvider, never()).write(any());
        verify(imageProvider, never()).delete(any());
    }

    @Test
    void handle_throwsWhenJewelNotFound() {
        var identityId = new IdentityId("admin1");
        when(identityProvider.read(identityId)).thenReturn(Optional.of(identityWith(identityId, true)));
        var jewelId = new JewelId("unknown");
        when(jewelProvider.read(jewelId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () ->
                removeImageFromJewel().handle(
                        new RemoveImageFromJewel.Command(identityId, jewelId, new ImageId("img1"))
                )
        );
        verify(imageProvider, never()).delete(any());
    }

    @Test
    void handle_throwsWhenIdentityNotFound() {
        var identityId = new IdentityId("unknown");
        when(identityProvider.read(identityId)).thenReturn(Optional.empty());

        assertThrows(RemoveImageFromJewel.UnknownIdentity.class, () ->
                removeImageFromJewel().handle(
                        new RemoveImageFromJewel.Command(identityId, new JewelId("prod1"), new ImageId("img1"))
                )
        );
        verify(jewelProvider, never()).write(any());
        verify(imageProvider, never()).delete(any());
    }

    @Test
    void handle_throwsWhenNotAdministrator() {
        var identityId = new IdentityId("user1");
        when(identityProvider.read(identityId)).thenReturn(Optional.of(identityWith(identityId, false)));

        assertThrows(RemoveImageFromJewel.NotAuthorized.class, () ->
                removeImageFromJewel().handle(
                        new RemoveImageFromJewel.Command(identityId, new JewelId("prod1"), new ImageId("img1"))
                )
        );
        verify(jewelProvider, never()).write(any());
        verify(imageProvider, never()).delete(any());
    }

    @Test
    void handle_throwsWhenIdentityIdMissing() {
        assertThrows(RemoveImageFromJewel.InvalidCommand.class, () ->
                removeImageFromJewel().handle(
                        new RemoveImageFromJewel.Command(null, new JewelId("prod1"), new ImageId("img1"))
                )
        );
        verify(imageProvider, never()).delete(any());
    }

    private RemoveImageFromJewel removeImageFromJewel() {
        return new RemoveImageFromJewel(identityProvider, jewelProvider, imageProvider);
    }

    private Identity identityWith(IdentityId id, boolean administrator) {
        var identity = new Identity();
        identity.setId(id);
        identity.setAdministrator(administrator);
        return identity;
    }

    private Jewel jewelWith(JewelId jewelId, String... imageIds) {
        var jewel = new Jewel();
        jewel.setId(jewelId);
        jewel.setImageIds(Arrays.stream(imageIds).map(ImageId::new).toList());
        return jewel;
    }

}
