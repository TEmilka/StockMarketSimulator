package org.example.stockmarketsimulator.service;

import jakarta.annotation.PostConstruct;
import org.example.stockmarketsimulator.model.Asset;
import org.example.stockmarketsimulator.repository.AssetsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DataAssetsInitializer {

    private final AssetsRepository assetsRepository;

    @Autowired
    public DataAssetsInitializer(AssetsRepository assetsRepository) {
        this.assetsRepository = assetsRepository;
    }

    @PostConstruct
    public void initDefaultAssets() {
        if (assetsRepository.count() == 0) {
            
            Asset asset1 = new Asset();
            asset1.setName("Apple Inc.");
            asset1.setSymbol("AAPL");
            asset1.setPrice(0.0);

            Asset asset2 = new Asset();
            asset2.setName("Tesla, Inc.");
            asset2.setSymbol("TSLA");
            asset2.setPrice(0.0);

            Asset asset3 = new Asset();
            asset3.setName("S&P 500 (ETF)");
            asset3.setSymbol("SPY");
            asset3.setPrice(0.0);

            Asset asset4 = new Asset();
            asset4.setName("Microsoft Corporation");
            asset4.setSymbol("MSFT");
            asset4.setPrice(0.0);

            Asset asset5 = new Asset();
            asset5.setName("Amazon.com, Inc.");
            asset5.setSymbol("AMZN");
            asset5.setPrice(0.0);

            Asset asset6 = new Asset();
            asset6.setName("Bitcoin");
            asset6.setSymbol("BINANCE:BTCUSDT");
            asset6.setPrice(0.0);

            Asset asset7 = new Asset();
            asset7.setName("Ethereum");
            asset7.setSymbol("BINANCE:ETHUSDT");
            asset7.setPrice(0.0);

            Asset asset8 = new Asset();
            asset8.setName("Solana");
            asset8.setSymbol("BINANCE:SOLUSDT");
            asset8.setPrice(0.0);

            Asset asset9 = new Asset();
            asset9.setName("Dogecoin");
            asset9.setSymbol("BINANCE:DOGEUSDT");
            asset9.setPrice(0.0);

            Asset asset10 = new Asset();
            asset10.setName("Cosmos");
            asset10.setSymbol("BINANCE:ATOMUSDT");
            asset10.setPrice(0.0);

            assetsRepository.saveAll(List.of(asset1, asset2, asset3, asset4, asset5, asset6, asset7, asset8, asset9, asset10));
        }
    }
}
