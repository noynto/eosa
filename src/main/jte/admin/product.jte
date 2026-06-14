@import me.noynto.eosa.product.Product
@import me.noynto.eosa.product.ProductCategory
@import me.noynto.eosa.product.ProductState
@param Product product

@template.layouts.admin(title = product.getName(), content = @`

<div class="max-w-2xl">

    <div class="flex items-center justify-between mb-8">
        <div class="flex items-center gap-3">
            <a href="/admin/products" class="text-xs text-muted hover:text-secondary transition-colors">← Produits</a>
            <span class="text-muted">/</span>
            <h1 class="text-xl font-medium">${product.getName()}</h1>
        </div>
        <a href="/products/${product.getId().value()}" target="_blank"
           class="flex items-center gap-1.5 text-xs text-secondary hover:text-primary border border-bg-border rounded-lg px-3 py-1.5 transition-colors">
            <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6"/><polyline points="15 3 21 3 21 9"/><line x1="10" y1="14" x2="21" y2="3"/></svg>
            Voir la page
        </a>
    </div>

    <!-- Accroche -->
    <section class="bg-bg border border-bg-border rounded-xl p-5 mb-4">
        <div class="flex items-center justify-between mb-3">
            <h2 class="text-sm font-medium">Accroche</h2>
            <span id="toast-tagline" class="text-xs text-success"></span>
        </div>
        <form hx-patch="/admin/products/${product.getId().value()}/tagline"
              hx-target="#toast-tagline" hx-swap="innerHTML"
              class="flex flex-col gap-3">
            <textarea name="tagline" rows="3" placeholder="Une courte description du produit…"
                      class="bg-bg-soft border border-bg-border rounded-lg px-3 py-2 text-sm text-primary placeholder-muted focus:outline-none focus:border-secondary resize-none w-full"
            >${product.getTagline() != null ? product.getTagline() : ""}</textarea>
            <div class="flex justify-end">
                <button type="submit" class="bg-primary text-accent px-4 py-1.5 rounded-lg text-xs font-medium hover:bg-primary-dark transition-colors">
                    Enregistrer
                </button>
            </div>
        </form>
    </section>

    <!-- Prix -->
    <section class="bg-bg border border-bg-border rounded-xl p-5 mb-4">
        <div class="flex items-center justify-between mb-3">
            <h2 class="text-sm font-medium">Prix</h2>
            <span id="toast-price" class="text-xs text-success"></span>
        </div>
        <form hx-patch="/admin/products/${product.getId().value()}/price"
              hx-target="#toast-price" hx-swap="innerHTML"
              class="flex items-center gap-3">
            <div class="relative">
                <input name="price" type="number" min="0" step="0.01"
                       value="${product.getPrice() != null ? product.getPrice().toPlainString() : ""}"
                       placeholder="0.00"
                       class="bg-bg-soft border border-bg-border rounded-lg pl-3 pr-8 py-2 text-sm text-primary placeholder-muted focus:outline-none focus:border-secondary w-36">
                <span class="absolute right-3 top-1/2 -translate-y-1/2 text-xs text-muted">€</span>
            </div>
            <button type="submit" class="bg-primary text-accent px-4 py-1.5 rounded-lg text-xs font-medium hover:bg-primary-dark transition-colors">
                Enregistrer
            </button>
        </form>
    </section>

    <!-- Catégorie -->
    <section class="bg-bg border border-bg-border rounded-xl p-5 mb-4">
        <div class="flex items-center justify-between mb-3">
            <h2 class="text-sm font-medium">Catégorie</h2>
            <span id="toast-category" class="text-xs text-success"></span>
        </div>
        <form hx-patch="/admin/products/${product.getId().value()}/category"
              hx-target="#toast-category" hx-swap="innerHTML"
              class="flex items-center gap-3">
            <select name="category"
                    class="bg-bg-soft border border-bg-border rounded-lg px-3 py-2 text-sm text-primary focus:outline-none focus:border-secondary">
                <option value="">— Aucune —</option>
                <option value="NECKLACE" selected="${product.getCategory() == ProductCategory.NECKLACE}">Collier</option>
                <option value="BRACELET" selected="${product.getCategory() == ProductCategory.BRACELET}">Bracelet</option>
            </select>
            <button type="submit" class="bg-primary text-accent px-4 py-1.5 rounded-lg text-xs font-medium hover:bg-primary-dark transition-colors">
                Enregistrer
            </button>
        </form>
    </section>

    <!-- Images -->
    @template.admin.partials.product-images(product = product)

    <!-- État -->
    <section class="bg-bg border border-bg-border rounded-xl p-5">
        <div class="flex items-center justify-between mb-3">
            <h2 class="text-sm font-medium">État</h2>
            <span id="toast-state" class="text-xs text-success"></span>
        </div>
        <form hx-patch="/admin/products/${product.getId().value()}/state"
              hx-target="#toast-state" hx-swap="innerHTML"
              class="flex items-center gap-3">
            <select name="state"
                    class="bg-bg-soft border border-bg-border rounded-lg px-3 py-2 text-sm text-primary focus:outline-none focus:border-secondary">
                <option value="DRAFTED" selected="${product.getState() == ProductState.DRAFTED}">Brouillon</option>
                <option value="PUBLISHED" selected="${product.getState() == ProductState.PUBLISHED}">Publié</option>
                <option value="ARCHIVED" selected="${product.getState() == ProductState.ARCHIVED}">Archivé</option>
            </select>
            <button type="submit" class="bg-primary text-accent px-4 py-1.5 rounded-lg text-xs font-medium hover:bg-primary-dark transition-colors">
                Enregistrer
            </button>
        </form>
    </section>

</div>

`)