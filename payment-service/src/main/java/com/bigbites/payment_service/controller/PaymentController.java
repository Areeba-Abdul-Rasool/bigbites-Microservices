package com.bigbites.payment_service.controller;

import com.bigbites.payment_service.entity.Payment;
import com.bigbites.payment_service.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<Payment> initiatePayment(@RequestBody Map<String, Object> request) {
        Long orderId = Long.valueOf(request.get("orderId").toString());
        String method = (String) request.get("method");
        Integer userId = Integer.valueOf(request.get("userId").toString());
        return ResponseEntity.ok(paymentService.initiatePayment(orderId, userId, method));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Payment> getById(@PathVariable Long id) {
        return ResponseEntity.ok(paymentService.getPaymentById(id));
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<Payment> getByOrderId(@PathVariable Long orderId) {
        return ResponseEntity.ok(paymentService.getPaymentByOrderId(orderId));
    }

    @GetMapping
    public ResponseEntity<List<Payment>> getAll() {
        return ResponseEntity.ok(paymentService.getAllPayments());
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<Payment>> getByStatus(@PathVariable String status) {
        return ResponseEntity.ok(paymentService.getByStatus(status));
    }

    @GetMapping("/method/{method}")
    public ResponseEntity<List<Payment>> getByMethod(@PathVariable String method) {
        return ResponseEntity.ok(paymentService.getByMethod(method));
    }

    @PostMapping("/{id}/refund")
    public ResponseEntity<Payment> refund(@PathVariable Long id, @RequestParam Integer userId) {
        return ResponseEntity.ok(paymentService.refundPayment(id, userId));
    }
}