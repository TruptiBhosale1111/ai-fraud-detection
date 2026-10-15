package com.example.fraud.repository;

import com.example.fraud.model.CallScan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CallScanRepository extends JpaRepository<CallScan, Long> {
}