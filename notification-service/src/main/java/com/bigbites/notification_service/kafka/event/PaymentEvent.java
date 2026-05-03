package com.bigbites.notification_service.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class PaymentEvent {
    private Long paymentId;
    private Long orderId;
    private Integer userId;
    private Double amount;
    private String method;
    private String status; // SUCCESS, FAILED, REFUNDED
    private String transactionRef;
}