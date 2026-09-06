package me.noynto.eosa;

import com.github.mustachejava.DefaultMustacheFactory;
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
import me.noynto.eosa.infrastructure.persistence.jdbc.JdbcConfiguration;
import me.noynto.eosa.infrastructure.security.SecuredCrypts;
import me.noynto.eosa.infrastructure.web.*;
import me.noynto.eosa.jewel.JewelCategory;
import me.noynto.eosa.jewel.JewelProvider;
import me.noynto.eosa.metal.MetalColorProvider;
import me.noynto.eosa.task.CreateDefaultAdministratorIdentityTask;

import java.util.Map;
import java.util.Set;

public class Bootstrap {

    public static void main(String[] args) {
        Properties properties = Configuration.getProperties();
        String baseUrl = properties.baseUrl().toString();

        // DATASOURCES
        JdbcConfiguration jdbcConfiguration = JdbcConfiguration.fromEnvironment();
        //// PROVIDERS
        IdentityProvider identityProvider = jdbcConfiguration.identities();
        IdentitySessionProvider identitySessionProvider = jdbcConfiguration.identitySessions();
        JewelProvider jewelProvider = jdbcConfiguration.jewels();
        ImageProvider imageProvider = jdbcConfiguration.images();
        CartProvider cartProvider = jdbcConfiguration.carts();
        MetalColorProvider metalColorProvider = jdbcConfiguration.metalColors();
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
        CreateJewel createJewel = new CreateJewel(identityProvider, jewelProvider);
        AddImagesToJewel addImagesToJewel = new AddImagesToJewel(jewelProvider, imageProvider);
        ReadJewelIds readJewelIds = new ReadJewelIds(jewelProvider);
        ReadJewel readJewel = new ReadJewel(jewelProvider);
        UpdateTaglineOfJewel updateTaglineOfJewel = new UpdateTaglineOfJewel(jewelProvider);
        UpdatePriceOfJewel updatePriceOfJewel = new UpdatePriceOfJewel(jewelProvider);
        UpdateCategoryOfJewel updateCategoryOfJewel = new UpdateCategoryOfJewel(jewelProvider);
        UpdateStateOfJewel updateStateOfJewel = new UpdateStateOfJewel(jewelProvider);
        DownloadImage downloadImage = new DownloadImage(imageProvider);
        AuthenticateIdentity authenticateIdentity = new AuthenticateIdentity(identityProvider, identitySessionProvider, cryptProvider);
        EnsureIdentityHasValidSession ensureIdentityHasValidSession = new EnsureIdentityHasValidSession(identitySessionProvider, identityProvider);
        EnsureIdentityHandler ensureIdentityHandler = new EnsureIdentityHandler(ensureIdentityHasValidSession);
        ReadCategoryStats readCategoryStats = new ReadCategoryStats(jewelProvider, readJewelIds);
        GetOrCreateCart getOrCreateCart = new GetOrCreateCart(cartProvider, shippingRuleProvider);
        EnsureCartHandler ensureCartHandler = new EnsureCartHandler(getOrCreateCart);
        AddJewelToCart addJewelToCart = new AddJewelToCart(cartProvider, jewelProvider, metalColorProvider, shippingRuleProvider);
        RemoveJewelFromCart removeJewelFromCart = new RemoveJewelFromCart(cartProvider, shippingRuleProvider);
        UpdateCartItemQuantity updateCartItemQuantity = new UpdateCartItemQuantity(cartProvider, shippingRuleProvider);
        InitiateCheckout initiateCheckout = new InitiateCheckout(cartProvider, stripeFetchedCheckouts, shippingRuleProvider);
        ConfirmCheckoutSession confirmCheckoutSession = new ConfirmCheckoutSession(stripeFetchedCheckouts, cartProvider);
        CreateAdministratorIdentity createAdministratorIdentity = new CreateAdministratorIdentity(identityProvider, cryptProvider);
        CreateMetalColor createMetalColor = new CreateMetalColor(metalColorProvider);
        AddImageToMetalColor addImageToMetalColor = new AddImageToMetalColor(metalColorProvider, imageProvider);
        ReadMetalColors readMetalColors = new ReadMetalColors(metalColorProvider);

        // TASK
        // Runs every activated one-shot task before exiting once — each task used to call
        // Runtime.getRuntime().exit() itself, which killed the JVM before a second activated
        // task ever ran.
        boolean ranOneShotTask = false;
        boolean oneShotTasksSucceeded = true;
        if (CreateDefaultAdministratorIdentityTask.activate()) {
            oneShotTasksSucceeded &= new CreateDefaultAdministratorIdentityTask(createAdministratorIdentity, properties).task();
            ranOneShotTask = true;
        }
        if (ranOneShotTask) {
            Runtime.getRuntime().exit(oneShotTasksSucceeded ? 0 : 1);
        }

        var pub = Javalin.create(javalinConfig -> {
            javalinConfig.staticFiles.add("/public", io.javalin.http.staticfiles.Location.CLASSPATH);
            javalinConfig.fileRenderer(new JavalinMustache(new DefaultMustacheFactory("templates")));
            javalinConfig.routes.get("/", new GetIndexHandler(readCategoryStats, readJewelIds, baseUrl));
            javalinConfig.routes.get("/jewels", new GetJewelsHandler(readJewelIds, Set.of(), "Tous les produits", baseUrl));
            javalinConfig.routes.get("/jewels/necklaces", new GetJewelsHandler(readJewelIds, Set.of(JewelCategory.NECKLACE), "Tous les colliers", baseUrl));
            javalinConfig.routes.get("/jewels/bracelets", new GetJewelsHandler(readJewelIds, Set.of(JewelCategory.BRACELET), "Tous les bracelets", baseUrl));
            javalinConfig.routes.get("/jewels/{id}", new GetJewelHandler(readJewel, readJewelIds, readMetalColors, baseUrl));
            javalinConfig.routes.get("/jewels/{id}/card", new GetJewelCardHandler(readJewel));
            javalinConfig.routes.get("/images/{id}", new GetImageHandler(downloadImage));

            // SEO
            javalinConfig.routes.get("/robots.txt", ctx -> ctx.contentType("text/plain").result(
                    "User-agent: *\n" +
                    "Disallow: /admin/\n" +
                    "Disallow: /sign-in\n" +
                    "Disallow: /cart\n" +
                    "Disallow: /checkout/\n" +
                    "\n" +
                    "Sitemap: " + baseUrl + "/sitemap.xml\n"
            ));
            javalinConfig.routes.get("/sitemap.xml", new GetSitemapHandler(readJewelIds, baseUrl));

            // LEGAL PART
            javalinConfig.routes.get("/legal", context -> context.render("legal.mustache", Map.of("title", "Eosa — Mentions légales", "description", "Mentions légales du site Eosa.", "ogImageUrl", baseUrl + "/hero.webp", "canonicalUrl", baseUrl + context.path())));
            javalinConfig.routes.get("/terms", context -> context.render("cgv.mustache", Map.of("title", "Eosa — Conditions générales de vente", "description", "Conditions générales de vente du site Eosa.", "ogImageUrl", baseUrl + "/hero.webp", "canonicalUrl", baseUrl + context.path())));
            javalinConfig.routes.get("/privacy", context -> context.render("privacy.mustache", Map.of("title", "Eosa — Politique de confidentialité", "description", "Politique de confidentialité du site Eosa.", "ogImageUrl", baseUrl + "/hero.webp", "canonicalUrl", baseUrl + context.path())));

            // HELP PAGES
            javalinConfig.routes.get("/returns", context -> context.render("returns.mustache", Map.of("title", "Eosa — Retours", "description", "14 jours pour changer d'avis, frais de retour offerts.", "ogImageUrl", baseUrl + "/hero.webp", "canonicalUrl", baseUrl + context.path())));
            javalinConfig.routes.get("/faq", context -> context.render("faq.mustache", Map.of("title", "Eosa — Questions fréquentes", "description", "Livraison, retours, entretien des bijoux : toutes les réponses à vos questions.", "ogImageUrl", baseUrl + "/hero.webp", "canonicalUrl", baseUrl + context.path())));
            javalinConfig.routes.get("/contact", context -> context.render("contact.mustache", Map.of("title", "Eosa — Contact", "description", "Une question ? Écrivez-nous, nous répondons sous 48h.", "ogImageUrl", baseUrl + "/hero.webp", "canonicalUrl", baseUrl + context.path())));

            // SHIPPING
            javalinConfig.routes.get("/shipping", new GetShippingPageHandler(shippingRuleProvider, baseUrl));
            javalinConfig.routes.get("/shipping/banner", new GetShippingBannerHandler(shippingRuleProvider));
            javalinConfig.routes.get("/shipping/info", new GetShippingInfoHandler(shippingRuleProvider));

            // CART PART
            javalinConfig.routes.before("/cart*", ensureCartHandler);
            javalinConfig.routes.get("/cart", new GetCartHandler(getOrCreateCart, baseUrl));
            javalinConfig.routes.post("/cart/items/{jewel-id}", new PostCartItemHandler(getOrCreateCart, addJewelToCart));
            javalinConfig.routes.patch("/cart/items/{item-id}", new PatchCartItemQuantityHandler(updateCartItemQuantity));
            javalinConfig.routes.delete("/cart/items/{item-id}", new DeleteCartItemHandler(removeJewelFromCart));

            // CHECKOUT PART
            javalinConfig.routes.post("/checkout", new PostCheckoutSessionHandler(initiateCheckout));
            javalinConfig.routes.get("/checkout/success", new GetCheckoutSuccessHandler(confirmCheckoutSession, baseUrl));

            javalinConfig.routes.get("/sign-in", ctx -> ctx.render("sign-in.mustache", Map.ofEntries(
                    Map.entry("title", "Connexion — Eosa"),
                    Map.entry("description", "Connexion à l'espace administrateur Eosa."),
                    Map.entry("ogImageUrl", baseUrl + "/hero.webp"),
                    Map.entry("canonicalUrl", baseUrl + ctx.path()),
                    Map.entry("hasError", ctx.queryParam("error") != null),
                    Map.entry("noindex", true)
            )));
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
            javalinConfig.routes.get("/admin/metal-colors", new GetAdminMetalColorsHandler(readMetalColors));
            javalinConfig.routes.post("/admin/metal-colors", new CreateMetalColorHandler(createMetalColor));
            javalinConfig.routes.post("/admin/metal-colors/{id}/image", new AddImageToMetalColorHandler(addImageToMetalColor));
        });
        pub.start();
    }

}
