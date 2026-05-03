package com.bigbites.notification_service.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class OrderStatusUpdatedEvent {
    private Long orderId;
    private Integer userId;
    private String status;
}
