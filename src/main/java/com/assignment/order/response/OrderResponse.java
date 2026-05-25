package com.assignment.order.response;

import com.assignment.order.model.enums.OrderStatus;

public record OrderResponse(
        Integer orderId,
        Integer customerId,       // maps from order.customerId
        OrderStatus status,
        String message
) {
}
