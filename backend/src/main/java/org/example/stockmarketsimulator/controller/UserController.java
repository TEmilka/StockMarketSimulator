package org.example.stockmarketsimulator.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AssetsRepository assetsRepository;

    @Operation(summary = "Get all users", description = "Retrieve a list of all users.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of users retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
    })
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
        if (user.getName() == null || user.getEmail() == null) {
            return ResponseEntity.badRequest().header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .body("Imię i email są wymagane");
        }
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
        try {
            Optional<User> user = userRepository.findById(id);
            if (user.isPresent()) {
                userRepository.deleteById(id);
                return ResponseEntity.noContent().build();
            } else {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Operation(summary = "Get user wallet details", description = "Retrieve the details of a user's wallet including assets and their amounts.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Wallet details retrieved successfully", content = @Content(schema = @Schema(implementation = UserWallet.class))),
            @ApiResponse(responseCode = "404", description = "User or wallet not found", content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
    })
    @GetMapping("/{userId}/wallet/details")
    public ResponseEntity<?> getUserWalletDetails(@PathVariable Long userId) {
        try {
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isPresent()) {
                UserWallet wallet = userOpt.get().getWallet();
                if (wallet == null) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                            .body("Portfel nie istnieje");
                }

                Map<Long, Double> assetsMap = wallet.getAssets();
                List<Map<String, Object>> detailedAssets = new ArrayList<>();

                for (Map.Entry<Long, Double> entry : assetsMap.entrySet()) {
                    Long assetId = entry.getKey();
                    Double amount = entry.getValue();

                    Optional<Asset> assetOpt = assetsRepository.findById(assetId);
                    if (assetOpt.isPresent()) {
                        Asset asset = assetOpt.get();
                        Map<String, Object> assetDetails = new HashMap<>();
                        assetDetails.put("id", asset.getId());
                        assetDetails.put("symbol", asset.getSymbol());
                        assetDetails.put("name", asset.getName());
                        assetDetails.put("price", asset.getPrice());
                        assetDetails.put("amount", amount);

                        detailedAssets.add(assetDetails);
                    }
                }
                return ResponseEntity.ok(detailedAssets);
            }
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .body("Użytkownik nie znaleziony");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .body("Błąd serwera: " + e.getMessage());
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
        try {
            Long assetId = ((Number) payload.get("assetId")).longValue();
            Double amount = ((Number) payload.get("amount")).doubleValue();

            Optional<User> userOpt = userRepository.findById(userId);
            Optional<Asset> assetOpt = assetsRepository.findById(assetId);

            if (userOpt.isPresent() && assetOpt.isPresent()) {
                User user = userOpt.get();
                UserWallet wallet = user.getWallet();
                if (wallet == null) {
                    wallet = new UserWallet(user);
                    user.setWallet(wallet);
                }

                wallet.addAsset(assetId, amount);
                userRepository.save(user);

                List<Map<String, Object>> updatedAssets = new ArrayList<>();
                for (Map.Entry<Long, Double> entry : wallet.getAssets().entrySet()) {
                    Optional<Asset> asset = assetsRepository.findById(entry.getKey());
                    asset.ifPresent(value -> {
                        Map<String, Object> assetDetails = new HashMap<>();
                        assetDetails.put("id", value.getId());
                        assetDetails.put("symbol", value.getSymbol());
                        assetDetails.put("name", value.getName());
                        assetDetails.put("price", value.getPrice());
                        assetDetails.put("amount", entry.getValue());
                        updatedAssets.add(assetDetails);
                    });
                }
                return ResponseEntity.ok(updatedAssets);
            }
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .body("Nie znaleziono użytkownika lub aktywa");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .body("Błąd serwera: " + e.getMessage());
        }
    }
}
