package com.bigbites.payment_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@FeignClient(name = "order-service", url = "${service.order.url}")
public interface OrderFeignClient {

    @GetMapping("/orders/{id}")
    Map<String, Object> getOrderById(@PathVariable("id") Long id);

    @PostMapping("/orders/{id}/status")
    Map<String, Object> updateOrderStatus(@PathVariable("id") Long id,
                                          @RequestParam("status") String status);
}