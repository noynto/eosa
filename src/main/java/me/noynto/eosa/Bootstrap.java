package me.noynto.eosa;

import com.github.mustachejava.DefaultMustacheFactory;
import io.javalin.Javalin;
import io.javalin.rendering.FileRenderer;
import io.javalin.rendering.template.JavalinMustache;
import java.time.Clock;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import me.noynto.eosa.application.*;
import me.noynto.eosa.cart.CartProvider;
import me.noynto.eosa.cart.CartShippingRuleProvider;
import me.noynto.eosa.charm.CharmProvider;
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
import me.noynto.eosa.infrastructure.mail.MailTemplates;
import me.noynto.eosa.infrastructure.mail.SmtpSentMails;
import me.noynto.eosa.infrastructure.mail.config.SmtpConfiguration;
import me.noynto.eosa.mail.MailProvider;
import me.noynto.eosa.task.CreateDefaultAdministratorIdentityTask;
import me.noynto.eosa.task.ExpirePendingWorkshopBookingsTask;
import me.noynto.eosa.task.RetryWorkshopEmailsTask;
import me.noynto.eosa.task.SendTestEmailTask;
import me.noynto.eosa.task.SendWorkshopRemindersTask;
import me.noynto.eosa.workshop.WorkshopBookingProvider;
import me.noynto.eosa.workshop.WorkshopSessionProvider;
import me.noynto.eosa.workshop.WorkshopVenueProvider;
import me.noynto.eosa.workshop.WorkshopWaitlistEntryProvider;
import org.eclipse.jetty.util.thread.VirtualThreadPool;

public class Bootstrap {

    public static void main(String[] args) {
        Properties properties = Configuration.getProperties();
        String baseUrl = properties.publicBaseUrl().toString();

        // DATASOURCES
        JdbcConfiguration jdbcConfiguration =
            JdbcConfiguration.fromEnvironment();
        //// PROVIDERS
        IdentityProvider identityProvider = jdbcConfiguration.identities();
        IdentitySessionProvider identitySessionProvider =
            jdbcConfiguration.identitySessions();
        JewelProvider jewelProvider = jdbcConfiguration.jewels();
        ImageProvider imageProvider = jdbcConfiguration.images();
        CartProvider cartProvider = jdbcConfiguration.carts();
        MetalColorProvider metalColorProvider = jdbcConfiguration.metalColors();
        CharmProvider charmProvider = jdbcConfiguration.charms();
        WorkshopVenueProvider workshopVenueProvider = jdbcConfiguration.workshopVenues();
        WorkshopSessionProvider workshopSessionProvider = jdbcConfiguration.workshopSessions();
        WorkshopBookingProvider workshopBookingProvider = jdbcConfiguration.workshopBookings();
        WorkshopWaitlistEntryProvider workshopWaitlistEntryProvider = jdbcConfiguration.workshopWaitlistEntries();
        Clock clock = Clock.systemUTC();
        CartShippingRuleProvider shippingRuleProvider =
            new ConfiguredCartShippingRules();

        // CLIENTS
        MailProvider mailProvider = new SmtpSentMails(
            SmtpConfiguration.getProperties(),
            new MailTemplates()
        );
        StripeProperties stripeProperties = StripeConfiguration.getProperties(
            properties
        );
        StripeHttpClient stripeHttpClient = new StripeHttpClient(
            StripeConfiguration.getClient(),
            stripeProperties
        );
        StripeCheckoutSessionResource stripeCheckoutSessionResource =
            new StripeCheckoutSessionResource(stripeHttpClient);
        //// PROVIDERS
        StripeFetchedCheckouts stripeFetchedCheckouts =
            new StripeFetchedCheckouts(stripeCheckoutSessionResource, baseUrl);

        // UTILS
        CryptProvider cryptProvider = new SecuredCrypts();

        // HANDLER
        CreateJewel createJewel = new CreateJewel(
            identityProvider,
            jewelProvider
        );
        AddImagesToJewel addImagesToJewel = new AddImagesToJewel(
            jewelProvider,
            imageProvider
        );
        RemoveImageFromJewel removeImageFromJewel = new RemoveImageFromJewel(
            identityProvider,
            jewelProvider,
            imageProvider
        );
        ReadJewelIds readJewelIds = new ReadJewelIds(jewelProvider);
        ReadJewel readJewel = new ReadJewel(jewelProvider);
        UpdateTaglineOfJewel updateTaglineOfJewel = new UpdateTaglineOfJewel(
            jewelProvider
        );
        UpdatePriceOfJewel updatePriceOfJewel = new UpdatePriceOfJewel(
            jewelProvider
        );
        UpdateCategoryOfJewel updateCategoryOfJewel = new UpdateCategoryOfJewel(
            jewelProvider
        );
        UpdateStateOfJewel updateStateOfJewel = new UpdateStateOfJewel(
            jewelProvider
        );
        DownloadImage downloadImage = new DownloadImage(imageProvider);
        AuthenticateIdentity authenticateIdentity = new AuthenticateIdentity(
            identityProvider,
            identitySessionProvider,
            cryptProvider
        );
        EnsureIdentityHasValidSession ensureIdentityHasValidSession =
            new EnsureIdentityHasValidSession(
                identitySessionProvider,
                identityProvider
            );
        EnsureIdentityHandler ensureIdentityHandler = new EnsureIdentityHandler(
            ensureIdentityHasValidSession
        );
        ReadCategoryStats readCategoryStats = new ReadCategoryStats(
            jewelProvider,
            readJewelIds
        );
        GetOrCreateCart getOrCreateCart = new GetOrCreateCart(
            cartProvider,
            shippingRuleProvider
        );
        EnsureCartHandler ensureCartHandler = new EnsureCartHandler(
            getOrCreateCart
        );
        AddJewelToCart addJewelToCart = new AddJewelToCart(
            cartProvider,
            jewelProvider,
            metalColorProvider,
            charmProvider,
            shippingRuleProvider
        );
        RemoveJewelFromCart removeJewelFromCart = new RemoveJewelFromCart(
            cartProvider,
            shippingRuleProvider
        );
        UpdateCartItemQuantity updateCartItemQuantity =
            new UpdateCartItemQuantity(cartProvider, shippingRuleProvider);
        InitiateCheckout initiateCheckout = new InitiateCheckout(
            cartProvider,
            stripeFetchedCheckouts,
            shippingRuleProvider
        );
        ConfirmCheckoutSession confirmCheckoutSession =
            new ConfirmCheckoutSession(stripeFetchedCheckouts, cartProvider);
        CreateAdministratorIdentity createAdministratorIdentity =
            new CreateAdministratorIdentity(identityProvider, cryptProvider);
        CreateMetalColor createMetalColor = new CreateMetalColor(
            metalColorProvider
        );
        AddImageToMetalColor addImageToMetalColor = new AddImageToMetalColor(
            metalColorProvider,
            imageProvider
        );
        ReadMetalColors readMetalColors = new ReadMetalColors(
            metalColorProvider
        );
        CreateCharm createCharm = new CreateCharm(charmProvider);
        AddImageToCharm addImageToCharm = new AddImageToCharm(
            charmProvider,
            imageProvider
        );
        ReadCharms readCharms = new ReadCharms(charmProvider);
        UpdateCharm updateCharm = new UpdateCharm(charmProvider);
        DeleteCharm deleteCharm = new DeleteCharm(charmProvider);
        CreateWorkshopVenue createWorkshopVenue = new CreateWorkshopVenue(workshopVenueProvider);
        ReadWorkshopVenues readWorkshopVenues = new ReadWorkshopVenues(workshopVenueProvider);
        ReadWorkshopVenue readWorkshopVenue = new ReadWorkshopVenue(workshopVenueProvider);
        UpdateWorkshopVenue updateWorkshopVenue = new UpdateWorkshopVenue(workshopVenueProvider);
        UpdateDefaultsOfWorkshopVenue updateDefaultsOfWorkshopVenue =
            new UpdateDefaultsOfWorkshopVenue(workshopVenueProvider);
        AddImageToWorkshopVenue addImageToWorkshopVenue = new AddImageToWorkshopVenue(
            workshopVenueProvider,
            imageProvider
        );
        ArchiveWorkshopVenue archiveWorkshopVenue = new ArchiveWorkshopVenue(workshopVenueProvider);
        ReadWorkshopSessions readWorkshopSessions = new ReadWorkshopSessions(
            workshopSessionProvider,
            workshopVenueProvider,
            workshopBookingProvider,
            workshopWaitlistEntryProvider,
            clock
        );
        NotifyWorkshopWaitlist notifyWorkshopWaitlist = new NotifyWorkshopWaitlist(
            workshopSessionProvider,
            workshopVenueProvider,
            workshopBookingProvider,
            workshopWaitlistEntryProvider,
            mailProvider,
            clock,
            properties.publicBaseUrl()
        );
        JoinWorkshopWaitlist joinWorkshopWaitlist = new JoinWorkshopWaitlist(
            workshopSessionProvider,
            workshopBookingProvider,
            workshopWaitlistEntryProvider,
            clock
        );
        LeaveWorkshopWaitlist leaveWorkshopWaitlist = new LeaveWorkshopWaitlist(workshopWaitlistEntryProvider, clock);
        ReadWorkshopWaitlistEntry readWorkshopWaitlistEntry = new ReadWorkshopWaitlistEntry(
            workshopWaitlistEntryProvider,
            workshopSessionProvider,
            workshopVenueProvider
        );
        ReadWorkshopWaitlist readWorkshopWaitlist = new ReadWorkshopWaitlist(workshopWaitlistEntryProvider);
        ReadWorkshopSession readWorkshopSession = new ReadWorkshopSession(
            workshopSessionProvider,
            readWorkshopSessions
        );
        CreateWorkshopSession createWorkshopSession = new CreateWorkshopSession(
            workshopVenueProvider,
            workshopSessionProvider,
            clock
        );
        UpdateWorkshopSession updateWorkshopSession = new UpdateWorkshopSession(
            workshopVenueProvider,
            workshopSessionProvider,
            workshopBookingProvider,
            notifyWorkshopWaitlist,
            clock
        );
        SendWorkshopBookingCancellation sendWorkshopBookingCancellation = new SendWorkshopBookingCancellation(
            workshopBookingProvider,
            workshopSessionProvider,
            workshopVenueProvider,
            mailProvider,
            clock,
            properties.publicBaseUrl()
        );
        CancelWorkshopSession cancelWorkshopSession = new CancelWorkshopSession(
            workshopSessionProvider,
            workshopVenueProvider,
            workshopBookingProvider,
            workshopWaitlistEntryProvider,
            sendWorkshopBookingCancellation,
            mailProvider,
            clock,
            properties.publicBaseUrl()
        );
        CancelWorkshopBookingByAdmin cancelWorkshopBookingByAdmin = new CancelWorkshopBookingByAdmin(
            workshopBookingProvider,
            sendWorkshopBookingCancellation,
            notifyWorkshopWaitlist,
            clock
        );
        CancelWorkshopBookingByParticipant cancelWorkshopBookingByParticipant = new CancelWorkshopBookingByParticipant(
            workshopBookingProvider,
            workshopSessionProvider,
            sendWorkshopBookingCancellation,
            notifyWorkshopWaitlist,
            clock
        );

        SendTestEmail sendTestEmail = new SendTestEmail(mailProvider);
        RequestWorkshopBooking requestWorkshopBooking = new RequestWorkshopBooking(
            workshopSessionProvider,
            workshopVenueProvider,
            workshopBookingProvider,
            mailProvider,
            clock,
            properties.publicBaseUrl()
        );
        ReadWorkshopBooking readWorkshopBooking = new ReadWorkshopBooking(
            workshopBookingProvider,
            workshopSessionProvider,
            workshopVenueProvider
        );
        ReadWorkshopBookings readWorkshopBookings = new ReadWorkshopBookings(workshopBookingProvider);
        SendWorkshopBookingConfirmation sendWorkshopBookingConfirmation = new SendWorkshopBookingConfirmation(
            workshopBookingProvider,
            workshopSessionProvider,
            workshopVenueProvider,
            mailProvider,
            clock,
            properties.publicBaseUrl()
        );
        ConfirmWorkshopBooking confirmWorkshopBooking = new ConfirmWorkshopBooking(
            workshopBookingProvider,
            sendWorkshopBookingConfirmation,
            workshopWaitlistEntryProvider,
            clock
        );
        ExpirePendingWorkshopBookings expirePendingWorkshopBookings =
            new ExpirePendingWorkshopBookings(workshopBookingProvider, notifyWorkshopWaitlist, clock);
        MarkWorkshopBookingAttended markWorkshopBookingAttended =
            new MarkWorkshopBookingAttended(workshopBookingProvider, workshopSessionProvider, clock);
        MarkWorkshopBookingNoShow markWorkshopBookingNoShow =
            new MarkWorkshopBookingNoShow(workshopBookingProvider, workshopSessionProvider, clock);
        MarkAllWorkshopBookingsAttended markAllWorkshopBookingsAttended =
            new MarkAllWorkshopBookingsAttended(workshopBookingProvider, workshopSessionProvider, clock);
        SendWorkshopReminders sendWorkshopReminders = new SendWorkshopReminders(
            workshopBookingProvider,
            workshopSessionProvider,
            workshopVenueProvider,
            mailProvider,
            clock
        );
        RetryWorkshopEmails retryWorkshopEmails = new RetryWorkshopEmails(
            workshopBookingProvider,
            sendWorkshopBookingConfirmation,
            sendWorkshopBookingCancellation,
            clock
        );

        // TASK
        // Runs every activated one-shot task before exiting once — each task used to call
        // Runtime.getRuntime().exit() itself, which killed the JVM before a second activated
        // task ever ran.
        boolean ranOneShotTask = false;
        boolean oneShotTasksSucceeded = true;
        if (CreateDefaultAdministratorIdentityTask.activate()) {
            oneShotTasksSucceeded &= new CreateDefaultAdministratorIdentityTask(
                createAdministratorIdentity,
                properties
            ).task();
            ranOneShotTask = true;
        }
        if (SendTestEmailTask.activate()) {
            oneShotTasksSucceeded &= new SendTestEmailTask(sendTestEmail).task();
            ranOneShotTask = true;
        }
        if (ExpirePendingWorkshopBookingsTask.activate()) {
            oneShotTasksSucceeded &= new ExpirePendingWorkshopBookingsTask(expirePendingWorkshopBookings).task();
            ranOneShotTask = true;
        }
        if (SendWorkshopRemindersTask.activate()) {
            oneShotTasksSucceeded &= new SendWorkshopRemindersTask(sendWorkshopReminders).task();
            ranOneShotTask = true;
        }
        if (RetryWorkshopEmailsTask.activate()) {
            oneShotTasksSucceeded &= new RetryWorkshopEmailsTask(retryWorkshopEmails).task();
            ranOneShotTask = true;
        }
        if (ranOneShotTask) {
            Runtime.getRuntime().exit(oneShotTasksSucceeded ? 0 : 1);
        }

        var pub = Javalin.create(javalinConfig -> {
            javalinConfig.jetty.port = properties.publicPort();
            // Virtual threads (Java 25): one cheap thread per request, capped at 250 concurrent requests.
            VirtualThreadPool publicThreadPool = new VirtualThreadPool(250);
            publicThreadPool.setName("public");
            javalinConfig.jetty.threadPool = publicThreadPool;
            javalinConfig.staticFiles.add(
                "/public",
                io.javalin.http.staticfiles.Location.CLASSPATH
            );
            javalinConfig.fileRenderer(
                new JavalinMustache(new DefaultMustacheFactory("templates"))
            );
            javalinConfig.routes.get(
                "/",
                new GetIndexHandler(readCategoryStats, readJewelIds, baseUrl)
            );
            javalinConfig.routes.get(
                "/jewels",
                new GetJewelsHandler(
                    readJewelIds,
                    Set.of(),
                    "Tous les produits",
                    baseUrl
                )
            );
            javalinConfig.routes.get(
                "/jewels/necklaces",
                new GetJewelsHandler(
                    readJewelIds,
                    Set.of(JewelCategory.NECKLACE),
                    "Tous les colliers",
                    baseUrl
                )
            );
            javalinConfig.routes.get(
                "/jewels/bracelets",
                new GetJewelsHandler(
                    readJewelIds,
                    Set.of(JewelCategory.BRACELET),
                    "Tous les bracelets",
                    baseUrl
                )
            );
            javalinConfig.routes.get(
                "/jewels/{id}",
                new GetJewelHandler(
                    readJewel,
                    readJewelIds,
                    readMetalColors,
                    readCharms,
                    baseUrl
                )
            );
            javalinConfig.routes.get(
                "/jewels/{id}/card",
                new GetJewelCardHandler(readJewel)
            );
            javalinConfig.routes.get(
                "/images/{id}",
                new GetImageHandler(downloadImage)
            );

            // SEO
            javalinConfig.routes.get("/robots.txt", ctx ->
                ctx
                    .contentType("text/plain")
                    .result(
                        "User-agent: *\n" +
                            "Disallow: /cart\n" +
                            "Disallow: /checkout/\n" +
                            "Disallow: /workshops/bookings/\n" +
                            "Disallow: /workshops/waitlist/\n" +
                            "\n" +
                            "Sitemap: " +
                            baseUrl +
                            "/sitemap.xml\n"
                    )
            );
            javalinConfig.routes.get(
                "/sitemap.xml",
                new GetSitemapHandler(readJewelIds, baseUrl)
            );

            // LEGAL PART
            javalinConfig.routes.get("/legal", context ->
                context.render(
                    "legal.mustache",
                    Map.of(
                        "title",
                        "Eosa — Mentions légales",
                        "description",
                        "Mentions légales du site Eosa.",
                        "ogImageUrl",
                        baseUrl + "/hero.webp",
                        "canonicalUrl",
                        baseUrl + context.path()
                    )
                )
            );
            javalinConfig.routes.get("/terms", context ->
                context.render(
                    "cgv.mustache",
                    Map.of(
                        "title",
                        "Eosa — Conditions générales de vente",
                        "description",
                        "Conditions générales de vente du site Eosa.",
                        "ogImageUrl",
                        baseUrl + "/hero.webp",
                        "canonicalUrl",
                        baseUrl + context.path()
                    )
                )
            );
            javalinConfig.routes.get("/privacy", context ->
                context.render(
                    "privacy.mustache",
                    Map.of(
                        "title",
                        "Eosa — Politique de confidentialité",
                        "description",
                        "Politique de confidentialité du site Eosa.",
                        "ogImageUrl",
                        baseUrl + "/hero.webp",
                        "canonicalUrl",
                        baseUrl + context.path()
                    )
                )
            );

            // BRAND PAGES
            javalinConfig.routes.get("/about", context ->
                context.render(
                    "about.mustache",
                    Map.of(
                        "title",
                        "Eosa — Notre histoire",
                        "description",
                        "EOSA, ce sont des bijoux en pierres naturelles faits main à Nancy par Chloé, un par un.",
                        "ogImageUrl",
                        baseUrl + "/story.webp",
                        "canonicalUrl",
                        baseUrl + context.path()
                    )
                )
            );
            GetWorkshopSessionHandler getWorkshopSessionHandler =
                new GetWorkshopSessionHandler(readWorkshopSession, clock, baseUrl);
            javalinConfig.routes.get("/workshops/sessions/{id}", getWorkshopSessionHandler);
            // 5 demandes (réservation ou liste d'attente) par heure et par adresse IP.
            RequestRateLimiter workshopRequestRateLimiter = new RequestRateLimiter(5, Duration.ofHours(1), clock);
            javalinConfig.routes.post(
                "/workshops/sessions/{id}/bookings",
                new PostWorkshopBookingHandler(
                    requestWorkshopBooking,
                    readWorkshopSession,
                    getWorkshopSessionHandler,
                    workshopRequestRateLimiter,
                    baseUrl
                )
            );
            javalinConfig.routes.post(
                "/workshops/sessions/{id}/waitlist",
                new PostWorkshopWaitlistHandler(joinWorkshopWaitlist, getWorkshopSessionHandler, workshopRequestRateLimiter)
            );
            javalinConfig.routes.get(
                "/workshops/waitlist/{token}/leave",
                new GetWorkshopWaitlistLeaveHandler(readWorkshopWaitlistEntry, baseUrl)
            );
            javalinConfig.routes.post(
                "/workshops/waitlist/{token}/leave",
                new PostWorkshopWaitlistLeaveHandler(leaveWorkshopWaitlist)
            );
            javalinConfig.routes.get(
                "/workshops/bookings/{token}/confirm",
                new GetWorkshopBookingConfirmHandler(readWorkshopBooking, clock, baseUrl)
            );
            javalinConfig.routes.post(
                "/workshops/bookings/{token}/confirm",
                new PostWorkshopBookingConfirmHandler(confirmWorkshopBooking)
            );
            javalinConfig.routes.get(
                "/workshops/bookings/{token}/cancel",
                new GetWorkshopBookingCancelHandler(readWorkshopBooking, clock, baseUrl)
            );
            javalinConfig.routes.post(
                "/workshops/bookings/{token}/cancel",
                new PostWorkshopBookingCancelHandler(cancelWorkshopBookingByParticipant)
            );
            javalinConfig.routes.get(
                "/workshops",
                new GetWorkshopsHandler(readWorkshopSessions, clock, baseUrl)
            );
            javalinConfig.routes.get(
                "/workshops/sessions/{id}/card",
                new GetWorkshopSessionCardHandler(readWorkshopSession, clock)
            );

            // HELP PAGES
            javalinConfig.routes.get("/returns", context ->
                context.render(
                    "returns.mustache",
                    Map.of(
                        "title",
                        "Eosa — Retours",
                        "description",
                        "14 jours pour changer d'avis, frais de retour offerts.",
                        "ogImageUrl",
                        baseUrl + "/hero.webp",
                        "canonicalUrl",
                        baseUrl + context.path()
                    )
                )
            );
            javalinConfig.routes.get("/faq", context ->
                context.render(
                    "faq.mustache",
                    Map.of(
                        "title",
                        "Eosa — Questions fréquentes",
                        "description",
                        "Livraison, retours, entretien des bijoux : toutes les réponses à vos questions.",
                        "ogImageUrl",
                        baseUrl + "/hero.webp",
                        "canonicalUrl",
                        baseUrl + context.path()
                    )
                )
            );
            javalinConfig.routes.get("/contact", context ->
                context.render(
                    "contact.mustache",
                    Map.of(
                        "title",
                        "Eosa — Contact",
                        "description",
                        "Une question ? Écrivez-nous, nous répondons sous 48h.",
                        "ogImageUrl",
                        baseUrl + "/hero.webp",
                        "canonicalUrl",
                        baseUrl + context.path()
                    )
                )
            );

            // SHIPPING
            javalinConfig.routes.get(
                "/shipping",
                new GetShippingPageHandler(shippingRuleProvider, baseUrl)
            );
            javalinConfig.routes.get(
                "/shipping/banner",
                new GetShippingBannerHandler(shippingRuleProvider)
            );
            javalinConfig.routes.get(
                "/shipping/info",
                new GetShippingInfoHandler(shippingRuleProvider)
            );

            // CART PART
            javalinConfig.routes.before("/cart*", ensureCartHandler);
            javalinConfig.routes.get(
                "/cart",
                new GetCartHandler(getOrCreateCart, baseUrl)
            );
            javalinConfig.routes.post(
                "/cart/items/{jewel-id}",
                new PostCartItemHandler(getOrCreateCart, addJewelToCart)
            );
            javalinConfig.routes.patch(
                "/cart/items/{item-id}",
                new PatchCartItemQuantityHandler(updateCartItemQuantity)
            );
            javalinConfig.routes.delete(
                "/cart/items/{item-id}",
                new DeleteCartItemHandler(removeJewelFromCart)
            );

            // CHECKOUT PART
            javalinConfig.routes.post(
                "/checkout",
                new PostCheckoutSessionHandler(initiateCheckout)
            );
            javalinConfig.routes.get(
                "/checkout/success",
                new GetCheckoutSuccessHandler(confirmCheckoutSession, baseUrl)
            );
        });
        pub.start();

        // Administration runs on its own port so it can be kept off the public network.
        var admin = Javalin.create(javalinConfig -> {
            javalinConfig.jetty.port = properties.adminPort();
            // Single-user back office: virtual threads capped at 16 concurrent requests.
            VirtualThreadPool adminThreadPool = new VirtualThreadPool(16);
            adminThreadPool.setName("admin");
            javalinConfig.jetty.threadPool = adminThreadPool;
            javalinConfig.staticFiles.add(
                "/public",
                io.javalin.http.staticfiles.Location.CLASSPATH
            );
            // Every admin template gets the public site URL (used by the "Voir le site" link).
            FileRenderer mustache = new JavalinMustache(
                new DefaultMustacheFactory("templates")
            );
            javalinConfig.fileRenderer((filePath, model, ctx) -> {
                Map<String, Object> modelWithPublicUrl = new HashMap<>(model);
                modelWithPublicUrl.put("publicUrl", baseUrl);
                return mustache.render(filePath, modelWithPublicUrl, ctx);
            });
            javalinConfig.routes.get("/", ctx -> ctx.redirect("/jewels"));
            javalinConfig.routes.get(
                "/images/{id}",
                new GetImageHandler(downloadImage)
            );
            javalinConfig.routes.get("/sign-in", ctx ->
                ctx.render(
                    "sign-in.mustache",
                    Map.of("hasError", ctx.queryParam("error") != null)
                )
            );
            javalinConfig.routes.post(
                "/sign-in",
                new PostSignInHandler(
                    authenticateIdentity,
                    "https".equalsIgnoreCase(properties.adminBaseUrl().getScheme())
                )
            );
            javalinConfig.routes.before(ensureIdentityHandler);
            javalinConfig.routes.get(
                "/jewels",
                new GetAdminJewelsHandler(readJewelIds)
            );
            javalinConfig.routes.post(
                "/jewels",
                new CreateJewelHandler(createJewel)
            );
            javalinConfig.routes.get(
                "/jewels/{id}",
                new GetAdminJewelHandler(readJewel)
            );
            javalinConfig.routes.get(
                "/jewels/{id}/row",
                new GetAdminJewelRowHandler(readJewel)
            );
            javalinConfig.routes.post(
                "/jewels/{id}/images",
                new AddImagesToJewelHandler(addImagesToJewel)
            );
            javalinConfig.routes.delete(
                "/jewels/{jewel-id}/images/{image-id}",
                new DeleteImageOfJewelHandler(removeImageFromJewel)
            );
            javalinConfig.routes.patch(
                "/jewels/{jewel-id}/tagline",
                new PatchTaglineOfJewelHandler(updateTaglineOfJewel)
            );
            javalinConfig.routes.patch(
                "/jewels/{jewel-id}/price",
                new PatchPriceOfJewelHandler(updatePriceOfJewel)
            );
            javalinConfig.routes.patch(
                "/jewels/{jewel-id}/category",
                new PatchCategoryOfJewelHandler(updateCategoryOfJewel)
            );
            javalinConfig.routes.patch(
                "/jewels/{jewel-id}/state",
                new PatchStateOfJewelHandler(updateStateOfJewel)
            );
            javalinConfig.routes.get(
                "/metal-colors",
                new GetAdminMetalColorsHandler(readMetalColors)
            );
            javalinConfig.routes.post(
                "/metal-colors",
                new CreateMetalColorHandler(createMetalColor)
            );
            javalinConfig.routes.post(
                "/metal-colors/{id}/image",
                new AddImageToMetalColorHandler(addImageToMetalColor)
            );
            javalinConfig.routes.get(
                "/charms",
                new GetAdminCharmsHandler(readCharms)
            );
            javalinConfig.routes.post(
                "/charms",
                new CreateCharmHandler(createCharm)
            );
            javalinConfig.routes.post(
                "/charms/{id}/image",
                new AddImageToCharmHandler(addImageToCharm)
            );
            javalinConfig.routes.patch(
                "/charms/{id}",
                new UpdateCharmHandler(updateCharm)
            );
            javalinConfig.routes.delete(
                "/charms/{id}",
                new DeleteCharmHandler(deleteCharm)
            );
            javalinConfig.routes.get(
                "/workshop-venues",
                new GetAdminWorkshopVenuesHandler(readWorkshopVenues)
            );
            javalinConfig.routes.post(
                "/workshop-venues",
                new CreateWorkshopVenueHandler(createWorkshopVenue)
            );
            javalinConfig.routes.get(
                "/workshop-venues/{id}",
                new GetAdminWorkshopVenueHandler(readWorkshopVenue)
            );
            javalinConfig.routes.patch(
                "/workshop-venues/{id}",
                new UpdateWorkshopVenueHandler(updateWorkshopVenue)
            );
            javalinConfig.routes.patch(
                "/workshop-venues/{id}/defaults",
                new UpdateDefaultsOfWorkshopVenueHandler(updateDefaultsOfWorkshopVenue)
            );
            javalinConfig.routes.post(
                "/workshop-venues/{id}/image",
                new AddImageToWorkshopVenueHandler(addImageToWorkshopVenue)
            );
            javalinConfig.routes.post(
                "/workshop-venues/{id}/archive",
                new ArchiveWorkshopVenueHandler(archiveWorkshopVenue)
            );
            javalinConfig.routes.get(
                "/workshops",
                new GetAdminWorkshopSessionsHandler(readWorkshopSessions, readWorkshopVenues, clock)
            );
            javalinConfig.routes.post(
                "/workshops",
                new CreateWorkshopSessionHandler(createWorkshopSession)
            );
            javalinConfig.routes.get(
                "/workshops/{id}",
                new GetAdminWorkshopSessionHandler(readWorkshopSession, readWorkshopVenues, readWorkshopBookings, readWorkshopWaitlist, clock)
            );
            javalinConfig.routes.patch(
                "/workshops/{id}",
                new UpdateWorkshopSessionHandler(updateWorkshopSession)
            );
            javalinConfig.routes.post(
                "/workshops/{id}/cancel",
                new CancelWorkshopSessionHandler(cancelWorkshopSession)
            );
            javalinConfig.routes.post(
                "/workshops/{id}/bookings/{bookingId}/cancel",
                new CancelWorkshopBookingByAdminHandler(cancelWorkshopBookingByAdmin)
            );
            javalinConfig.routes.post(
                "/workshops/{id}/bookings/{bookingId}/attended",
                new MarkWorkshopBookingAttendedHandler(markWorkshopBookingAttended)
            );
            javalinConfig.routes.post(
                "/workshops/{id}/bookings/{bookingId}/no-show",
                new MarkWorkshopBookingNoShowHandler(markWorkshopBookingNoShow)
            );
            javalinConfig.routes.post(
                "/workshops/{id}/attended",
                new MarkAllWorkshopBookingsAttendedHandler(markAllWorkshopBookingsAttended)
            );
        });
        admin.start();
    }
}
