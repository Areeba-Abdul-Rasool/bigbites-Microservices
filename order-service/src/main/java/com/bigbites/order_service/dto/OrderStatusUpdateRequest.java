package com.bigbites.order_service.dto;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatusUpdateRequest {
    private String status; // PENDING, CONFIRMED, PREPARING, DELIVERED, CANCELLED
}