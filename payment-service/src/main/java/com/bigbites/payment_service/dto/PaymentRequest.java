package com.bigbites.payment_service.dto;

import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class PaymentRequest {
    private Long orderId;
    private Double amount;
    private String method; // CASH, CARD, WALLET, ONLINE
}