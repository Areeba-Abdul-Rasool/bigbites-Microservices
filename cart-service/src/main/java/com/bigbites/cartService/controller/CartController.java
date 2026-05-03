package com.bigbites.cartService.controller;

import com.bigbites.cartService.entity.Cart;
import com.bigbites.cartService.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @PostMapping("/{userId}/add")
    public ResponseEntity<Cart> addToCart(@PathVariable Integer userId,
                                          @RequestBody Map<String, Object> request) {
        return ResponseEntity.ok(cartService.addToCart(userId, request));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<Cart> getCart(@PathVariable Integer userId) {
        return ResponseEntity.ok(cartService.getCart(userId));
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<String> clearCart(@PathVariable Integer userId) {
        cartService.clearCart(userId);
        return ResponseEntity.ok("Cart emptied");
    }

    @PostMapping("/{userId}/checkout")
    public ResponseEntity<Map<String, Object>> checkout(@PathVariable Integer userId) {
        return ResponseEntity.ok(cartService.checkout(userId));
    }
}