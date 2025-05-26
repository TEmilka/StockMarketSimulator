package org.example.stockmarketsimulator.repository;

import org.example.stockmarketsimulator.model.Transactions;
import org.example.stockmarketsimulator.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransactionsRepository extends JpaRepository<Transactions, Long> {
    List<Transactions> findByUserOrderByTimestampDesc(User user);
}
