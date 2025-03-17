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

    @ElementCollection(fetch = FetchType.EAGER)
    @MapKeyJoinColumn(name = "asset_id")
    @Column(name = "quantity")
    private Map<Asset, Double> userAssets = new HashMap<>();

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

    public Map<Asset, Double> getUserAssets() {
        return userAssets;
    }

    public void setUserAssets(Map<Asset, Double> userAssets) {
        this.userAssets = userAssets;
    }

    public void addAsset(Asset asset, double quantity) {
        this.userAssets.put(asset, this.userAssets.getOrDefault(asset, 0.0) + quantity);
    }
}
