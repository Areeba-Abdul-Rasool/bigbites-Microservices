package com.bigbites.productService.kafka;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class LowStockEvent {
    private Long productId;
    private String productName;
    private Integer currentStock;
    private Integer threshold;
}
