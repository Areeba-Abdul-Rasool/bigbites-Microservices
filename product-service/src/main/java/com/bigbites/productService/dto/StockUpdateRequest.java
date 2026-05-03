package com.bigbites.productService.dto;

import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class StockUpdateRequest {
    private Integer quantity; // positive = restock, negative = deduct
}