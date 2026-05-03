package com.bigbites.order_service.dto;
import lombok.*;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderRequest {
    private Integer userId;
    private List<OrderItemRequest> items;
    private String paymentMethod; // CASH, CARD, WALLET
}