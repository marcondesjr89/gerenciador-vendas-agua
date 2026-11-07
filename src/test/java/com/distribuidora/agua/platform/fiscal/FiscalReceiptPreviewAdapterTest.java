package com.distribuidora.agua.platform.fiscal;

import com.distribuidora.agua.platform.feature.FeatureFlagKey;
import com.distribuidora.agua.platform.feature.FeatureFlagService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("FiscalReceiptPreviewAdapter Tests")
class FiscalReceiptPreviewAdapterTest {

    @Mock
    private FeatureFlagService featureFlagService;

    private FiscalReceiptPreviewAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new FiscalReceiptPreviewAdapter(featureFlagService);
    }

    @Test
    @DisplayName("Should return empty optional when fiscal-receipt-preview feature flag is disabled")
    void shouldReturnEmptyWhenFeatureFlagDisabled() {
        when(featureFlagService.isEnabled(FeatureFlagKey.FISCAL_RECEIPT_PREVIEW)).thenReturn(false);

        FiscalReceiptRequest request = createSampleRequest();
        Optional<FiscalReceipt> result = adapter.generateReceipt(request);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should generate structured fiscal receipt when feature flag is enabled")
    void shouldGenerateFiscalReceiptWhenFeatureFlagEnabled() {
        when(featureFlagService.isEnabled(FeatureFlagKey.FISCAL_RECEIPT_PREVIEW)).thenReturn(true);

        FiscalReceiptRequest request = createSampleRequest();
        Optional<FiscalReceipt> result = adapter.generateReceipt(request);

        assertThat(result).isPresent();
        FiscalReceipt receipt = result.get();

        assertThat(receipt.orderId()).isEqualTo(request.orderId());
        assertThat(receipt.orderCode()).isEqualTo("ORD-202611-0001");
        assertThat(receipt.storeName()).contains("DISTRIBUIDORA DE AGUA");
        assertThat(receipt.storeCnpj()).isNotBlank();
        assertThat(receipt.receiptNumber()).startsWith("REC-");
        assertThat(receipt.verificationCode()).startsWith("NFCe-MOCK-");
        assertThat(receipt.items()).hasSize(2);
        assertThat(receipt.subtotal()).isEqualByComparingTo("45.00");
        assertThat(receipt.deliveryFee()).isEqualByComparingTo("5.00");
        assertThat(receipt.totalAmount()).isEqualByComparingTo("50.00");
        assertThat(receipt.paymentMethod()).isEqualTo("PIX");
        assertThat(receipt.status()).isEqualTo("PREVIEW_GENERATED");

        // Verify formatted receipt text layout
        assertThat(receipt.formattedText())
                .contains("DOCUMENTO AUXILIAR DE VENDA - PREVIA FISCAL")
                .contains("ORD-202611-0001")
                .contains("Galao 20L")
                .contains("50.00")
                .contains(receipt.verificationCode())
                .contains("Lei 12.741/2012");
    }

    private FiscalReceiptRequest createSampleRequest() {
        UUID orderId = UUID.randomUUID();
        FiscalReceiptItem item1 = new FiscalReceiptItem(
                "GAL-20L-COMP",
                "Galao 20L Retornavel (Completo)",
                1,
                new BigDecimal("30.00"),
                new BigDecimal("30.00")
        );
        FiscalReceiptItem item2 = new FiscalReceiptItem(
                "GAR-500ML-PACK",
                "Fardo 12x Garrafas 500ml",
                1,
                new BigDecimal("15.00"),
                new BigDecimal("15.00")
        );

        return new FiscalReceiptRequest(
                orderId,
                "ORD-202611-0001",
                "Carlos Eduardo",
                "11987654321",
                List.of(item1, item2),
                new BigDecimal("45.00"),
                new BigDecimal("5.00"),
                BigDecimal.ZERO,
                new BigDecimal("50.00"),
                "PIX"
        );
    }
}
