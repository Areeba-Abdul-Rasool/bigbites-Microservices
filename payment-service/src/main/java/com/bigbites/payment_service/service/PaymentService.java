package com.bigbites.payment_service.service;

import com.bigbites.payment_service.client.OrderFeignClient;
import com.bigbites.payment_service.entity.Payment;
import com.bigbites.payment_service.kafka.KafkaTopics;
import com.bigbites.payment_service.kafka.PaymentEvent;
import com.bigbites.payment_service.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderFeignClient orderFeignClient;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Transactional
    public Payment initiatePayment(Long orderId, Integer userId, String method) {
        Map<String, Object> order = orderFeignClient.getOrderById(orderId);

        if (order == null) {
            throw new RuntimeException("Order not found with id: " + orderId);
        }

        Double amount = Double.valueOf(order.get("totalAmount").toString());

        if (amount <= 0) {
            throw new RuntimeException("Invalid order amount for id: " + orderId);
        }

        String txRef = "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        Payment payment = new Payment();
        payment.setOrderId(orderId);
        payment.setAmount(amount);
        payment.setMethod(method);
        payment.setTransactionRef(txRef);
        payment.setStatus("SUCCESS");
        payment.setCreatedAt(LocalDateTime.now());
        Payment saved = paymentRepository.save(payment);

        orderFeignClient.updateOrderStatus(orderId, "CONFIRMED");

        kafkaTemplate.send(KafkaTopics.PAYMENT_SUCCESS, PaymentEvent.builder()
                .paymentId(saved.getId())
                .orderId(orderId)
                .amount(amount)
                .method(method)
                .userId(userId)
                .status("SUCCESS")
                .transactionRef(txRef)
                .build());

        log.info("Published PAYMENT_SUCCESS event orderId={} txRef={}", orderId, txRef);
        return saved;
    }

    public Payment getPaymentById(Long id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Payment not found: " + id));
    }

    public Payment getPaymentByOrderId(Long orderId) {
        return paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Payment not found for order: " + orderId));
    }

    public List<Payment> getAllPayments() {
        return paymentRepository.findAll();
    }

    public List<Payment> getByStatus(String status) {
        return paymentRepository.findByStatus(status);
    }

    public List<Payment> getByMethod(String method) {
        return paymentRepository.findByMethod(method);
    }

    @Transactional
    public Payment refundPayment(Long id, Integer userId) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Payment not found: " + id));

        if (!"SUCCESS".equals(payment.getStatus())) {
            throw new RuntimeException("Only successful payments can be refunded");
        }

        payment.setStatus("REFUNDED");
        Payment saved = paymentRepository.save(payment);

        orderFeignClient.updateOrderStatus(payment.getOrderId(), "CANCELLED");

        kafkaTemplate.send(KafkaTopics.PAYMENT_REFUNDED, PaymentEvent.builder()
                .paymentId(saved.getId())
                .orderId(saved.getOrderId())
                .amount(saved.getAmount())
                .method(saved.getMethod())
                .userId(userId)
                .status("REFUNDED")
                .transactionRef(saved.getTransactionRef())
                .build());

        log.info("Payment {} refunded", id);
        return saved;
    }
}