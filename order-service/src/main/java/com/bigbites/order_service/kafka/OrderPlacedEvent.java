package com.bigbites.order_service.kafka;
import lombok.*;
import java.util.List;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class OrderPlacedEvent {
    private Long orderId;
    private Integer userId;
    private Double totalAmount;
    private List<OrderItemEvent> items;
}
