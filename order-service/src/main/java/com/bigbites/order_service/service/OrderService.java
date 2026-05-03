package com.bigbites.order_service.service;

import com.bigbites.order_service.clients.ProductFeignClient;
import com.bigbites.order_service.entity.Order;
import com.bigbites.order_service.entity.OrderItem;
import com.bigbites.order_service.exception.OrderNotFoundException;
import com.bigbites.order_service.kafka.KafkaTopics;
import com.bigbites.order_service.kafka.OrderItemEvent;
import com.bigbites.order_service.kafka.OrderPlacedEvent;
import com.bigbites.order_service.kafka.OrderStatusUpdatedEvent;
import com.bigbites.order_service.repository.OrderItemRepository;
import com.bigbites.order_service.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductFeignClient productFeignClient;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Transactional
    public Order placeOrder(Integer userId, List<Map<String, Object>> items) {

        Order order = new Order();
        order.setUserId(userId);
        order.setStatus("PENDING");
        order.setCreatedAt(LocalDateTime.now());
        Order savedOrder = orderRepository.save(order);

        double total = 0.0;
        List<OrderItemEvent> itemEvents = new ArrayList<>();

        for (Map<String, Object> item : items) {
            Long productId = Long.valueOf(item.get("productId").toString());
            int quantity = Integer.parseInt(item.get("quantity").toString());

            Map<String, Object> response = productFeignClient.getProductById(productId);
            Map<String, Object> product = (Map<String, Object>) response.get("data");
            double price = Double.parseDouble(product.get("price").toString());

            Map<String, Object> stockReq = new HashMap<>();
            stockReq.put("quantity", quantity);
            productFeignClient.deductStock(productId, stockReq);

            OrderItem orderItem = new OrderItem();
            orderItem.setOrderId(savedOrder.getId());
            orderItem.setProductId(productId);
            orderItem.setQuantity(quantity);
            orderItem.setPrice(price);
            orderItemRepository.save(orderItem);

            total += price * quantity;
            itemEvents.add(new OrderItemEvent(productId, quantity, price));
        }

        savedOrder.setTotalAmount(total);
        orderRepository.save(savedOrder);

        kafkaTemplate.send(KafkaTopics.ORDER_PLACED, OrderPlacedEvent.builder()
                .orderId(savedOrder.getId())
                .userId(userId)
                .totalAmount(total)
                .items(itemEvents)
                .build());

        log.info("Published ORDER_PLACED event for orderId={}", savedOrder.getId());
        return savedOrder;
    }

    public Order getOrderById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));
    }

    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    public List<Order> getOrdersByUser(Integer userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public List<Order> getOrdersByStatus(String status) {
        return orderRepository.findByStatus(status);
    }

    public List<OrderItem> getOrderItems(Long orderId) {
        return orderItemRepository.findByOrderId(orderId);
    }

    @Transactional
    public Order updateOrderStatus(Long id, String status) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));
        order.setStatus(status);
        orderRepository.save(order);  // FIX: was missing save before kafka publish

        kafkaTemplate.send(KafkaTopics.ORDER_STATUS_UPDATED, OrderStatusUpdatedEvent.builder()
                .orderId(order.getId())
                .userId(order.getUserId())
                .status(status)
                .build());

        log.info("Published ORDER_STATUS_UPDATED event orderId={} status={}", id, status);
        return order;
    }

    @Transactional
    public void cancelOrder(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));

        if ("DELIVERED".equals(order.getStatus())) {
            throw new IllegalStateException("Cannot cancel a delivered order");
        }

        order.setStatus("CANCELLED");
        orderRepository.save(order);  // FIX: was never saved in original

        kafkaTemplate.send(KafkaTopics.ORDER_CANCELLED, OrderStatusUpdatedEvent.builder()
                .orderId(order.getId())
                .userId(order.getUserId())
                .status("CANCELLED")
                .build());

        log.info("Published ORDER_CANCELLED event orderId={}", id);
    }
}