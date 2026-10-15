package com.example.fraud.repository;

import com.example.fraud.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    long countByTransactionTimeBetween(LocalDateTime start, LocalDateTime end);

    List<Transaction> findByUserId(String userId);
}