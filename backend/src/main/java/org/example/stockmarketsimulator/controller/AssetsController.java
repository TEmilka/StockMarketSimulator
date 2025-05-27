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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/assets")
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
    public ResponseEntity<?> getAssets(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        try {
            List<Asset> assets = assetsRepository.findAll();

            // Filtrowanie po nazwie lub symbolu
            if (search != null && !search.isEmpty()) {
                String searchLower = search.toLowerCase();
                assets = assets.stream()
                        .filter(asset ->
                                asset.getSymbol().toLowerCase().contains(searchLower) ||
                                asset.getName().toLowerCase().contains(searchLower))
                        .collect(Collectors.toList());
            }

            // Sortowanie po cenie lub nazwie
            if (sortBy != null) {
                boolean asc = sortDirection == null || sortDirection.equalsIgnoreCase("asc");
                Comparator<Asset> comparator;
                if ("price".equalsIgnoreCase(sortBy)) {
                    comparator = Comparator.comparing(Asset::getPrice);
                } else if ("name".equalsIgnoreCase(sortBy)) {
                    comparator = Comparator.comparing(Asset::getName, String.CASE_INSENSITIVE_ORDER);
                } else {
                    comparator = Comparator.comparing(Asset::getId);
                }
                if (!asc) {
                    comparator = comparator.reversed();
                }
                assets = assets.stream().sorted(comparator).collect(Collectors.toList());
            }

            if (page == null || size == null) {
                // Old behavior: return plain array for tests
                return ResponseEntity.ok(assets);
            }

            // Paginacja
            int fromIndex = Math.min(page * size, assets.size());
            int toIndex = Math.min(fromIndex + size, assets.size());
            List<Asset> pagedAssets = assets.subList(fromIndex, toIndex);

            Map<String, Object> response = new HashMap<>();
            response.put("content", pagedAssets);
            response.put("page", page);
            response.put("size", size);
            response.put("totalElements", assets.size());
            response.put("totalPages", (int) Math.ceil((double) assets.size() / size));

            return ResponseEntity.ok(response);
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
