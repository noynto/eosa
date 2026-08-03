package me.noynto.eosa;

import com.github.mustachejava.DefaultMustacheFactory;
import com.mongodb.client.MongoDatabase;
import io.javalin.Javalin;
import io.javalin.rendering.template.JavalinMustache;
import me.noynto.eosa.application.*;
import me.noynto.eosa.cart.CartProvider;
import me.noynto.eosa.cart.CartShippingRuleProvider;
import me.noynto.eosa.hash.CryptProvider;
import me.noynto.eosa.identity.IdentityProvider;
import me.noynto.eosa.identity.IdentitySessionProvider;
import me.noynto.eosa.image.ImageProvider;
import me.noynto.eosa.infrastructure.fetch.stripe.adapter.StripeFetchedCheckouts;
import me.noynto.eosa.infrastructure.fetch.stripe.config.StripeConfiguration;
import me.noynto.eosa.infrastructure.fetch.stripe.config.StripeHttpClient;
import me.noynto.eosa.infrastructure.fetch.stripe.config.StripeProperties;
import me.noynto.eosa.infrastructure.fetch.stripe.resource.StripeCheckoutSessionResource;
import me.noynto.eosa.infrastructure.persistence.*;
import me.noynto.eosa.infrastructure.persistence.mongo.*;
import me.noynto.eosa.infrastructure.security.SecuredCrypts;
import me.noynto.eosa.infrastructure.web.*;
import me.noynto.eosa.jewel.JewelCategory;
import me.noynto.eosa.jewel.JewelProvider;
import me.noynto.eosa.task.CreateDefaultAdministratorIdentityTask;
import me.noynto.eosa.task.MigrateProductsCollectionToJewelsTask;

import java.util.Map;
import java.util.Set;

public class Bootstrap {

    public static void main(String[] args) {
        Properties properties = Configuration.getProperties();

        // DATASOURCES
        MongoProperties mongoProperties = MongoConfiguration.getProperties();
        MongoDatabase mongoDatabase = MongoConfiguration.getDatabase(mongoProperties);
        //// PROVIDERS
        IdentityProvider mongoPersistedIdentities = new MongoPersistedIdentities(MongoConfiguredIdentities.getCollection(mongoDatabase));
        IdentitySessionProvider mongoPersistedSessions = new MongoPersistedIdentitySessions(MongoConfiguredIdentitySessions.getCollection(mongoDatabase));
        JewelProvider mongoPersistedJewels = new MongoPersistedJewels(MongoConfiguredJewels.getCollection(mongoDatabase));
        ImageProvider mongoPersistedImages = new MongoPersistedImages(MongoConfiguredImages.getBucket(mongoDatabase));
        CartProvider mongoPersistedCarts = new MongoPersistedCarts(MongoConfiguredCarts.getCollection(mongoDatabase));
        CartShippingRuleProvider shippingRuleProvider = new ConfiguredCartShippingRules();

        // CLIENTS
        StripeProperties stripeProperties = StripeConfiguration.getProperties(properties);
        StripeHttpClient stripeHttpClient = new StripeHttpClient(StripeConfiguration.getClient(), stripeProperties);
        StripeCheckoutSessionResource stripeCheckoutSessionResource = new StripeCheckoutSessionResource(stripeHttpClient);
        //// PROVIDERS
        StripeFetchedCheckouts stripeFetchedCheckouts = new StripeFetchedCheckouts(stripeCheckoutSessionResource, properties.baseUrl().toString());

        // UTILS
        CryptProvider cryptProvider = new SecuredCrypts();

        // HANDLER
        CreateJewel createJewel = new CreateJewel(mongoPersistedIdentities, mongoPersistedJewels);
        AddImagesToJewel addImagesToJewel = new AddImagesToJewel(mongoPersistedJewels, mongoPersistedImages);
        ReadJewelIds readJewelIds = new ReadJewelIds(mongoPersistedJewels);
        ReadJewel readJewel = new ReadJewel(mongoPersistedJewels);
        UpdateTaglineOfJewel updateTaglineOfJewel = new UpdateTaglineOfJewel(mongoPersistedJewels);
        UpdatePriceOfJewel updatePriceOfJewel = new UpdatePriceOfJewel(mongoPersistedJewels);
        UpdateCategoryOfJewel updateCategoryOfJewel = new UpdateCategoryOfJewel(mongoPersistedJewels);
        UpdateStateOfJewel updateStateOfJewel = new UpdateStateOfJewel(mongoPersistedJewels);
        DownloadImage downloadImage = new DownloadImage(mongoPersistedImages);
        AuthenticateIdentity authenticateIdentity = new AuthenticateIdentity(mongoPersistedIdentities, mongoPersistedSessions, cryptProvider);
        EnsureIdentityHasValidSession ensureIdentityHasValidSession = new EnsureIdentityHasValidSession(mongoPersistedSessions, mongoPersistedIdentities);
        EnsureIdentityHandler ensureIdentityHandler = new EnsureIdentityHandler(ensureIdentityHasValidSession);
        ReadCategoryStats readCategoryStats = new ReadCategoryStats(mongoPersistedJewels, readJewelIds);
        GetOrCreateCart getOrCreateCart = new GetOrCreateCart(mongoPersistedCarts, shippingRuleProvider);
        EnsureCartHandler ensureCartHandler = new EnsureCartHandler(getOrCreateCart);
        AddJewelToCart addJewelToCart = new AddJewelToCart(mongoPersistedCarts, mongoPersistedJewels, shippingRuleProvider);
        RemoveJewelFromCart removeJewelFromCart = new RemoveJewelFromCart(mongoPersistedCarts, shippingRuleProvider);
        UpdateCartItemQuantity updateCartItemQuantity = new UpdateCartItemQuantity(mongoPersistedCarts, shippingRuleProvider);
        InitiateCheckout initiateCheckout = new InitiateCheckout(mongoPersistedCarts, stripeFetchedCheckouts, shippingRuleProvider);
        ConfirmCheckoutSession confirmCheckoutSession = new ConfirmCheckoutSession(stripeFetchedCheckouts, mongoPersistedCarts);
        CreateAdministratorIdentity createAdministratorIdentity = new CreateAdministratorIdentity(mongoPersistedIdentities, cryptProvider);

        // TASK
        if (CreateDefaultAdministratorIdentityTask.activate()) {
            new CreateDefaultAdministratorIdentityTask(createAdministratorIdentity, properties).task();
        }
        if (MigrateProductsCollectionToJewelsTask.activate()) {
            new MigrateProductsCollectionToJewelsTask(mongoDatabase).task();
        }

        var pub = Javalin.create(javalinConfig -> {
            javalinConfig.staticFiles.add("/public", io.javalin.http.staticfiles.Location.CLASSPATH);
            javalinConfig.fileRenderer(new JavalinMustache(new DefaultMustacheFactory("templates")));
            javalinConfig.routes.get("/", new GetIndexHandler(readCategoryStats, readJewelIds));
            javalinConfig.routes.get("/jewels", new GetJewelsHandler(readJewelIds, Set.of(), "Tous les produits"));
            javalinConfig.routes.get("/jewels/necklaces", new GetJewelsHandler(readJewelIds, Set.of(JewelCategory.NECKLACE), "Tous les colliers"));
            javalinConfig.routes.get("/jewels/bracelets", new GetJewelsHandler(readJewelIds, Set.of(JewelCategory.BRACELET), "Tous les bracelets"));
            javalinConfig.routes.get("/jewels/{id}", new GetJewelHandler(readJewel, readJewelIds));
            javalinConfig.routes.get("/jewels/{id}/card", new GetJewelCardHandler(readJewel));
            javalinConfig.routes.get("/images/{id}", new GetImageHandler(downloadImage));

            // SEO
            javalinConfig.routes.get("/robots.txt", ctx -> ctx.contentType("text/plain").result(
                    "User-agent: *\n" +
                    "Disallow: /admin/\n" +
                    "Disallow: /sign-in\n" +
                    "Disallow: /cart\n" +
                    "Disallow: /checkout/\n"
            ));

            // LEGAL PART
            javalinConfig.routes.get("/legal", context -> context.render("legal.mustache", Map.of("title", "Eosa — Mentions légales")));
            javalinConfig.routes.get("/terms", context -> context.render("cgv.mustache", Map.of("title", "Eosa — Conditions générales de vente")));
            javalinConfig.routes.get("/privacy", context -> context.render("privacy.mustache", Map.of("title", "Eosa — Politique de confidentialité")));

            // SHIPPING
            javalinConfig.routes.get("/shipping/banner", new GetShippingBannerHandler(shippingRuleProvider));
            javalinConfig.routes.get("/shipping/info", new GetShippingInfoHandler(shippingRuleProvider));

            // CART PART
            javalinConfig.routes.before("/cart*", ensureCartHandler);
            javalinConfig.routes.get("/cart", new GetCartHandler(getOrCreateCart));
            javalinConfig.routes.post("/cart/items/{jewel-id}", new PostCartItemHandler(getOrCreateCart, addJewelToCart));
            javalinConfig.routes.patch("/cart/items/{jewel-id}", new PatchCartItemQuantityHandler(updateCartItemQuantity));
            javalinConfig.routes.delete("/cart/items/{jewel-id}", new DeleteCartItemHandler(removeJewelFromCart));

            // CHECKOUT PART
            javalinConfig.routes.post("/checkout", new PostCheckoutSessionHandler(initiateCheckout));
            javalinConfig.routes.get("/checkout/success", new GetCheckoutSuccessHandler(confirmCheckoutSession));

            javalinConfig.routes.get("/sign-in", ctx -> ctx.render("sign-in.mustache", Map.of("title", "Connexion — Eosa", "hasError", ctx.queryParam("error") != null, "noindex", true)));
            javalinConfig.routes.post("/sign-in", new PostSignInHandler(authenticateIdentity));
            javalinConfig.routes.before("/admin/*", ensureIdentityHandler);
            javalinConfig.routes.get("/admin/jewels", new GetAdminJewelsHandler(readJewelIds));
            javalinConfig.routes.post("/admin/jewels", new CreateJewelHandler(createJewel));
            javalinConfig.routes.get("/admin/jewels/{id}", new GetAdminJewelHandler(readJewel));
            javalinConfig.routes.get("/admin/jewels/{id}/row", new GetAdminJewelRowHandler(readJewel));
            javalinConfig.routes.post("/admin/jewels/{id}/images", new AddImagesToJewelHandler(addImagesToJewel));
            javalinConfig.routes.patch("/admin/jewels/{jewel-id}/tagline", new PatchTaglineOfJewelHandler(updateTaglineOfJewel));
            javalinConfig.routes.patch("/admin/jewels/{jewel-id}/price", new PatchPriceOfJewelHandler(updatePriceOfJewel));
            javalinConfig.routes.patch("/admin/jewels/{jewel-id}/category", new PatchCategoryOfJewelHandler(updateCategoryOfJewel));
            javalinConfig.routes.patch("/admin/jewels/{jewel-id}/state", new PatchStateOfJewelHandler(updateStateOfJewel));
        });
        pub.start();
    }

}
