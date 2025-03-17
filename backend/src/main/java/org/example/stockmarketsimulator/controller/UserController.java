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

import java.util.List;
import java.util.Map;
import java.util.Optional;

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
        try {
            User savedUser = userRepository.save(user);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedUser);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Błąd serwera: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        try {
            Optional<User> user = userRepository.findById(id);
            if (user.isPresent()) {
                userRepository.deleteById(id);
                return ResponseEntity.noContent().build();
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Użytkownik nie znaleziony");
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Błąd serwera: " + e.getMessage());
        }
    }
    @PostMapping("/{userId}/addAsset")
    public ResponseEntity<?> addAssetToUserWallet(@PathVariable Long userId, @RequestBody Map<String, Object> assetRequest) {
        try {
            if (!assetRequest.containsKey("assetId") || !assetRequest.containsKey("quantity")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Brak wymaganych pól (assetId, quantity)");
            }

            Long assetId = ((Number) assetRequest.get("assetId")).longValue();
            Double quantity = ((Number) assetRequest.get("quantity")).doubleValue();

            if (quantity <= 0) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Ilość aktywów musi być większa niż 0");
            }

            Optional<User> userOpt = userRepository.findById(userId);
            Optional<Asset> assetOpt = assetsRepository.findById(assetId);

            if (userOpt.isPresent() && assetOpt.isPresent()) {
                User user = userOpt.get();
                Asset asset = assetOpt.get();
                user.addAssetToWallet(asset, quantity);
                userRepository.save(user);
                return ResponseEntity.status(HttpStatus.CREATED).body("Aktywo dodane do portfela");
            }

            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Użytkownik lub aktywo nie znalezione");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Błąd serwera: " + e.getMessage());
        }
    }
    @GetMapping("/{userId}/wallet")
    public ResponseEntity<?> getUserWallet(@PathVariable Long userId) {
        try {
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isPresent()) {
                return ResponseEntity.ok(userOpt.get().getWallet());
            }
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Użytkownik nie znaleziony");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Błąd serwera: " + e.getMessage());
        }
    }
}
