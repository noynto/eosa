package me.noynto.eosa;

import com.mongodb.client.MongoDatabase;
import gg.jte.ContentType;
import gg.jte.TemplateEngine;
import io.javalin.Javalin;
import io.javalin.rendering.template.JavalinJte;
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
import me.noynto.eosa.product.ProductCategory;
import me.noynto.eosa.product.ProductProvider;
import me.noynto.eosa.task.CreateDefaultAdministratorIdentityTask;

import java.nio.file.Path;
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
        ProductProvider mongoPersistedProducts = new MongoPersistedProducts(MongoConfiguredProducts.getCollection(mongoDatabase));
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
        CreateProduct createProduct = new CreateProduct(mongoPersistedIdentities, mongoPersistedProducts);
        AddImagesToProduct addImagesToProduct = new AddImagesToProduct(mongoPersistedProducts, mongoPersistedImages);
        ReadProductIds readProductIds = new ReadProductIds(mongoPersistedProducts);
        ReadProduct readProduct = new ReadProduct(mongoPersistedProducts);
        UpdateTaglineOfProduct updateTaglineOfProduct = new UpdateTaglineOfProduct(mongoPersistedProducts);
        UpdatePriceOfProduct updatePriceOfProduct = new UpdatePriceOfProduct(mongoPersistedProducts);
        UpdateCategoryOfProduct updateCategoryOfProduct = new UpdateCategoryOfProduct(mongoPersistedProducts);
        UpdateStateOfProduct updateStateOfProduct = new UpdateStateOfProduct(mongoPersistedProducts);
        DownloadImage downloadImage = new DownloadImage(mongoPersistedImages);
        AuthenticateIdentity authenticateIdentity = new AuthenticateIdentity(mongoPersistedIdentities, mongoPersistedSessions, cryptProvider);
        EnsureIdentityHasValidSession ensureIdentityHasValidSession = new EnsureIdentityHasValidSession(mongoPersistedSessions, mongoPersistedIdentities);
        EnsureIdentityHandler ensureIdentityHandler = new EnsureIdentityHandler(ensureIdentityHasValidSession);
        ReadCategoryStats readCategoryStats = new ReadCategoryStats(mongoPersistedProducts, readProductIds);
        GetOrCreateCart getOrCreateCart = new GetOrCreateCart(mongoPersistedCarts, shippingRuleProvider);
        EnsureCartHandler ensureCartHandler = new EnsureCartHandler(getOrCreateCart);
        AddProductToCart addProductToCart = new AddProductToCart(mongoPersistedCarts, mongoPersistedProducts, shippingRuleProvider);
        RemoveProductFromCart removeProductFromCart = new RemoveProductFromCart(mongoPersistedCarts, shippingRuleProvider);
        UpdateCartItemQuantity updateCartItemQuantity = new UpdateCartItemQuantity(mongoPersistedCarts, shippingRuleProvider);
        InitiateCheckout initiateCheckout = new InitiateCheckout(mongoPersistedCarts, stripeFetchedCheckouts, shippingRuleProvider);
        ConfirmCheckoutSession confirmCheckoutSession = new ConfirmCheckoutSession(stripeFetchedCheckouts, mongoPersistedCarts);
        CreateAdministratorIdentity createAdministratorIdentity = new CreateAdministratorIdentity(mongoPersistedIdentities, cryptProvider);

        // TASK
        if (CreateDefaultAdministratorIdentityTask.activate()) {
            new CreateDefaultAdministratorIdentityTask(createAdministratorIdentity, properties).task();
        }

        // JTE
        Path targetDirectory = Path.of("jte-classes"); // This is the directory where compiled templates are located.
        TemplateEngine templateEngine = TemplateEngine.createPrecompiled(targetDirectory, ContentType.Html);

        var pub = Javalin.create(javalinConfig -> {
            javalinConfig.staticFiles.add("/public", io.javalin.http.staticfiles.Location.CLASSPATH);
            javalinConfig.fileRenderer(new JavalinJte(templateEngine));
            javalinConfig.routes.get("/", new GetIndexHandler(readCategoryStats, readProductIds));
            javalinConfig.routes.get("/products", new GetProductsHandler(readProductIds, Set.of(), "Tous les produits"));
            javalinConfig.routes.get("/products/necklaces", new GetProductsHandler(readProductIds, Set.of(ProductCategory.NECKLACE), "Tous les colliers"));
            javalinConfig.routes.get("/products/bracelets", new GetProductsHandler(readProductIds, Set.of(ProductCategory.BRACELET), "Tous les bracelets"));
            javalinConfig.routes.get("/products/{id}", new GetProductHandler(readProduct, readProductIds));
            javalinConfig.routes.get("/products/{id}/card", new GetProductCardHandler(readProduct));
            javalinConfig.routes.get("/images/{id}", new GetImageHandler(downloadImage));

            // LEGAL PART
            javalinConfig.routes.get("/legal", context -> context.render("legal.jte"));
            javalinConfig.routes.get("/terms", context -> context.render("cgv.jte"));
            javalinConfig.routes.get("/privacy", context -> context.render("privacy.jte"));

            // SHIPPING
            javalinConfig.routes.get("/shipping/banner", new GetShippingBannerHandler(shippingRuleProvider));
            javalinConfig.routes.get("/shipping/info", new GetShippingInfoHandler(shippingRuleProvider));

            // CART PART
            javalinConfig.routes.before("/cart*", ensureCartHandler);
            javalinConfig.routes.get("/cart", new GetCartHandler(getOrCreateCart));
            javalinConfig.routes.post("/cart/items/{product-id}", new PostCartItemHandler(getOrCreateCart, addProductToCart));
            javalinConfig.routes.patch("/cart/items/{product-id}", new PatchCartItemQuantityHandler(updateCartItemQuantity));
            javalinConfig.routes.delete("/cart/items/{product-id}", new DeleteCartItemHandler(removeProductFromCart));

            // CHECKOUT PART
            javalinConfig.routes.post("/checkout", new PostCheckoutSessionHandler(initiateCheckout));
            javalinConfig.routes.get("/checkout/success", new GetCheckoutSuccessHandler(confirmCheckoutSession));

            javalinConfig.routes.get("/sign-in", ctx -> ctx.render("sign-in.jte", Map.of("error", ctx.queryParam("error") != null)));
            javalinConfig.routes.post("/sign-in", new PostSignInHandler(authenticateIdentity));
            javalinConfig.routes.before("/admin/*", ensureIdentityHandler);
            javalinConfig.routes.get("/admin/products", new GetAdminProductsHandler(readProductIds));
            javalinConfig.routes.post("/admin/products", new CreateProductHandler(createProduct));
            javalinConfig.routes.get("/admin/products/{id}", new GetAdminProductHandler(readProduct));
            javalinConfig.routes.get("/admin/products/{id}/row", new GetAdminProductRowHandler(readProduct));
            javalinConfig.routes.post("/admin/products/{id}/images", new AddImagesToProductHandler(addImagesToProduct));
            javalinConfig.routes.patch("/admin/products/{product-id}/tagline", new PatchTaglineOfProductHandler(updateTaglineOfProduct));
            javalinConfig.routes.patch("/admin/products/{product-id}/price", new PatchPriceOfProductHandler(updatePriceOfProduct));
            javalinConfig.routes.patch("/admin/products/{product-id}/category", new PatchCategoryOfProductHandler(updateCategoryOfProduct));
            javalinConfig.routes.patch("/admin/products/{product-id}/state", new PatchStateOfProductHandler(updateStateOfProduct));
        });
        pub.start();
    }

}
