package org.example.stockmarketsimulator.controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.example.stockmarketsimulator.exception.BadRequestException;
import org.example.stockmarketsimulator.exception.ResourceNotFoundException;
import org.example.stockmarketsimulator.model.Asset;
import org.example.stockmarketsimulator.model.User;
import org.example.stockmarketsimulator.model.UserWallet;
import org.example.stockmarketsimulator.repository.AssetsRepository;
import org.example.stockmarketsimulator.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@RequestMapping("/api/users")
@SecurityRequirement(name = "bearerAuth") // Wymaga autoryzacji dla wszystkich endpointów
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AssetsRepository assetsRepository;

    private PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Operation(
            summary = "Pobierz wszystkich użytkowników",
            description = "Zwraca listę wszystkich użytkowników. Wymaga roli ADMIN."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Pobrano listę użytkowników"),
            @ApiResponse(responseCode = "403", description = "Brak uprawnień", content = @Content),
            @ApiResponse(responseCode = "500", description = "Błąd serwera", content = @Content)
    })
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<?> getUsers() {
        try {
            List<User> users = userRepository.findAll();
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "Wystąpił błąd podczas pobierania użytkowników");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
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
        if (user.getUsername() == null || user.getEmail() == null || user.getPassword() == null) {
            throw new BadRequestException("Nazwa użytkownika, email i hasło są wymagane.");
        }

        String encryptedPassword = passwordEncoder.encode(user.getPassword());
        user.setPassword(encryptedPassword);

        user.setWallet(new UserWallet(user));
        User savedUser = userRepository.save(user);

        return new ResponseEntity<>(savedUser, HttpStatus.CREATED);
    }

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
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Użytkownik o ID " + id + " nie został znaleziony"));

        userRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

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
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @GetMapping("/{userId}/wallet/details")
    public ResponseEntity<?> getUserWalletDetails(@PathVariable String userId) {
        try {
            Long userIdLong = Long.parseLong(userId);
            User user = userRepository.findById(userIdLong)
                    .orElseThrow(() -> new ResourceNotFoundException("Użytkownik nie został znaleziony"));

            UserWallet wallet = user.getWallet();
            if (wallet == null) {
                return ResponseEntity.ok(Collections.emptyList());
            }

            List<Map<String, Object>> detailedAssets = new ArrayList<>();
            for (Map.Entry<Long, Double> entry : wallet.getAssets().entrySet()) {
                Asset asset = assetsRepository.findById(entry.getKey())
                        .orElseThrow(() -> new ResourceNotFoundException("Aktywo o ID " + entry.getKey() + " nie zostało znalezione"));
                Map<String, Object> assetDetails = new HashMap<>();
                assetDetails.put("id", asset.getId());
                assetDetails.put("symbol", asset.getSymbol());
                assetDetails.put("name", asset.getName());
                assetDetails.put("price", asset.getPrice());
                assetDetails.put("amount", entry.getValue());
                detailedAssets.add(assetDetails);
            }
            return ResponseEntity.ok(detailedAssets);
        } catch (NumberFormatException e) {
            throw new BadRequestException("Nieprawidłowy format ID użytkownika");
        }
    }

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
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @PostMapping("/{userId}/wallet/add")
    public ResponseEntity<?> addAssetToWallet(@PathVariable Long userId, @RequestBody Map<String, Object> payload) {
        Long assetId = ((Number) payload.get("assetId")).longValue();
        Double amount = ((Number) payload.get("amount")).doubleValue();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Użytkownik o ID " + userId + " nie został znaleziony"));

        Asset asset = assetsRepository.findById(assetId)
                .orElseThrow(() -> new ResourceNotFoundException("Aktywo o ID " + assetId + " nie zostało znalezione"));

        UserWallet wallet = user.getWallet();
        if (wallet == null) {
            wallet = new UserWallet(user);
            user.setWallet(wallet);
        }

        wallet.addAsset(assetId, amount);
        userRepository.save(user);

        List<Map<String, Object>> updatedAssets = new ArrayList<>();
        for (Map.Entry<Long, Double> entry : wallet.getAssets().entrySet()) {
            Asset foundAsset = assetsRepository.findById(entry.getKey())
                    .orElseThrow(() -> new ResourceNotFoundException("Aktywo o ID " + entry.getKey() + " nie zostało znalezione"));
            Map<String, Object> assetDetails = new HashMap<>();
            assetDetails.put("id", foundAsset.getId());
            assetDetails.put("symbol", foundAsset.getSymbol());
            assetDetails.put("name", foundAsset.getName());
            assetDetails.put("price", foundAsset.getPrice());
            assetDetails.put("amount", entry.getValue());
            updatedAssets.add(assetDetails);
        }

        return ResponseEntity.ok(updatedAssets);
    }
}
