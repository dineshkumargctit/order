package com.assignment.order.service;

import com.assignment.order.model.event.OrderPlacedEvent;
import com.assignment.order.request.OrderRequest;
import com.assignment.order.model.entity.Order;
import com.assignment.order.model.enums.OrderStatus;
import com.assignment.order.repo.OrderRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class OrderService {
    private final OrderRepository repository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public OrderService(OrderRepository repository, KafkaTemplate<String, Object> kafkaTemplate) {
        this.repository = repository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Transactional
    @CircuitBreaker(name="createOrderBreaker", fallbackMethod = "createOrderFallback")
    @Retry(name = "createOrderRetry", fallbackMethod = "handleServerErrorFallback")
    public Order createOrder(Integer orderId,  OrderRequest orderRequest)   throws Exception {

        Order order = new Order(orderId, orderRequest.customerId(), orderRequest.productId(), orderRequest.quantity(), LocalDateTime.now(), OrderStatus.PENDING_INVENTORY_CHECK );

        repository.save(order);

        // Emit the placement event asynchronously out to Kafka
        OrderPlacedEvent event = new OrderPlacedEvent(orderId, orderRequest.customerId(), orderRequest.productId(), orderRequest.quantity());
        kafkaTemplate.send("order-placed-topic", String.valueOf(orderId), event).get();

        return order;
    }

    public Order createOrderFallback(Long orderId, Double amount, Throwable exception) {
        log.error("Error in placing Order, triggering fallback Error: {}", exception.getMessage());

        return new Order(0,0,0,0,LocalDateTime.now(), null);
    }

    public Order handleServerErrorFallback(Long orderId, Double amount, Throwable exception) {
        log.error("Error in placing Order after multiple Retry Error: {}", exception.getMessage());

        return new Order(0,0,0,0,LocalDateTime.now(), null);
    }


    /*public Order getOrderStatus(Integer orderId) {
        Optional<Order> orderById = repository.findById(orderId);
        return orderById.orElse(new Order(0,null,null,null,null,null));
    }*/

    public Order getOrderByOrderId(Integer orderId) {
        Optional<Order> orderById = repository.findById(orderId);
        return orderById.orElse(new Order(0,null,null,null,null,null));
    }

    public List<Order> getOrdersByCustomerId(Integer customerId) {
        //return List.of((Order) repository.findByCustomerId(customerId));
        return repository.findByCustomerId(customerId);
    }
}
