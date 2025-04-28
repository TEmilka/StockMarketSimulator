package org.example.stockmarketsimulator.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.example.stockmarketsimulator.exception.BadRequestException;
import org.example.stockmarketsimulator.exception.ResourceNotFoundException;
import org.example.stockmarketsimulator.model.Asset;
import org.example.stockmarketsimulator.model.User;
import org.example.stockmarketsimulator.model.UserWallet;
import org.example.stockmarketsimulator.repository.AssetsRepository;
import org.example.stockmarketsimulator.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AssetsRepository assetsRepository;

    private PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();


    @Operation(summary = "Get all users", description = "Retrieve a list of all users.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of users retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
    })
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public List<User> getUsers() {
        return userRepository.findAll();
    }

    @Operation(summary = "Add a new user", description = "Create a new user with a wallet.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "User created successfully", content = @Content(schema = @Schema(implementation = User.class))),
            @ApiResponse(responseCode = "400", description = "Bad request, missing name or email", content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
    })
    @PostMapping
    public ResponseEntity<?> addUser(@RequestBody User user) {
        if (user.getUsername() == null || user.getEmail() == null || user.getPassword() == null) {
            throw new BadRequestException("Imię, email i hasło są wymagane");
        }

        // Szyfrowanie hasła
        String encryptedPassword = passwordEncoder.encode(user.getPassword());
        user.setPassword(encryptedPassword);

        user.setWallet(new UserWallet(user));
        User savedUser = userRepository.save(user);

        return new ResponseEntity<>(savedUser, HttpStatus.CREATED);
    }

    @Operation(summary = "Delete a user", description = "Delete an existing user by ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "User deleted successfully"),
            @ApiResponse(responseCode = "404", description = "User not found", content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Użytkownik o ID " + id + " nie został znaleziony"));

        userRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get user wallet details", description = "Retrieve the details of a user's wallet including assets and their amounts.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Wallet details retrieved successfully", content = @Content(schema = @Schema(implementation = UserWallet.class))),
            @ApiResponse(responseCode = "404", description = "User or wallet not found", content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
    })
    @GetMapping("/{userId}/wallet/details")
    public ResponseEntity<?> getUserWalletDetails(@PathVariable String userId) {
        try {
            Long userIdLong = Long.parseLong(userId);
            User user = userRepository.findById(userIdLong)
                    .orElseThrow(() -> new ResourceNotFoundException("Użytkownik nie znaleziony"));

            UserWallet wallet = user.getWallet();
            if (wallet == null) {
                return ResponseEntity.ok(Collections.emptyList());
            }

            List<Map<String, Object>> detailedAssets = new ArrayList<>();
            for (Map.Entry<Long, Double> entry : wallet.getAssets().entrySet()) {
                Asset asset = assetsRepository.findById(entry.getKey())
                        .orElseThrow(() -> new ResourceNotFoundException("Aktywo nie znalezione"));
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

    @Operation(summary = "Add asset to user wallet", description = "Add a specific amount of an asset to a user's wallet.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Asset added to wallet successfully", content = @Content),
            @ApiResponse(responseCode = "404", description = "User or asset not found", content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
    })
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
            Asset foundAsset = assetsRepository.findById(entry.getKey()).orElseThrow(() -> new ResourceNotFoundException("Aktywo o ID " + entry.getKey() + " nie zostało znalezione"));
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
