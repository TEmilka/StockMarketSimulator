package org.example.stockmarketsimulator.controller;

import org.example.stockmarketsimulator.exception.BadRequestException;
import org.example.stockmarketsimulator.exception.ResourceNotFoundException;
import org.example.stockmarketsimulator.model.User;
import org.example.stockmarketsimulator.model.UserWallet;
import org.example.stockmarketsimulator.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/users")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Pobierz wszystkich użytkowników",
            description = "Zwraca listę wszystkich użytkowników. Wymaga roli ADMIN."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Pobrano listę użytkowników"),
            @ApiResponse(responseCode = "403", description = "Brak uprawnień", content = @Content),
            @ApiResponse(responseCode = "500", description = "Błąd serwera", content = @Content)
    })
    @GetMapping
    public ResponseEntity<?> getUsers() {
        try {
            return ResponseEntity.ok(userService.getAllUsers());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Wystąpił błąd podczas pobierania użytkowników",
                                "message", e.getMessage()));
        }
    }

    @Operation(
            summary = "Dodaj nowego użytkownika",
            description = "Tworzy nowego użytkownika wraz z portfelem."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Użytkownik utworzony pomyślnie", content = @Content(schema = @Schema(implementation = User.class))),
            @ApiResponse(responseCode = "400", description = "Błędne dane - wymagane imię, email i hasło", content = @Content),
            @ApiResponse(responseCode = "500", description = "Błąd serwera", content = @Content)
    })
    @PostMapping
    public ResponseEntity<?> addUser(@RequestBody User user) {
        return new ResponseEntity<>(userService.createUser(user), HttpStatus.CREATED);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Usuń użytkownika",
            description = "Usuwa użytkownika na podstawie ID. Wymaga roli ADMIN."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Użytkownik usunięty pomyślnie"),
            @ApiResponse(responseCode = "404", description = "Użytkownik nie znaleziony", content = @Content),
            @ApiResponse(responseCode = "403", description = "Brak uprawnień", content = @Content),
            @ApiResponse(responseCode = "500", description = "Błąd serwera", content = @Content)
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(
            summary = "Pobierz szczegóły portfela użytkownika",
            description = "Zwraca szczegóły aktywów i ich ilości w portfelu użytkownika. Wymaga roli USER lub ADMIN."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Pobrano szczegóły portfela", content = @Content(schema = @Schema(implementation = UserWallet.class))),
            @ApiResponse(responseCode = "404", description = "Użytkownik lub portfel nie znaleziony", content = @Content),
            @ApiResponse(responseCode = "403", description = "Brak uprawnień", content = @Content),
            @ApiResponse(responseCode = "500", description = "Błąd serwera", content = @Content)
    })
    @GetMapping("/{userId}/wallet/details")
    public ResponseEntity<?> getUserWalletDetails(@PathVariable String userId) {
        return ResponseEntity.ok(userService.getWalletDetails(Long.parseLong(userId)));
    }

    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(
            summary = "Dodaj aktywo do portfela użytkownika",
            description = "Dodaje określoną ilość aktywa do portfela użytkownika. Wymaga roli USER lub ADMIN."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Aktywo dodane pomyślnie", content = @Content),
            @ApiResponse(responseCode = "404", description = "Użytkownik lub aktywo nie znalezione", content = @Content),
            @ApiResponse(responseCode = "403", description = "Brak uprawnień", content = @Content),
            @ApiResponse(responseCode = "500", description = "Błąd serwera", content = @Content)
    })
    @PostMapping("/{userId}/wallet/add")
    public ResponseEntity<?> addAssetToWallet(@PathVariable Long userId, @RequestBody Map<String, Object> payload) {
        Long assetId = Long.valueOf(payload.get("assetId").toString());
        Double amount = Double.valueOf(payload.get("amount").toString());
        return ResponseEntity.ok(userService.addAssetToWallet(userId, assetId, amount));
    }

    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @PostMapping("/{id}/add-funds")
    public ResponseEntity<?> addFunds(@PathVariable Long id, @RequestBody Map<String, Object> payload) {
        double amount = Double.parseDouble(payload.get("amount").toString());
        return ResponseEntity.ok(userService.addFunds(id, amount));
    }

    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @PostMapping("/{userId}/wallet/trade")
    public ResponseEntity<?> tradeAsset(@PathVariable Long userId, @RequestBody Map<String, Object> payload) {
        String type = payload.get("type").toString();
        Long assetId = Long.valueOf(payload.get("assetId").toString());
        Double amount = Double.valueOf(payload.get("amount").toString());
        return ResponseEntity.ok(userService.tradeAsset(userId, type, assetId, amount));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{userId}/transactions")
    public ResponseEntity<?> getUserTransactions(@PathVariable Long userId) {
        return ResponseEntity.ok(userService.getUserTransactions(userId));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{userId}/aggregated")
    public ResponseEntity<?> getAggregatedUserData(@PathVariable Long userId) {
        return ResponseEntity.ok(userService.getAggregatedUserData(userId));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, Object> handleNotFound(ResourceNotFoundException ex) {
        return Map.of("error", ex.getMessage(), "status", 404);
    }

    @ExceptionHandler(BadRequestException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleBadRequest(BadRequestException ex) {
        return Map.of("error", ex.getMessage(), "status", 400);
    }
}
