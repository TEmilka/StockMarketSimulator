package org.example.stockmarketsimulator.repository;

import org.example.stockmarketsimulator.model.Asset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AssetsRepository extends JpaRepository<Asset, Long> {
}
