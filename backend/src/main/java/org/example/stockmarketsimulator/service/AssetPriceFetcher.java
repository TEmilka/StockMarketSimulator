package org.example.stockmarketsimulator.service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.example.stockmarketsimulator.config.RabbitConfig;
import org.example.stockmarketsimulator.model.Asset;
import org.example.stockmarketsimulator.model.AssetPriceHistory;
import org.example.stockmarketsimulator.model.Transactions;
import org.example.stockmarketsimulator.model.User;
import org.example.stockmarketsimulator.model.UserWallet;
import org.example.stockmarketsimulator.repository.AssetPriceHistoryRepository;
import org.example.stockmarketsimulator.repository.AssetsRepository;
import org.example.stockmarketsimulator.repository.TransactionsRepository;
import org.example.stockmarketsimulator.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PostConstruct;

@Service
public class AssetPriceFetcher {

    private final RestTemplate restTemplate = new RestTemplate();
    private final AssetsRepository assetsRepository;
    private final String API_TOKEN = "d0ln61hr01qpni305vj0d0ln61hr01qpni305vjg";
    private final Logger logger = LoggerFactory.getLogger(AssetPriceFetcher.class);

    @Autowired
    private AmqpTemplate amqpTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AssetPriceHistoryRepository assetPriceHistoryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TransactionsRepository transactionsRepository;

    @Autowired
    public AssetPriceFetcher(AssetsRepository assetsRepository) {
        this.assetsRepository = assetsRepository;
    }

    public Map<String, Double> fetchPrices(List<Asset> assets) {
        Map<String, Double> prices = new HashMap<>();

        for (Asset asset : assets) {
            String symbol = asset.getSymbol(); // np. BINANCE:BTCUSDT, AAPL
            //coinmarketcup //coingeco
            String url = "https://finnhub.io/api/v1/quote?symbol=" + symbol + "&token=" + API_TOKEN;

            try {
                ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                        url, HttpMethod.GET, null,
                        new ParameterizedTypeReference<>() {}
                );

                Map<String, Object> quote = response.getBody();
                if (quote != null && quote.get("c") != null) {
                    double price = ((Number) quote.get("c")).doubleValue();
                    prices.put(symbol, price);
                }

            } catch (Exception e) {
                logger.warn("Błąd podczas pobierania ceny dla symbolu {}: {}", symbol, e.getMessage());
            }
        }

        return prices;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private void recalculateAllUsersProfit() {
        List<User> users = userRepository.findAll();
        for (User user : users) {
            double profit = 0.0;
            UserWallet wallet = user.getWallet();
            if (wallet != null) {
                List<Transactions> userTransactions = transactionsRepository.findByUserOrderByTimestampDesc(user);
                for (Map.Entry<Long, Double> entry : wallet.getAssets().entrySet()) {
                    Long assetId = entry.getKey();
                    Double amount = entry.getValue();
                    Asset asset = assetsRepository.findById(assetId).orElse(null);
                    if (asset == null) continue;

                    double spent = userTransactions.stream()
                        .filter(t -> t.getAsset().getId().equals(assetId) && t.getType() == Transactions.TransactionType.BUY)
                        .mapToDouble(t -> t.getAmount() * t.getPrice()).sum()
                        -
                        userTransactions.stream()
                        .filter(t -> t.getAsset().getId().equals(assetId) && t.getType() == Transactions.TransactionType.SELL)
                        .mapToDouble(t -> t.getAmount() * t.getPrice()).sum();

                    double currentValue = amount * asset.getPrice();
                    profit += currentValue - spent;
                }
            }
            user.setProfit(profit);
            userRepository.save(user);
        }
    }

    @Scheduled(fixedDelay = 30000)
    @PostConstruct
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateAssetPrices() {
        try {
            List<Asset> assets = assetsRepository.findAll();
            Map<String, Double> prices = fetchPrices(assets);

            for (Asset asset : assets) {
                Double newPrice = prices.get(asset.getSymbol());
                if (newPrice != null) {
                    asset.setPrice(newPrice);
                    // Dodaj wpis do historii
                    AssetPriceHistory history = new AssetPriceHistory(asset, newPrice, LocalDateTime.now());
                    assetPriceHistoryRepository.save(history);
                }
            }

            assetsRepository.saveAll(assets);
            logger.info("Ceny aktywów zostały zaktualizowane");
            // Wysyłanie wiadomości do RabbitMQ
            for (Asset asset : assets) {
                if (prices.containsKey(asset.getSymbol())) {
                    String message = objectMapper.writeValueAsString(asset);
                    amqpTemplate.convertAndSend(RabbitConfig.EXCHANGE, "asset.price.updated", message);
                    logger.debug("Wysłano wiadomość do kolejki: {}", message);
                }
            }

            // Przelicz profit dla wszystkich użytkowników po aktualizacji cen
            recalculateAllUsersProfit();

        } catch (Exception e) {
            logger.error("Błąd podczas aktualizacji cen aktywów: {}", e.getMessage());
        }
    }
}

