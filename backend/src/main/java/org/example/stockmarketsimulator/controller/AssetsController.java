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
import org.example.stockmarketsimulator.service.AssetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/assets")
public class AssetsController {

    private final AssetService assetService;

    @Autowired
    public AssetsController(AssetService assetService) {
        this.assetService = assetService;
    }

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
            var assets = assetService.getAssets(search, sortBy, sortDirection, page, size);
            if (page == null || size == null) {
                return ResponseEntity.ok(assets.get("content"));
            }
            return ResponseEntity.ok(assets);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                        "error", "Wystąpił błąd podczas pobierania aktywów",
                        "message", e.getMessage()
                    ));
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
        try {
            return new ResponseEntity<>(assetService.createAsset(asset), HttpStatus.CREATED);
        } catch (BadRequestException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                        "error", "Wystąpił błąd podczas zapisywania aktywa",
                        "message", e.getMessage()
                    ));
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
        assetService.deleteAsset(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/history")
    public ResponseEntity<?> getAssetHistory(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(assetService.getAssetHistory(id));
        } catch (BadRequestException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage(), "status", 404));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Wystąpił błąd", "message", e.getMessage()));
        }
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, Object> handleResourceNotFound(ResourceNotFoundException ex) {
        return Map.of(
            "error", ex.getMessage(),
            "status", 404
        );
    }

    @ExceptionHandler(BadRequestException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleBadRequest(BadRequestException ex) {
        return Map.of(
            "error", ex.getMessage(),
            "status", 400
        );
    }
}
