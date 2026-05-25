package com.assignment.order.model.event;

public record OrderPlacedEvent(
        Integer orderId,
        Integer customerId,
        Integer productId,
        Integer quantity
) {
}
