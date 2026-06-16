@import me.noynto.eosa.cart.CartShippingRule
@param CartShippingRule rule

<div class="bg-primary text-accent text-center text-xs py-1.5 tracking-wide">
    ✶ Livraison offerte dès ${rule.getFreeThreshold().stripTrailingZeros().toPlainString()} € ✶
</div>