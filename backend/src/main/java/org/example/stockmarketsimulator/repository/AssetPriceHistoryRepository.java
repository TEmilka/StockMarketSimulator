package org.example.stockmarketsimulator.repository;

import org.example.stockmarketsimulator.model.Asset;
import org.example.stockmarketsimulator.model.AssetPriceHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AssetPriceHistoryRepository extends JpaRepository<AssetPriceHistory, Long> {
    List<AssetPriceHistory> findByAssetOrderByTimestampAsc(Asset asset);
    List<AssetPriceHistory> findTop30ByAssetOrderByTimestampDesc(Asset asset);
}
