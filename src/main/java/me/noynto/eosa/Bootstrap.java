package me.noynto.eosa;

import com.mongodb.client.MongoDatabase;
import gg.jte.ContentType;
import gg.jte.TemplateEngine;
import io.javalin.Javalin;
import io.javalin.rendering.template.JavalinJte;
import me.noynto.eosa.application.AddImagesToProduct;
import me.noynto.eosa.application.CreateAdministratorIdentity;
import me.noynto.eosa.application.CreateProduct;
import me.noynto.eosa.application.DownloadImage;
import me.noynto.eosa.application.ReadProduct;
import me.noynto.eosa.application.ReadProductIds;
import me.noynto.eosa.hash.CryptProvider;
import me.noynto.eosa.identity.IdentityProvider;
import me.noynto.eosa.image.ImageProvider;
import me.noynto.eosa.infrastructure.persistence.MongoPersistedIdentities;
import me.noynto.eosa.infrastructure.persistence.MongoPersistedImages;
import me.noynto.eosa.infrastructure.persistence.MongoPersistedProducts;
import me.noynto.eosa.infrastructure.persistence.MongoPersistedSessions;
import me.noynto.eosa.infrastructure.persistence.mongo.*;
import me.noynto.eosa.infrastructure.security.SecuredCrypts;
import me.noynto.eosa.infrastructure.web.*;
import me.noynto.eosa.product.ProductProvider;
import me.noynto.eosa.session.SessionProvider;

import java.nio.file.Path;

public class Bootstrap {

    public static void main(String[] args) {
        String adminId = System.getenv("EOSA_ADMIN_ID");
        String adminSecret = System.getenv("EOSA_ADMIN_SECRET");
        MongoConfiguration mongoConfiguration = new MongoConfiguration();
        MongoProperties mongoProperties = mongoConfiguration.getProperties();
        MongoDatabase mongoDatabase = mongoConfiguration.getDatabase(mongoProperties);

        // PROVIDER
        MongoConfiguredIdentities mongoConfiguredIdentities = new MongoConfiguredIdentities();
        IdentityProvider mongoPersistedIdentities = new MongoPersistedIdentities(mongoConfiguredIdentities.getCollection(mongoDatabase));
        MongoConfiguredSessions mongoConfiguredSessions = new MongoConfiguredSessions();
        SessionProvider mongoPersistedSessions = new MongoPersistedSessions(mongoConfiguredSessions.getCollection(mongoDatabase));
        MongoConfiguredProducts mongoConfiguredProducts = new MongoConfiguredProducts();
        ProductProvider mongoPersistedProducts = new MongoPersistedProducts(mongoConfiguredProducts.getCollection(mongoDatabase));
        MongoConfiguredImages mongoConfiguredImages = new MongoConfiguredImages();
        ImageProvider mongoPersistedImages = new MongoPersistedImages(mongoConfiguredImages.getBucket(mongoDatabase));
        CryptProvider cryptProvider = new SecuredCrypts();

        // JTE
        Path targetDirectory = Path.of("jte-classes"); // This is the directory where compiled templates are located.
        TemplateEngine templateEngine = TemplateEngine.createPrecompiled(targetDirectory, ContentType.Html);

        // HANDLER

        CreateProduct createProduct = new CreateProduct(mongoPersistedProducts);
        AddImagesToProduct addImagesToProduct = new AddImagesToProduct(mongoPersistedProducts, mongoPersistedImages);
        ReadProductIds readProductIds = new ReadProductIds(mongoPersistedProducts);
        ReadProduct readProduct = new ReadProduct(mongoPersistedProducts);
        DownloadImage downloadImage = new DownloadImage(mongoPersistedImages);
        CreateAdministratorIdentity createAdministratorIdentity = new CreateAdministratorIdentity(mongoPersistedIdentities, cryptProvider);
        createAdministratorIdentity.handle(new CreateAdministratorIdentity.Command("admin", "admin"));
        var pub = Javalin.create(javalinConfig -> {
            javalinConfig.fileRenderer(new JavalinJte(templateEngine));
            javalinConfig.routes.get("/", context -> context.render("index.jte"));
            javalinConfig.routes.get("/products", new GetProductsHandler(readProductIds));
            javalinConfig.routes.get("/products/{id}", new GetProductHandler(readProduct));
            javalinConfig.routes.get("/products/{id}/card", new GetProductCardHandler(readProduct));
            javalinConfig.routes.get("/images/{id}", new GetImageHandler(downloadImage));
            javalinConfig.routes.get("/cart", context -> context.render("cart.jte"));
            javalinConfig.routes.get("/payment", context -> context.render("payment.jte"));
            javalinConfig.routes.get("/admin/sign-in", context -> context.render("sign-in.jte"));
            javalinConfig.routes.post("/admin/products", new CreateProductHandler(createProduct, adminId, adminSecret));
            javalinConfig.routes.post("/admin/products/{id}/images", new AddImagesToProductHandler(addImagesToProduct, adminId, adminSecret));

        });
        pub.start();
    }

}
