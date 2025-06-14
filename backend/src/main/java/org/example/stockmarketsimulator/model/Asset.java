package org.example.stockmarketsimulator.model;

import jakarta.persistence.*;

@Entity
@Table(name = "assets", indexes = {
    @Index(name = "idx_asset_symbol", columnList = "symbol")
})
public class Asset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String symbol;
    private String name;
    private double price;

    public Asset() {}

    public Asset(String symbol, double price, String name) {
        this.symbol = symbol;
        this.price = price;
        this.name = name;
    }
    public Asset(Long id,String symbol, double price, String name) {
        this.id = id;
        this.symbol = symbol;
        this.price = price;
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }
}
