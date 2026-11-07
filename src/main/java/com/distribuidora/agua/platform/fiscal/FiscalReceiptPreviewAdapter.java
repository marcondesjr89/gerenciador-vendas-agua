package com.distribuidora.agua.platform.fiscal;

import com.distribuidora.agua.platform.feature.FeatureFlagKey;
import com.distribuidora.agua.platform.feature.FeatureFlagService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Adapter implementing FiscalReceiptPort for preview generation in Phase 1.
 * Controlled by the runtime feature flag 'feature.fiscal-receipt-preview'.
 */
@Component
public class FiscalReceiptPreviewAdapter implements FiscalReceiptPort {

    private static final Logger log = LoggerFactory.getLogger(FiscalReceiptPreviewAdapter.class);

    public static final String STORE_NAME = "DISTRIBUIDORA DE AGUA BOA ESPERANCA LTDA";
    public static final String STORE_CNPJ = "12.345.678/0001-90";
    public static final String STORE_ADDRESS = "AV. DAS AGUAS, 1000 - CENTRO";

    private final FeatureFlagService featureFlagService;

    public FiscalReceiptPreviewAdapter(FeatureFlagService featureFlagService) {
        this.featureFlagService = featureFlagService;
    }

    @Override
    public Optional<FiscalReceipt> generateReceipt(FiscalReceiptRequest request) {
        if (!featureFlagService.isEnabled(FeatureFlagKey.FISCAL_RECEIPT_PREVIEW)) {
            log.debug("Fiscal receipt preview is disabled via feature flag; skipping generation for order [{}]",
                    request.orderId());
            return Optional.empty();
        }

        Instant now = Instant.now();
        String receiptNumber = "REC-" + now.toEpochMilli();
        String verificationCode = "NFCe-MOCK-" + UUID.randomUUID().toString().replace("-", "")
                .substring(0, 16).toUpperCase();

        String formattedText = buildFormattedReceiptText(request, receiptNumber, verificationCode, now);

        FiscalReceipt receipt = new FiscalReceipt(
                receiptNumber,
                STORE_NAME,
                STORE_CNPJ,
                STORE_ADDRESS,
                request.orderId(),
                request.orderCode(),
                now,
                request.items(),
                request.subtotal(),
                request.deliveryFee(),
                request.discount(),
                request.totalAmount(),
                request.paymentMethod(),
                verificationCode,
                "PREVIEW_GENERATED",
                formattedText
        );

        log.info("Generated fiscal receipt preview [{}] for order [{}]", receiptNumber, request.orderCode());
        return Optional.of(receipt);
    }

    private String buildFormattedReceiptText(
            FiscalReceiptRequest request,
            String receiptNumber,
            String verificationCode,
            Instant issuedAt
    ) {
        StringBuilder sb = new StringBuilder();
        sb.append("================================================\n");
        sb.append(String.format("   %s\n", STORE_NAME));
        sb.append(String.format("         CNPJ: %s\n", STORE_CNPJ));
        sb.append(String.format("        %s\n", STORE_ADDRESS));
        sb.append("================================================\n");
        sb.append("DOCUMENTO AUXILIAR DE VENDA - PREVIA FISCAL\n");
        sb.append("NAO POSSUI VALOR FISCAL (EM HOMOLOGACAO / TESTE)\n");
        sb.append("------------------------------------------------\n");
        sb.append(String.format("RECIBO N:  %s\n", receiptNumber));
        sb.append(String.format("PEDIDO:    %s\n", request.orderCode()));
        sb.append(String.format("EMISSAO:   %s\n", issuedAt));
        if (request.customerName() != null || request.customerPhone() != null) {
            String name = request.customerName() != null ? request.customerName() : "Consumidor";
            String phone = request.customerPhone() != null ? " (" + request.customerPhone() + ")" : "";
            sb.append(String.format("CLIENTE:   %s%s\n", name, phone));
        }
        sb.append("------------------------------------------------\n");
        sb.append(String.format("%-14s %-16s %3s %8s %8s\n", "COD", "DESCRICAO", "QTD", "UNIT", "TOTAL"));
        sb.append("------------------------------------------------\n");

        if (request.items() != null) {
            for (FiscalReceiptItem item : request.items()) {
                String desc = item.description().length() > 16
                        ? item.description().substring(0, 16)
                        : item.description();
                sb.append(String.format("%-14s %-16s %3d %8.2f %8.2f\n",
                        item.sku(), desc, item.quantity(), item.unitPrice(), item.totalPrice()));
            }
        }

        sb.append("------------------------------------------------\n");
        sb.append(String.format("SUBTOTAL:                                 R$ %8.2f\n", request.subtotal()));
        sb.append(String.format("FRETE:                                    R$ %8.2f\n", request.deliveryFee()));
        sb.append(String.format("DESCONTO:                                 R$ %8.2f\n", request.discount()));
        sb.append(String.format("VALOR TOTAL:                              R$ %8.2f\n", request.totalAmount()));
        sb.append(String.format("FORMA DE PAGAMENTO:                       %s\n", request.paymentMethod()));
        sb.append("------------------------------------------------\n");
        sb.append("CHAVE DE AUTENTICACAO:\n");
        sb.append(String.format("%s\n", verificationCode));
        sb.append("------------------------------------------------\n");
        sb.append("Tributos Totais Incidentes (Lei 12.741/2012): ~18.25%\n");
        sb.append("================================================\n");

        return sb.toString();
    }
}
