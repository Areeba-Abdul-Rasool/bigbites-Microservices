package com.bigbites.order_service.clients;

import com.bigbites.order_service.entity.Product;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@FeignClient(name = "product-service", url = "${service.product.url}")
public interface ProductFeignClient {

    @GetMapping("products/{id}")
    Map<String, Object> getProductById(@PathVariable Long id);

    @PostMapping("products/{id}/stock")
    Map<String, Object> deductStock(@PathVariable Long id, @RequestBody Map<String, Object> request);

}