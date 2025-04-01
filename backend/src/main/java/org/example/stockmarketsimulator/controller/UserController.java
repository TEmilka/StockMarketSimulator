package org.example.stockmarketsimulator.controller;

import org.example.stockmarketsimulator.model.Asset;
import org.example.stockmarketsimulator.model.User;
import org.example.stockmarketsimulator.model.UserWallet;
import org.example.stockmarketsimulator.repository.AssetsRepository;
import org.example.stockmarketsimulator.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
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

    @GetMapping
    public List<User> getUsers() {
        return userRepository.findAll();
    }

    @PostMapping
    public ResponseEntity<?> addUser(@RequestBody User user) {
        if (user.getName() == null || user.getEmail() == null) {
            return ResponseEntity.badRequest().body("Imię i email są wymagane");
        }

        user.setWallet(new UserWallet(user));
        User savedUser = userRepository.save(user);

        return new ResponseEntity<>(savedUser, HttpStatus.CREATED);
    }

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

    @GetMapping("/{userId}/wallet/details")
    public ResponseEntity<?> getUserWalletDetails(@PathVariable Long userId) {
        try {
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isPresent()) {
                UserWallet wallet = userOpt.get().getWallet();
                if (wallet == null) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Portfel nie istnieje");
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
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Użytkownik nie znaleziony");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Błąd serwera: " + e.getMessage());
        }
    }

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
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Nie znaleziono użytkownika lub aktywa");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Błąd serwera: " + e.getMessage());
        }
    }



}
