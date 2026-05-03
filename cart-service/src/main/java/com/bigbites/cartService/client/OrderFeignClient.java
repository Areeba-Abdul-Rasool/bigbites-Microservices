package com.bigbites.cartService.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(name = "order-service", url = "http://localhost:8081")
public interface OrderFeignClient {

    @PostMapping("/orders")
    Map<String, Object> placeOrder(@RequestBody Map<String, Object> request);
}