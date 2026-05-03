package com.bigbites.order_service.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;
}