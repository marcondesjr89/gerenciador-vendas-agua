package com.distribuidora.agua.catalog.api;

import com.distribuidora.agua.catalog.application.ProductCatalogService;
import com.distribuidora.agua.catalog.domain.ProductType;
import com.distribuidora.agua.platform.api.ApiExceptionHandler;
import com.distribuidora.agua.platform.api.ResourceNotFoundException;
import com.distribuidora.agua.platform.config.SecurityConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CatalogController.class)
@Import({SecurityConfig.class, ApiExceptionHandler.class})
class CatalogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProductCatalogService catalogService;

    @Test
    @DisplayName("Should return 200 OK and list of active products")
    void shouldListActiveProducts() throws Exception {
        ProductResponse response = new ProductResponse(
                1L,
                "GALLON-20L",
                "Mineral Water Gallon 20L",
                new BigDecimal("20.00"),
                true,
                ProductType.FULL_WATER,
                new BigDecimal("30.00"),
                50,
                20,
                true,
                0L
        );

        when(catalogService.listActiveProducts()).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/catalog/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].sku").value("GALLON-20L"))
                .andExpect(jsonPath("$[0].name").value("Mineral Water Gallon 20L"))
                .andExpect(jsonPath("$[0].productType").value("FULL_WATER"));
    }

    @Test
    @DisplayName("Should return 200 OK when product is found by ID")
    void shouldGetProductById() throws Exception {
        ProductResponse response = new ProductResponse(
                1L,
                "GALLON-20L",
                "Mineral Water Gallon 20L",
                new BigDecimal("20.00"),
                true,
                ProductType.FULL_WATER,
                new BigDecimal("30.00"),
                50,
                20,
                true,
                0L
        );

        when(catalogService.getProductById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/catalog/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.sku").value("GALLON-20L"));
    }

    @Test
    @DisplayName("Should return 404 Not Found when product ID does not exist")
    void shouldReturn404WhenNotFound() throws Exception {
        when(catalogService.getProductById(999L))
                .thenThrow(new ResourceNotFoundException("Product not found with ID: 999"));

        mockMvc.perform(get("/api/v1/catalog/products/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.title").value("Recurso Nao Encontrado"))
                .andExpect(jsonPath("$.detail").value("Product not found with ID: 999"));
    }

    @Test
    @DisplayName("Should return 201 Created and Location header on valid product creation")
    void shouldCreateProduct() throws Exception {
        ProductRequest request = new ProductRequest(
                "GALLON-10L",
                "Water Gallon 10L",
                new BigDecimal("10.00"),
                true,
                ProductType.REFILL,
                new BigDecimal("18.00"),
                30,
                15
        );

        ProductResponse response = new ProductResponse(
                2L,
                "GALLON-10L",
                "Water Gallon 10L",
                new BigDecimal("10.00"),
                true,
                ProductType.REFILL,
                new BigDecimal("18.00"),
                30,
                15,
                true,
                0L
        );

        when(catalogService.createProduct(any(ProductRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/catalog/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/v1/catalog/products/2"))
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.sku").value("GALLON-10L"));
    }

    @Test
    @DisplayName("Should return 400 Bad Request RFC 7807 when payload fails validation")
    void shouldReturn400OnValidationFailure() throws Exception {
        String invalidJson = """
                {
                    "sku": "",
                    "name": "",
                    "capacityLiters": -5.0,
                    "returnable": true,
                    "productType": null,
                    "unitPrice": -10.0,
                    "stockFull": -1,
                    "stockEmpty": -2
                }
                """;

        mockMvc.perform(post("/api/v1/catalog/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("Dados de requisicao invalidos"))
                .andExpect(jsonPath("$.invalidParams").isArray())
                .andExpect(jsonPath("$.invalidParams[?(@.field == 'sku')]").exists())
                .andExpect(jsonPath("$.invalidParams[?(@.field == 'name')]").exists())
                .andExpect(jsonPath("$.invalidParams[?(@.field == 'unitPrice')]").exists());
    }

    @Test
    @DisplayName("Should return 200 OK on valid stock adjustment")
    void shouldAdjustStock() throws Exception {
        StockAdjustmentRequest request = new StockAdjustmentRequest(60, 25);
        ProductResponse response = new ProductResponse(
                1L,
                "GALLON-20L",
                "Mineral Water Gallon 20L",
                new BigDecimal("20.00"),
                true,
                ProductType.FULL_WATER,
                new BigDecimal("30.00"),
                60,
                25,
                true,
                0L
        );

        when(catalogService.adjustStock(eq(1L), any(StockAdjustmentRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/catalog/products/1/stock-adjustments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stockFull").value(60))
                .andExpect(jsonPath("$.stockEmpty").value(25));
    }

    @Test
    @DisplayName("Should return 400 Bad Request when stock adjustment has negative values")
    void shouldRejectNegativeStockAdjustment() throws Exception {
        String invalidAdjustment = """
                {
                    "stockFull": -5,
                    "stockEmpty": 10
                }
                """;

        mockMvc.perform(post("/api/v1/catalog/products/1/stock-adjustments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidAdjustment))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.invalidParams[?(@.field == 'stockFull')]").exists());
    }
}
