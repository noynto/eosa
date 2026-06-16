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