package com.distribuidora.agua.payment.api;

import com.distribuidora.agua.payment.application.PaymentProcessorService;
import com.distribuidora.agua.payment.domain.Payment;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

/**
 * REST controller for idempotent payment processing requiring the Idempotency-Key header.
 */
@RestController
@RequestMapping("/api/v1/payments")
@Tag(name = "Payments", description = "Endpoints para processamento de pagamentos com garantia de idempotencia")
public class PaymentController {

    private final PaymentProcessorService paymentProcessorService;

    public PaymentController(PaymentProcessorService paymentProcessorService) {
        this.paymentProcessorService = paymentProcessorService;
    }

    @PostMapping
    @Operation(summary = "Processa o pagamento de um pedido com idempotencia obrigatoria via cabecalho Idempotency-Key")
    public ResponseEntity<PaymentResponse> processPayment(
            @Parameter(name = "Idempotency-Key", in = ParameterIn.HEADER, required = true, description = "Chave unica de idempotencia da transacao")
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody ProcessPaymentRequest request
    ) {
        boolean isReplay = paymentProcessorService.isReplay(idempotencyKey);
        Payment payment = paymentProcessorService.processPayment(idempotencyKey, request.toCommand());

        PaymentResponse response = PaymentResponse.fromDomain(payment);

        if (isReplay) {
            return ResponseEntity.ok(response);
        }

        URI location = URI.create("/api/v1/payments/" + payment.getId());
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulta comprovante de pagamento por ID")
    public ResponseEntity<PaymentResponse> getById(@PathVariable Long id) {
        Payment payment = paymentProcessorService.findById(id);
        return ResponseEntity.ok(PaymentResponse.fromDomain(payment));
    }

    @GetMapping("/by-order/{orderId}")
    @Operation(summary = "Lista todos os pagamentos vinculados a um pedido")
    public ResponseEntity<List<PaymentResponse>> getByOrderId(@PathVariable Long orderId) {
        List<PaymentResponse> payments = paymentProcessorService.findByOrderId(orderId).stream()
                .map(PaymentResponse::fromDomain)
                .toList();
        return ResponseEntity.ok(payments);
    }
}
