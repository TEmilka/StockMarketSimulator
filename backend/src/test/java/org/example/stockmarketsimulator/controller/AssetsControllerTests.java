package org.example.stockmarketsimulator.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import org.example.stockmarketsimulator.exception.GlobalExceptionHandler;
import org.example.stockmarketsimulator.exception.ResourceNotFoundException;
import org.example.stockmarketsimulator.model.Asset;
import org.example.stockmarketsimulator.service.AssetService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.*;

/**
 * Pokrycie testami endpointów AssetsController:
 * - GET /api/v1/assets
 * - POST /api/v1/assets
 * - DELETE /api/v1/assets/{id}
 * - GET /api/v1/assets/{id}/history
 */

@ExtendWith(MockitoExtension.class)
public class AssetsControllerTests {

    private MockMvc mockMvc;

    @Mock
    private AssetService assetService;

    @InjectMocks
    private AssetsController assetsController;

    @BeforeEach
    public void setup() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(assetsController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void contextLoads() throws Exception {
        assertThat(assetsController).isNotNull();
    }

    @Test
    void getAssets_shouldReturnAssets() throws Exception {
        // Given
        Asset asset = new Asset("AAPL", 150.0, "Apple Inc.");
        Map<String, Object> response = Map.of("content", Collections.singletonList(asset));
        when(assetService.getAssets(null, null, null, null, null)).thenReturn(response);

        // When & Then
        mockMvc.perform(get("/api/v1/assets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].symbol").value("AAPL"))
                .andExpect(jsonPath("$[0].name").value("Apple Inc."))
                .andExpect(jsonPath("$[0].price").value(150.0));
    }

    @Test
    void addAsset_shouldReturnCreatedAsset() throws Exception {
        // Given
        Asset savedAsset = new Asset(1L, "AAPL", 150.0, "Apple Inc.");
        when(assetService.createAsset(any(Asset.class))).thenReturn(savedAsset);

        // When & Then
        mockMvc.perform(post("/api/v1/assets")
                .contentType("application/json")
                .content("{\"symbol\":\"AAPL\",\"price\":150.0,\"name\":\"Apple Inc.\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.symbol").value("AAPL"))
                .andExpect(jsonPath("$.name").value("Apple Inc."))
                .andExpect(jsonPath("$.price").value(150.0));
    }

    @Test
    void deleteAsset_shouldReturnNoContent() throws Exception {
        // Given
        Long assetId = 1L;
        doNothing().when(assetService).deleteAsset(assetId);

        // When & Then
        mockMvc.perform(delete("/api/v1/assets/{id}", assetId))
                .andExpect(status().isNoContent());

        verify(assetService, times(1)).deleteAsset(assetId);
    }

    @Test
    void deleteAsset_shouldReturnNotFound() throws Exception {
        // Given
        Long assetId = 1L;
        doThrow(new ResourceNotFoundException("Aktywo o ID 1 nie zostało znalezione."))
                .when(assetService).deleteAsset(assetId);

        // When & Then
        mockMvc.perform(delete("/api/v1/assets/{id}", assetId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Aktywo o ID 1 nie zostało znalezione."))
                .andExpect(jsonPath("$.status").value(404));

        verify(assetService, times(1)).deleteAsset(assetId);
    }

    @Test
    void getAssetHistory_shouldReturnHistory() throws Exception {
        // Given
        List<Map<String, Object>> history = List.of(
            Map.of("timestamp", "2024-06-01T12:00:00", "price", 100.0)
        );
        when(assetService.getAssetHistory(1L)).thenReturn(history);

        // When & Then
        mockMvc.perform(get("/api/v1/assets/1/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].price").value(100.0));
    }
}
