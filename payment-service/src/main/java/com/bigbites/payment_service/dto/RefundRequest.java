package com.bigbites.payment_service.dto;

import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class RefundRequest {
    private String reason;
}