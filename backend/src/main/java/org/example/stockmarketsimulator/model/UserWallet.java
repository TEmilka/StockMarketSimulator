package org.example.stockmarketsimulator.model;

import jakarta.persistence.*;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "user_wallet")
public class UserWallet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ElementCollection
    @CollectionTable(name = "wallet_assets", joinColumns = @JoinColumn(name = "wallet_id"))
    @MapKeyJoinColumn(name = "asset_id")
    @Column(name = "amount")
    private Map<Asset, Double> assets = new HashMap<>();

    public UserWallet() {}

    public UserWallet(User user) {
        this.user = user;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Map<Asset, Double> getAssets() {
        return assets;
    }

    public void addAsset(Asset asset, double amount) {
        assets.merge(asset, amount, Double::sum);
    }

    public void removeAsset(Asset asset, double amount) {
        assets.computeIfPresent(asset, (k, v) -> (v - amount) > 0 ? v - amount : null);
    }
}
