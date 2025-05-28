package org.example.stockmarketsimulator.service;

import org.example.stockmarketsimulator.exception.BadRequestException;
import org.example.stockmarketsimulator.exception.ResourceNotFoundException;
import org.example.stockmarketsimulator.model.*;
import org.example.stockmarketsimulator.repository.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final AssetsRepository assetsRepository;
    private final TransactionsRepository transactionsRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,
                      AssetsRepository assetsRepository,
                      TransactionsRepository transactionsRepository,
                      PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.assetsRepository = assetsRepository;
        this.transactionsRepository = transactionsRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public User createUser(User user) {
        if (user.getUsername() == null || user.getEmail() == null || user.getPassword() == null) {
            throw new BadRequestException("Nazwa użytkownika, email i hasło są wymagane.");
        }

        String encryptedPassword = passwordEncoder.encode(user.getPassword());
        user.setPassword(encryptedPassword);
        user.setWallet(new UserWallet(user));
        
        return userRepository.save(user);
    }

    public void deleteUser(Long id) {
        User user = getUserById(id);
        userRepository.deleteById(id);
    }

    public List<Map<String, Object>> getWalletDetails(Long userId) {
        User user = getUserById(userId);
        UserWallet wallet = user.getWallet();
        if (wallet == null) {
            return Collections.emptyList();
        }

        List<Map<String, Object>> detailedAssets = new ArrayList<>();
        for (Map.Entry<Long, Double> entry : wallet.getAssets().entrySet()) {
            Asset asset = assetsRepository.findById(entry.getKey())
                    .orElseThrow(() -> new ResourceNotFoundException("Aktywo o ID " + entry.getKey() + " nie zostało znalezione"));
            
            detailedAssets.add(Map.of(
                "id", asset.getId(),
                "symbol", asset.getSymbol(),
                "name", asset.getName(),
                "price", asset.getPrice(),
                "amount", entry.getValue()
            ));
        }
        return detailedAssets;
    }

    @Transactional
    public List<Map<String, Object>> addAssetToWallet(Long userId, Long assetId, Double amount) {
        User user = getUserById(userId);
        Asset asset = assetsRepository.findById(assetId)
                .orElseThrow(() -> new ResourceNotFoundException("Aktywo nie zostało znalezione"));

        UserWallet wallet = user.getWallet();
        if (wallet == null) {
            wallet = new UserWallet(user);
            user.setWallet(wallet);
        }

        wallet.addAsset(assetId, amount);
        userRepository.save(user);

        return getWalletDetails(userId);
    }

    @Transactional
    public void recalculateProfit(User user) {
        UserWallet wallet = user.getWallet();
        if (wallet == null) {
            user.setProfit(0.0);
            userRepository.save(user);
            return;
        }

        double profit = 0.0;
        List<Transactions> userTransactions = transactionsRepository.findByUserOrderByTimestampDesc(user);
        
        for (Map.Entry<Long, Double> entry : wallet.getAssets().entrySet()) {
            Asset asset = assetsRepository.findById(entry.getKey()).orElse(null);
            if (asset == null) continue;

            Double amount = entry.getValue();
            double spent = calculateSpentAmount(userTransactions, entry.getKey());
            double currentValue = amount * asset.getPrice();
            profit += currentValue - spent;
        }
        
        user.setProfit(profit);
        userRepository.save(user);
    }

    private double calculateSpentAmount(List<Transactions> transactions, Long assetId) {
        return transactions.stream()
                .filter(t -> t.getAsset().getId().equals(assetId))
                .mapToDouble(t -> t.getType() == Transactions.TransactionType.BUY ? 
                        t.getAmount() * t.getPrice() : 
                        -t.getAmount() * t.getPrice())
                .sum();
    }

    @Transactional
    public Map<String, Object> addFunds(Long userId, double amount) {
        if (amount <= 0) {
            throw new BadRequestException("Kwota musi być większa od zera");
        }

        User user = getUserById(userId);
        user.setAccountBalance(user.getAccountBalance() + amount);
        userRepository.save(user);

        return Map.of(
            "accountBalance", user.getAccountBalance(),
            "profit", user.getProfit()
        );
    }

    @Transactional
    public List<Map<String, Object>> tradeAsset(Long userId, String type, Long assetId, Double amount) {
        User user = getUserById(userId);
        Asset asset = assetsRepository.findById(assetId)
                .orElseThrow(() -> new ResourceNotFoundException("Aktywo nie zostało znalezione"));

        UserWallet wallet = user.getWallet();
        if (wallet == null) {
            wallet = new UserWallet(user);
            user.setWallet(wallet);
        }

        double price = asset.getPrice();

        if ("BUY".equalsIgnoreCase(type)) {
            handleBuyTransaction(user, asset, amount, price, wallet);
        } else if ("SELL".equalsIgnoreCase(type)) {
            handleSellTransaction(user, asset, amount, price, wallet);
        } else {
            throw new BadRequestException("Nieprawidłowy typ transakcji");
        }

        recalculateProfit(user);
        userRepository.save(user);
        return getWalletDetails(userId);
    }

    private void handleBuyTransaction(User user, Asset asset, Double amount, double price, UserWallet wallet) {
        double totalCost = price * amount;
        if (user.getAccountBalance() < totalCost) {
            throw new BadRequestException("Brak środków na koncie");
        }

        wallet.addAsset(asset.getId(), amount);
        user.setAccountBalance(user.getAccountBalance() - totalCost);
        saveTransaction(user, asset, amount, price, Transactions.TransactionType.BUY);
    }

    private void handleSellTransaction(User user, Asset asset, Double amount, double price, UserWallet wallet) {
        Double owned = wallet.getAssets().getOrDefault(asset.getId(), 0.0);
        if (owned < amount) {
            throw new BadRequestException("Nie posiadasz wystarczającej ilości aktywa");
        }

        wallet.removeAsset(asset.getId(), amount);
        user.setAccountBalance(user.getAccountBalance() + (price * amount));
        saveTransaction(user, asset, amount, price, Transactions.TransactionType.SELL);
    }

    private void saveTransaction(User user, Asset asset, Double amount, double price, Transactions.TransactionType type) {
        Transactions transaction = new Transactions();
        transaction.setUser(user);
        transaction.setAsset(asset);
        transaction.setAmount(amount);
        transaction.setPrice(price);
        transaction.setType(type);
        transaction.setTimestamp(LocalDateTime.now());
        transactionsRepository.save(transaction);
    }

    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Użytkownik nie został znaleziony"));
    }

    public List<Map<String, Object>> getUserTransactions(Long userId) {
        User user = getUserById(userId);
        List<Transactions> transactions = transactionsRepository.findByUserOrderByTimestampDesc(user);
        
        List<Map<String, Object>> result = new ArrayList<>();
        for (Transactions t : transactions) {
            Map<String, Object> transactionMap = new HashMap<>();
            transactionMap.put("id", t.getId());
            transactionMap.put("type", t.getType().toString());
            transactionMap.put("assetSymbol", t.getAsset().getSymbol());
            transactionMap.put("assetName", t.getAsset().getName());
            transactionMap.put("amount", t.getAmount());
            transactionMap.put("price", t.getPrice());
            transactionMap.put("totalValue", t.getAmount() * t.getPrice());
            transactionMap.put("timestamp", t.getTimestamp().toString());
            result.add(transactionMap);
        }
        return result;
    }

    public Map<String, Object> getAggregatedUserData(Long userId) {
        User user = getUserById(userId);
        Map<String, Object> response = new HashMap<>();
        
        response.put("user", Map.of(
            "id", user.getId(),
            "username", user.getUsername(),
            "email", user.getEmail(),
            "accountBalance", user.getAccountBalance(),
            "profit", user.getProfit()
        ));
        
        response.put("transactions", transactionsRepository.findByUserOrderByTimestampDesc(user));
        
        if (user.getWallet() != null) {
            response.put("assets", getWalletDetails(userId));
        }
        
        return response;
    }
}
