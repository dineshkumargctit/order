package com.assignment.order.controller;

import com.assignment.order.request.OrderRequest;
import com.assignment.order.response.OrderResponse;
import com.assignment.order.model.entity.Order;
import com.assignment.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@Slf4j
@RequestMapping("order")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping()
    public ResponseEntity<OrderResponse> placeOrder(@Valid @RequestBody OrderRequest orderRequest){
        try {
            // Generated OrderID here to make OrderId idempotent
            Integer orderId = Math.abs(UUID.randomUUID().hashCode());
            Order order = orderService.createOrder(orderId, orderRequest);

            String userMsg = "Order request placed into processing pipeline successfully.";
            //OrderResponse response = OrderMapper.INSTANCE.toOrderResponse(order);
            //OrderResponse response = OrderMapper.INSTANCE.toOrderResponse(order, userMsg);
            //OrderResponse response = orderMapper.toOrderResponse(order, userMsg);
            OrderResponse response = new OrderResponse(order.getOrderId(),order.getCustomerId(),order.getStatus(),userMsg);// OrderMapper.INSTANCE.toOrderResponse(order, userMsg);

            return ResponseEntity.accepted().body(response);
        }
        catch (Exception e) {
            OrderResponse errorResponse = new OrderResponse(
                    null,null, null, "Error - Broker handoff failed: " + e.getMessage()
            );
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }


    /*@GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrderById(@PathVariable Integer orderId) {
        log.info("Received request to fetch details for Order ID: {}", orderId);

        Order order = orderService.getOrderStatus(orderId);

        return ResponseEntity.ok(order);
    }*/

    // Method A - Only fires if ?orderId is present in the URI
    @GetMapping(params = "orderId")
    public ResponseEntity<Order> getOrderByOrderId(@RequestParam("orderId") Integer orderId) {
        return ResponseEntity.ok(orderService.getOrderByOrderId(orderId));
    }

    // Method B - Only fires if ?customerId is present in the URI
    @GetMapping(params = "customerId")
    public ResponseEntity<List<Order>> getByStatus(@RequestParam("customerId") Integer customerId) {
        return ResponseEntity.ok(orderService.getOrdersByCustomerId(customerId));
    }



}
