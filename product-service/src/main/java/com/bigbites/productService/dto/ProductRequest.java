package com.bigbites.productService.dto;

import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ProductRequest {
    private String name;
    private String description;
    private Double price;
    private Integer stock;
}