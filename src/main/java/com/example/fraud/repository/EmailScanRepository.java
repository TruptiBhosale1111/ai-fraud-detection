package com.example.fraud.repository;

import com.example.fraud.model.EmailScan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmailScanRepository extends JpaRepository<EmailScan, Long> {
}