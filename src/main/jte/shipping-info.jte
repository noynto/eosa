@import me.noynto.eosa.cart.CartShippingRule
@param CartShippingRule rule

<div class="flex items-center gap-2.5">
    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#601414" stroke-width="2"><rect x="1" y="3" width="15" height="13" rx="2"/><path d="m16 8 4-2v10l-4-2"/></svg>
    Livraison offerte dès ${rule.getFreeThreshold().stripTrailingZeros().toPlainString()} € · expédié sous 48h
</div>