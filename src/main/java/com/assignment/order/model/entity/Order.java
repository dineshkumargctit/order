package com.assignment.order.model.entity;

import com.assignment.order.model.enums.OrderStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@Table(name="CUSTOMER_ORDERS")
@AllArgsConstructor
@NoArgsConstructor
public class Order {
    @Id
    private Integer orderId;
    private Integer customerId;
    private Integer productId;
    private Integer quantity;
    private LocalDateTime orderTime = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    private OrderStatus status;
}
