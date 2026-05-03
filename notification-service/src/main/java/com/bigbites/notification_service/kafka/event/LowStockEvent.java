package com.bigbites.notification_service.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class LowStockEvent {
    private Long productId;
    private String productName;
    private Integer currentStock;
    private Integer threshold;
}
