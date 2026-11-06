package com.distribuidora.agua.catalog.api;

import com.distribuidora.agua.catalog.application.ProductCatalogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/catalog/products")
@Tag(name = "Catalog", description = "Endpoints for managing product catalog, container volumes and dual stock balances")
public class CatalogController {

    private final ProductCatalogService catalogService;

    public CatalogController(ProductCatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping
    @Operation(summary = "List active catalog products", description = "Returns active products in catalog with Redis caching and graceful PostgreSQL fallback")
    @ApiResponse(responseCode = "200", description = "Products successfully retrieved")
    public ResponseEntity<List<ProductResponse>> listActiveProducts() {
        return ResponseEntity.ok(catalogService.listActiveProducts());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get product by ID", description = "Retrieves details of a specific product")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Product found"),
            @ApiResponse(responseCode = "404", description = "Product not found")
    })
    public ResponseEntity<ProductResponse> getProductById(@PathVariable Long id) {
        return ResponseEntity.ok(catalogService.getProductById(id));
    }

    @PostMapping
    @Operation(summary = "Create new catalog product", description = "Creates a new product item and invalidates the catalog cache")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Product successfully created"),
            @ApiResponse(responseCode = "400", description = "Invalid payload or duplicate SKU")
    })
    public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody ProductRequest request) {
        ProductResponse created = catalogService.createProduct(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PostMapping("/{id}/stock-adjustments")
    @Operation(summary = "Adjust stock balances", description = "Updates full units and empty bottles stock for a specific product and invalidates cache")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Stock successfully adjusted"),
            @ApiResponse(responseCode = "400", description = "Negative stock quantities provided"),
            @ApiResponse(responseCode = "404", description = "Product not found")
    })
    public ResponseEntity<ProductResponse> adjustStock(
            @PathVariable Long id,
            @Valid @RequestBody StockAdjustmentRequest request) {
        ProductResponse updated = catalogService.adjustStock(id, request);
        return ResponseEntity.ok(updated);
    }
}
