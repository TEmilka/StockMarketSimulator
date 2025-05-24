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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

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
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        try {
            User user = userRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Użytkownik o ID " + id + " nie został znaleziony"));
            userRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage(), "status", 404));
        }
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
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage(), "status", 404));
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
        try {
            logger.debug("Received request to add asset. UserId: {}, Payload: {}", userId, payload);

            if (!payload.containsKey("assetId") || !payload.containsKey("amount")) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Missing required fields: assetId and amount"));
            }

            Long assetId = Long.valueOf(payload.get("assetId").toString());
            Double amount = Double.valueOf(payload.get("amount").toString());

            logger.debug("Looking for user with ID: {}", userId);
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Użytkownik o ID " + userId + " nie został znaleziony"));

            logger.debug("Looking for asset with ID: {}", assetId);
            Asset asset = assetsRepository.findById(assetId)
                    .orElseThrow(() -> new ResourceNotFoundException("Aktywo o ID " + assetId + " nie zostało znalezione"));

            logger.debug("Found asset: {}", asset.getSymbol());

            UserWallet wallet = user.getWallet();
            if (wallet == null) {
                logger.debug("Creating new wallet for user");
                wallet = new UserWallet(user);
                user.setWallet(wallet);
            }

            logger.debug("Adding {} units of asset {} to wallet", amount, asset.getSymbol());
            wallet.addAsset(assetId, amount);
            userRepository.save(user);

            logger.debug("Successfully added asset to wallet");
            return getUserWalletDetails(userId.toString());

        } catch (ResourceNotFoundException e) {
            String msg = e.getMessage();
            int userIdIdx = msg != null && msg.contains("Aktywo o ID") ? msg.indexOf("Aktywo o ID") : -1;
            if (msg != null && msg.startsWith("Użytkownik o ID")) {
                // user not found
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", msg, "status", 404));
            } else if (msg != null && msg.startsWith("Aktywo o ID")) {
                // asset not found
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", msg, "status", 404));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Nie znaleziono zasobu", "status", 404));
            }
        } catch (NumberFormatException e) {
            logger.error("Number format error: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid number format for assetId or amount"));
        } catch (Exception e) {
            logger.error("Error adding asset to wallet", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to add asset: " + e.getMessage()));
        }
    }

    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(@PathVariable Long id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String role = auth.getAuthorities().stream().findFirst().map(a -> a.getAuthority()).orElse("");
        String userIdFromToken = auth.getAuthorities().stream()
            .filter(a -> a.getAuthority().startsWith("ROLE_"))
            .findFirst().map(a -> a.getAuthority()).orElse("");
        // Pozwól adminowi na wszystko, userowi tylko na swoje dane
        User user = userRepository.findById(id)
                .orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Użytkownik nie został znaleziony", "status", 404));
        }
        if (!role.equals("ROLE_ADMIN") && !user.getUsername().equals(auth.getName())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Brak uprawnień do tego zasobu", "status", 403));
        }
        return ResponseEntity.ok(Map.of(
                "id", user.getId(),
                "username", user.getUsername(),
                "email", user.getEmail(),
                "accountBalance", user.getAccountBalance(),
                "profit", user.getProfit()
        ));
    }

    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @PostMapping("/{id}/add-funds")
    public ResponseEntity<?> addFunds(@PathVariable Long id, @RequestBody Map<String, Object> payload) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String role = auth.getAuthorities().stream().findFirst().map(a -> a.getAuthority()).orElse("");
        User user = userRepository.findById(id).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Użytkownik nie został znaleziony", "status", 404));
        }
        if (!role.equals("ROLE_ADMIN") && !user.getUsername().equals(auth.getName())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Brak uprawnień do tego zasobu", "status", 403));
        }
        try {
            double amount = Double.parseDouble(payload.get("amount").toString());
            if (amount <= 0) {
                return ResponseEntity.badRequest().body(Map.of("error", "Kwota musi być większa od zera"));
            }
            user.setAccountBalance(user.getAccountBalance() + amount);
            userRepository.save(user);
            return ResponseEntity.ok(Map.of(
                "accountBalance", user.getAccountBalance(),
                "profit", user.getProfit()
            ));
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Nieprawidłowa kwota"));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage(), "status", 404));
        }
    }
}
