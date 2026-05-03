package com.bigbites.productService.service;

import com.bigbites.productService.dto.ProductRequest;
import com.bigbites.productService.entity.Product;
import com.bigbites.productService.kafka.KafkaTopics;
import com.bigbites.productService.kafka.LowStockEvent;
import com.bigbites.productService.kafka.StockDeductedEvent;
import com.bigbites.productService.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final int LOW_STOCK_THRESHOLD = 5;

    public Product createProduct(ProductRequest request) {
        Product product = Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .stock(request.getStock())
                .createdAt(LocalDateTime.now())
                .build();
        return productRepository.save(product);
    }

    public List<Product> createBulkProducts(List<ProductRequest> requests) {
        List<Product> products = requests.stream()
                .map(r -> Product.builder()
                        .name(r.getName())
                        .description(r.getDescription())
                        .price(r.getPrice())
                        .stock(r.getStock())
                        .createdAt(LocalDateTime.now())
                        .build())
                .toList();
        return productRepository.saveAll(products);
    }

    public Product getProductById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found: " + id));
    }

    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    public List<Product> searchProducts(String keyword) {
        return productRepository.findByNameContainingIgnoreCase(keyword);
    }

    public List<Product> getAvailableProducts() {
        return productRepository.findByStockGreaterThan(0);
    }

    public List<Product> getLowStockProducts(int threshold) {
        return productRepository.findLowStockProducts(threshold);
    }

    @Transactional
    public Product updateProduct(Long id, ProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found: " + id));
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStock(request.getStock());
        return productRepository.save(product);
    }

    @Transactional
    public Product adjustStock(Long id, int quantity) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found: " + id));

        if (product.getStock() < quantity) {
            throw new RuntimeException("Insufficient stock for product: " + id);
        }

        product.setStock(product.getStock() - quantity);
        productRepository.save(product);

        kafkaTemplate.send(KafkaTopics.STOCK_DEDUCTED, StockDeductedEvent.builder()
                .productId(product.getId())
                .productName(product.getName())
                .quantityDeducted(quantity)
                .remainingStock(product.getStock())
                .build());

        if (product.getStock() <= LOW_STOCK_THRESHOLD) {
            kafkaTemplate.send(KafkaTopics.LOW_STOCK_ALERT, LowStockEvent.builder()
                    .productId(product.getId())
                    .productName(product.getName())
                    .currentStock(product.getStock())
                    .threshold(LOW_STOCK_THRESHOLD)
                    .build());
            log.warn("Published LOW_STOCK_ALERT for product={} stock={}", product.getName(), product.getStock());
        }

        log.info("Published STOCK_DEDUCTED for productId={} qty={}", id, quantity);
        return product;
    }

    public void deleteProduct(Long id) {
        productRepository.deleteById(id);
    }
}

