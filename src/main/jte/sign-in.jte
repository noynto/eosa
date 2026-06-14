@param Boolean error

@template.layouts.main(title = "Connexion — Eosa", content = @`

<main class="min-h-[60vh] flex items-center justify-center px-5 py-16">
    <div class="w-full max-w-sm">
        <h1 class="font-serif text-3xl text-primary mb-8 text-center">Administration</h1>

        @if(error != null && error)
        <p class="text-sm text-red-600 text-center mb-6">Identifiants incorrects.</p>
        @endif

        <form method="post" action="/sign-in" class="flex flex-col gap-4">
            <div class="flex flex-col gap-1.5">
                <label for="name" class="text-xs text-secondary tracking-wide">Nom</label>
                <input id="name" name="name" type="text" autocomplete="username" required
                       class="bg-bg border border-bg-border rounded-full px-4 py-2.5 text-sm text-primary placeholder-muted focus:outline-none focus:border-secondary">
            </div>
            <div class="flex flex-col gap-1.5">
                <label for="secret" class="text-xs text-secondary tracking-wide">Mot de passe</label>
                <input id="secret" name="secret" type="password" autocomplete="current-password" required
                       class="bg-bg border border-bg-border rounded-full px-4 py-2.5 text-sm text-primary placeholder-muted focus:outline-none focus:border-secondary">
            </div>
            <button type="submit"
                    class="mt-2 bg-primary text-accent px-6 py-2.5 rounded-full text-sm font-medium hover:opacity-90 transition-opacity">
                Se connecter
            </button>
        </form>
    </div>
</main>

`)