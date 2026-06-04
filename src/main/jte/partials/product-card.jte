@import me.noynto.eosa.product.Product
@param Product product

<a href="/products/${product.getId().value()}" class="group">
    <div class="bg-bg-soft aspect-square rounded-3xl relative overflow-hidden">
        @if(!product.getImageIds().isEmpty())
            <img src="/images/${product.getImageIds().getFirst().value()}"
                 alt="${product.getName()}"
                 class="w-full h-full object-cover">
        @else
            <svg viewBox="0 0 200 200" class="w-full h-full">
                <ellipse cx="100" cy="148" rx="52" ry="9" fill="#775546" opacity="0.15"/>
                <circle cx="100" cy="106" r="38" fill="#B8A88E" opacity="0.4"/>
            </svg>
        @endif
        <button class="absolute top-3.5 right-3.5 w-9 h-9 bg-bg rounded-full flex items-center justify-center" aria-label="Favoris">
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="#601414" stroke-width="2"><path d="M19 14c1.49-1.46 3-3.21 3-5.5A5.5 5.5 0 0 0 16.5 3c-1.76 0-3 .5-4.5 2-1.5-1.5-2.74-2-4.5-2A5.5 5.5 0 0 0 2 8.5c0 2.29 1.51 4.04 3 5.5l7 7Z"/></svg>
        </button>
    </div>
    <div class="mt-3.5 px-1">
        <div class="flex justify-between items-baseline mb-1">
            <div class="font-serif text-[17px]">${product.getName()}</div>
            <div class="text-[15px] font-medium">${product.getPrice().stripTrailingZeros().toPlainString()} €</div>
        </div>
        @if(product.getTagline() != null)
            <div class="text-xs text-secondary">${product.getTagline()}</div>
        @endif
    </div>
</a>