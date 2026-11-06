package com.distribuidora.agua.catalog.application;

import com.distribuidora.agua.catalog.api.ProductRequest;
import com.distribuidora.agua.catalog.api.ProductResponse;
import com.distribuidora.agua.catalog.api.StockAdjustmentRequest;
import com.distribuidora.agua.catalog.domain.Product;
import com.distribuidora.agua.catalog.infrastructure.ProductRepository;
import com.distribuidora.agua.platform.api.ResourceNotFoundException;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProductCatalogService {

    private final ProductRepository productRepository;
    private final StockMovementService stockMovementService;

    public ProductCatalogService(ProductRepository productRepository, StockMovementService stockMovementService) {
        this.productRepository = productRepository;
        this.stockMovementService = stockMovementService;
    }

    @Cacheable(value = "catalog-products", unless = "#result.isEmpty()")
    @Transactional(readOnly = true)
    public List<ProductResponse> listActiveProducts() {
        return productRepository.findByActiveTrue()
                .stream()
                .map(ProductResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProductResponse getProductById(Long id) {
        return productRepository.findById(id)
                .map(ProductResponse::fromEntity)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + id));
    }

    @CacheEvict(value = "catalog-products", allEntries = true)
    @Transactional
    public ProductResponse createProduct(ProductRequest request) {
        if (productRepository.findBySku(request.sku()).isPresent()) {
            throw new IllegalArgumentException("Product with SKU already exists: " + request.sku());
        }

        Product product = Product.builder()
                .sku(request.sku())
                .name(request.name())
                .capacityLiters(request.capacityLiters())
                .returnable(request.returnable())
                .productType(request.productType())
                .unitPrice(request.unitPrice())
                .stockFull(request.stockFull())
                .stockEmpty(request.stockEmpty())
                .active(true)
                .build();

        Product saved = productRepository.save(product);
        return ProductResponse.fromEntity(saved);
    }

    @CacheEvict(value = "catalog-products", allEntries = true)
    @Transactional
    public ProductResponse adjustStock(Long productId, StockAdjustmentRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + productId));

        stockMovementService.adjustStock(product, request.stockFull(), request.stockEmpty());
        return ProductResponse.fromEntity(product);
    }
}
