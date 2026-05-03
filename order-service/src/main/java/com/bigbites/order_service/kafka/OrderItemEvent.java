package com.bigbites.order_service.kafka;

import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class OrderItemEvent {
    private Long productId;
    private Integer quantity;
    private Double price;
}