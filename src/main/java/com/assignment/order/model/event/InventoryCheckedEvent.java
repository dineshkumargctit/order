package com.assignment.order.model.event;

public record InventoryCheckedEvent(
        Integer orderId,
        Integer productId,
        boolean isAvailable,
        String remarks
) {
}
