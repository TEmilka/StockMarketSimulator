package org.example.stockmarketsimulator.model;

import jakarta.persistence.*;

@Entity
@Table(name = "wallet_assets")
public class WalletAssets {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "wallet_id", nullable = false)
    private UserWallet userWallet;

    @ManyToOne
    @JoinColumn(name = "asset_id", nullable = false)
    private Asset asset;

    private double amount;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public UserWallet getUserWallet() {
        return userWallet;
    }

    public void setUserWallet(UserWallet userWallet) {
        this.userWallet = userWallet;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public Asset getAsset() {
        return asset;
    }

    public void setAsset(Asset asset) {
        this.asset = asset;
    }
}
