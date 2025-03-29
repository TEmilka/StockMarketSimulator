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
    @MapKeyColumn(name = "asset_id")
    @Column(name = "amount")
    private Map<Long, Double> assets = new HashMap<>();

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

    public Map<Long, Double> getAssets() {
        return assets;
    }

    public void addAsset(Long asset, double amount) {
        assets.merge(asset, amount, Double::sum);
    }

    public void removeAsset(Long asset, double amount) {
        assets.computeIfPresent(asset, (k, v) -> (v - amount) > 0 ? v - amount : null);
    }
}
