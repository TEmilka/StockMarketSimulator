package org.example.stockmarketsimulator.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.example.stockmarketsimulator.exception.GlobalExceptionHandler;
import org.example.stockmarketsimulator.model.Asset;
import org.example.stockmarketsimulator.repository.AssetsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
public class AssetsControllerTests {

    private MockMvc mockMvc;

    @Mock
    private AssetsRepository assetsRepository;

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
        when(assetsRepository.findAll()).thenReturn(Collections.singletonList(asset));

        // When & Then
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/assets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].symbol").value("AAPL"))
                .andExpect(jsonPath("$[0].name").value("Apple Inc."))
                .andExpect(jsonPath("$[0].price").value(150.0));
    }

    @Test
    void addAsset_shouldReturnCreatedAsset() throws Exception {

        // Given
        Asset asset = new Asset("AAPL", 150.0, "Apple Inc.");
        Asset savedAsset = new Asset(1L, "AAPL", 150.0, "Apple Inc.");

        // When & Then
        when(assetsRepository.save(any(Asset.class))).thenReturn(savedAsset);
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/api/assets")
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
        Asset asset = new Asset("AAPL", 150.0, "Apple Inc.");
        when(assetsRepository.findById(assetId)).thenReturn(Optional.of(asset));

        // When & Then
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .delete("/api/assets/{id}", assetId))
                .andExpect(status().isNoContent());

        verify(assetsRepository, times(1)).deleteById(assetId);
    }

    @Test
    void deleteAsset_shouldReturnNotFound() throws Exception {

        // Given
        Long assetId = 1L;
        when(assetsRepository.findById(assetId)).thenReturn(Optional.empty());
        // When & Then
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .delete("/api/assets/{id}", assetId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Aktywo o ID 1 nie zostało znalezione."))
                .andExpect(jsonPath("$.status").value(404));

        // Verify that the deleteById method was not called
        verify(assetsRepository, never()).deleteById(assetId);
    }
}
