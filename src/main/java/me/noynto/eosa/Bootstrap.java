package me.noynto.eosa;

import gg.jte.ContentType;
import gg.jte.TemplateEngine;
import io.javalin.Javalin;
import io.javalin.rendering.template.JavalinJte;

import java.nio.file.Path;

public class Bootstrap {

    public static void main(String[] args) {
        Path targetDirectory = Path.of("jte-classes"); // This is the directory where compiled templates are located.
        TemplateEngine templateEngine = TemplateEngine.createPrecompiled(targetDirectory, ContentType.Html);
        var app = Javalin.create(javalinConfig -> {
            javalinConfig.fileRenderer(new JavalinJte(templateEngine));
            javalinConfig.routes.get("/", context -> context.render("index.jte"));
            javalinConfig.routes.get("/products/1", context -> context.render("product.jte"));
            javalinConfig.routes.get("/cart", context -> context.render("cart.jte"));
            javalinConfig.routes.get("/payment", context -> context.render("payment.jte"));
        });
        app.start();
    }

}
