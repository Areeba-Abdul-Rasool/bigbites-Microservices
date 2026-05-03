package com.bigbites.notification_service.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class OrderPlacedEvent {
    private Long orderId;
    private Integer userId;
    private Double totalAmount;
    private List<OrderItemEvent> items;
}
