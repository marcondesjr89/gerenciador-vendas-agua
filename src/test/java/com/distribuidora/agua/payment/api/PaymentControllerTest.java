package com.distribuidora.agua.payment.api;

import com.distribuidora.agua.orders.domain.Modality;
import com.distribuidora.agua.orders.domain.Order;
import com.distribuidora.agua.orders.domain.OrderStatus;
import com.distribuidora.agua.orders.domain.PaymentMethod;
import com.distribuidora.agua.payment.application.PaymentProcessorService;
import com.distribuidora.agua.payment.application.ProcessPaymentCommand;
import com.distribuidora.agua.payment.domain.Payment;
import com.distribuidora.agua.payment.domain.PaymentStatus;
import com.distribuidora.agua.platform.api.ApiExceptionHandler;
import com.distribuidora.agua.platform.api.ResourceNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PaymentController.class)
@Import(ApiExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("PaymentController WebMvc Tests")
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PaymentProcessorService paymentProcessorService;

    private Order order;
    private Payment samplePayment;

    @BeforeEach
    void setUp() {
        order = Order.builder()
                .id(1L)
                .orderCode("ORD-20261107-100001")
                .customerName("Paulo Lima")
                .customerPhone("11999998888")
                .modality(Modality.PICKUP)
                .paymentMethod(PaymentMethod.PIX)
                .totalAmount(new BigDecimal("35.00"))
                .status(OrderStatus.PENDING)
                .build();

        samplePayment = new Payment(
                50L,
                order,
                "idemp-key-test-1",
                PaymentMethod.PIX,
                new BigDecimal("35.00"),
                PaymentStatus.PAID,
                Instant.now()
        );
    }

    @Test
    @DisplayName("POST /api/v1/payments with valid Idempotency-Key should return 201 Created")
    void shouldProcessPaymentAndReturn201Created() throws Exception {
        ProcessPaymentRequest request = new ProcessPaymentRequest(
                1L,
                PaymentMethod.PIX,
                new BigDecimal("35.00")
        );

        when(paymentProcessorService.processPayment(eq("idemp-key-test-1"), any(ProcessPaymentCommand.class)))
                .thenReturn(samplePayment);
        when(paymentProcessorService.isReplay("idemp-key-test-1")).thenReturn(false);

        mockMvc.perform(post("/api/v1/payments")
                        .header("Idempotency-Key", "idemp-key-test-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/payments/50"))
                .andExpect(jsonPath("$.id").value(50))
                .andExpect(jsonPath("$.orderId").value(1))
                .andExpect(jsonPath("$.idempotencyKey").value("idemp-key-test-1"))
                .andExpect(jsonPath("$.paymentMethod").value("PIX"))
                .andExpect(jsonPath("$.paidAmount").value(35.00))
                .andExpect(jsonPath("$.status").value("PAID"));
    }

    @Test
    @DisplayName("POST /api/v1/payments when idempotency replay occurs should return 200 OK")
    void shouldReturn200OkOnIdempotentReplay() throws Exception {
        ProcessPaymentRequest request = new ProcessPaymentRequest(
                1L,
                PaymentMethod.PIX,
                new BigDecimal("35.00")
        );

        when(paymentProcessorService.processPayment(eq("idemp-key-test-1"), any(ProcessPaymentCommand.class)))
                .thenReturn(samplePayment);
        when(paymentProcessorService.isReplay("idemp-key-test-1")).thenReturn(true);

        mockMvc.perform(post("/api/v1/payments")
                        .header("Idempotency-Key", "idemp-key-test-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(50))
                .andExpect(jsonPath("$.idempotencyKey").value("idemp-key-test-1"));
    }

    @Test
    @DisplayName("POST /api/v1/payments without Idempotency-Key header should return 400 Bad Request RFC 7807")
    void shouldReturn400WhenIdempotencyKeyHeaderMissing() throws Exception {
        ProcessPaymentRequest request = new ProcessPaymentRequest(
                1L,
                PaymentMethod.PIX,
                new BigDecimal("35.00")
        );

        mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Cabecalho Obrigatorio Ausente"));
    }

    @Test
    @DisplayName("GET /api/v1/payments/{id} should return payment details")
    void shouldReturnPaymentById() throws Exception {
        when(paymentProcessorService.findById(50L)).thenReturn(samplePayment);

        mockMvc.perform(get("/api/v1/payments/50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(50))
                .andExpect(jsonPath("$.idempotencyKey").value("idemp-key-test-1"));
    }

    @Test
    @DisplayName("GET /api/v1/payments/{id} should return 404 when payment not found")
    void shouldReturn404WhenPaymentNotFound() throws Exception {
        when(paymentProcessorService.findById(999L))
                .thenThrow(new ResourceNotFoundException("Pagamento nao encontrado com id: 999"));

        mockMvc.perform(get("/api/v1/payments/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Recurso Nao Encontrado"));
    }

    @Test
    @DisplayName("GET /api/v1/payments/by-order/{orderId} should return list of payments")
    void shouldReturnPaymentsByOrderId() throws Exception {
        when(paymentProcessorService.findByOrderId(1L)).thenReturn(List.of(samplePayment));

        mockMvc.perform(get("/api/v1/payments/by-order/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(50));
    }
}
