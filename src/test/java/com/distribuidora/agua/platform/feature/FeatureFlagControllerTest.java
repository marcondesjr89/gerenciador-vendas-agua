package com.distribuidora.agua.platform.feature;

import com.distribuidora.agua.platform.api.ApiExceptionHandler;
import com.distribuidora.agua.platform.api.ResourceNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FeatureFlagController.class)
@Import(ApiExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FeatureFlagController MockMvc Tests")
class FeatureFlagControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private FeatureFlagService featureFlagService;

    @Test
    @DisplayName("GET /api/v1/platform/features should return list of all feature flags")
    void shouldReturnAllFeatureFlags() throws Exception {
        FeatureFlagState flag1 = new FeatureFlagState(
                "feature.fiscal-receipt-preview",
                "fiscal-receipt-preview",
                true,
                "Enables fiscal receipt preview",
                Instant.now()
        );
        FeatureFlagState flag2 = new FeatureFlagState(
                "feature.free-delivery-promo",
                "free-delivery-promo",
                false,
                "Enables free delivery promo",
                Instant.now()
        );

        when(featureFlagService.listAll()).thenReturn(List.of(flag1, flag2));

        mockMvc.perform(get("/api/v1/platform/features"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].key").value("feature.fiscal-receipt-preview"))
                .andExpect(jsonPath("$[0].enabled").value(true))
                .andExpect(jsonPath("$[1].key").value("feature.free-delivery-promo"))
                .andExpect(jsonPath("$[1].enabled").value(false));
    }

    @Test
    @DisplayName("GET /api/v1/platform/features/{key} should return single flag detail")
    void shouldReturnSingleFeatureFlag() throws Exception {
        FeatureFlagState flag = new FeatureFlagState(
                "feature.fiscal-receipt-preview",
                "fiscal-receipt-preview",
                true,
                "Enables fiscal receipt preview",
                Instant.now()
        );

        when(featureFlagService.getFlag("fiscal-receipt-preview")).thenReturn(flag);

        mockMvc.perform(get("/api/v1/platform/features/fiscal-receipt-preview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.key").value("feature.fiscal-receipt-preview"))
                .andExpect(jsonPath("$.alias").value("fiscal-receipt-preview"))
                .andExpect(jsonPath("$.enabled").value(true));
    }

    @Test
    @DisplayName("GET /api/v1/platform/features/{key} with unknown key should return 404 RFC 7807")
    void shouldReturn404WhenFlagNotFound() throws Exception {
        when(featureFlagService.getFlag("unknown-key"))
                .thenThrow(new ResourceNotFoundException("Feature flag not found: unknown-key"));

        mockMvc.perform(get("/api/v1/platform/features/unknown-key"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Recurso Nao Encontrado"))
                .andExpect(jsonPath("$.detail").value("Feature flag not found: unknown-key"));
    }

    @Test
    @DisplayName("PATCH /api/v1/platform/features/{key} should update flag and return 200 OK")
    void shouldUpdateFeatureFlagSuccessfully() throws Exception {
        UpdateFeatureFlagRequest request = new UpdateFeatureFlagRequest(true);
        FeatureFlagState updated = new FeatureFlagState(
                "feature.free-delivery-promo",
                "free-delivery-promo",
                true,
                "Enables free delivery promo",
                Instant.now()
        );

        when(featureFlagService.updateFlag(eq("free-delivery-promo"), eq(true))).thenReturn(updated);

        mockMvc.perform(patch("/api/v1/platform/features/free-delivery-promo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alias").value("free-delivery-promo"))
                .andExpect(jsonPath("$.enabled").value(true));
    }

    @Test
    @DisplayName("PATCH /api/v1/platform/features/{key} with null enabled should return 400 Bad Request")
    void shouldReturn400WhenPayloadInvalid() throws Exception {
        String invalidPayload = "{\"enabled\": null}";

        mockMvc.perform(patch("/api/v1/platform/features/free-delivery-promo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Dados de requisicao invalidos"));
    }
}
