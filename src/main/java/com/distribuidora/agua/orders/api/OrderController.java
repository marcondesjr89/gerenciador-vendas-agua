package com.distribuidora.agua.orders.api;

import com.distribuidora.agua.orders.application.OrderService;
import com.distribuidora.agua.orders.domain.Order;
import com.distribuidora.agua.orders.domain.OrderStatus;
import com.distribuidora.agua.platform.fiscal.FiscalReceipt;
import com.distribuidora.agua.platform.fiscal.FiscalReceiptItem;
import com.distribuidora.agua.platform.fiscal.FiscalReceiptPort;
import com.distribuidora.agua.platform.fiscal.FiscalReceiptRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * REST controller for Order lifecycle management, status transitions and fiscal receipt generation.
 */
@RestController
@RequestMapping("/api/v1/orders")
@Tag(name = "Orders", description = "Endpoints para gestao do ciclo de vida de pedidos, frete e comprovante fiscal")
public class OrderController {

    private final OrderService orderService;
    private final FiscalReceiptPort fiscalReceiptPort;

    public OrderController(OrderService orderService, FiscalReceiptPort fiscalReceiptPort) {
        this.orderService = orderService;
        this.fiscalReceiptPort = fiscalReceiptPort;
    }

    @PostMapping
    @Operation(summary = "Cria um novo pedido com baixa de estoque atomica, calculo de frete e previa de cupom fiscal")
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        Order order = orderService.createOrder(request.toCommand());
        Optional<FiscalReceipt> receipt = generateFiscalReceiptIfEnabled(order);

        OrderResponse response = OrderResponse.fromDomain(order, receipt.orElse(null));
        URI location = URI.create("/api/v1/orders/" + order.getId());
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    @Operation(summary = "Consulta paginada de pedidos com filtros opcionais por status e telefone")
    public ResponseEntity<Page<OrderResponse>> listOrders(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) String customerPhone,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<Order> orderPage = orderService.findOrders(status, customerPhone, pageable);
        return ResponseEntity.ok(orderPage.map(OrderResponse::fromDomain));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Recupera os detalhes completos de um pedido por ID com itens e recibo fiscal")
    public ResponseEntity<OrderResponse> getById(@PathVariable Long id) {
        Order order = orderService.findById(id);
        Optional<FiscalReceipt> receipt = generateFiscalReceiptIfEnabled(order);
        return ResponseEntity.ok(OrderResponse.fromDomain(order, receipt.orElse(null)));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Avanca o status do pedido ou cancela com estorno automatico de galoes")
    public ResponseEntity<OrderResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateOrderStatusRequest request
    ) {
        Order updated;
        if (request.status() == OrderStatus.CANCELLED) {
            String reason = request.cancellationReason() != null && !request.cancellationReason().isBlank()
                    ? request.cancellationReason().trim()
                    : "Cancelamento solicitado via API";
            updated = orderService.cancelOrder(id, reason);
        } else {
            updated = orderService.updateStatus(id, request.status());
        }

        Optional<FiscalReceipt> receipt = generateFiscalReceiptIfEnabled(updated);
        return ResponseEntity.ok(OrderResponse.fromDomain(updated, receipt.orElse(null)));
    }

    private Optional<FiscalReceipt> generateFiscalReceiptIfEnabled(Order order) {
        List<FiscalReceiptItem> receiptItems = order.getItems() != null
                ? order.getItems().stream()
                .map(i -> new FiscalReceiptItem(
                        i.getProduct().getSku(),
                        i.getProduct().getName(),
                        i.getQuantity(),
                        i.getUnitPrice(),
                        i.getTotalItemAmount()
                ))
                .toList()
                : List.of();

        UUID orderUuid = UUID.nameUUIDFromBytes(("order-" + order.getId()).getBytes(StandardCharsets.UTF_8));

        FiscalReceiptRequest fiscalRequest = new FiscalReceiptRequest(
                orderUuid,
                order.getOrderCode(),
                order.getCustomerName(),
                order.getCustomerPhone(),
                receiptItems,
                order.getSubtotal(),
                order.getDeliveryFee(),
                BigDecimal.ZERO,
                order.getTotalAmount(),
                order.getPaymentMethod().name()
        );

        return fiscalReceiptPort.generateReceipt(fiscalRequest);
    }
}
