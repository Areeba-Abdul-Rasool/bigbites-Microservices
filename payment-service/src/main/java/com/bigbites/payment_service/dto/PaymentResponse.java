package com.bigbites.payment_service.dto;
import lombok.*;
import java.time.LocalDateTime;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class PaymentResponse {
    private Long id;
    private Long orderId;
    private Double amount;
    private String method;
    private String status; // PENDING, SUCCESS, FAILED, REFUNDED
    private String transactionRef;
    private LocalDateTime createdAt;
}