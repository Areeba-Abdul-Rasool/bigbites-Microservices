package com.bigbites.productService.controller;

import com.bigbites.productService.dto.ApiResponse;
import com.bigbites.productService.dto.ProductRequest;
import com.bigbites.productService.entity.Product;
import com.bigbites.productService.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @PostMapping
    public ResponseEntity<ApiResponse<Product>> create(@RequestBody ProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Product created", productService.createProduct(request)));
    }

    @PostMapping("/bulk")
    public ResponseEntity<ApiResponse<List<Product>>> createBulk(@RequestBody List<ProductRequest> requests) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Products created", productService.createBulkProducts(requests)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Product>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Product fetched", productService.getProductById(id)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Product>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success("All products", productService.getAllProducts()));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<Product>>> search(@RequestParam String keyword) {
        return ResponseEntity.ok(ApiResponse.success("Search results", productService.searchProducts(keyword)));
    }

    @GetMapping("/available")
    public ResponseEntity<ApiResponse<List<Product>>> getAvailable() {
        return ResponseEntity.ok(ApiResponse.success("Available products", productService.getAvailableProducts()));
    }

    @GetMapping("/low-stock")
    public ResponseEntity<ApiResponse<List<Product>>> getLowStock(
            @RequestParam(defaultValue = "5") int threshold) {
        return ResponseEntity.ok(ApiResponse.success("Low stock products", productService.getLowStockProducts(threshold)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Product>> update(@PathVariable Long id, @RequestBody ProductRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Product updated", productService.updateProduct(id, request)));
    }

    @PostMapping("/{id}/stock")
    public ResponseEntity<ApiResponse<Product>> adjustStock(@PathVariable Long id,
                                                            @RequestBody Map<String, Object> request) {
        int quantity = Integer.parseInt(request.get("quantity").toString());
        return ResponseEntity.ok(ApiResponse.success("Stock adjusted", productService.adjustStock(id, quantity)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.ok(ApiResponse.success("Product deleted", null));
    }
}