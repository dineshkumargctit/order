package com.assignment.order.service;

import com.assignment.order.model.entity.Order;
import com.assignment.order.model.enums.OrderStatus;
import com.assignment.order.model.event.OrderPlacedEvent;
import com.assignment.order.repo.OrderRepository;
import com.assignment.order.request.OrderRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

//import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class)
public class OrderServiceTest {
    @Mock
    private OrderRepository repository;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private OrderService orderService;

    private OrderRequest sampleRequest;
    private Integer orderId;

    @BeforeEach
    void setUp() {
        orderId = 101;
        // Mocking OrderRequest record/DTO components (customerId, productId, quantity)
        sampleRequest = new OrderRequest(1, 202, 5);
    }

    @Test
    void createOrder_Success() throws Exception {
        // Arrange
        // Mocking the asynchronous Kafka call to return a completed future to satisfy .get()
        CompletableFuture<SendResult<String, Object>> future = CompletableFuture.completedFuture(mock(SendResult.class));
        when(kafkaTemplate.send(eq("order-placed-topic"), eq(String.valueOf(orderId)), any(OrderPlacedEvent.class)))
                .thenReturn(future);

        // Act
        Order createdOrder = orderService.createOrder(orderId, sampleRequest);

        // Assertions for the returned object
        assertNotNull(createdOrder);
        assertEquals(orderId, createdOrder.getOrderId()); // Assuming getter exists based on standard conventions
        assertEquals(OrderStatus.PENDING_INVENTORY_CHECK, createdOrder.getStatus());

        // Verify Database interactions
        verify(repository, times(1)).save(any(Order.class));

        // Verify Kafka Event Payload matching incoming data
        ArgumentCaptor<OrderPlacedEvent> eventCaptor = ArgumentCaptor.forClass(OrderPlacedEvent.class);
        verify(kafkaTemplate, times(1)).send(eq("order-placed-topic"), eq(String.valueOf(orderId)), eventCaptor.capture());

        OrderPlacedEvent capturedEvent = eventCaptor.getValue();
        assertEquals(orderId, capturedEvent.orderId());
        assertEquals(sampleRequest.customerId(), capturedEvent.customerId());
    }

    @Test
    void getOrderByOrderId_Found() {
        // Arrange
        Order existingOrder = new Order(orderId, 1, 202, 5, null, OrderStatus.PENDING_INVENTORY_CHECK);
        when(repository.findById(orderId)).thenReturn(Optional.of(existingOrder));

        // Act
        Order result = orderService.getOrderByOrderId(orderId);

        // Assert
        assertNotNull(result);
        assertEquals(orderId, result.getOrderId());
        verify(repository, times(1)).findById(orderId);
    }

    @Test
    void getOrderByOrderId_NotFound_ReturnsEmptyOrderFallback() {
        // Arrange
        when(repository.findById(orderId)).thenReturn(Optional.empty());

        // Act
        Order result = orderService.getOrderByOrderId(orderId);

        // Assert
        assertNotNull(result);
        assertEquals(0, result.getOrderId()); // Expecting default fallback object values
        assertNull(result.getStatus());
    }

/*    @Test
    void getOrdersByCustomerId_Success() {
        // Arrange
        Integer customerId = 1;
        Order mockOrder = new Order(orderId, customerId, 202, 5, null, OrderStatus.PENDING_INVENTORY_CHECK);

        // Handling the cast in your implementation `(Order) repository.findByCustomerId(...)`
        //when(repository.findByCustomerId(customerId)).thenReturn(mockOrder);
        when(repository.findByCustomerId(customerId)).thenReturn((List<Order>) mockOrder);

        // Act
        List<Order> results = orderService.getOrdersByCustomerId(customerId);

        // Assert
        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals(customerId, results.get(0).getCustomerId());
        verify(repository, times(1)).findByCustomerId(customerId);
    }*/

    @Test
    void getOrdersByCustomerId_Success() {
        // Arrange
        Integer customerId = 1;
        Order mockOrder = new Order(orderId, customerId, 202, 5, null, OrderStatus.PENDING_INVENTORY_CHECK);

        // Stub it to return a List instead of a single object
        when(repository.findByCustomerId(customerId)).thenReturn(List.of(mockOrder));

        // Act
        List<Order> results = orderService.getOrdersByCustomerId(customerId);

        // Assert
        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals(customerId, results.get(0).getCustomerId());
        verify(repository, times(1)).findByCustomerId(customerId);
    }

    @Test
    void testResilienceFallbacks_DirectCall() {
        // Note: Direct calls test the logic inside the fallbacks.
        // Aop-based annotations like @CircuitBreaker are validated integration style via @SpringBootTest.
        Throwable exception = new RuntimeException("Simulated failure");

        Order circuitBreakerFallbackResult = orderService.createOrderFallback(101L, 500.0, exception);
        Order retryFallbackResult = orderService.handleServerErrorFallback(101L, 500.0, exception);

        assertEquals(0, circuitBreakerFallbackResult.getOrderId());
        assertEquals(0, retryFallbackResult.getOrderId());
    }
}
