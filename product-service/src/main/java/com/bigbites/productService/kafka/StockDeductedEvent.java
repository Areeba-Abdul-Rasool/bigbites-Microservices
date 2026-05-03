package com.bigbites.productService.kafka;

import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class StockDeductedEvent {
    private Long productId;
    private String productName;
    private Integer quantityDeducted;
    private Integer remainingStock;
}