package org.example.stockmarketsimulator.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.example.stockmarketsimulator.config.RabbitConfig;
import org.example.stockmarketsimulator.model.Asset;
import org.example.stockmarketsimulator.repository.AssetsRepository;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    public AssetPriceFetcher(AssetsRepository assetsRepository) {
        this.assetsRepository = assetsRepository;
    }

    public Map<String, Double> fetchPrices(List<Asset> assets) {
        Map<String, Double> prices = new HashMap<>();

        for (Asset asset : assets) {
            String symbol = asset.getSymbol(); // np. BINANCE:BTCUSDT, AAPL
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

    @Scheduled(fixedDelay = 30000)
    @PostConstruct
    public void updateAssetPrices() {
        try {
            List<Asset> assets = assetsRepository.findAll();
            Map<String, Double> prices = fetchPrices(assets);

            for (Asset asset : assets) {
                Double newPrice = prices.get(asset.getSymbol());
                if (newPrice != null) {
                    asset.setPrice(newPrice);
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
        } catch (Exception e) {
            logger.error("Błąd podczas aktualizacji cen aktywów: {}", e.getMessage());
        }
    }
}

