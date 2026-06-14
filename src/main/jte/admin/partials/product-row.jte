@import me.noynto.eosa.product.Product
@param Product product

<a href="/admin/products/${product.getId().value()}"
   class="grid grid-cols-[1fr_auto_auto] px-4 py-3 border-b border-bg-border last:border-0 hover:bg-bg-soft transition-colors items-center">
    <span class="text-sm text-primary">${product.getName()}</span>
    <span class="w-24 flex justify-center">
        @if(product.getState() != null)
            <span class="text-[11px] px-2 py-0.5 rounded-full
                @if(product.getState() == me.noynto.eosa.product.ProductState.PUBLISHED)bg-success/10 text-success
                @elseif(product.getState() == me.noynto.eosa.product.ProductState.DRAFTED)bg-bg-border text-secondary
                @else bg-bg-border text-muted
                @endif">
                ${product.getState().name().toLowerCase()}
            </span>
        @endif
    </span>
    <span class="w-20 text-center text-xs text-muted">
        @if(product.getCategory() != null)${product.getCategory().name().toLowerCase()}@endif
    </span>
</a>