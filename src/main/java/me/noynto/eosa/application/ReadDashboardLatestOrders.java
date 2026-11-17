package me.noynto.eosa.application;

import me.noynto.eosa.order.Order;
import me.noynto.eosa.order.OrderProvider;

import java.util.List;

/** Les dernières commandes enregistrées, la plus récente d'abord. */
public record ReadDashboardLatestOrders(
        OrderProvider orderProvider
) {

    static final int COUNT = 5;

    public Result handle() {
        return new Result(orderProvider.readIds(null, null, null, null)
                .limit(COUNT)
                .map(id -> orderProvider.read(id).orElseThrow())
                .toList());
    }

    public record Result(
            List<Order> orders
    ) {
    }

}
