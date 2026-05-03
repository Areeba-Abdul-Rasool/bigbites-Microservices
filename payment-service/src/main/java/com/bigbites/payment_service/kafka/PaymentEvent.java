package com.bigbites.payment_service.kafka;

import lombok.*;

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