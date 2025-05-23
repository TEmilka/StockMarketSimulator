package org.example.stockmarketsimulator.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.example.stockmarketsimulator.exception.BadRequestException;
import org.example.stockmarketsimulator.exception.ResourceNotFoundException;
import org.example.stockmarketsimulator.model.Asset;
import org.example.stockmarketsimulator.model.AssetPriceHistory;
import org.example.stockmarketsimulator.repository.AssetsRepository;
import org.example.stockmarketsimulator.repository.AssetPriceHistoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/assets")
public class AssetsController {

    @Autowired
    private AssetsRepository assetsRepository;

    @Autowired
    private AssetPriceHistoryRepository assetPriceHistoryRepository;

    @Operation(
            summary = "Pobierz wszystkie aktywa",
            description = "Zwraca listę wszystkich dostępnych aktywów. Publiczny endpoint — nie wymaga logowania."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista aktywów pobrana pomyślnie", content = @Content(schema = @Schema(implementation = Asset.class))),
            @ApiResponse(responseCode = "500", description = "Wewnętrzny błąd serwera", content = @Content)
    })
    @GetMapping
    public ResponseEntity<?> getAssets() {
        try {
            List<Asset> assets = assetsRepository.findAll();
            return ResponseEntity.ok(assets);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "Wystąpił błąd podczas pobierania aktywów");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @Operation(
            summary = "Dodaj nowe aktywo",
            description = "Tworzy nowe aktywo. Wymaga roli ADMIN."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Aktywo utworzone pomyślnie", content = @Content(schema = @Schema(implementation = Asset.class))),
            @ApiResponse(responseCode = "400", description = "Nieprawidłowe żądanie - brak wymaganych pól", content = @Content),
            @ApiResponse(responseCode = "500", description = "Wewnętrzny błąd serwera", content = @Content)
    })
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<?> addAsset(@RequestBody Asset asset) {
        if (asset.getName() == null || asset.getSymbol() == null) {
            throw new BadRequestException("Nazwa i symbol aktywa są wymagane.");
        }

        try {
            Asset savedAsset = assetsRepository.save(asset);
            return new ResponseEntity<>(savedAsset, HttpStatus.CREATED);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "Wystąpił błąd podczas zapisywania aktywa");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @Operation(
            summary = "Usuń aktywo",
            description = "Usuwa aktywo na podstawie ID. Wymaga roli ADMIN."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Aktywo usunięte pomyślnie"),
            @ApiResponse(responseCode = "404", description = "Aktywo nie zostało znalezione", content = @Content),
            @ApiResponse(responseCode = "500", description = "Wewnętrzny błąd serwera", content = @Content)
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<?> deleteAsset(@PathVariable Long id) {
        try {
            Optional<Asset> assetOpt = assetsRepository.findById(id);
            if (assetOpt.isEmpty()) {
                // Return 404 with error structure
                Map<String, Object> response = new HashMap<>();
                response.put("error", "Aktywo o ID " + id + " nie zostało znalezione.");
                response.put("status", 404);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            assetsRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "Wystąpił błąd podczas usuwania aktywa");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/{id}/history")
    public ResponseEntity<?> getAssetHistory(@PathVariable Long id) {
        Optional<Asset> assetOpt = assetsRepository.findById(id);
        if (assetOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Aktywo nie zostało znalezione", "status", 404));
        }
        Asset asset = assetOpt.get();
        // Ostatnie 30 punktów, posortowane rosnąco po dacie
        List<AssetPriceHistory> historyList = assetPriceHistoryRepository.findTop30ByAssetOrderByTimestampDesc(asset);
        historyList.sort((a, b) -> a.getTimestamp().compareTo(b.getTimestamp()));
        List<Map<String, Object>> history = new ArrayList<>();
        for (AssetPriceHistory h : historyList) {
            Map<String, Object> point = new HashMap<>();
            point.put("timestamp", h.getTimestamp().toString());
            point.put("price", h.getPrice());
            history.add(point);
        }
        return ResponseEntity.ok(history);
    }
}
