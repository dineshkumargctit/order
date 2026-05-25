package com.assignment.order.service;

import com.assignment.order.model.enums.OrderStatus;
import com.assignment.order.model.event.InventoryCheckedEvent;
import com.assignment.order.repo.OrderRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class OrderFeedbackConsumer {

    private final OrderRepository repository;

    public OrderFeedbackConsumer(OrderRepository repository) {
        this.repository = repository;
    }

    @KafkaListener(topics = "inventory-checked-topic", groupId = "order-feedback-group")
    public void handleInventoryFeedback(InventoryCheckedEvent event,  Acknowledgment ack) {
        log.info("Received inventory feedback loop for order: {}", event.orderId());

        repository.findById(event.orderId()).ifPresent(order -> {
            if (event.isAvailable()) {
                //order.setStatus("CONFIRMED");
                order.setStatus(OrderStatus.CONFIRMED);
            } else {
                //order.setStatus("REJECTED");
                order.setStatus(OrderStatus.FAILED_OUT_OF_STOCK);
            }
            repository.save(order);
            ack.acknowledge();
            log.info("Order status finalized as: {}", order.getStatus());
        });
    }
}
