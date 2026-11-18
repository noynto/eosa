package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import me.noynto.eosa.application.DownloadVideoRange;
import me.noynto.eosa.shared.VideoId;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;

public record GetVideoHandler(DownloadVideoRange downloadVideoRange) implements Handler {

    @Override
    public void handle(Context ctx) {
        DownloadVideoRange.Result result;
        try {
            result = downloadVideoRange.handle(new DownloadVideoRange.Command(new VideoId(ctx.pathParam("id")), ctx.header("Range")));
        } catch (RuntimeException e) {
            ctx.status(404);
            return;
        }
        int size = result.video().getSize();
        // Safari et iOS ne lisent une vidéo que si le serveur répond aux plages.
        ctx.header("Accept-Ranges", "bytes");
        if (result.outcome() == DownloadVideoRange.Outcome.NOT_SATISFIABLE) {
            ctx.status(416);
            ctx.header("Content-Range", "bytes */" + size);
            return;
        }
        ctx.contentType(result.video().getFormat());
        ctx.header("Content-Security-Policy", "default-src 'none'; style-src 'unsafe-inline'; sandbox");
        // L'adresse porte l'identifiant de la vidéo : une autre vidéo aura une autre adresse.
        ctx.header("Cache-Control", "public, max-age=31536000, immutable");
        ctx.header("Content-Length", String.valueOf(result.length()));
        if (result.outcome() == DownloadVideoRange.Outcome.PARTIAL) {
            ctx.status(206);
            ctx.header("Content-Range", "bytes " + result.start() + "-" + result.end() + "/" + size);
        }
        try {
            OutputStream out = ctx.outputStream();
            while (result.content().hasNext()) {
                out.write(result.content().next());
            }
        } catch (IOException e) {
            // Le navigateur coupe souvent la connexion en changeant de plage : ce n'est pas une erreur du site.
            throw new UncheckedIOException(e);
        }
    }

}
