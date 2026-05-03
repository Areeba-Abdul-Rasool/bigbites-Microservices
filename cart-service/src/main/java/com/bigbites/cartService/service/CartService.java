package com.bigbites.cartService.service;

import com.bigbites.cartService.client.OrderFeignClient;
import com.bigbites.cartService.client.ProductFeignClient;
import com.bigbites.cartService.entity.Cart;
import com.bigbites.cartService.entity.CartItem;
import com.bigbites.cartService.exceptions.CartNotFoundException;
import com.bigbites.cartService.repository.CartRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final ProductFeignClient productFeignClient;
    private final OrderFeignClient orderFeignClient;

    public Cart addToCart(Integer userId, Map<String, Object> request) {
        Long productId = Long.valueOf(request.get("productId").toString());
        Integer quantity = Integer.valueOf(request.get("quantity").toString());

        Map<String, Object> response = productFeignClient.getProductById(productId);
        Map<String, Object> product = (Map<String, Object>) response.get("data");

        Double price = Double.valueOf(product.get("price").toString());
        Integer stock = Integer.valueOf(product.get("stock").toString());

        if (stock < quantity) {
            throw new RuntimeException("Not enough stock for productId: " + productId);
        }

        Cart cart = cartRepository.findByUserId(userId)
                .orElseGet(() -> {
                    Cart newCart = new Cart();
                    newCart.setUserId(userId);
                    return newCart;
                });

        CartItem existingItem = cart.getItems().stream()
                .filter(i -> i.getProductId().equals(productId))
                .findFirst()
                .orElse(null);

        if (existingItem != null) {
            existingItem.setQuantity(existingItem.getQuantity() + quantity);
        } else {
            CartItem item = new CartItem();
            item.setProductId(productId);
            item.setQuantity(quantity);
            item.setPrice(price);
            item.setCart(cart);
            cart.getItems().add(item);
        }

        return cartRepository.save(cart);
    }

    public Cart getCart(Integer userId) {
        return cartRepository.findByUserId(userId)
                .orElseThrow(() -> new CartNotFoundException("Cart not found for user: " + userId));
    }

    public Map<String, Object> checkout(Integer userId) {
        Cart cart = getCart(userId);

        if (cart.getItems().isEmpty()) {
            throw new RuntimeException("Cart is empty");
        }

        List<Map<String, Object>> items = cart.getItems().stream()
                .map(item -> {
                    Map<String, Object> map = new HashMap<>(); // FIX: HashMap not Map.of()
                    map.put("productId", item.getProductId());
                    map.put("quantity", item.getQuantity());
                    return map;
                })
                .toList();

        Map<String, Object> orderRequest = new HashMap<>(); // FIX: HashMap not Map.of()
        orderRequest.put("userId", userId);
        orderRequest.put("items", items);

        Map<String, Object> order = orderFeignClient.placeOrder(orderRequest);

        clearCart(userId);
        return order;
    }

    public void clearCart(Integer userId) {
        Cart cart = getCart(userId);
        cart.getItems().clear();
        cartRepository.save(cart);
    }
}
