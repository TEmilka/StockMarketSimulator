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
import org.example.stockmarketsimulator.model.Transactions;
import org.example.stockmarketsimulator.model.Transactions.TransactionType;
import org.example.stockmarketsimulator.repository.AssetsRepository;
import org.example.stockmarketsimulator.repository.UserRepository;
import org.example.stockmarketsimulator.repository.TransactionsRepository;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ResponseStatus;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@RequestMapping("/api/v1/users")
@SecurityRequirement(name = "bearerAuth") // Wymaga autoryzacji dla wszystkich endpointów
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AssetsRepository assetsRepository;

    @Autowired
    private TransactionsRepository transactionsRepository;

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
        Long userIdLong;
        try {
            userIdLong = Long.parseLong(userId);
        } catch (NumberFormatException e) {
            throw new BadRequestException("Nieprawidłowy format ID użytkownika");
        }
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
        if (!payload.containsKey("assetId") || !payload.containsKey("amount")) {
            throw new BadRequestException("Missing required fields: assetId and amount");
        }
        Long assetId;
        Double amount;
        try {
            assetId = Long.valueOf(payload.get("assetId").toString());
            amount = Double.valueOf(payload.get("amount").toString());
        } catch (NumberFormatException e) {
            throw new BadRequestException("Invalid number format for assetId or amount");
        }
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

        return getUserWalletDetails(userId.toString());
    }

    // Pomocnicza metoda do przeliczania profitu użytkownika
    private void recalculateProfit(User user) {
        UserWallet wallet = user.getWallet();
        if (wallet == null) {
            user.setProfit(0.0);
            userRepository.save(user);
            return;
        }
        double profit = 0.0;
        List<Transactions> userTransactions = transactionsRepository.findByUserOrderByTimestampDesc(user);
        // Dla każdego aktywa w portfelu
        for (Map.Entry<Long, Double> entry : wallet.getAssets().entrySet()) {
            Long assetId = entry.getKey();
            Double amount = entry.getValue();
            Asset asset = assetsRepository.findById(assetId).orElse(null);
            if (asset == null) continue;

            // Kwota wydana na zakup tej ilości aktywa (suma kwot kupna - suma kwot sprzedaży)
            double spent = userTransactions.stream()
                .filter(t -> t.getAsset().getId().equals(assetId) && t.getType() == TransactionType.BUY)
                .mapToDouble(t -> t.getAmount() * t.getPrice()).sum()
                -
                userTransactions.stream()
                .filter(t -> t.getAsset().getId().equals(assetId) && t.getType() == TransactionType.SELL)
                .mapToDouble(t -> t.getAmount() * t.getPrice()).sum();

            double currentValue = amount * asset.getPrice();
            profit += currentValue - spent;
        }
        user.setProfit(profit);
        userRepository.save(user);
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

    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @PostMapping("/{userId}/wallet/trade")
    public ResponseEntity<?> tradeAsset(
            @PathVariable Long userId,
            @RequestBody Map<String, Object> payload) {
        try {
            String type = payload.get("type").toString();
            Long assetId = Long.valueOf(payload.get("assetId").toString());
            Double amount = Double.valueOf(payload.get("amount").toString());

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Użytkownik o ID " + userId + " nie został znaleziony"));
            Asset asset = assetsRepository.findById(assetId)
                    .orElseThrow(() -> new ResourceNotFoundException("Aktywo o ID " + assetId + " nie zostało znalezione"));

            double price = asset.getPrice();
            UserWallet wallet = user.getWallet();
            if (wallet == null) {
                wallet = new UserWallet(user);
                user.setWallet(wallet);
            }

            if ("BUY".equalsIgnoreCase(type)) {
                double totalCost = price * amount;
                if (user.getAccountBalance() < totalCost) {
                    return ResponseEntity.badRequest().body(Map.of("error", "Brak środków na koncie"));
                }
                wallet.addAsset(assetId, amount);
                user.setAccountBalance(user.getAccountBalance() - totalCost);
                // Zapisz transakcję kupna
                Transactions transaction = new Transactions();
                transaction.setUser(user);
                transaction.setAsset(asset);
                transaction.setAmount(amount);
                transaction.setPrice(price);
                transaction.setType(Transactions.TransactionType.BUY);
                transaction.setTimestamp(java.time.LocalDateTime.now());
                transactionsRepository.save(transaction);
            } else if ("SELL".equalsIgnoreCase(type)) {
                Double owned = wallet.getAssets().getOrDefault(assetId, 0.0);
                if (owned < amount) {
                    return ResponseEntity.badRequest().body(Map.of("error", "Nie posiadasz wystarczającej ilości aktywa"));
                }
                wallet.removeAsset(assetId, amount);
                double totalGain = price * amount;
                user.setAccountBalance(user.getAccountBalance() + totalGain);
                // Zapisz transakcję sprzedaży
                Transactions transaction = new Transactions();
                transaction.setUser(user);
                transaction.setAsset(asset);
                transaction.setAmount(amount);
                transaction.setPrice(price);
                transaction.setType(Transactions.TransactionType.SELL);
                transaction.setTimestamp(java.time.LocalDateTime.now());
                transactionsRepository.save(transaction);
            } else {
                return ResponseEntity.badRequest().body(Map.of("error", "Nieprawidłowy typ transakcji"));
            }

            // Przelicz profit po transakcji
            recalculateProfit(user);

            userRepository.save(user);
            return getUserWalletDetails(userId.toString());
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage(), "status", 404));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Błąd podczas realizacji transakcji: " + e.getMessage()));
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{userId}/transactions")
    public ResponseEntity<?> getUserTransactions(@PathVariable Long userId) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Użytkownik nie został znaleziony"));

            List<Transactions> transactions = transactionsRepository.findByUserOrderByTimestampDesc(user);
            List<Map<String, Object>> formattedTransactions = new ArrayList<>();

            for (Transactions t : transactions) {
                Map<String, Object> transaction = new HashMap<>();
                transaction.put("id", t.getId());
                transaction.put("type", t.getType().toString());
                transaction.put("assetSymbol", t.getAsset().getSymbol());
                transaction.put("assetName", t.getAsset().getName());
                transaction.put("amount", t.getAmount());
                transaction.put("price", t.getPrice());
                transaction.put("totalValue", t.getAmount() * t.getPrice());
                transaction.put("timestamp", t.getTimestamp().toString());
                formattedTransactions.add(transaction);
            }

            return ResponseEntity.ok(formattedTransactions);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Wystąpił błąd podczas pobierania transakcji"));
        }
    }

    // --- GLOBAL EXCEPTION HANDLERS ---
    @ExceptionHandler(org.example.stockmarketsimulator.exception.ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, Object> handleNotFound(ResourceNotFoundException ex) {
        return Map.of("error", ex.getMessage(), "status", 404);
    }

    @ExceptionHandler(org.example.stockmarketsimulator.exception.BadRequestException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleBadRequest(BadRequestException ex) {
        return Map.of("error", ex.getMessage(), "status", 400);
    }
}
