package com.example.fraud.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "qr_scans")
public class QrScan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Lob
    @Column(name = "raw_data", columnDefinition = "TEXT")
    private String rawData;

    @Column(name = "payee_vpa")
    private String payeeVpa;

    @Column(name = "payee_name")
    private String payeeName;

    @Column(name = "amount")
    private String amount;

    @Column(name = "qr_type")
    private String qrType;

    @Column(name = "risk_score")
    private int riskScore;

    @Column(name = "verdict")
    private String verdict;

    @Column(name = "scanned_at")
    private LocalDateTime scannedAt;

    @Transient
    private List<String> reasons = new ArrayList<>();

    @Transient
    private String advice;

    public QrScan() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getRawData() { return rawData; }
    public void setRawData(String rawData) { this.rawData = rawData; }

    public String getPayeeVpa() { return payeeVpa; }
    public void setPayeeVpa(String payeeVpa) { this.payeeVpa = payeeVpa; }

    public String getPayeeName() { return payeeName; }
    public void setPayeeName(String payeeName) { this.payeeName = payeeName; }

    public String getAmount() { return amount; }
    public void setAmount(String amount) { this.amount = amount; }

    public String getQrType() { return qrType; }
    public void setQrType(String qrType) { this.qrType = qrType; }

    public int getRiskScore() { return riskScore; }
    public void setRiskScore(int riskScore) { this.riskScore = riskScore; }

    public String getVerdict() { return verdict; }
    public void setVerdict(String verdict) { this.verdict = verdict; }

    public LocalDateTime getScannedAt() { return scannedAt; }
    public void setScannedAt(LocalDateTime scannedAt) { this.scannedAt = scannedAt; }

    public List<String> getReasons() { return reasons; }
    public void setReasons(List<String> reasons) { this.reasons = reasons; }

    public String getAdvice() { return advice; }
    public void setAdvice(String advice) { this.advice = advice; }
}