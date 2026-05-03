package com.bigbites.order_service.kafka;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class OrderStatusUpdatedEvent {
    private Long orderId;
    private Integer userId;
    private String status;
}
