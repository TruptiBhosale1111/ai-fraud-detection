package com.example.fraud.repository;

import com.example.fraud.model.OfferScan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OfferScanRepository extends JpaRepository<OfferScan, Long> {
}