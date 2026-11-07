package com.distribuidora.agua.platform.feature;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for inspecting and updating runtime feature flags.
 */
@RestController
@RequestMapping("/api/v1/platform/features")
@Tag(name = "Platform Features", description = "Endpoints para gerenciamento de Feature Flags em tempo de execucao")
public class FeatureFlagController {

    private final FeatureFlagService featureFlagService;

    public FeatureFlagController(FeatureFlagService featureFlagService) {
        this.featureFlagService = featureFlagService;
    }

    @GetMapping
    @Operation(summary = "Lista todas as feature flags do sistema e seus respectivos estados")
    public ResponseEntity<List<FeatureFlagResponse>> listAll() {
        List<FeatureFlagResponse> response = featureFlagService.listAll().stream()
                .map(FeatureFlagResponse::fromState)
                .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{key}")
    @Operation(summary = "Consulta o estado de uma feature flag especifica por chave ou alias")
    public ResponseEntity<FeatureFlagResponse> getByKey(@PathVariable String key) {
        FeatureFlagState state = featureFlagService.getFlag(key);
        return ResponseEntity.ok(FeatureFlagResponse.fromState(state));
    }

    @PatchMapping("/{key}")
    @Operation(summary = "Alterna o estado de uma feature flag em tempo de execucao sem necessidade de restart")
    public ResponseEntity<FeatureFlagResponse> update(
            @PathVariable String key,
            @Valid @RequestBody UpdateFeatureFlagRequest request
    ) {
        FeatureFlagState updated = featureFlagService.updateFlag(key, request.enabled());
        return ResponseEntity.ok(FeatureFlagResponse.fromState(updated));
    }
}
