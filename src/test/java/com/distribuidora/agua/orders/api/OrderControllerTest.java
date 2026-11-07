package com.distribuidora.agua.orders.api;

import com.distribuidora.agua.catalog.domain.Product;
import com.distribuidora.agua.catalog.domain.ProductType;
import com.distribuidora.agua.orders.application.CreateOrderCommand;
import com.distribuidora.agua.orders.application.OrderService;
import com.distribuidora.agua.orders.domain.Modality;
import com.distribuidora.agua.orders.domain.Order;
import com.distribuidora.agua.orders.domain.OrderStatus;
import com.distribuidora.agua.orders.domain.PaymentMethod;
import com.distribuidora.agua.platform.api.ApiExceptionHandler;
import com.distribuidora.agua.platform.api.ResourceNotFoundException;
import com.distribuidora.agua.platform.fiscal.FiscalReceipt;
import com.distribuidora.agua.platform.fiscal.FiscalReceiptPort;
import com.distribuidora.agua.platform.fiscal.FiscalReceiptRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
@Import(ApiExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("OrderController WebMvc Tests")
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OrderService orderService;

    @MockBean
    private FiscalReceiptPort fiscalReceiptPort;

    private Product product;
    private Order sampleOrder;

    @BeforeEach
    void setUp() {
        product = Product.builder()
                .id(1L)
                .sku("GAL-20L-COMP")
                .name("Galao 20L Completo")
                .productType(ProductType.FULL_WATER)
                .unitPrice(new BigDecimal("30.00"))
                .capacityLiters(new BigDecimal("20.00"))
                .stockFull(50)
                .stockEmpty(10)
                .returnable(true)
                .active(true)
                .build();

        sampleOrder = Order.builder()
                .id(100L)
                .orderCode("ORD-20261107-100001")
                .customerName("Mariana Santos")
                .customerPhone("11988887777")
                .modality(Modality.DELIVERY)
                .paymentMethod(PaymentMethod.PIX)
                .deliveryFee(new BigDecimal("5.00"))
                .status(OrderStatus.PENDING)
                .build();
        sampleOrder.addItem(product, 2, false);
        sampleOrder.recalculateTotals();
    }

    @Test
    @DisplayName("POST /api/v1/orders should create order and return 201 Created with Fiscal Receipt preview when enabled")
    void shouldCreateOrderWithFiscalReceiptPreview() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest(
                "Mariana Santos",
                "11988887777",
                Modality.DELIVERY,
                PaymentMethod.PIX,
                List.of(new CreateOrderItemRequest(1L, 2, false))
        );

        FiscalReceipt receipt = new FiscalReceipt(
                "REC-100001",
                "DISTRIBUIDORA DE AGUA BOA ESPERANCA LTDA",
                "12.345.678/0001-90",
                "AV. DAS AGUAS, 1000 - CENTRO",
                UUID.randomUUID(),
                "ORD-20261107-100001",
                Instant.now(),
                List.of(),
                new BigDecimal("60.00"),
                new BigDecimal("5.00"),
                BigDecimal.ZERO,
                new BigDecimal("65.00"),
                "PIX",
                "NFCe-MOCK-A1B2C3D4",
                "PREVIEW_GENERATED",
                "Texto formatado do recibo fiscal"
        );

        when(orderService.createOrder(any(CreateOrderCommand.class))).thenReturn(sampleOrder);
        when(fiscalReceiptPort.generateReceipt(any(FiscalReceiptRequest.class))).thenReturn(Optional.of(receipt));

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/orders/100"))
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.orderCode").value("ORD-20261107-100001"))
                .andExpect(jsonPath("$.customerName").value("Mariana Santos"))
                .andExpect(jsonPath("$.modality").value("DELIVERY"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.subtotal").value(60.00))
                .andExpect(jsonPath("$.deliveryFee").value(5.00))
                .andExpect(jsonPath("$.totalAmount").value(65.00))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].productSku").value("GAL-20L-COMP"))
                .andExpect(jsonPath("$.fiscalReceipt.receiptNumber").value("REC-100001"))
                .andExpect(jsonPath("$.fiscalReceipt.verificationCode").value("NFCe-MOCK-A1B2C3D4"));
    }

    @Test
    @DisplayName("POST /api/v1/orders should create order with fiscalReceipt=null when feature flag is disabled")
    void shouldCreateOrderWithoutFiscalReceiptWhenDisabled() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest(
                "Mariana Santos",
                "11988887777",
                Modality.PICKUP,
                PaymentMethod.CASH,
                List.of(new CreateOrderItemRequest(1L, 1, false))
        );

        when(orderService.createOrder(any(CreateOrderCommand.class))).thenReturn(sampleOrder);
        when(fiscalReceiptPort.generateReceipt(any(FiscalReceiptRequest.class))).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fiscalReceipt").doesNotExist());
    }

    @Test
    @DisplayName("POST /api/v1/orders should return 400 Bad Request when validation fails")
    void shouldReturn400WhenPayloadInvalid() throws Exception {
        CreateOrderRequest invalidRequest = new CreateOrderRequest(
                "", // Blank name
                "", // Blank phone
                null,
                null,
                List.of() // Empty items
        );

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Dados de requisicao invalidos"));
    }

    @Test
    @DisplayName("GET /api/v1/orders should return paginated list of orders")
    void shouldReturnPaginatedOrders() throws Exception {
        when(orderService.findOrders(any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(sampleOrder)));

        mockMvc.perform(get("/api/v1/orders")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].orderCode").value("ORD-20261107-100001"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("GET /api/v1/orders/{id} should return order details")
    void shouldReturnOrderById() throws Exception {
        when(orderService.findById(100L)).thenReturn(sampleOrder);
        when(fiscalReceiptPort.generateReceipt(any())).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/orders/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.orderCode").value("ORD-20261107-100001"));
    }

    @Test
    @DisplayName("GET /api/v1/orders/{id} should return 404 when order does not exist")
    void shouldReturn404WhenOrderNotFound() throws Exception {
        when(orderService.findById(999L))
                .thenThrow(new ResourceNotFoundException("Pedido nao encontrado com id: 999"));

        mockMvc.perform(get("/api/v1/orders/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Recurso Nao Encontrado"));
    }

    @Test
    @DisplayName("PATCH /api/v1/orders/{id}/status should update status to CONFIRMED")
    void shouldUpdateOrderStatusSuccessfully() throws Exception {
        UpdateOrderStatusRequest patchRequest = new UpdateOrderStatusRequest(OrderStatus.CONFIRMED, null);

        Order confirmedOrder = Order.builder()
                .id(100L)
                .orderCode("ORD-20261107-100001")
                .customerName("Mariana Santos")
                .customerPhone("11988887777")
                .modality(Modality.DELIVERY)
                .paymentMethod(PaymentMethod.PIX)
                .deliveryFee(new BigDecimal("5.00"))
                .status(OrderStatus.CONFIRMED)
                .build();
        confirmedOrder.addItem(product, 2, false);
        confirmedOrder.recalculateTotals();

        when(orderService.updateStatus(100L, OrderStatus.CONFIRMED)).thenReturn(confirmedOrder);
        when(fiscalReceiptPort.generateReceipt(any())).thenReturn(Optional.empty());

        mockMvc.perform(patch("/api/v1/orders/100/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(patchRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    @DisplayName("PATCH /api/v1/orders/{id}/status to CANCELLED should invoke cancelOrder with reason")
    void shouldCancelOrderWhenStatusIsCancelled() throws Exception {
        UpdateOrderStatusRequest patchRequest = new UpdateOrderStatusRequest(OrderStatus.CANCELLED, "Cliente desistiu");

        Order cancelledOrder = Order.builder()
                .id(100L)
                .orderCode("ORD-20261107-100001")
                .customerName("Mariana Santos")
                .customerPhone("11988887777")
                .modality(Modality.DELIVERY)
                .paymentMethod(PaymentMethod.PIX)
                .deliveryFee(new BigDecimal("5.00"))
                .status(OrderStatus.CANCELLED)
                .build();
        cancelledOrder.addItem(product, 2, false);
        cancelledOrder.recalculateTotals();

        when(orderService.cancelOrder(100L, "Cliente desistiu")).thenReturn(cancelledOrder);
        when(fiscalReceiptPort.generateReceipt(any())).thenReturn(Optional.empty());

        mockMvc.perform(patch("/api/v1/orders/100/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(patchRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        verify(orderService).cancelOrder(100L, "Cliente desistiu");
    }

    @Test
    @DisplayName("PATCH /api/v1/orders/{id}/status should return 422 when state transition is illegal")
    void shouldReturn422WhenTransitionIsIllegal() throws Exception {
        UpdateOrderStatusRequest patchRequest = new UpdateOrderStatusRequest(OrderStatus.IN_TRANSIT, null);

        when(orderService.updateStatus(100L, OrderStatus.IN_TRANSIT))
                .thenThrow(new IllegalStateException("Orders with modality PICKUP cannot transition to IN_TRANSIT"));

        mockMvc.perform(patch("/api/v1/orders/100/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(patchRequest)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.title").value("Regra de Negocio Violada"))
                .andExpect(jsonPath("$.detail").value("Orders with modality PICKUP cannot transition to IN_TRANSIT"));
    }
}
