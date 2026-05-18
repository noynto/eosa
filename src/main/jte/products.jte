@import java.util.List
@import me.noynto.eosa.shared.ProductId
@param List<ProductId> productIds

@template.layouts.main(title = "Eosa — Tous les produits", content = @`

<section class="px-5 md:px-8 py-10">
    <div class="flex items-baseline justify-between mb-8">
        <h1 class="font-serif text-3xl font-normal">Tous les produits</h1>
        <span class="text-xs text-secondary">${productIds.size()} pièces</span>
    </div>

    <div class="grid grid-cols-2 md:grid-cols-4 gap-4 md:gap-5">
        @for(var productId : productIds)
            <div hx-get="/products/${productId.value()}/card"
                 hx-trigger="load"
                 hx-swap="outerHTML">
                <div class="bg-bg-soft aspect-square rounded-3xl animate-pulse"></div>
                <div class="mt-3.5 px-1 flex flex-col gap-2">
                    <div class="h-4 bg-bg-soft rounded-full animate-pulse w-3/4"></div>
                    <div class="h-3 bg-bg-soft rounded-full animate-pulse w-1/3"></div>
                </div>
            </div>
        @endfor
    </div>
</section>

`)