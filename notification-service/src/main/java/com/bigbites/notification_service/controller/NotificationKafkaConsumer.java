package com.bigbites.notification_service.controller;

import com.bigbites.notification_service.entity.Notification;
import com.bigbites.notification_service.kafka.KafkaTopics;
import com.bigbites.notification_service.kafka.event.*;
import com.bigbites.notification_service.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationKafkaConsumer {

    private final NotificationRepository notificationRepository;

    // ── order events ──────────────────────────────────────────

    @KafkaListener(topics = KafkaTopics.ORDER_PLACED, groupId = "notification-group")
    public void onOrderPlaced(OrderPlacedEvent event) {
        log.info("Consumed ORDER_PLACED event for orderId={}", event.getOrderId());
        save(
                event.getUserId(),
                String.format("Your order #%d has been placed. Total: PKR %.2f",
                        event.getOrderId(), event.getTotalAmount()),
                "ORDER_PLACED"
        );
    }

    @KafkaListener(topics = KafkaTopics.ORDER_STATUS_UPDATED, groupId = "notification-group")
    public void onOrderStatusUpdated(OrderStatusUpdatedEvent event) {
        log.info("Consumed ORDER_STATUS_UPDATED event orderId={} status={}", event.getOrderId(), event.getStatus());
        save(
                event.getUserId(),
                String.format("Order #%d status updated to: %s", event.getOrderId(), event.getStatus()),
                "ORDER_STATUS_UPDATE"
        );
    }

    @KafkaListener(topics = KafkaTopics.ORDER_CANCELLED, groupId = "notification-group")
    public void onOrderCancelled(OrderStatusUpdatedEvent event) {
        log.info("Consumed ORDER_CANCELLED event orderId={}", event.getOrderId());
        save(
                event.getUserId(),
                String.format("Order #%d has been cancelled.", event.getOrderId()),
                "ORDER_CANCELLED"
        );
    }

    // ── payment events ────────────────────────────────────────

    @KafkaListener(topics = KafkaTopics.PAYMENT_SUCCESS, groupId = "notification-group")
    public void onPaymentSuccess(PaymentEvent event) {
        log.info("Consumed PAYMENT_SUCCESS event orderId={}", event.getOrderId());
        save(
                event.getUserId(),
                String.format("Payment of PKR %.2f confirmed for Order #%d. Ref: %s",
                        event.getAmount(), event.getOrderId(), event.getTransactionRef()),
                "PAYMENT_SUCCESS"
        );
    }

    @KafkaListener(topics = KafkaTopics.PAYMENT_FAILED, groupId = "notification-group")
    public void onPaymentFailed(PaymentEvent event) {
        log.info("Consumed PAYMENT_FAILED event orderId={}", event.getOrderId());
        save(
                event.getUserId(),
                String.format("Payment of PKR %.2f FAILED for Order #%d. Please try again.",
                        event.getAmount(), event.getOrderId()),
                "PAYMENT_FAILED"
        );
    }

    @KafkaListener(topics = KafkaTopics.PAYMENT_REFUNDED, groupId = "notification-group")
    public void onPaymentRefunded(PaymentEvent event) {
        log.info("Consumed PAYMENT_REFUNDED event paymentId={}", event.getPaymentId());
        save(
                event.getUserId(),
                String.format("Refund of PKR %.2f processed for Order #%d. Ref: %s",
                        event.getAmount(), event.getOrderId(), event.getTransactionRef()),
                "PAYMENT_REFUNDED"
        );
    }

    // ── product events ────────────────────────────────────────

    @KafkaListener(topics = KafkaTopics.LOW_STOCK_ALERT, groupId = "notification-group")
    public void onLowStock(LowStockEvent event) {
        log.warn("Consumed LOW_STOCK_ALERT for product={} stock={}", event.getProductName(), event.getCurrentStock());
        // notify admin (userId = 1 as admin)
        save(
                1,
                String.format("LOW STOCK: '%s' has only %d units left. Please restock.",
                        event.getProductName(), event.getCurrentStock()),
                "LOW_STOCK_ALERT"
        );
    }

    @KafkaListener(topics = KafkaTopics.STOCK_DEDUCTED, groupId = "notification-group")
    public void onStockDeducted(StockDeductedEvent event) {
        log.info("Consumed STOCK_DEDUCTED for product={}", event.getProductName());
        save(
                1,
                String.format("Stock update: '%s' — %d units deducted. Remaining: %d",
                        event.getProductName(), event.getQuantityDeducted(), event.getRemainingStock()),
                "STOCK_DEDUCTED"
        );
    }

    // ── helper ────────────────────────────────────────────────
    private void save(Integer userId, String message, String type) {
        Notification n = new Notification();
        n.setUserId(userId);
        n.setMessage(message);
        n.setType(type);
        n.setStatus("SENT");
        n.setCreatedAt(LocalDateTime.now());
        notificationRepository.save(n);
    }
}