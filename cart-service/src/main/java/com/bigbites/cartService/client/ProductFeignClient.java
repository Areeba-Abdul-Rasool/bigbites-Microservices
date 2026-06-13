package com.bigbites.cartService.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@FeignClient(name = "product-service", url = "http://localhost:8082/products")
public interface ProductFeignClient {

    @GetMapping("/{id}")
    Map<String, Object> getProductById(@PathVariable Long id);

    @PatchMapping("/{id}/stock")
    Map<String, Object> adjustStock(@PathVariable Long id, @RequestBody Map<String, Object> request);
}