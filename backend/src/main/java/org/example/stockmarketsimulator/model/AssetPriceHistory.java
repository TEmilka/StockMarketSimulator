package org.example.stockmarketsimulator.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "asset_price_history")
public class AssetPriceHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "asset_id", nullable = false)
    private Asset asset;

    @Column(nullable = false)
    private double price;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    public AssetPriceHistory() {}

    public AssetPriceHistory(Asset asset, double price, LocalDateTime timestamp) {
        this.asset = asset;
        this.price = price;
        this.timestamp = timestamp;
    }

    public Long getId() {
        return id;
    }

    public Asset getAsset() {
        return asset;
    }

    public void setAsset(Asset asset) {
        this.asset = asset;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
