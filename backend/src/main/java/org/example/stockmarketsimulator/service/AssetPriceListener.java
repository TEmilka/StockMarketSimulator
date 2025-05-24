package org.example.stockmarketsimulator.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.stockmarketsimulator.config.RabbitConfig;
import org.example.stockmarketsimulator.model.Asset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class AssetPriceListener {

    private static final Logger logger = LoggerFactory.getLogger(AssetPriceListener.class);

    @RabbitListener(queues = RabbitConfig.ASSET_QUEUE)
    public void receivePriceUpdate(String message) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            Asset asset = objectMapper.readValue(message, Asset.class);

            logger.info("Odebrano aktualizację: {} - ${}", asset.getSymbol(), asset.getPrice());

            if ("TSLA".equals(asset.getSymbol()) && asset.getPrice() > 800) {
                logger.warn("ALERT: TSLA przekroczyła $800! Aktualna cena: ${}", asset.getPrice());
            }

            if ("BINANCE:BTCUSDT".equals(asset.getSymbol()) && asset.getPrice() < 50000) {
                logger.warn("ALERT: BTC spadł poniżej $50,000! Aktualna cena: ${}", asset.getPrice());
            }

        } catch (Exception e) {
            logger.error("Błąd podczas przetwarzania wiadomości: {}", message, e);
        }
    }
}
